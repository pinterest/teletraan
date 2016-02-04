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

import com.pinterest.clusterservice.bean.PlacementBean;
import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.clusterservice.dao.PlacementDAO;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;

public class DBPlacementDAOImpl implements PlacementDAO {
    private static String INSERT_PLACEMENT = "INSERT INTO placements SET %s";

    private static String GET_BY_ID = "SELECT * FROM placements WHERE id=?";

    private static String GET_BY_PROVIDER = "SELECT * FROM placements WHERE provider=?";

    private BasicDataSource dataSource;

    public DBPlacementDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void insert(PlacementBean bean) throws Exception {
        SetClause setClause = bean.genSetClause();
        String clause = String.format(INSERT_PLACEMENT, setClause.getClause());
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public PlacementBean getById(String id) throws Exception {
        ResultSetHandler<PlacementBean> h = new BeanHandler<PlacementBean>(PlacementBean.class);
        return new QueryRunner(dataSource).query(GET_BY_ID, h, id);
    }

    @Override
    public PlacementBean getByProvider(String provider) throws Exception {
        ResultSetHandler<PlacementBean> h = new BeanHandler<PlacementBean>(PlacementBean.class);
        return new QueryRunner(dataSource).query(GET_BY_PROVIDER, h, provider);
    }
}
