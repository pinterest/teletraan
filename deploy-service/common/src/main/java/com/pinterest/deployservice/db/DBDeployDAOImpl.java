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

import com.pinterest.deployservice.bean.DeployBean;
import com.pinterest.deployservice.bean.DeployQueryResultBean;
import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.bean.UpdateStatement;
import com.pinterest.deployservice.common.StateMachines;
import com.pinterest.deployservice.dao.DeployDAO;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.commons.lang.StringUtils;
import org.joda.time.Interval;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Arrays;

public class DBDeployDAOImpl implements DeployDAO {

    private static final String INSERT_DEPLOYMENT_TEMPLATE =
        "INSERT INTO deploys SET %s";
    private static final String UPDATE_DEPLOYMENT_TEMPLATE =
        "UPDATE deploys SET %s WHERE deploy_id=?";
    private static final String UPDATE_DEPLOY_SAFELY_TEMPLATE =
        "UPDATE deploys SET %s WHERE deploy_id=? AND state=?";
    private static final String GET_DEPLOYMENT_BY_ID =
        "SELECT * FROM deploys WHERE deploy_id=?";
    private static final String DELETE_DEPLOYMENT =
        "DELETE FROM deploys WHERE deploy_id=?";
    private static final String GET_ALL_DEPLOYMENTS_TEMPLATE =
        "SELECT * FROM deploys %s";
    private static final String GET_ALL_DEPLOYMENTS_WITH_COMMIT_TEMPLATE =
        "SELECT deploys.* FROM deploys " +
            "INNER JOIN builds ON deploys.build_id=builds.build_id " +
            "%s";
    private static final String GET_COUNT_FOR_ALL_DEPLOYMENTS_TEMPLATE =
        "SELECT COUNT(*) FROM deploys %s";
    private static final String GET_COUNT_FOR_ALL_DEPLOYMENTS_WITH_COMMIT_TEMPLATE =
        "SELECT COUNT(*) FROM deploys " +
            "INNER JOIN builds ON deploys.build_id=builds.build_id " +
            "%s";
    private static final String GET_ACCEPTED_DEPLOYS_TEMPLATE =
        "SELECT * FROM deploys WHERE env_id=? AND deploy_type IN (?) " +
            "AND acc_status='ACCEPTED' AND start_date>? AND start_date<? ORDER BY start_date DESC"
            + " LIMIT ?";
    private static final String GET_ACCEPTED_DEPLOYS_DELAYED_TEMPLATE =
        "SELECT * FROM deploys WHERE env_id=? AND deploy_type NOT IN ('ROLLBACK', 'STOP') " +
            "AND acc_status='ACCEPTED' AND start_date>? " +
            "AND state in ('SUCCEEDING', 'SUCCEEDED') AND suc_date<? " +
            "ORDER BY start_date DESC LIMIT 1";
    private static final String
        COUNT_OF_NONREGULAR_DEPLOYS =
        "SELECT COUNT(*) FROM deploys WHERE env_id=? AND deploy_type IN ('ROLLBACK', 'STOP') AND "
            + "start_date > ?";
    private static final String COUNT_TOTAL_BY_ENVID =
        "SELECT COUNT(*) FROM deploys WHERE env_id=?";
    private static final String DELETE_UNUSED_DEPLOYS =
        "DELETE FROM deploys WHERE env_id=? AND last_update<? " +
            "AND NOT EXISTS (SELECT 1 FROM environs WHERE environs.deploy_id = deploys.deploy_id) "
            + "ORDER BY last_update ASC LIMIT ?";
    private static final String COUNT_DAILY_DEPLOYS =
        "SELECT COUNT(*) FROM deploys WHERE start_date >= UNIX_TIMESTAMP(CURDATE())*1000";
    private static final String COUNT_ACTIVE_DEPLOYS =
        "SELECT COUNT(*) FROM deploys WHERE state='RUNNING'";

    private final BasicDataSource dataSource;

    public DBDeployDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public DeployBean getById(String deploymentId) throws Exception {
        ResultSetHandler<DeployBean> h = new BeanHandler<>(DeployBean.class);
        return new QueryRunner(dataSource).query(GET_DEPLOYMENT_BY_ID, h, deploymentId);
    }

    @Override
    public DeployQueryResultBean getAllDeploys(DeployQueryFilter filterBean) throws Exception {
        QueryRunner run = new QueryRunner(dataSource);
        ResultSetHandler<List<DeployBean>> h = new BeanListHandler<>(DeployBean.class);

        String queryStr, queryCountStr;
        if (StringUtils.isNotEmpty(filterBean.getFilter().getCommit())) {
            // TODO pretty hacky
            // It is very important to delete the commit from the filter, since we
            // want to return all deploys with commits later than this commit
            filterBean.getFilter().setCommit(null);
            filterBean.generateClauseAndValues();
            queryStr =
                String
                    .format(GET_ALL_DEPLOYMENTS_WITH_COMMIT_TEMPLATE, filterBean.getWhereClause());
            if (filterBean.getWhereClause().contains("LIMIT")) {
                String[] separateByLimit = filterBean.getWhereClause().split("LIMIT");
                queryCountStr =
                    String
                        .format(GET_COUNT_FOR_ALL_DEPLOYMENTS_WITH_COMMIT_TEMPLATE, separateByLimit[0]);
            } else {
                queryCountStr =
                    String
                        .format(GET_COUNT_FOR_ALL_DEPLOYMENTS_WITH_COMMIT_TEMPLATE, filterBean.getWhereClause());
            }
        } else {
            filterBean.generateClauseAndValues();
            queryStr = String.format(GET_ALL_DEPLOYMENTS_TEMPLATE, filterBean.getWhereClause());
            if (filterBean.getWhereClause().contains("LIMIT")) {
                String[] separateByLimit = filterBean.getWhereClause().split("LIMIT");
                queryCountStr =
                    String
                        .format(GET_COUNT_FOR_ALL_DEPLOYMENTS_TEMPLATE, separateByLimit[0]);
            } else {
                queryCountStr =
                    String
                        .format(GET_COUNT_FOR_ALL_DEPLOYMENTS_TEMPLATE, filterBean.getWhereClause());
            }
        }

        Connection connection = dataSource.getConnection();
        try {
            List<DeployBean>
                deployBeans =
                run.query(connection, queryStr, h, filterBean.getValueArray());
            int len = filterBean.getValueArray().length;
            Object[] newArr;
            if (filterBean.getWhereClause().contains("LIMIT")) {
                newArr = new Object[len - 2];
                newArr = Arrays.copyOf(filterBean.getValueArray(), len - 2);
            } else {
                newArr = new Object[len];
                newArr = Arrays.copyOf(filterBean.getValueArray(), len);
            }
            long
                total = run.query(connection, queryCountStr, SingleResultSetHandlerFactory.newObjectHandler(), newArr);
            long
                maxToReturn =
                filterBean.getFilter().getPageIndex() * filterBean.getFilter().getPageSize();
            return new DeployQueryResultBean(deployBeans, total, total > maxToReturn);
        } finally {
            DbUtils.closeQuietly(connection);
        }
    }

