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
package com.pinterest.deployservice.dao;

import com.pinterest.deployservice.bean.AgentBean;

import java.util.List;

/**
 * A collection of methods to help interact with table agents
 */
public interface AgentDAO {
    public void insertOrUpdate(AgentBean agentBean) throws Exception;

    public void update(String hostId, String envId, AgentBean updateBean) throws Exception;

    public void resetFailedAgents(String envId, String deployId) throws Exception;

    public void updateAgentById(String hostId, AgentBean bean) throws Exception;

    public void delete(String hostId, String envId) throws Exception;

    // TODO refector this
    public List<AgentBean> getByHost(String hostName) throws Exception;

    public List<AgentBean> getByHostId(String hostId) throws Exception;

    public List<AgentBean> getAllByEnv(String envId) throws Exception;

    public List<AgentBean> getByEnvAndFirstDeployTime(String envId, long time) throws Exception;

    public AgentBean getByHostEnvIds(String hostId, String envId) throws Exception;

    public void deleteAllById(String hostId) throws Exception;

    // return how many agents are deploying for this env, regardless of deployId
    public long countDeployingAgent(String envId) throws Exception;

    // return how many agents are doing first time deploy for this env.
    public long countFirstDeployingAgent(String envId) throws Exception;

    // return how many agents are failing during first time deploy for this env
    public long countFailedFirstDeployingAgent(String envId) throws Exception;

    // return how many agent already succeeded for certain deploy
    public long countSucceededAgent(String envId, String deployId) throws Exception;

    // return how many agent reports for this env regardless of deployId or stage
    public long countAgentByEnv(String envId) throws Exception;

    // return how many agent state is PAUSED_BY_SYSTEM, or stuck/failed
    public long countStuckAgent(String envId, String deployId) throws Exception;
}
