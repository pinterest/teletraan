/**
 * Copyright (c) 2016-2025 Pinterest, Inc.
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

import com.pinterest.deployservice.bean.PromoteBean;
import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.dao.PromoteDAO;
import java.util.List;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;

public class DBPromoteDAOImpl implements PromoteDAO {
    private static final String GET_BY_ID = "SELECT * FROM promotes WHERE env_id=?";
    private static final String GET_AUTO_PROMOTE_ENV_IDS =
            "SELECT DISTINCT env_id FROM promotes WHERE type!='MANUAL' "
                    + "AND pred_stage IS NOT NULL";
    private static final String INSERT_TEMPLATE = "INSERT INTO promotes SET %s";
    private static final String UPDATE_TEMPLATE = "UPDATE promotes SET %s WHERE env_id=?";
    private static final String DELETE = "DELETE FROM promotes WHERE env_id=?";

    private BasicDataSource dataSource;

    public DBPromoteDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public PromoteBean getById(String envId) throws Exception {
        ResultSetHandler<PromoteBean> h = new BeanHandler<PromoteBean>(PromoteBean.class);
        return new QueryRunner(dataSource).query(GET_BY_ID, h, envId);
    }

    @Override
    public List<String> getAutoPromoteEnvIds() throws Exception {
        return new QueryRunner(dataSource)
                .query(
                        GET_AUTO_PROMOTE_ENV_IDS,
                        SingleResultSetHandlerFactory.<String>newListObjectHandler());
    }

    @Override
    public void delete(String envId) throws Exception {
        new QueryRunner(dataSource).update(DELETE, envId);
    }

    @Override
    public void insert(PromoteBean promoteBean) throws Exception {
        SetClause setClause = promoteBean.genSetClause();
        String clause = String.format(INSERT_TEMPLATE, setClause.getClause());
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public void update(String envId, PromoteBean promoteBean) throws Exception {
        SetClause setClause = promoteBean.genSetClause();
        String clause = String.format(UPDATE_TEMPLATE, setClause.getClause());
        setClause.addValue(envId);
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }
}
