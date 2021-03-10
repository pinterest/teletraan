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

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.AgentBean;
import com.pinterest.deployservice.bean.DeployStage;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.bean.HostState;
import com.pinterest.deployservice.dao.AgentDAO;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.dao.UtilDAO;
import com.pinterest.deployservice.rodimus.RodimusManager;
import com.pinterest.deployservice.handler.HostHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.*;

public class HostTerminator implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(HostTerminator.class);
    private final AgentDAO agentDAO;
    private final HostDAO hostDAO;
    private final UtilDAO utilDAO;
    private final RodimusManager rodimusManager;
    private final HostHandler hostHandler;

    public HostTerminator(ServiceContext serviceContext) {
        agentDAO = serviceContext.getAgentDAO();
        hostDAO = serviceContext.getHostDAO();
        utilDAO = serviceContext.getUtilDAO();
        rodimusManager = serviceContext.getRodimusManager();
        hostHandler = new HostHandler(serviceContext);
    }

    private void terminateHost(HostBean host) throws Exception {
        // if host is terminated before stopping successfully, delete record directly
        if (removeTerminatedHost(host)) {
            return;
        }

        String hostId = host.getHost_id();
        List<AgentBean> agentBeans = agentDAO.getByHostId(hostId);
        boolean stopSucceeded = true;
        for (AgentBean agentBean : agentBeans) {
            if (agentBean.getDeploy_stage() != DeployStage.STOPPED) {
                stopSucceeded = false;
            }
        }

        if (stopSucceeded) {
            LOG.info(String.format("Host %s is stopped. Terminate it.", hostId));
            String clusterName = host.getGroup_name();
            rodimusManager.terminateHostsByClusterName(clusterName, Collections.singletonList(hostId));
        }
    }

    private boolean removeTerminatedHost(HostBean host) throws Exception {
        String hostId = host.getHost_id();
        Collection<String> terminatedHosts = rodimusManager.getTerminatedHosts(Collections.singletonList(hostId));
        if (terminatedHosts.contains(hostId)) {
            LOG.info(String.format("Delete records of terminated host {}", hostId));
            hostHandler.removeHost(hostId);
            return true;
        }
        return false;
    }

    private void processBatch() throws Exception {
        List<HostBean> hosts = hostDAO.getTerminatingHosts();
        Collections.shuffle(hosts);
        for (HostBean host : hosts) {
            String lockName = String.format("HOSTTERMINATOR-%s", host.getHost_id());
            Connection connection = utilDAO.getLock(lockName);
            if (connection != null) {
                LOG.info(String.format("DB lock operation is successful: get lock %s", lockName));
                try {
                    if (host.getState() == HostState.PENDING_TERMINATE) {
                        terminateHost(host);
                    } else if (host.getState() == HostState.TERMINATING) {
                        removeTerminatedHost(host);
                    }
                } catch (Exception e) {
                    LOG.error("Failed to process {} host {}", host.getState().toString(), host.getHost_id(), e);
                } finally {
                    utilDAO.releaseLock(lockName, connection);
                    LOG.info(String.format("DB lock operation is successful: release lock %s", lockName));
                }
            } else {
                LOG.warn(String.format("DB lock operation fails: failed to get lock %s", lockName));
            }
        }
    }

    @Override
    public void run() {
        try {
            LOG.info("Start to run HostTerminator");
            processBatch();
        } catch (Throwable t) {
            LOG.error("Faile to run HostTerminator", t);
        }
    }
}
