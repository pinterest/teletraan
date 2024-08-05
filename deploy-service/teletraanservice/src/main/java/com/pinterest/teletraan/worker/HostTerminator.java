/**
 * Copyright (c) 2016-2024 Pinterest, Inc.
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
import com.pinterest.deployservice.bean.AgentState;
import com.pinterest.deployservice.bean.DeployStage;
import com.pinterest.deployservice.bean.HostAgentBean;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.bean.HostState;
import com.pinterest.deployservice.dao.AgentDAO;
import com.pinterest.deployservice.dao.HostAgentDAO;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.dao.UtilDAO;
import com.pinterest.deployservice.handler.HostHandler;
import com.pinterest.deployservice.rodimus.RodimusManager;
import com.pinterest.teletraan.universal.metrics.ErrorBudgetCounterFactory;
import io.micrometer.core.instrument.Counter;
import java.sql.Connection;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostTerminator implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(HostTerminator.class);
    private final AgentDAO agentDAO;
    private final HostAgentDAO hostAgentDAO;
    private final HostDAO hostDAO;
    private final UtilDAO utilDAO;
    private final RodimusManager rodimusManager;
    private final HostHandler hostHandler;
    private final Counter errorBudgetSuccess;
    private final Counter errorBudgetFailure;

    public HostTerminator(ServiceContext serviceContext) {
        agentDAO = serviceContext.getAgentDAO();
        hostDAO = serviceContext.getHostDAO();
        utilDAO = serviceContext.getUtilDAO();
        rodimusManager = serviceContext.getRodimusManager();
        hostAgentDAO = serviceContext.getHostAgentDAO();
        hostHandler = new HostHandler(serviceContext);

        errorBudgetSuccess =
                ErrorBudgetCounterFactory.createSuccessCounter(this.getClass().getSimpleName());
        errorBudgetFailure =
                ErrorBudgetCounterFactory.createFailureCounter(this.getClass().getSimpleName());
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
            if (agentBean.getDeploy_stage() != DeployStage.STOPPED
                    && agentBean.getState() != AgentState.PAUSED_BY_SYSTEM) {
                stopSucceeded = false;
                break;
            }
        }

        if (stopSucceeded) {
            LOG.info(String.format("Host %s is stopped. Terminate it.", hostId));

            Boolean replaceHost = host.getState() == HostState.PENDING_TERMINATE;
            String clusterName = host.getGroup_name();
            HostAgentBean hostAgentBean = hostAgentDAO.getHostById(hostId);
            if (hostAgentBean != null) {
                // HostBean.getGroup_name() does not necessarily return the Auto scaling group
                // name. Therefore correct it if we can get the ASG name from HostAgentDAO.
                clusterName = hostAgentBean.getAuto_scaling_group();
            } else if (!replaceHost) {
                LOG.warn(
                        "Failed to get ASG name for host {}, using group name {} instead. Host can still be replaced.",
                        hostId,
                        clusterName);
            }

            rodimusManager.terminateHostsByClusterName(
                    clusterName, Collections.singletonList(hostId), replaceHost);
        }
    }

    private boolean removeTerminatedHost(HostBean host) throws Exception {
        String hostId = host.getHost_id();
        Collection<String> terminatedHosts =
                rodimusManager.getTerminatedHosts(Collections.singletonList(hostId));
        if (terminatedHosts.contains(hostId)) {
            LOG.info("Delete records of terminated host {}", hostId);
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
                    if (host.isPendingTerminate()) {
                        terminateHost(host);
                    } else if (host.getState() == HostState.TERMINATING) {
                        removeTerminatedHost(host);
                    }
                } catch (Exception e) {
                    LOG.error(
                            "Failed to process {} host {}", host.getState(), host.getHost_id(), e);
                } finally {
                    utilDAO.releaseLock(lockName, connection);
                    LOG.info(
                            String.format(
                                    "DB lock operation is successful: release lock %s", lockName));
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

            errorBudgetSuccess.increment();
        } catch (Throwable t) {
            LOG.error("HostTerminator failed", t);

            errorBudgetFailure.increment();
        }
    }
}
