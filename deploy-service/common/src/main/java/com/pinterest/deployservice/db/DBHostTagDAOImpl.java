/*
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

import com.pinterest.deployservice.bean.HostTagBean;
import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.dao.HostTagDAO;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;


public class DBHostTagDAOImpl implements HostTagDAO {

    private static final String INSERT_HOST_TAG_TEMPLATE = "INSERT INTO host_tag SET %s ON DUPLICATE KEY UPDATE %s";
    private static final String DELETE_HOST_TAG_BY_HOST_ID = "DELETE FROM host_tag WHERE host_id = ?";
    private static final String GET_HOST_TAG = "SELECT * FROM host_tag WHERE host_id = ? AND tag_name = ? AND tag_value = ?";

    private BasicDataSource dataSource;

    public DBHostTagDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }


    @Override
    public void insertOrUpdate(HostTagBean hostTagBean) throws Exception {
        SetClause setClause = hostTagBean.genSetClause();
        String clause = String.format(INSERT_HOST_TAG_TEMPLATE, setClause.getClause(), HostTagBean.UPDATE_CLAUSE);
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }


    @Override
    public HostTagBean get(String hostId, String key, String value) throws Exception {
        ResultSetHandler<HostTagBean> h = new BeanHandler<HostTagBean>(HostTagBean.class);
        return new QueryRunner(dataSource).query(GET_HOST_TAG, h, hostId, key, value);
    }

    @Override
    public void deleteAllByHostId(String hostId) throws Exception {
        new QueryRunner(dataSource).update(DELETE_HOST_TAG_BY_HOST_ID, hostId);
    }

}
