/**
 * Copyright (c) 2016 Pinterest, Inc.
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

import com.pinterest.deployservice.bean.AgentErrorBean;
import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.dao.AgentErrorDAO;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;

public class DBAgentErrorDAOImpl implements AgentErrorDAO {
    private static final String INSERT_ERROR_TEMPLATE = "INSERT INTO agent_errors SET %s";
    private static final String UPDATE_ERROR_TEMPLATE =
            "UPDATE agent_errors SET %s WHERE host_name=? AND env_id=?";
    private static final String GET_ERROR =
            "SELECT * FROM agent_errors WHERE host_name=? AND env_id=?";
    private static final String GET_ERROR_BY_HOSTID_AND_ENVID =
            "SELECT * FROM agent_errors WHERE host_id=? AND env_id=?";

    private BasicDataSource dataSource;

    public DBAgentErrorDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public AgentErrorBean get(String hostName, String envId) throws Exception {
        ResultSetHandler<AgentErrorBean> h = new BeanHandler<AgentErrorBean>(AgentErrorBean.class);
        return new QueryRunner(dataSource).query(GET_ERROR, h, hostName, envId);
    }

    @Override
    public void insert(AgentErrorBean agentErrorBean) throws Exception {
        SetClause setClause = agentErrorBean.genSetClause();
        String clause = String.format(INSERT_ERROR_TEMPLATE, setClause.getClause());
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public void update(String hostName, String envId, AgentErrorBean agentErrorBean)
            throws Exception {
        SetClause setClause = agentErrorBean.genSetClause();
        String clause = String.format(UPDATE_ERROR_TEMPLATE, setClause.getClause());
        setClause.addValue(hostName);
        setClause.addValue(envId);
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public AgentErrorBean getByHostIdAndEnvId(String hostId, String envId) throws Exception {
        ResultSetHandler<AgentErrorBean> h = new BeanHandler<AgentErrorBean>(AgentErrorBean.class);
        return new QueryRunner(dataSource).query(GET_ERROR_BY_HOSTID_AND_ENVID, h, hostId, envId);
    }
}
