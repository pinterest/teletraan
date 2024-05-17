/**
 * Copyright 2024 Pinterest, Inc.
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

import com.pinterest.deployservice.bean.PindeployBean;
import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.dao.PindeployDAO;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;

/* count table cache to store # of actively deploying agents and # of existing agents */
public class DBPindeployDAOImpl implements PindeployDAO {
    private static final String GET_PINDEPLOY =
        "SELECT * FROM pindeploy WHERE env_id=?";
    private static final String DELETE_PINDEPLOY =
        "DELETE * FROM pindeploy WHERE pipeline=?";
    private static final String INSERT_OR_UPDATE_PINDEPLOY =
        "INSERT INTO pindeploy SET %s ON DUPLICATE KEY UPDATE pipeline=?, is_pindeploy=?";

    private BasicDataSource dataSource;

    public DBPindeployDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public PindeployBean get(String envId) throws Exception {
        ResultSetHandler<PindeployBean> h = new BeanHandler<PindeployBean>(PindeployBean.class);
        return new QueryRunner(dataSource).query(GET_PINDEPLOY, h, envId);
    }

    @Override
    public void delete(String pipeline) throws Exception {
        ResultSetHandler<PindeployBean> h = new BeanHandler<PindeployBean>(PindeployBean.class);
        new QueryRunner(dataSource).query(DELETE_PINDEPLOY, h, pipeline);
    }

    @Override
    public void insertOrUpdate(PindeployBean pindeployBean) throws Exception {
        SetClause setClause = pindeployBean.genSetClause();
        String clause = String.format(INSERT_OR_UPDATE_PINDEPLOY, setClause.getClause());
        setClause.addValue(pindeployBean.getPipeline());
        setClause.addValue(pindeployBean.getIs_pindeploy());
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }
}
