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
package com.pinterest.clusterservice.db;

import com.pinterest.clusterservice.bean.HostTypeBean;
import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.clusterservice.dao.HostTypeDAO;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;

public class DBHostTypeDAOImpl implements HostTypeDAO {
    private static String INSERT_HOST_TYPE = "INSERT INTO host_types SET %s";

    private static String GET_BY_ID = "SELECT * FROM host_types WHERE id=?";

    private static String GET_BY_PROVIDER = "SELECT * FROM host_types WHERE provider=?";

    private BasicDataSource dataSource;

    public DBHostTypeDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void insert(HostTypeBean bean) throws Exception {
        SetClause setClause = bean.genSetClause();
        String clause = String.format(INSERT_HOST_TYPE, setClause.getClause());
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public HostTypeBean getById(String id) throws Exception {
        ResultSetHandler<HostTypeBean> h = new BeanHandler<HostTypeBean>(HostTypeBean.class);
        return new QueryRunner(dataSource).query(GET_BY_ID, h, id);
    }

    @Override
    public HostTypeBean getByProvider(String provider) throws Exception {
        ResultSetHandler<HostTypeBean> h = new BeanHandler<HostTypeBean>(HostTypeBean.class);
        return new QueryRunner(dataSource).query(GET_BY_PROVIDER, h, provider);
    }
}
