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
package com.pinterest.deployservice.db;

import com.pinterest.deployservice.bean.*;
import com.pinterest.deployservice.dao.AgentDAO;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

import java.util.*;

public class DBAgentDAOImpl implements AgentDAO {
    private static final String UPDATE_AGENT_TEMPLATE =
        "UPDATE agents SET %s WHERE host_id=? AND env_id=?";
    private static final String UPDATE_AGENTS_BY_HOSTIDS =
            "UPDATE agents SET %s WHERE host_id IN (?) AND env_id=?";
    private static final String UPDATE_AGENT_BY_ID_TEMPLATE =
        "UPDATE agents SET %s WHERE host_id=?";
    private static final String RESET_FAILED_AGENTS =
        "UPDATE agents SET state='RESET' " +
        "WHERE env_id=? AND deploy_id=? AND " +
        "status!='SUCCEEDED' AND status!='UNKNOWN' AND state!='RESET'";
    private static final String INSERT_OR_UPDATE_AGENT_TEMPLATE =
        "INSERT INTO agents SET %s ON DUPLICATE KEY UPDATE %s";
    private static final String DELETE_AGENT =
        "DELETE FROM agents WHERE host_id=? AND env_id=?";
    private static final String DELETE_AGENT_BY_ID =
        "DELETE FROM agents WHERE host_id=?";
    private static final String GET_AGENT_BY_HOST =
        "SELECT * FROM agents WHERE host_name=?";
    private static final String GET_AGENT_BY_HOSTID =
        "SELECT * FROM agents WHERE host_id=?";
    private static final String GET_ALL_AGENT_BY_ENV =
        "SELECT * FROM agents WHERE env_id=?";
    private static final String GET_AGENT_BY_ENV_AND_FIRST_DEPLOY_TIME =
        "SELECT * FROM agents WHERE env_id=? AND first_deploy_time>?";
    private static final String GET_BY_IDS =
        "SELECT * FROM agents WHERE host_id=? AND env_id=?";
    private static final String GET_DEPLOYING_TOTAL =
        "SELECT COUNT(*) FROM agents " +
            "WHERE env_id=? AND ((deploy_stage!=? AND state!='PAUSED_BY_USER' AND first_deploy=?) OR state = 'STOP' )";
    private static final String GET_DEPLOYING_TOTAL_WITH_HOST_TAG =
        "SELECT COUNT(*) FROM agents INNER JOIN host_tags ON host_tags.host_id = agents.host_id " +
            "WHERE agents.env_id=? AND host_tags.env_id=? AND ((agents.deploy_stage!=? AND agents.state!='PAUSED_BY_USER' AND agents.first_deploy=?) OR agents.state = 'STOP' ) " +
            "AND host_tags.tag_name = ? AND host_tags.tag_value = ?";

    private static final String GET_FAILED_FIRST_DEPLOY_TOTAL =
        "SELECT COUNT(*) FROM agents " +
                "WHERE env_id=? AND deploy_stage!=? AND status !='SUCCEEDED' AND status!= 'UNKNOWN' AND first_deploy=1";
    private static final String GET_SUCCEEDED_TOTAL =
        "SELECT COUNT(*) FROM agents " +
            "WHERE env_id=? AND deploy_id=? AND deploy_stage in (?,?) AND state!='PAUSED_BY_USER'";
    private static final String GET_STUCK_TOTAL =
        "SELECT COUNT(*) FROM agents " +
            "WHERE env_id=? AND deploy_id=? AND state='PAUSED_BY_SYSTEM'";
    private static final String GET_NON_FIRST_TIME_DEPLOY_TOTAL =
        "SELECT COUNT(*) FROM agents WHERE env_id=? AND first_deploy=0";
    private static final String COUNT_ALL_AGENT_BY_ENV = "SELECT COUNT(*) FROM agents WHERE env_id=?";
    private static final String COUNT_ALL_AGENT_BY_ENV_NAME = "SELECT COUNT(*) FROM agents WHERE env_name=?";
    private static final String COUNT_SERVING_TOTAL = "SELECT COUNT(*) FROM agents WHERE env_id=? AND deploy_stage=?";
    private static final String COUNT_SERVING_AND_NORMAL_TOTAL = "SELECT COUNT(*) FROM agents WHERE env_id=? AND deploy_stage=? AND state=?";
    private static final String COUNT_FINISHED_AGENTS_BY_DEPLOY =
        "SELECT COUNT(*) FROM agents WHERE deploy_id=? AND (deploy_stage='SERVING_BUILD' OR state='PAUSED_BY_USER' OR state='PAUSED_BY_SYSTEM')";
    private static final String COUNT_FINISHED_AGENTS_BY_DEPLOY_WITH_HOST_TAGS =
        "SELECT COUNT(*) FROM agents INNER JOIN host_tags ON host_tags.host_id = agents.host_id " +
            "WHERE agents.env_id=? AND host_tags.env_id=? AND agents.deploy_id=? AND (agents.deploy_stage='SERVING_BUILD' OR agents.state='PAUSED_BY_USER' OR agents.state='PAUSED_BY_SYSTEM') " +
        "AND host_tags.tag_name = ? AND host_tags.tag_value IN (?)";
    private static final String COUNT_AGENTS_BY_DEPLOY =
        "SELECT COUNT(*) FROM agents WHERE deploy_id=?";
    private static final String COUNT_ALL_DEPLOYED_HOSTS =
            "SELECT COUNT(DISTINCT(host_id)) FROM agents WHERE deploy_id IS NOT NULL";

   

