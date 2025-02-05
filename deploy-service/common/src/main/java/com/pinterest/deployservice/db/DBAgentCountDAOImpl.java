/**
 * Copyright (c) 2020-2024 Pinterest, Inc.
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

import com.pinterest.deployservice.bean.AgentCountBean;
import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.dao.AgentCountDAO;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;

/* count table cache to store # of actively deploying agents and # of existing agents */
public class DBAgentCountDAOImpl implements AgentCountDAO {
    private static final String GET_COUNT = "SELECT * FROM agent_counts WHERE env_id=?";
    private static final String DELETE_COUNT = "DELETE * FROM agent_counts WHERE env_id=?";
    private static final String INSERT_OR_UPDATE_COUNT =
            "INSERT INTO agent_counts SET %s ON DUPLICATE KEY UPDATE %s";

    private BasicDataSource dataSource;

    public DBAgentCountDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public AgentCountBean get(String envId) throws Exception {
        ResultSetHandler<AgentCountBean> h = new BeanHandler<AgentCountBean>(AgentCountBean.class);
        return new QueryRunner(dataSource).query(GET_COUNT, h, envId);
    }

    @Override
    public void delete(String envId) throws Exception {
        ResultSetHandler<AgentCountBean> h = new BeanHandler<AgentCountBean>(AgentCountBean.class);
        new QueryRunner(dataSource).query(DELETE_COUNT, h, envId);
    }

    @Override
    public void insertOrUpdate(AgentCountBean agentCountBean) throws Exception {
        SetClause setClause = agentCountBean.genSetClause();
        String clause =
                String.format(
                        INSERT_OR_UPDATE_COUNT,
                        setClause.getClause(),
                        AgentCountBean.UPDATE_CLAUSE);
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }
}
