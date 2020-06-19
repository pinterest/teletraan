/*
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


import com.pinterest.deployservice.bean.HostAgentBean;
import com.pinterest.deployservice.dao.HostAgentDAO;
import com.pinterest.deployservice.bean.SetClause;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class DBHostAgentDAOImpl implements HostAgentDAO {
    private static final String INSERT_HOST_AGENT = "INSERT INTO hosts_and_agents SET %s ON DUPLICATE KEY UPDATE %s";
    private static final String DELETE_HOST_BY_NAME = "DELETE FROM hosts_and_agents WHERE host_name=?";
    private static final String GET_HOST_BY_NAME = "SELECT * FROM hosts_and_agents WHERE host_name=?";
    private static final String GET_HOSTS_BY_AGENT = "SELECT * FROM hosts_and_agents WHERE agent_version=? ORDER BY host_name LIMIT ?,?";

    private BasicDataSource dataSource;

    public DBHostAgentDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void insert(HostAgentBean hostAgentBean) throws Exception {
        SetClause setClause = hostAgentBean.genSetClause();
        String clause = String.format(INSERT_HOST_AGENT, setClause.getClause(), HostAgentBean.UPDATE_CLAUSE);
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public HostAgentBean get(String hostName) throws Exception {
        ResultSetHandler<HostAgentBean> h = new BeanHandler<>(HostAgentBean.class);
        return new QueryRunner(dataSource).query(GET_HOST_BY_NAME, h, hostName);
    }

    @Override
    public void delete(String hostName) throws Exception {
        new QueryRunner(dataSource).update(DELETE_HOST_BY_NAME, hostName);
    }

    @Override
    public List<HostAgentBean> getHostsByAgent(String agentVersion, long pageIndex, int pageSize) throws Exception {
        ResultSetHandler<List<HostAgentBean>> h = new BeanListHandler<>(HostAgentBean.class);
        return new QueryRunner(dataSource).query(GET_HOSTS_BY_AGENT, h, agentVersion, (pageIndex - 1) * pageSize, pageSize);
    }
}