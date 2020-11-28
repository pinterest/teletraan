/**
 * Copyright 2020 Pinterest, Inc.
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

public class DBAgentCountDAOImpl implements AgentCountDAO {
    private static final String GET_COUNT =
        "SELECT * FROM agent_counts WHERE env_id=?";
    private static final String DELETE_COUNT =
        "DELETE * FROM agent_counts WHERE env_id=?";
    private static final String INSERT_COUNT =
        "INSERT INTO agent_counts SET %s";
    private static final String INCREMENT_EXISTING_COUNT =
        "UPDATE agent_counts SET existing_count = existing_count + 1 WHERE env_id=?";
    private static final String INCREMENT_ACTIVE_COUNT =
        "UPDATE agent_counts SET active_count = active_count + 1 WHERE env_id=?";
    private static final String DECREMENT_EXISTING_COUNT =
        "UPDATE agent_counts SET existing_count = existing_count - 1 WHERE env_id=? and existing_count > 0";
    private static final String DECREMENT_ACTIVE_COUNT =
        "UPDATE agent_counts SET active_count = active_count - 1 WHERE env_id=? and active_count > 0";

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
        return new QueryRunner(dataSource).query(DELETE_COUNT, h, envId);
    }

    @Override
    public void incrementExistingCountByOne(String envId) throws Exception {
        new QueryRunner(dataSource).update(INCR_EXISTING_COUNT, envId);
    }

    @Override
    public void decrementExistingCountByOne(String envId) throws Exception {
        new QueryRunner(dataSource).update(DECR_EXISTING_COUNT, envId);
    }

    @Override
    public void incrementActiveCountByOne(String envId) throws Exception {
        new QueryRunner(dataSource).update(INCR_ACTIVE_COUNT, envId);
    }

    @Override
    public void decrementActiveCountByOne(String envId) throws Exception {
        new QueryRunner(dataSource).update(DECR_ACTIVE_COUNT, envId);
    }

    @Override
    public void insert(AgentCountBean agentCountBean) throws Exception {
        SetClause setClause = agentCountBean.genSetClause();
        String clause = String.format(INSERT_COUNT, setClause.getClause());
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

}