    private BasicDataSource dataSource;

    public DBAgentDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void update(String hostId, String envId, AgentBean agentBean) throws Exception {
        SetClause setClause = agentBean.genSetClause();
        String clause = String.format(UPDATE_AGENT_TEMPLATE, setClause.getClause());
        setClause.addValue(hostId);
        setClause.addValue(envId);
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());

    }

    @Override
    public void updateMultiple(Collection<String> hostIds, String envId, AgentBean agentBean) throws Exception {
        SetClause setClause = agentBean.genSetClause();
        String clause = String.format(UPDATE_AGENTS_BY_HOSTIDS, setClause.getClause());
        new QueryRunner(dataSource).update(clause, setClause.getValueArray(), hostIds, envId);
    }

    @Override
    public void resetFailedAgents(String envId, String deployId) throws Exception {
        new QueryRunner(dataSource).update(RESET_FAILED_AGENTS, envId, deployId);
    }

    @Override
    public void updateAgentById(String hostId, AgentBean bean) throws Exception {
        SetClause setClause = bean.genSetClause();
        String clause = String.format(UPDATE_AGENT_BY_ID_TEMPLATE, setClause.getClause());
        setClause.addValue(hostId);
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public void insertOrUpdate(AgentBean updateBean) throws Exception {
        SetClause setClause = updateBean.genSetClause();
        String clause = String.format(INSERT_OR_UPDATE_AGENT_TEMPLATE, setClause.getClause(), AgentBean.UPDATE_CLAUSE);
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public void delete(String hostId, String envId) throws Exception {
        new QueryRunner(dataSource).update(DELETE_AGENT, hostId, envId);
    }

    @Override
    public void deleteAllById(String id) throws Exception {
        new QueryRunner(dataSource).update(DELETE_AGENT_BY_ID, id);
    }

    @Override
    public List<AgentBean> getByHost(String hostName) throws Exception {
        ResultSetHandler<List<AgentBean>> h = new BeanListHandler<>(AgentBean.class);
        return new QueryRunner(dataSource).query(GET_AGENT_BY_HOST, h, hostName);
    }

    @Override
    public List<AgentBean> getByHostId(String hostId) throws Exception {
        ResultSetHandler<List<AgentBean>> h = new BeanListHandler<>(AgentBean.class);
        return new QueryRunner(dataSource).query(GET_AGENT_BY_HOSTID, h, hostId);
    }

    @Override
    public List<AgentBean> getAllByEnv(String envId) throws Exception {
        ResultSetHandler<List<AgentBean>> h = new BeanListHandler<>(AgentBean.class);
        return new QueryRunner(dataSource).query(GET_ALL_AGENT_BY_ENV, h, envId);
    }

    @Override
    public List<AgentBean> getByEnvAndFirstDeployTime(String envId, long time) throws Exception {
        ResultSetHandler<List<AgentBean>> h = new BeanListHandler<>(AgentBean.class);
        return new QueryRunner(dataSource).query(GET_AGENT_BY_ENV_AND_FIRST_DEPLOY_TIME, h, envId, time);
    }

    @Override
    public AgentBean getByHostEnvIds(String hostId, String envId) throws Exception {
        ResultSetHandler<AgentBean> h = new BeanHandler<>(AgentBean.class);
        return new QueryRunner(dataSource).query(GET_BY_IDS, h, hostId, envId);
    }

    @Override
    public long countDeployingAgent(String envId) throws Exception {
        return countDeployingAgentInternal(envId, 0);
    }

    @Override
    public long countFirstDeployingAgent(String envId) throws Exception {
        return countDeployingAgentInternal(envId, 1);
    }

    @Override
    public long countFailedFirstDeployingAgent(String envId) throws Exception {
        Long n = new QueryRunner(dataSource).query(GET_FAILED_FIRST_DEPLOY_TOTAL,
                SingleResultSetHandlerFactory.<Long>newObjectHandler(), envId, DeployStage.SERVING_BUILD.toString());
        return n == null ? 0 : n;
    }

    private long countDeployingAgentInternal(String envId, int firstDeploy) throws Exception {
        Long n = new QueryRunner(dataSource).query(GET_DEPLOYING_TOTAL,
                SingleResultSetHandlerFactory.<Long>newObjectHandler(), envId, DeployStage.SERVING_BUILD.toString(), firstDeploy);
        return n == null ? 0 : n;
    }

    @Override
    public long countSucceededAgent(String envId, String deployId) throws Exception {
        Long n = new QueryRunner(dataSource).query(GET_SUCCEEDED_TOTAL,
            SingleResultSetHandlerFactory.<Long>newObjectHandler(), envId, deployId, DeployStage.SERVING_BUILD.toString(), DeployStage.STOPPED.toString());
        return n == null ? 0 : n;
    }

    @Override
    public long countStuckAgent(String envId, String deployId) throws Exception {
        Long n = new QueryRunner(dataSource).query(GET_STUCK_TOTAL,
            SingleResultSetHandlerFactory.<Long>newObjectHandler(), envId, deployId);
        return n == null ? 0 : n;
    }

    @Override
    public long countAgentByEnv(String envId) throws Exception {
        Long n = new QueryRunner(dataSource).query(COUNT_ALL_AGENT_BY_ENV,
            SingleResultSetHandlerFactory.<Long>newObjectHandler(), envId);
        return n == null ? 0 : n;
    }

    @Override
    public long countNonFirstDeployingAgent(String envId) throws Exception{
        Long n = new QueryRunner(dataSource).query(GET_NON_FIRST_TIME_DEPLOY_TOTAL,
                SingleResultSetHandlerFactory.<Long>newObjectHandler(), envId);
        return n == null ? 0 : n;
    }

    @Override
    public long countServingTotal(String envId) throws Exception {
        Long n = new QueryRunner(dataSource).query(COUNT_SERVING_TOTAL, SingleResultSetHandlerFactory.<Long>newObjectHandler(),
                envId, DeployStage.SERVING_BUILD.toString());
        return n == null ? 0 : n;
    }

    @Override
    public long countServingAndNormalTotal(String envId) throws Exception {
        Long n = new QueryRunner(dataSource).query(COUNT_SERVING_AND_NORMAL_TOTAL, SingleResultSetHandlerFactory.<Long>newObjectHandler(),
                envId, DeployStage.SERVING_BUILD.toString(), AgentState.NORMAL.toString());
        return n == null ? 0 : n;
    }

    @Override
    public long countFinishedAgentsByDeploy(String deployId) throws Exception {
        Long n = new QueryRunner(dataSource).query(COUNT_FINISHED_AGENTS_BY_DEPLOY,
            SingleResultSetHandlerFactory.<Long>newObjectHandler(), deployId);
        return n == null ? 0 : n;
    }

    @Override
    public long countAgentsByDeploy(String deployId) throws Exception {
        Long n = new QueryRunner(dataSource).query(COUNT_AGENTS_BY_DEPLOY,
            SingleResultSetHandlerFactory.<Long>newObjectHandler(), deployId);
        return n == null ? 0 : n;
    }

    @Override
    public long countDeployingAgentWithHostTag(String envId, String tagName, String tagValue) throws Exception {
        Long n = new QueryRunner(dataSource).query(GET_DEPLOYING_TOTAL_WITH_HOST_TAG,
            SingleResultSetHandlerFactory.<Long>newObjectHandler(), envId, envId, DeployStage.SERVING_BUILD.toString(), 0, tagName, tagValue);
        return n == null ? 0 : n;
    }

    @Override
    public long countFinishedAgentsByDeployWithHostTags(String envId, String deployId, String tagName, List<String> tagValues) throws Exception {
        Long n = new QueryRunner(dataSource).query(COUNT_FINISHED_AGENTS_BY_DEPLOY_WITH_HOST_TAGS,
            SingleResultSetHandlerFactory.<Long>newObjectHandler(), envId, envId, deployId, tagName, tagValues);
        return n == null ? 0 : n;
    }

    @Override
    public long countDeployedHosts() throws Exception {
        Long n = new QueryRunner(dataSource).query(COUNT_ALL_DEPLOYED_HOSTS,
                SingleResultSetHandlerFactory.<Long>newObjectHandler());
        return n == null ? 0 : n;
    }
}
