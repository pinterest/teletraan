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


import com.pinterest.arcee.bean.HealthCheckErrorBean;
import com.pinterest.arcee.dao.HealthCheckErrorDAO;
import com.pinterest.deployservice.bean.SetClause;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;

public class DBHealthCheckErrorDAOImpl implements HealthCheckErrorDAO {

    private static String INSERT_HEALTH_CHECK_ERROR = "INSERT INTO healthcheck_errors SET %s";

    private static String DELETE_HRALTH_CHECK_ERROR_BY_ID = "DELETE FROM healthcheck_errors WHERE id=?";

    private static String GET_HEALTH_CHECK_ERROR_BY_ID = "SELECT * FROM healthcheck_errors WHERE id=?";

    private BasicDataSource dataSource;

    public DBHealthCheckErrorDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void insertHealthCheckError(HealthCheckErrorBean healthCheckErrorBean) throws Exception {
        SetClause setClause = healthCheckErrorBean.genSetClause();
        String clause = String.format(INSERT_HEALTH_CHECK_ERROR, setClause.getClause());
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public void removeHealthCheckErrorById(String id) throws Exception {
        new QueryRunner(dataSource).update(DELETE_HRALTH_CHECK_ERROR_BY_ID, id);
    }

    @Override
    public HealthCheckErrorBean getHealthCheckErrorById(String id) throws Exception {
        ResultSetHandler<HealthCheckErrorBean> h = new BeanHandler<HealthCheckErrorBean>(HealthCheckErrorBean.class);
        return new QueryRunner(dataSource).query(GET_HEALTH_CHECK_ERROR_BY_ID, h, id);
    }
}
