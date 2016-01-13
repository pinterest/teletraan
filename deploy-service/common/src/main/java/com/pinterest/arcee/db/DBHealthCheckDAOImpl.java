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
package com.pinterest.arcee.db;

import com.pinterest.arcee.bean.HealthCheckBean;
import com.pinterest.arcee.bean.HealthCheckState;
import com.pinterest.arcee.bean.HealthCheckType;
import com.pinterest.arcee.dao.HealthCheckDAO;
import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.db.SingleResultSetHandlerFactory;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import java.util.List;

public class DBHealthCheckDAOImpl implements HealthCheckDAO {

    private static String INSERT_HEALTH_CHECK = "INSERT INTO health_checks SET %s";

    private static String UPDATE_HRALTH_CHECK_BY_ID = "UPDATE health_checks SET %s WHERE id=?";

    private static String DELETE_HRALTH_CHECK_BY_ID = "DELETE FROM health_checks WHERE id=?";

    private static String GET_HEALTH_CHECK_BY_ID = "SELECT * FROM health_checks WHERE id=?";

    private static String GET_HEALTH_CHECK_BY_GROUP = "SELECT * FROM health_checks WHERE group_name=? ORDER BY last_worked_on DESC LIMIT ?,?";

    private static String GET_RECENT_HEALTH_CHECK_STATUS = "SELECT status FROM health_checks WHERE group_name=? AND state=? ORDER BY last_worked_on DESC LIMIT ?";

    private static String GET_ONGOING_HEALTHCHECKS = "SELECT * FROM health_checks WHERE state!=?";

    private static String GET_ONGOING_HEALTHCHECKS_BY_GROUP = "SELECT * FROM health_checks WHERE group_name=? and type=? and state!=?";

    private static String GET_REGULAR_HEALTH_CHECK_BY_GROUP_AND_TIME = "SELECT * FROM health_checks WHERE group_name=? and type=? and last_worked_on>?";

    private BasicDataSource dataSource;

    public DBHealthCheckDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void insertHealthCheck(HealthCheckBean healthCheckBean) throws Exception {
        SetClause setClause = healthCheckBean.genSetClause();
        String clause = String.format(INSERT_HEALTH_CHECK, setClause.getClause());
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public void updateHealthCheckById(String id, HealthCheckBean healthCheckBean) throws Exception {
        SetClause setClause = healthCheckBean.genSetClause();
        String clause = String.format(UPDATE_HRALTH_CHECK_BY_ID, setClause.getClause());
        setClause.addValue(id);
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public void removeHealthCheckById(String id) throws Exception {
        new QueryRunner(dataSource).update(DELETE_HRALTH_CHECK_BY_ID, id);
    }

    @Override
    public HealthCheckBean getHealthCheckById(String id) throws Exception {
        ResultSetHandler<HealthCheckBean> h = new BeanHandler<HealthCheckBean>(HealthCheckBean.class);
        return new QueryRunner(dataSource).query(GET_HEALTH_CHECK_BY_ID, h, id);
    }

    @Override
    public List<HealthCheckBean> getHealthChecksByGroup(String groupName, int pageIndex, int pageSize) throws Exception {
        QueryRunner run = new QueryRunner(this.dataSource);
        long start = (pageIndex - 1) * pageSize;
        ResultSetHandler<List<HealthCheckBean>> h = new BeanListHandler<>(HealthCheckBean.class);
        return run.query(GET_HEALTH_CHECK_BY_GROUP, h, groupName, start, pageSize);
    }

    @Override
    public List<HealthCheckBean> getOngoingHealthChecks() throws Exception {
        ResultSetHandler<List<HealthCheckBean>> h = new BeanListHandler<HealthCheckBean>(HealthCheckBean.class);
        return new QueryRunner(dataSource).query(GET_ONGOING_HEALTHCHECKS, h, HealthCheckState.COMPLETED.toString());
    }

    @Override
    public List<HealthCheckBean> getOngoingRegularHealthChecksByGroup(String groupName) throws Exception {
        ResultSetHandler<List<HealthCheckBean>> h = new BeanListHandler<HealthCheckBean>(HealthCheckBean.class);
        return new QueryRunner(dataSource).query(GET_ONGOING_HEALTHCHECKS_BY_GROUP, h, groupName, HealthCheckType.TIME_TRIGGERED.toString(),
            HealthCheckState.COMPLETED.toString());
    }

    @Override
    public List<HealthCheckBean> getRegularHealthChecksByGroupAndTime(String groupName, long time) throws Exception {
        ResultSetHandler<List<HealthCheckBean>> h = new BeanListHandler<HealthCheckBean>(HealthCheckBean.class);
        return new QueryRunner(dataSource).query(GET_REGULAR_HEALTH_CHECK_BY_GROUP_AND_TIME, h, groupName,
            HealthCheckType.TIME_TRIGGERED.toString(), time);
    }

    @Override
    public List<String> getRecentHealthCheckStatus(String groupName, int pageSize) throws Exception {
        return new QueryRunner(dataSource).query(GET_RECENT_HEALTH_CHECK_STATUS,
                SingleResultSetHandlerFactory.<String>newListObjectHandler(), groupName, HealthCheckState.COMPLETED.toString(), pageSize);
    }
}