    @Override
    public void delete(String deployId) throws Exception {
        new QueryRunner(dataSource).update(DELETE_DEPLOYMENT, deployId);
    }

    @Override
    public void update(String deployId, DeployBean deployBean) throws Exception {
        SetClause setClause = deployBean.genSetClause();
        String clause = String.format(UPDATE_DEPLOYMENT_TEMPLATE, setClause.getClause());
        setClause.addValue(deployId);
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public UpdateStatement genUpdateStatement(String deployId, DeployBean deployBean) {
        SetClause setClause = deployBean.genSetClause();
        String clause = String.format(UPDATE_DEPLOYMENT_TEMPLATE, setClause.getClause());
        setClause.addValue(deployId);
        return new UpdateStatement(clause, setClause.getValueArray());
    }

    @Override
    public void insert(DeployBean deployBean) throws Exception {
        SetClause setClause = deployBean.genSetClause();
        String clause = String.format(INSERT_DEPLOYMENT_TEMPLATE, setClause.getClause());
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public UpdateStatement genInsertStatement(DeployBean deployBean) {
        SetClause setClause = deployBean.genSetClause();
        String clause = String.format(INSERT_DEPLOYMENT_TEMPLATE, setClause.getClause());
        return new UpdateStatement(clause, setClause.getValueArray());
    }

    @Override
    public List<DeployBean> getAcceptedDeploys(String envId, Interval interval, int size)
        throws Exception {
        ResultSetHandler<List<DeployBean>> h = new BeanListHandler<>(DeployBean.class);
        return new QueryRunner(dataSource).query(
                GET_ACCEPTED_DEPLOYS_TEMPLATE,
                h,
                envId,
                StateMachines.AUTO_PROMOTABLE_DEPLOY_TYPE,
                interval.getStartMillis(),
                interval.getEndMillis(),
                size);
    }


    @Override
    public List<DeployBean> getAcceptedDeploysDelayed(String envId, Interval interval)
        throws Exception {
        ResultSetHandler<List<DeployBean>> h = new BeanListHandler<>(DeployBean.class);
        return new QueryRunner(dataSource).query(GET_ACCEPTED_DEPLOYS_DELAYED_TEMPLATE, h,
                envId,
                interval.getStartMillis(),
                interval.getEndMillis());
    }

    @Override
    public Long countNonRegularDeploys(String envId, long after) throws Exception {
        Long count = new QueryRunner(dataSource).query(COUNT_OF_NONREGULAR_DEPLOYS,
            SingleResultSetHandlerFactory.<Long>newObjectHandler(), envId, after);
        return count;
    }

    @Override
    public int updateStateSafely(String deployId, String currentState, DeployBean updateBean)
        throws Exception {
        SetClause setClause = updateBean.genSetClause();
        String clause = String.format(UPDATE_DEPLOY_SAFELY_TEMPLATE, setClause.getClause());
        setClause.addValue(deployId);
        setClause.addValue(currentState);
        return new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public long countDeploysByEnvId(String envId) throws Exception {
        Long
            n =
            new QueryRunner(dataSource)
                .query(COUNT_TOTAL_BY_ENVID, SingleResultSetHandlerFactory.<Long>newObjectHandler(),
                    envId);
        return n == null ? 0 : n;
    }

    @Override
    public void deleteUnusedDeploys(String envId, long timeThreshold, long numOfDeploys)
        throws Exception {
        new QueryRunner(dataSource)
            .update(DELETE_UNUSED_DEPLOYS, envId, timeThreshold, numOfDeploys);
    }

    @Override
    public boolean isThereADeployWithBuildId(String buildId) throws Exception {
        return new QueryRunner(dataSource).query("SELECT EXISTS(SELECT * FROM deploys WHERE build_id =?)",
            new ScalarHandler<Integer>()) == 1;
    }

    @Override
    public long getDailyDeployCount() throws SQLException {
        Long n = new QueryRunner(dataSource)
            .query(COUNT_DAILY_DEPLOYS, SingleResultSetHandlerFactory.<Long>newObjectHandler());
        return n == null ? 0 : n;
    }

    @Override
    public long getRunningDeployCount() throws SQLException {
        Long n = new QueryRunner(dataSource)
            .query(COUNT_ACTIVE_DEPLOYS, SingleResultSetHandlerFactory.<Long>newObjectHandler());
        return n == null ? 0 : n;
    }
}
