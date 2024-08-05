/**
 * Copyright (c) 2016-2018 Pinterest, Inc.
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
import java.util.Collection;
import java.util.List;

/** A collection of methods to help interact with table agents */
public interface AgentDAO {
    void insertOrUpdate(AgentBean agentBean) throws Exception;

    void update(String hostId, String envId, AgentBean updateBean) throws Exception;

    void updateMultiple(Collection<String> hostIds, String envId, AgentBean agentBean)
            throws Exception;

    void resetFailedAgents(String envId, String deployId) throws Exception;

    void updateAgentById(String hostId, AgentBean bean) throws Exception;

    void delete(String hostId, String envId) throws Exception;

    // TODO refector this
    List<AgentBean> getByHost(String hostName) throws Exception;

    List<AgentBean> getByHostId(String hostId) throws Exception;

    List<AgentBean> getAllByEnv(String envId) throws Exception;

    List<AgentBean> getByEnvAndFirstDeployTime(String envId, long time) throws Exception;

    AgentBean getByHostEnvIds(String hostId, String envId) throws Exception;

    void deleteAllById(String hostId) throws Exception;

    // return how many agents are deploying for this env, including agents whose state is STOP
    long countDeployingAgent(String envId) throws Exception;

    // return how many agents are doing first time deploy for this env.
    long countFirstDeployingAgent(String envId) throws Exception;

    // return how many agents are failing during first time deploy for this env
    long countFailedFirstDeployingAgent(String envId) throws Exception;

    // return how many agent already succeeded for certain deploy
    long countSucceededAgent(String envId, String deployId) throws Exception;

    // return how many agent reports for this env regardless of deployId or stage
    long countAgentByEnv(String envId) throws Exception;

    // return how many agent state is PAUSED_BY_SYSTEM, or stuck/failed
    long countStuckAgent(String envId, String deployId) throws Exception;

    // return how many agents that are not first time deploy for the environment
    long countNonFirstDeployingAgent(String envId) throws Exception;

    long countServingTotal(String envId) throws Exception;

    long countServingAndNormalTotal(String envId) throws Exception;

    long countFinishedAgentsByDeploy(String deployId) throws Exception;

    long countAgentsByDeploy(String deployId) throws Exception;

    long countDeployingAgentWithHostTag(String envId, String tagName, String tagValue)
            throws Exception;

    long countFinishedAgentsByDeployWithHostTags(
            String envId, String deployId, String tagName, List<String> tagValues) throws Exception;

    // return how many hosts that are deployed
    long countDeployedHosts() throws Exception;
}
