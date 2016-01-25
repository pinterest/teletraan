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
package com.pinterest.teletraan.worker;


import com.pinterest.arcee.autoscaling.AutoScaleGroupManager;
import com.pinterest.arcee.bean.AsgLifecycleEventBean;
import com.pinterest.arcee.dao.AsgLifecycleEventDAO;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.AgentBean;
import com.pinterest.deployservice.bean.DeployStage;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.bean.HostState;
import com.pinterest.deployservice.dao.AgentDAO;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.dao.UtilDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.util.Collections;
import java.util.List;

public class LifecycleUpdator implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(LifecycleUpdator.class);
    private final AsgLifecycleEventDAO asgLifecycleEventDAO;
    private final AgentDAO agentDAO;
    private final HostDAO hostDAO;
    private final UtilDAO utilDAO;
    private final AutoScaleGroupManager autoScaleGroupManager;


    public LifecycleUpdator(ServiceContext serviceContext) {
        asgLifecycleEventDAO = serviceContext.getAsgLifecycleEventDAO();
        agentDAO = serviceContext.getAgentDAO();
        hostDAO = serviceContext.getHostDAO();
        utilDAO = serviceContext.getUtilDAO();
        autoScaleGroupManager = serviceContext.getAutoScaleGroupManager();
    }

    public void processLifecycle(String hookId) throws Exception {
        List<AsgLifecycleEventBean> lifecycleEventBeans = asgLifecycleEventDAO.getAsgLifecycleEventByHook(hookId);
        for (AsgLifecycleEventBean lifecycleEventBean : lifecycleEventBeans) {
            String hostId = lifecycleEventBean.getHost_id();
            String tokenId = lifecycleEventBean.getToken_id();
            List<HostBean> hostBeans = hostDAO.getHostsByHostId(hostId);
            if (hostBeans.isEmpty()) {
                LOG.info("Host {} has been removed. Delete token {} from asgLifecycleEventDAO", hostId, tokenId);
                autoScaleGroupManager.completeLifecycleAction(hookId, tokenId, lifecycleEventBean.getGroup_name());
                asgLifecycleEventDAO.deleteAsgLifecycleEventById(tokenId);
            } else {
                List<AgentBean> agentBeans = agentDAO.getByHostId(hostId);
                boolean stopSucceeded = true;
                for (AgentBean agentBean : agentBeans) {
                    if (agentBean.getDeploy_stage() != DeployStage.STOPPED) {
                        stopSucceeded = false;
                    }
                }

                if (stopSucceeded) {
                    LOG.info("Complete lifecycle action for host {}", hostId, tokenId);
                    autoScaleGroupManager.completeLifecycleAction(hookId, tokenId, lifecycleEventBean.getGroup_name());
                    asgLifecycleEventDAO.deleteAsgLifecycleEventById(tokenId);

                    HostBean hostBean = new HostBean();
                    hostBean.setState(HostState.TERMINATING);
                    hostBean.setLast_update(System.currentTimeMillis());
                    hostDAO.updateHostById(hostId, hostBean);
                }
            }
        }
    }

    public void processBatch() throws Exception {
        List<String> hookIds = asgLifecycleEventDAO.getHookIdsFromAsgLifeCycleEvent();
        if (hookIds.isEmpty()) {
            return;
        }

        Collections.shuffle(hookIds);
        for (String hookId : hookIds) {
            LOG.info("Start to process lifecycle hook {}", hookId);
            String lockName = String.format("LIFECYCLEHOOK-%s", hookId);
            Connection connection =  utilDAO.getLock(lockName);
            if (connection != null) {
                try {
                    processLifecycle(hookId);
                } catch (Exception ex) {
                    LOG.error("Failed to process lifecycle hook {}", hookId);
                } finally {
                    utilDAO.releaseLock(lockName, connection);
                }
            } else {
                LOG.warn("Failed to get lock {}", lockName);
            }
        }
    }

    @Override
    public void run() {
        try {
            LOG.info("Start to run LifecycleUpdator");
            processBatch();
        } catch (Throwable t) {
            LOG.error("Failed to run LifecycleUpdator");
        }
    }
}
