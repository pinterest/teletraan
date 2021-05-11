/*
 * Copyright 2020 Pinterest, Inc.
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

import com.pinterest.deployservice.bean.HostAgentBean;
import com.pinterest.deployservice.bean.HostState;
import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.dao.HostAgentDAO;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class DBHostAgentDAOImpl implements HostAgentDAO {
    private static final String INSERT_HOST_TEMPLATE = "INSERT INTO hosts_and_agents SET %s ON DUPLICATE KEY UPDATE %s";
    private static final String UPDATE_HOST_BY_ID = "UPDATE hosts_and_agents SET %s WHERE host_id=?";
    private static final String DELETE_HOST_BY_ID = "DELETE FROM hosts_and_agents WHERE host_id=?";
    private static final String GET_HOST_BY_NAME = "SELECT * FROM hosts_and_agents WHERE host_name=?";
    private static final String GET_HOST_BY_HOSTID = "SELECT * FROM hosts_and_agents WHERE host_id=?";
    private static final String GET_STALE_HOST = "SELECT DISTINCT hosts_and_agents.* FROM hosts_and_agents WHERE hosts_and_agents.last_update<?";
    private static final String GET_STALE_ENV_HOST = "SELECT DISTINCT hosts_and_agents.* FROM hosts_and_agents INNER JOIN hosts_and_envs ON hosts_and_agents.host_name=hosts_and_envs.host_name WHERE hosts_and_agents.last_update<?";
    private static final String GET_HOSTS_BY_AGENT = "SELECT * FROM hosts_statuses WHERE agent_version=? ORDER BY host_id LIMIT ?,?";

    private BasicDataSource dataSource;

    public DBHostAgentDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void insert(HostAgentBean hostAgentBean) throws Exception {
        SetClause setClause = hostAgentBean.genSetClause();
        String clause = String.format(INSERT_HOST_TEMPLATE, setClause.getClause(), HostAgentBean.UPDATE_CLAUSE);
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public void update(String id, HostAgentBean bean) throws Exception {
        SetClause setClause = bean.genSetClause();
        String clause = String.format(UPDATE_HOST_BY_ID, setClause.getClause());
        setClause.addValue(id);
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }
    @Override
    public void delete(String id) throws Exception {
        new QueryRunner(dataSource).update(DELETE_HOST_BY_ID, id);
    }

    @Override
    public HostAgentBean getHostByName(String hostName) throws Exception {
        ResultSetHandler<HostAgentBean> h = new BeanHandler<>(HostAgentBean.class);
        return new QueryRunner(dataSource).query(GET_HOST_BY_NAME, h, hostName);
    }

    @Override
    public HostAgentBean getHostById(String hostId) throws Exception {
        ResultSetHandler<HostAgentBean> h = new BeanHandler<>(HostAgentBean.class);
        return new QueryRunner(dataSource).query(GET_HOST_BY_HOSTID, h, hostId);
    }

    @Override
    public List<HostAgentBean> getStaleHosts(long after) throws Exception {
        ResultSetHandler<List<HostAgentBean>> h = new BeanListHandler<>(HostAgentBean.class);
        return new QueryRunner(dataSource).query(GET_STALE_HOST, h, after);
    }

    @Override
    public List<HostAgentBean> getStaleEnvHosts(long after) throws Exception {
        ResultSetHandler<List<HostAgentBean>> h = new BeanListHandler<>(HostAgentBean.class);
        return new QueryRunner(dataSource).query(GET_STALE_ENV_HOST, h, after);
    }

    @Override
    public List<HostAgentBean> getHostsByAgent(String agentVersion, long pageIndex, int pageSize) throws Exception {
        ResultSetHandler<List<HostAgentBean>> h = new BeanListHandler<>(HostAgentBean.class);
        return new QueryRunner(dataSource).query(GET_HOSTS_BY_AGENT, h, agentVersion, (pageIndex - 1) * pageSize, pageSize);
    }
}
