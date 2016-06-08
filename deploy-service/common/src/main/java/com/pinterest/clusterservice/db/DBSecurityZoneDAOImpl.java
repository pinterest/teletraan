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
package com.pinterest.clusterservice.db;

import com.pinterest.clusterservice.bean.SecurityZoneBean;
import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.clusterservice.dao.SecurityZoneDAO;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

import java.util.Collection;
import java.util.List;

public class DBSecurityZoneDAOImpl implements SecurityZoneDAO {
    private static String INSERT_NETWORK_ZONE = "INSERT INTO security_zones SET %s";

    private static String GET_BY_ID = "SELECT * FROM security_zones WHERE id=?";

    private static String GET_BY_PROVIDER_AND_ABSTRACTNAME = "SELECT * FROM security_zones WHERE provider=? AND abstract_name=?";

    private static String GET_ALL = "SELECT * FROM security_zones ORDER by abstract_name LIMIT ?,?";

    private static String GET_BY_PROVIDER = "SELECT * FROM security_zones WHERE provider=? ORDER by abstract_name";

    private BasicDataSource dataSource;

    public DBSecurityZoneDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void insert(SecurityZoneBean bean) throws Exception {
        SetClause setClause = bean.genSetClause();
        String clause = String.format(INSERT_NETWORK_ZONE, setClause.getClause());
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public SecurityZoneBean getById(String id) throws Exception {
        ResultSetHandler<SecurityZoneBean> h = new BeanHandler<SecurityZoneBean>(SecurityZoneBean.class);
        return new QueryRunner(dataSource).query(GET_BY_ID, h, id);
    }

    @Override
    public SecurityZoneBean getByProviderAndAbstractName(String provider, String abstractName) throws Exception {
        ResultSetHandler<SecurityZoneBean> h = new BeanHandler<SecurityZoneBean>(SecurityZoneBean.class);
        return new QueryRunner(dataSource).query(GET_BY_PROVIDER_AND_ABSTRACTNAME, h, provider, abstractName);
    }

    @Override
    public Collection<SecurityZoneBean> getAll(int pageIndex, int pageSize) throws Exception {
        QueryRunner run = new QueryRunner(this.dataSource);
        long start = (pageIndex - 1) * pageSize;
        ResultSetHandler<List<SecurityZoneBean>> h = new BeanListHandler<SecurityZoneBean>(SecurityZoneBean.class);
        return run.query(GET_ALL, h, start, pageSize);
    }

    @Override
    public Collection<SecurityZoneBean> getByProvider(String provider) throws Exception {
        ResultSetHandler<List<SecurityZoneBean>> h = new BeanListHandler<SecurityZoneBean>(SecurityZoneBean.class);
        return new QueryRunner(dataSource).query(GET_BY_PROVIDER, h, provider);
    }
}
