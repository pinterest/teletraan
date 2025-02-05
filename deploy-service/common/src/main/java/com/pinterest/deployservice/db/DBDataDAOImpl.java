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

import com.pinterest.deployservice.bean.DataBean;
import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.dao.DataDAO;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;

public class DBDataDAOImpl implements DataDAO {
    private static final String INSERT_DATA_TEMPLATE = "INSERT INTO datas SET %s";
    private static final String UPDATE_DATA_TEMPLATE = "UPDATE datas SET %s WHERE data_id=?";
    private static final String INSERT_OR_UPDATE_DATA_TEMPLATE =
            "INSERT INTO datas SET %s ON DUPLICATE KEY UPDATE timestamp=?, data=?";
    private static final String DELETE_DATA = "DELETE FROM datas WHERE data_id=?";
    private static final String GET_DATA_BY_ID = "SELECT * FROM datas WHERE data_id=?";

    private BasicDataSource dataSource;

    public DBDataDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public DataBean getById(String id) throws Exception {
        ResultSetHandler<DataBean> h = new BeanHandler<DataBean>(DataBean.class);
        return new QueryRunner(dataSource).query(GET_DATA_BY_ID, h, id);
    }

    @Override
    public void delete(String id) throws Exception {
        new QueryRunner(dataSource).update(DELETE_DATA, id);
    }

    @Override
    public void insert(DataBean bean) throws Exception {
        SetClause setClause = bean.genSetClause();
        String clause = String.format(INSERT_DATA_TEMPLATE, setClause.getClause());
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public void update(String id, DataBean bean) throws Exception {
        SetClause setClause = bean.genSetClause();
        String clause = String.format(UPDATE_DATA_TEMPLATE, setClause.getClause());
        setClause.addValue(id);
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public void insertOrUpdate(String id, DataBean bean) throws Exception {
        SetClause setClause = bean.genSetClause();
        String clause = String.format(INSERT_OR_UPDATE_DATA_TEMPLATE, setClause.getClause());
        setClause.addValue(System.currentTimeMillis());
        setClause.addValue(bean.getData());
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }
}
