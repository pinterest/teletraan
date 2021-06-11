/**
 * Copyright 2016 Pinterest, Inc.
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

import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.bean.UpdateStatement;
import com.pinterest.deployservice.dao.EnvironDAO;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.lang.StringUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Comparator;

public class DBEnvironDAOImpl implements EnvironDAO {
    private static final String INSERT_ENV_TEMPLATE =
        "INSERT INTO environs SET %s";
    private static final String UPDATE_ENV_BY_ID_TEMPLATE =
        "UPDATE environs SET %s WHERE env_id=?";
    private static final String UPDATE_ENV_BY_STAGE_TEMPLATE =
        "UPDATE environs SET %s WHERE env_name=? AND stage_name=?";
    private static final String UPDATE_ALL =
        "UPDATE environs SET %s";
    private static final String SET_EXTERNAL_ID =
        "UPDATE environs SET external_id=? WHERE env_name=? AND stage_name=?";
    private static final String GET_ENV_BY_ID =
        "SELECT * FROM environs WHERE env_id=?";
    private static final String GET_ENV_BY_NAME =
        "SELECT * FROM environs WHERE env_name=?";
    private static final String GET_ENV_BY_STAGE =
        "SELECT * FROM environs WHERE env_name=? AND stage_name=?";
    private static final String GET_ENV_BY_CLUSTER =
        "SELECT * FROM environs WHERE cluster_name=?";
    private static final String GET_ALL_ENV =
        "SELECT DISTINCT env_name FROM environs WHERE env_name LIKE ? ORDER BY env_name ASC LIMIT ?,?";
    private static final String GET_ALL_ENV2 =
        "SELECT DISTINCT env_name FROM environs ORDER BY env_name ASC LIMIT ?,?";
    private static final String DELETE_ENV =
        "DELETE FROM environs WHERE env_id=?";
    private static final String GET_ENVS_BY_HOST_TMPL =
        "SELECT e.* FROM environs e " +
            "INNER JOIN hosts_and_envs he ON he.env_id = e.env_id " +
            "WHERE he.host_name = '%s'";
    private static final String GET_ENVS_BY_GROUPS_TMPL =
            "SELECT e.* FROM environs e " +
            "INNER JOIN groups_and_envs ge ON ge.env_id = e.env_id " +
            "WHERE ge.group_name IN (%s)";
    private static final String COUNT_HOSTS_BY_CAPACITY =
        "SELECT COUNT(DISTINCT host_name) FROM (" +
            "SELECT h.host_name FROM hosts h INNER JOIN groups_and_envs ge ON ge.group_name = h.group_name WHERE ge.env_id=? " +
            "UNION DISTINCT " +
            "SELECT  hh.host_name FROM hosts hh INNER JOIN hosts_and_envs he WHERE hh.host_name=he.host_name AND he.env_id=?) x";
    private static final String GET_HOSTS_BY_CAPACITY =
        "SELECT DISTINCT host_name FROM (" +
            "SELECT h.host_name FROM hosts h INNER JOIN groups_and_envs ge ON ge.group_name = h.group_name WHERE ge.env_id=? " +
            "UNION DISTINCT " +
            "SELECT hh.host_name FROM hosts hh INNER JOIN hosts_and_envs he WHERE hh.host_name=he.host_name AND he.env_id=?) x";
    private static final String GET_OVERRIDE_HOSTS_BY_CAPACITY =
        "SELECT DISTINCT h.host_name FROM hosts h " +
            "INNER JOIN " +
            "(SELECT group_name FROM groups_and_envs WHERE env_id=?) gs " +
            "ON h.group_name=gs.group_name " +
            "INNER JOIN " +
            "(SELECT hes.host_name FROM hosts_and_envs hes " +
            "   INNER JOIN environs e " +
            "   ON hes.env_id = e.env_id " +
            "   WHERE e.env_name=? and e.stage_name!=?) hs " +
            "ON h.host_name=hs.host_name";
    private static final String GET_MISSING_HOSTS =
            "SELECT * FROM hosts_and_envs he WHERE he.env_id=? AND NOT EXISTS (SELECT 1 FROM hosts h WHERE h.host_name = he.host_name);";
    private static final String GET_CURRENT_DEPLOY_IDS =
        "SELECT deploy_id FROM environs WHERE env_state='NORMAL' AND deploy_id IS NOT NULL";
    private static final String GET_ALL_ENV_IDS =
        "SELECT env_id FROM environs";
    private static final String GET_ALL_ENVS =
        "SELECT * FROM environs";
    private static final String GET_ALL_SIDECAR_ENVS =
        "SELECT * FROM environs where system_priority > 0";
    private static final String DELETE_SCHEDULE =
        "UPDATE environs SET schedule_id=null where env_name=? AND stage_name=?";
    private static final String DELETE_CLUSTER =
        "UPDATE environs SET cluster_name=null where env_name=? AND stage_name=?";
    private static final String GET_ENV_BY_CONSTRAINT_ID = "SELECT * FROM environs WHERE deploy_constraint_id = ?";
    private static final String DELETE_DEPLOY_CONSTRAINT =
        "UPDATE environs SET deploy_constraint_id=null WHERE env_name=? AND stage_name=?";

    private BasicDataSource dataSource;

    public DBEnvironDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void insert(EnvironBean bean) throws Exception {
        SetClause setClause = bean.genSetClause();
        String clause = String.format(INSERT_ENV_TEMPLATE, setClause.getClause());
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public void update(String envId, EnvironBean bean) throws Exception {
        SetClause setClause = bean.genSetClause();
        String clause = String.format(UPDATE_ENV_BY_ID_TEMPLATE, setClause.getClause());
        setClause.addValue(envId);
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public void update(String envName, String envStage, EnvironBean bean) throws Exception {
        SetClause setClause = bean.genSetClause();
        String clause = String.format(UPDATE_ENV_BY_STAGE_TEMPLATE, setClause.getClause());
        setClause.addValue(envName);
        setClause.addValue(envStage);
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public void updateAll(EnvironBean bean) throws Exception {
        SetClause setClause = bean.genSetClause();
        String clause = String.format(UPDATE_ALL, setClause.getClause());
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public void setExternalId(EnvironBean bean, String externalId) throws Exception {
        ResultSetHandler<EnvironBean> h = new BeanHandler<EnvironBean>(EnvironBean.class);
        SetClause setClause = bean.genSetClause();
        String clause = String.format(SET_EXTERNAL_ID, setClause.getClause());
        String envName = bean.getEnv_name();
        String stageName = bean.getStage_name();
        new QueryRunner(dataSource).update(clause, externalId, envName, stageName);
    }

    @Override
    public UpdateStatement genUpdateStatement(String envId, EnvironBean bean) {
        SetClause setClause = bean.genSetClause();
        String clause = String.format(UPDATE_ENV_BY_ID_TEMPLATE, setClause.getClause());
        setClause.addValue(envId);
        return new UpdateStatement(clause, setClause.getValueArray());
    }

    @Override
    public void delete(String envId) throws Exception {
        new QueryRunner(dataSource).update(DELETE_ENV, envId);
    }

    @Override
    public List<String> getAllEnvNames(String nameFilter, long pageIndex, int pageSize) throws Exception {
        if (StringUtils.isNotEmpty(nameFilter)) {
            String filter = String.format("%%%s%%", nameFilter);
            return new QueryRunner(dataSource).query(GET_ALL_ENV,
                SingleResultSetHandlerFactory.<String>newListObjectHandler(), filter, (pageIndex - 1) * pageSize, pageSize);
        } else {
            return new QueryRunner(dataSource).query(GET_ALL_ENV2,
                SingleResultSetHandlerFactory.<String>newListObjectHandler(), (pageIndex - 1) * pageSize, pageSize);
        }
    }

    @Override
    public EnvironBean getById(String envId) throws Exception {
        ResultSetHandler<EnvironBean> h = new BeanHandler<EnvironBean>(EnvironBean.class);
        return new QueryRunner(dataSource).query(GET_ENV_BY_ID, h, envId);
    }

    @Override
    public List<EnvironBean> getByName(String envName) throws Exception {
        ResultSetHandler<List<EnvironBean>> h = new BeanListHandler<EnvironBean>(EnvironBean.class);
        return new QueryRunner(dataSource).query(GET_ENV_BY_NAME, h, envName);
    }

    @Override
    public EnvironBean getByStage(String envName, String envStage) throws Exception {
        ResultSetHandler<EnvironBean> h = new BeanHandler<EnvironBean>(EnvironBean.class);
        return new QueryRunner(dataSource).query(GET_ENV_BY_STAGE, h, envName, envStage);
    }

    @Override
    public EnvironBean getByCluster(String clusterName) throws Exception {
        ResultSetHandler<EnvironBean> h = new BeanHandler<EnvironBean>(EnvironBean.class);
        return new QueryRunner(dataSource).query(GET_ENV_BY_CLUSTER, h, clusterName);
    }

    @Override
    public List<String> getOverrideHosts(String envId, String envName, String envStage) throws Exception {
        return new QueryRunner(dataSource).query(GET_OVERRIDE_HOSTS_BY_CAPACITY,
            SingleResultSetHandlerFactory.<String>newListObjectHandler(), envId, envName, envStage);
    }

    @Override
    public long countTotalCapacity(String envId, String envName, String envStage) throws Exception {
        long total = new QueryRunner(dataSource).query(COUNT_HOSTS_BY_CAPACITY,
            SingleResultSetHandlerFactory.<Long>newObjectHandler(), envId, envId);
        List<String> overrideHosts = getOverrideHosts(envId, envName, envStage);
        return total - overrideHosts.size();
    }

    @Override
    public List<String> getTotalCapacityHosts(String envId, String envName, String envStage) throws Exception {
        List<String> totalHosts = new QueryRunner(dataSource).query(GET_HOSTS_BY_CAPACITY,
            SingleResultSetHandlerFactory.<String>newListObjectHandler(), envId, envId);
        List<String> overrideHosts = getOverrideHosts(envId, envName, envStage);
        if (!overrideHosts.isEmpty()) {
            totalHosts.removeAll(new HashSet<String>(overrideHosts));
        }
        return totalHosts;
    }

    @Override
    public Collection<String> getMissingHosts(String envId) throws Exception {
        return new QueryRunner(dataSource).query(GET_MISSING_HOSTS, SingleResultSetHandlerFactory.<String>newListObjectHandler(), envId);
    }

    @Override
    public List<EnvironBean> getEnvsByHost(String host) throws Exception {
        ResultSetHandler<List<EnvironBean>> h = new BeanListHandler<EnvironBean>(EnvironBean.class);
        List<EnvironBean> hostEnvs = new QueryRunner(dataSource).query(String.format(GET_ENVS_BY_HOST_TMPL, host), h);
        Set<EnvironBean> envSet = new TreeSet<EnvironBean>((EnvironBean e1, EnvironBean e2) ->e1.getEnv_id().compareTo(e2.getEnv_id()));
        envSet.addAll(hostEnvs);
        return new ArrayList<EnvironBean>(envSet);
    }

    @Override
    public List<EnvironBean> getEnvsByGroups(Collection<String> groups) throws Exception {
        ResultSetHandler<List<EnvironBean>> h = new BeanListHandler<>(EnvironBean.class);
        String groupStr = QueryUtils.genStringGroupClause(groups);
        List<EnvironBean> groupEnvs = new QueryRunner(dataSource).query(String.format(GET_ENVS_BY_GROUPS_TMPL, groupStr), h);
        Set<EnvironBean> envSet = new TreeSet<EnvironBean>((EnvironBean e1, EnvironBean e2) ->e1.getEnv_id().compareTo(e2.getEnv_id()));
        envSet.addAll(groupEnvs);
        return new ArrayList<EnvironBean>(envSet);
    }

    @Override
    public List<String> getCurrentDeployIds() throws Exception {
        return new QueryRunner(dataSource).query(GET_CURRENT_DEPLOY_IDS,
            SingleResultSetHandlerFactory.<String>newListObjectHandler());
    }

    @Override
    public List<String> getAllEnvIds() throws Exception {
        return new QueryRunner(dataSource).query(GET_ALL_ENV_IDS, SingleResultSetHandlerFactory.<String>newListObjectHandler());
    }

    @Override
    public List<EnvironBean> getAllEnvs() throws Exception {
        ResultSetHandler<List<EnvironBean>> h = new BeanListHandler<>(EnvironBean.class);
        return new QueryRunner(dataSource).query(GET_ALL_ENVS, h);
    }

    @Override
    public List<EnvironBean> getAllSidecarEnvs() throws Exception {
        ResultSetHandler<List<EnvironBean>> h = new BeanListHandler<>(EnvironBean.class);
        return new QueryRunner(dataSource).query(GET_ALL_SIDECAR_ENVS, h);
    }

    @Override
    public void deleteSchedule(String envName, String stageName) throws Exception {
        new QueryRunner(dataSource).update(DELETE_SCHEDULE, envName, stageName);
    }

    @Override
    public void deleteCluster(String envName, String stageName) throws Exception {
        new QueryRunner(dataSource).update(DELETE_CLUSTER, envName, stageName);
    }

    @Override
    public EnvironBean getEnvByDeployConstraintId(String constraintId) throws Exception {
        ResultSetHandler<EnvironBean> h = new BeanHandler<EnvironBean>(EnvironBean.class);
        return new QueryRunner(dataSource).query(GET_ENV_BY_CONSTRAINT_ID, h, constraintId);
    }

    @Override
    public void deleteConstraint(String envName, String stageName) throws Exception {
        new QueryRunner(dataSource).update(DELETE_DEPLOY_CONSTRAINT, envName, stageName);
    }
}
