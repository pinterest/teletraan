/**
 * Copyright (c) 2016-2024 Pinterest, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pinterest.deployservice.db;

import com.pinterest.deployservice.bean.AgentState;
import com.pinterest.deployservice.bean.AgentStatus;
import com.pinterest.deployservice.bean.DeployStage;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.bean.HostState;
import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.dao.HostDAO;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

public class DBHostDAOImpl implements HostDAO {
    private static final String DELETE_ALL_BY_ID =
            "DELETE hosts, agents, agent_errors FROM hosts LEFT JOIN agents ON hosts.host_id=agents.host_id "
                    + "LEFT JOIN agent_errors ON agents.host_name=agent_errors.host_name WHERE hosts.host_id=?";
    private static final String UPDATE_HOST_BY_ID = "UPDATE hosts SET %s WHERE host_id=?";
    private static final String INSERT_HOST_TEMPLATE =
            "INSERT INTO hosts SET %s ON DUPLICATE KEY UPDATE %s";
    private static final String INSERT_UPDATE_TEMPLATE =
            "INSERT INTO hosts %s VALUES %s ON DUPLICATE KEY UPDATE ip=?, last_update=?, account_id=?, "
                    + "state=IF(state!=? AND state!=? AND state!=?, VALUES(state), state), "
                    + "host_name=CASE WHEN host_name IS NULL THEN ? WHEN host_name=host_id THEN ? ELSE host_name END, "
                    + "ip=CASE WHEN ip IS NULL THEN ? ELSE ip END";
    private static final String DELETE_HOST_BY_ID = "DELETE FROM hosts WHERE host_id=?";
    private static final String REMOVE_HOST_FROM_GROUP =
            "DELETE FROM hosts WHERE host_id=? AND group_name=?";
    private static final String GET_HOSTS_BY_GROUP =
            "SELECT * FROM hosts WHERE group_name=? ORDER BY host_name LIMIT ?,?";
    private static final String GET_GROUP_SIZE =
            "SELECT COUNT(host_id) FROM hosts WHERE group_name=?";
    private static final String GET_ALL_HOSTS_BY_GROUP =
            "SELECT * FROM hosts WHERE group_name=? AND state!='TERMINATING'";
    private static final String GET_HOST_BY_NAME = "SELECT * FROM hosts WHERE host_name=?";
    private static final String GET_HOST_BY_HOSTID = "SELECT * FROM hosts WHERE host_id=?";
    private static final String GET_HOSTS_BY_STATES =
            "SELECT * FROM hosts WHERE state in (?, ?, ?) GROUP BY host_id ORDER BY last_update";
    private static final String GET_GROUP_NAMES_BY_HOST =
            "SELECT group_name FROM hosts WHERE host_name=?";
    private static final String GET_STALE_AGENTLESS_HOST_IDS =
            "SELECT DISTINCT hosts.host_id FROM hosts LEFT JOIN hosts_and_agents ON hosts.host_id = hosts_and_agents.host_id WHERE hosts.last_update < ? AND hosts_and_agents.host_id IS NULL ORDER BY hosts.last_update DESC LIMIT ?";
    private static final String GET_AGENTLESS_HOSTS =
            "SELECT hosts.* FROM hosts LEFT JOIN hosts_and_agents ON hosts.host_id = hosts_and_agents.host_id WHERE hosts.last_update > ? AND hosts_and_agents.host_id IS NULL ORDER BY hosts.last_update DESC LIMIT ?";
    private static final String GET_HOST_NAMES_BY_GROUP =
            "SELECT host_name FROM hosts WHERE group_name=?";
    private static final String GET_HOST_IDS_BY_GROUP =
            "SELECT DISTINCT host_id FROM hosts WHERE group_name=?";
    private static final String GET_HOSTS_BY_ENVID =
            "SELECT h.* FROM hosts h INNER JOIN groups_and_envs ge ON ge.group_name = h.group_name WHERE ge.env_id=? UNION DISTINCT SELECT hs.* FROM hosts hs INNER JOIN hosts_and_envs he ON he.host_name = hs.host_name WHERE he.env_id=?";
    private static final String GET_HOST_BY_ENVID_AND_HOSTID =
            "SELECT DISTINCT e.* FROM hosts e INNER JOIN groups_and_envs ge ON ge.group_name = e.group_name WHERE ge.env_id=? AND e.host_id=?";
    private static final String GET_HOST_BY_ENVID_AND_HOSTNAME1 =
            "SELECT hs.* FROM hosts hs INNER JOIN groups_and_envs ge ON ge.group_name = hs.group_name WHERE ge.env_id=? AND hs.host_name=?";
    private static final String GET_HOST_BY_ENVID_AND_HOSTNAME2 =
            "SELECT hs.* FROM hosts hs INNER JOIN hosts_and_envs he ON he.host_name = hs.host_name WHERE he.env_id=? AND he.host_name=?";
    private static final String GET_RETIRED_HOSTIDS_BY_GROUP =
            "SELECT DISTINCT host_id FROM hosts WHERE (can_retire=1 OR can_retire=3) AND group_name=? AND state not in (?,?,?) ORDER BY can_retire";
    private static final String GET_RETIRED_AND_FAILED_HOSTIDS_BY_GROUP =
            "SELECT DISTINCT h.host_id FROM hosts h INNER JOIN agents a ON a.host_id=h.host_id WHERE h.can_retire=1 AND h.group_name=? AND h.state not in (?,?,?) and a.status not in (?,?)";
    private static final String GET_FAILED_HOSTIDS_BY_GROUP =
            "SELECT DISTINCT h.host_id FROM hosts h INNER JOIN agents a ON a.host_id=h.host_id WHERE h.group_name=? AND h.state not in (?,?,?) and a.status not in (?,?)";
    private static final String GET_NEW_AND_SERVING_BUILD_HOSTIDS_BY_GROUP =
            "SELECT host_id FROM agents x WHERE x.state = ? AND x.deploy_stage = ? "
                    + "AND x.host_id IN (SELECT DISTINCT h.host_id AS host_id FROM hosts h INNER JOIN agents a ON a.host_id=h.host_id WHERE h.can_retire=0 AND h.group_name=? AND h.state not in (?,?,?)) "
                    + "GROUP BY x.host_id HAVING count(*) = (SELECT count(*) FROM agents y WHERE y.host_id = x.host_id) ORDER BY host_id";
    private static final String GET_NEW_HOSTIDS_BY_GROUP =
            "SELECT DISTINCT host_id FROM hosts WHERE can_retire=0 AND group_name=? AND state not in (?,?,?)";
    private static final String GET_TERMINATING_HOST_IDS_BY_GROUP =
            "SELECT DISTINCT host_id FROM hosts WHERE state in (?, ?, ?) AND group_name=?";
    private static final String GET_ACCOUNTID_BY_HOSTNAME =
            "SELECT DISTINCT account_id FROM hosts WHERE host_name=?";

    private BasicDataSource dataSource;

    public DBHostDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<String> getGroupNamesByHost(String hostName) throws Exception {
        return new QueryRunner(dataSource)
                .query(
                        GET_GROUP_NAMES_BY_HOST,
                        SingleResultSetHandlerFactory.<String>newListObjectHandler(),
                        hostName);
    }

    @Override
    public List<String> getHostNamesByGroup(String groupName) throws Exception {
        return new QueryRunner(dataSource)
                .query(
                        GET_HOST_NAMES_BY_GROUP,
                        SingleResultSetHandlerFactory.<String>newListObjectHandler(),
                        groupName);
    }

    @Override
    public Collection<String> getHostIdsByGroup(String groupName) throws Exception {
        return new QueryRunner(dataSource)
                .query(
                        GET_HOST_IDS_BY_GROUP,
                        SingleResultSetHandlerFactory.<String>newListObjectHandler(),
                        groupName);
    }

    @Override
    public Long getGroupSize(String groupName) throws Exception {
        Long n =
                new QueryRunner(dataSource)
                        .query(
                                GET_GROUP_SIZE,
                                SingleResultSetHandlerFactory.<Long>newObjectHandler(),
                                groupName);
        return n == null ? 0 : n;
    }

    @Override
    public void insert(HostBean hostBean) throws Exception {
        SetClause setClause = hostBean.genSetClause();
        String clause =
                String.format(INSERT_HOST_TEMPLATE, setClause.getClause(), HostBean.UPDATE_CLAUSE);
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public void insertOrUpdate(
            String hostName,
            String ip,
            String hostId,
            String state,
            Set<String> groupNames,
            String accountId)
            throws Exception {
        long now = System.currentTimeMillis();
        // TODO need to refactoring this to be more generic to all columns, e.g. use
        // genStringGroupClause() like the other DAOs
        // If state is PENDING_TERMINATE, PENDING_TERMINATE_NO_REPLACE or TERMINATING, do not
        // overwrite its state
        StringBuilder names =
                new StringBuilder("(host_id,group_name,create_date,last_update,state,account_id");
        if (hostName != null) {
            names.append(",host_name");
        }
        if (ip != null) {
            names.append(",ip");
        }
        names.append(")");

        StringBuilder sb = new StringBuilder();
        List<Object> params = new ArrayList<>();
        for (String groupName : groupNames) {

            sb.append("(?, ?, ?, ?, ?, ?");
            params.add(hostId);
            params.add(groupName);
            params.add(now);
            params.add(now);
            params.add(state);
            params.add(accountId);

            if (hostName != null) {
                sb.append(", ?");
                params.add(hostName);
            }

            if (ip != null) {
                sb.append(", ?");
                params.add(ip);
            }

            sb.append("),");
        }
        sb.setLength(sb.length() - 1);
        params.addAll(
                Arrays.asList(
                        ip,
                        now,
                        accountId,
                        HostState.PENDING_TERMINATE.toString(),
                        HostState.TERMINATING.toString(),
                        HostState.PENDING_TERMINATE_NO_REPLACE.toString(),
                        hostName,
                        hostName,
                        ip));

        new QueryRunner(dataSource)
                .update(String.format(INSERT_UPDATE_TEMPLATE, names, sb), params.toArray());
    }

    @Override
    public void deleteById(String id) throws Exception {
        new QueryRunner(dataSource).update(DELETE_HOST_BY_ID, id);
    }

    @Override
    public void deleteAllById(String id) throws Exception {
        new QueryRunner(dataSource).update(DELETE_ALL_BY_ID, id);
    }

    @Override
    public void removeHostFromGroup(String hostId, String groupName) throws Exception {
        new QueryRunner(dataSource).update(REMOVE_HOST_FROM_GROUP, hostId, groupName);
    }

    @Override
    public List<HostBean> getHostsByGroup(String groupName, long pageIndex, int pageSize)
            throws Exception {
        ResultSetHandler<List<HostBean>> h = new BeanListHandler<>(HostBean.class);
        return new QueryRunner(dataSource)
                .query(GET_HOSTS_BY_GROUP, h, groupName, (pageIndex - 1) * pageSize, pageSize);
    }

    @Override
    public List<HostBean> getHosts(String hostName) throws Exception {
        ResultSetHandler<List<HostBean>> h = new BeanListHandler<>(HostBean.class);
        return new QueryRunner(dataSource).query(GET_HOST_BY_NAME, h, hostName);
    }

    @Override
    public List<HostBean> getHostsByHostId(String hostId) throws Exception {
        ResultSetHandler<List<HostBean>> h = new BeanListHandler<>(HostBean.class);
        return new QueryRunner(dataSource).query(GET_HOST_BY_HOSTID, h, hostId);
    }

    @Override
    public List<HostBean> getTerminatingHosts() throws Exception {
        ResultSetHandler<List<HostBean>> h = new BeanListHandler<>(HostBean.class);
        return new QueryRunner(dataSource)
                .query(
                        GET_HOSTS_BY_STATES,
                        h,
                        HostState.PENDING_TERMINATE.toString(),
                        HostState.TERMINATING.toString(),
                        HostState.PENDING_TERMINATE_NO_REPLACE.toString());
    }

    @Override
    public List<String> getStaleAgentlessHostIds(long lastUpdateBefore, int limit)
            throws SQLException {
        return new QueryRunner(dataSource)
                .query(
                        GET_STALE_AGENTLESS_HOST_IDS,
                        SingleResultSetHandlerFactory.<String>newListObjectHandler(),
                        lastUpdateBefore,
                        limit);
    }

    @Override
    public List<HostBean> getAgentlessHosts(long lastUpdateAfter, int limit) throws SQLException {
        ResultSetHandler<List<HostBean>> h = new BeanListHandler<>(HostBean.class);
        return new QueryRunner(dataSource).query(GET_AGENTLESS_HOSTS, h, lastUpdateAfter, limit);
    }

    @Override
    public List<HostBean> getAllActiveHostsByGroup(String groupName) throws Exception {
        ResultSetHandler<List<HostBean>> h = new BeanListHandler<>(HostBean.class);
        return new QueryRunner(dataSource).query(GET_ALL_HOSTS_BY_GROUP, h, groupName);
    }

    @Override
    public void updateHostById(String id, HostBean bean) throws Exception {
        SetClause setClause = bean.genSetClause();
        String clause = String.format(UPDATE_HOST_BY_ID, setClause.getClause());
        setClause.addValue(id);
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public Collection<HostBean> getHostsByEnvId(String envId) throws Exception {
        ResultSetHandler<List<HostBean>> h = new BeanListHandler<>(HostBean.class);
        return new QueryRunner(dataSource).query(GET_HOSTS_BY_ENVID, h, envId, envId);
    }

    @Override
    public HostBean getByEnvIdAndHostId(String envId, String hostId) throws Exception {
        ResultSetHandler<HostBean> h = new BeanHandler<>(HostBean.class);
        return new QueryRunner(dataSource).query(GET_HOST_BY_ENVID_AND_HOSTID, h, envId, hostId);
    }

    @Override
    public Collection<HostBean> getByEnvIdAndHostName(String envId, String hostName)
            throws Exception {
        ResultSetHandler<List<HostBean>> h = new BeanListHandler<>(HostBean.class);
        Collection<HostBean> hostBeans =
                new QueryRunner(dataSource)
                        .query(GET_HOST_BY_ENVID_AND_HOSTNAME1, h, envId, hostName);
        if (hostBeans.isEmpty()) {
            return new QueryRunner(dataSource)
                    .query(GET_HOST_BY_ENVID_AND_HOSTNAME2, h, envId, hostName);
        }
        return hostBeans;
    }

    @Override
    public Collection<String> getToBeRetiredHostIdsByGroup(String groupName) throws Exception {
        return new QueryRunner(dataSource)
                .query(
                        GET_RETIRED_HOSTIDS_BY_GROUP,
                        SingleResultSetHandlerFactory.<String>newListObjectHandler(),
                        groupName,
                        HostState.PENDING_TERMINATE.toString(),
                        HostState.TERMINATING.toString(),
                        HostState.PENDING_TERMINATE_NO_REPLACE.toString());
    }

    @Override
    public Collection<String> getToBeRetiredAndFailedHostIdsByGroup(String groupName)
            throws Exception {
        return new QueryRunner(dataSource)
                .query(
                        GET_RETIRED_AND_FAILED_HOSTIDS_BY_GROUP,
                        SingleResultSetHandlerFactory.<String>newListObjectHandler(),
                        groupName,
                        HostState.PENDING_TERMINATE.toString(),
                        HostState.TERMINATING.toString(),
                        HostState.PENDING_TERMINATE_NO_REPLACE.toString(),
                        AgentStatus.UNKNOWN.toString(),
                        AgentStatus.SUCCEEDED.toString());
    }

    @Override
    public Collection<String> getNewAndServingBuildHostIdsByGroup(String groupName)
            throws Exception {
        return new QueryRunner(dataSource)
                .query(
                        GET_NEW_AND_SERVING_BUILD_HOSTIDS_BY_GROUP,
                        SingleResultSetHandlerFactory.<String>newListObjectHandler(),
                        AgentState.NORMAL.toString(),
                        DeployStage.SERVING_BUILD.toString(),
                        groupName,
                        HostState.PENDING_TERMINATE.toString(),
                        HostState.TERMINATING.toString(),
                        HostState.PENDING_TERMINATE_NO_REPLACE.toString());
    }

    @Override
    public Collection<String> getNewHostIdsByGroup(String groupName) throws Exception {
        return new QueryRunner(dataSource)
                .query(
                        GET_NEW_HOSTIDS_BY_GROUP,
                        SingleResultSetHandlerFactory.<String>newListObjectHandler(),
                        groupName,
                        HostState.PENDING_TERMINATE.toString(),
                        HostState.TERMINATING.toString(),
                        HostState.PENDING_TERMINATE_NO_REPLACE.toString());
    }

    @Override
    public Collection<String> getFailedHostIdsByGroup(String groupName) throws Exception {
        return new QueryRunner(dataSource)
                .query(
                        GET_FAILED_HOSTIDS_BY_GROUP,
                        SingleResultSetHandlerFactory.<String>newListObjectHandler(),
                        groupName,
                        HostState.PENDING_TERMINATE.toString(),
                        HostState.TERMINATING.toString(),
                        HostState.PENDING_TERMINATE_NO_REPLACE.toString(),
                        AgentStatus.UNKNOWN.toString(),
                        AgentStatus.SUCCEEDED.toString());
    }

    @Override
    public Collection<String> getTerminatingHostIdsByGroup(String groupName) throws Exception {
        return new QueryRunner(dataSource)
                .query(
                        GET_TERMINATING_HOST_IDS_BY_GROUP,
                        SingleResultSetHandlerFactory.<String>newListObjectHandler(),
                        HostState.PENDING_TERMINATE.toString(),
                        HostState.TERMINATING.toString(),
                        HostState.PENDING_TERMINATE_NO_REPLACE.toString(),
                        groupName);
    }

    @Override
    public String getAccountIdByHost(String hostName) throws Exception {
        return new QueryRunner(dataSource)
                .query(
                        GET_ACCOUNTID_BY_HOSTNAME,
                        SingleResultSetHandlerFactory.<String>newObjectHandler(),
                        hostName);
    }
}
