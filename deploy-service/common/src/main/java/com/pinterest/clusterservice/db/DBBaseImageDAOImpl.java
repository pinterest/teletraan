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

import com.pinterest.clusterservice.bean.BaseImageBean;
import com.pinterest.clusterservice.dao.BaseImageDAO;
import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.db.SingleResultSetHandlerFactory;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

import java.util.Collection;
import java.util.List;

public class DBBaseImageDAOImpl implements BaseImageDAO {

    private static String INSERT_IMAGE = "INSERT INTO base_images SET %s";

    private static String GET_BY_ID = "SELECT * FROM base_images WHERE id=?";

    private static String GET_ALL = "SELECT * FROM base_images ORDER BY publish_date DESC LIMIT ?,?";

    private static String GET_ABSTRACT_NAME_BY_PROVIDER = "SELECT DISTINCT abstract_name FROM base_images WHERE provider=?";

    private static String GET_BY_ABSTRACTNAME = "SELECT * FROM base_images WHERE abstract_name=? ORDER BY publish_date DESC";

    private BasicDataSource dataSource;

    public DBBaseImageDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void insert(BaseImageBean bean) throws Exception {
        SetClause setClause = bean.genSetClause();
        String clause = String.format(INSERT_IMAGE, setClause.getClause());
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public BaseImageBean getById(String id) throws Exception {
        ResultSetHandler<BaseImageBean> h = new BeanHandler<BaseImageBean>(BaseImageBean.class);
        return new QueryRunner(dataSource).query(GET_BY_ID, h, id);
    }

    @Override
    public Collection<BaseImageBean> getAll(int pageIndex, int pageSize) throws Exception {
        long start = (pageIndex - 1) * pageSize;
        ResultSetHandler<List<BaseImageBean>> h = new BeanListHandler<BaseImageBean>(BaseImageBean.class);
        return new QueryRunner(this.dataSource).query(GET_ALL, h, start, pageSize);
    }

    @Override
    public Collection<String> getAbstractNamesByProvider(String provider) throws Exception {
        return new QueryRunner(dataSource).query(GET_ABSTRACT_NAME_BY_PROVIDER, SingleResultSetHandlerFactory.<String>newListObjectHandler(), provider);
    }

    @Override
    public Collection<BaseImageBean> getByAbstractName(String abstractName) throws Exception {
        ResultSetHandler<List<BaseImageBean>> h = new BeanListHandler<BaseImageBean>(BaseImageBean.class);
        return new QueryRunner(dataSource).query(GET_BY_ABSTRACTNAME, h, abstractName);
    }
}
