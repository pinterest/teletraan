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

import com.pinterest.clusterservice.bean.ClusterBean;
import com.pinterest.clusterservice.dao.ClusterDAO;
import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.db.SingleResultSetHandlerFactory;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;

public class DBClusterDAOImpl implements ClusterDAO {

    private static String INSERT_CLUSTER_CONFIG = "INSERT INTO clusters SET %s";

    private static String UPDATE_CLUSTER_CONFIG = "UPDATE clusters SET %s WHERE cluster_name=?";

    private static String DELETE_CLUSTER_CONFIG = "DELETE FROM clusters WHERE cluster_name=?";

    private static String GET_CLUSTER_CONFIG = "SELECT * FROM clusters WHERE cluster_name=?";

    private static final String GET_PROVIDER_BY_CLUSTERNAME = "SELECT provider FROM clusters WHERE cluster_name=?";

    private BasicDataSource dataSource;

    public DBClusterDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void insert(ClusterBean bean) throws Exception {
        SetClause setClause = bean.genSetClause();
        String clause = String.format(INSERT_CLUSTER_CONFIG, setClause.getClause());
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public void update(String clusterName, ClusterBean bean) throws Exception {
        SetClause setClause = bean.genSetClause();
        String clause = String.format(UPDATE_CLUSTER_CONFIG, setClause.getClause());
        setClause.addValue(clusterName);
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public void delete(String clusterName) throws Exception {
        new QueryRunner(dataSource).update(DELETE_CLUSTER_CONFIG, clusterName);
    }

    @Override
    public ClusterBean getByClusterName(String clusterName) throws Exception {
        ResultSetHandler<ClusterBean> h = new BeanHandler<ClusterBean>(ClusterBean.class);
        return new QueryRunner(dataSource).query(GET_CLUSTER_CONFIG, h, clusterName);
    }

    @Override
    public String getProviderByClusterName(String clusterName) throws Exception {
        return new QueryRunner(dataSource).query(GET_PROVIDER_BY_CLUSTERNAME, SingleResultSetHandlerFactory.<String>newObjectHandler(), clusterName);
    }
}
