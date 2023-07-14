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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.HostAgentBean;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.bean.HostState;
import com.pinterest.deployservice.rodimus.RodimusManager;

/**
 * Housekeeping on stuck and dead agents
 * <p>
 * if an agent has not ping server for certain time, we will cross check with
 * authoritative source to confirm if the host is terminated, and handle the
 * agent status accordingly
 */
public class AgentJanitor extends SimpleAgentJanitor {
    private static final Logger LOG = LoggerFactory.getLogger(AgentJanitor.class);
    private final RodimusManager rodimusManager;
    private long maxLaunchLatencyThreshold;
    private long absoluteThreshold = 24 * 3600 * 1000; // 1 day
    private int agentlessHostBatchSize = 300;

    public AgentJanitor(ServiceContext serviceContext, int minStaleHostThreshold,
            int maxStaleHostThreshold, int maxLaunchLatencyThreshold) {
        super(serviceContext, minStaleHostThreshold, maxStaleHostThreshold);
        rodimusManager = serviceContext.getRodimusManager();
        this.maxLaunchLatencyThreshold = maxLaunchLatencyThreshold * 1000;
    }

    private Set<String> getTerminatedHostsFromSource(List<String> staleHostIds) {
        int batchSize = 10;
        Set<String> terminatedHosts = new HashSet<>();
        for (int i = 0; i < staleHostIds.size(); i += batchSize) {
            try {
                terminatedHosts.addAll(rodimusManager
                        .getTerminatedHosts(staleHostIds.subList(i, Math.min(i + batchSize, staleHostIds.size()))));
            } catch (Exception ex) {
                LOG.error("Failed to get terminated hosts", ex);
            }
        }
        return terminatedHosts;
    }

    private Long getInstanceLaunchGracePeriod(String clusterName) {
        Long launchGracePeriod = null;
        if (clusterName != null) {
            try {
                launchGracePeriod = rodimusManager.getClusterInstanceLaunchGracePeriod(clusterName);
            } catch (Exception ex) {
                LOG.error("failed to get launch grace period for cluster {}, exception: {}", clusterName, ex);
            }
        }
        return launchGracePeriod == null ? maxLaunchLatencyThreshold : launchGracePeriod * 1000;
    }

    private boolean isHostStale(HostAgentBean hostAgentBean) {
        if (hostAgentBean == null || hostAgentBean.getLast_update() == null) {
            return false;
        }

        long current_time = System.currentTimeMillis();
        HostBean hostBean;
        try {
            hostBean = hostDAO.getHostsByHostId(hostAgentBean.getHost_id()).get(0);
        } catch (Exception ex) {
            LOG.error("failed to get host bean for ({}), {}", hostAgentBean, ex);
            return false;
        }

        Long launchGracePeriod = getInstanceLaunchGracePeriod(hostAgentBean.getAuto_scaling_group());
        if ((hostBean.getState() == HostState.PROVISIONED)
                && (current_time - hostAgentBean.getLast_update() >= launchGracePeriod)) {
            return true;
        }
        if (hostBean.getState() != HostState.TERMINATING && !hostBean.isPendingTerminate() &&
                (current_time - hostAgentBean.getLast_update() >= maxStaleHostThreshold)) {
            return true;
        }

        if (current_time - hostAgentBean.getLast_update() >= absoluteThreshold) {
            return true;
        }
        return false;
    }

    /**
     * Process stale hosts which have not pinged since
     * current_time - minStaleHostThreshold
     * They will be candidates for stale hosts which will be removed in future
     * executions.
     * Either mark them as UNREACHABLE, or remove if confirmed with source of truth.
     *
     * @throws Exception
     */
    private void determineStaleHostCandidates() {
        long current_time = System.currentTimeMillis();
        long minThreshold = current_time - minStaleHostThreshold;
        List<HostAgentBean> unreachableHosts;
        try {
            unreachableHosts = hostAgentDAO.getStaleHosts(minThreshold);
        } catch (Exception ex) {
            LOG.error("failed to get potential unreachable hosts", ex);
            return;
        }
        ArrayList<String> unreachableHostIds = new ArrayList<>();
        unreachableHosts.stream().map(hostAgent -> unreachableHostIds.add(hostAgent.getHost_id()));

        Set<String> terminatedHosts = getTerminatedHostsFromSource(unreachableHostIds);
        for (String unreachableId : unreachableHostIds) {
            if (terminatedHosts.contains(unreachableId)) {
                removeStaleHost(unreachableId);
            } else {
                markUnreachableHost(unreachableId);
            }
        }
    }

    /**
     * Process stale hosts which have not pinged since
     * current_time - maxStaleHostThreshold
     * They are confirmed stale hosts, should be removed from Teletraan
     *
     * @throws Exception
     */
    private void processStaleHosts() {
        long current_time = System.currentTimeMillis();
        long maxThreshold = current_time - maxStaleHostThreshold;
        List<HostAgentBean> staleHosts;
        try {
            staleHosts = hostAgentDAO.getStaleHosts(maxThreshold);
        } catch (Exception ex) {
            LOG.error("failed to get potential stale hosts", ex);
            return;
        }

        Map<String, HostAgentBean> staleHostMap = new HashMap<>();
        staleHosts.stream().map(hostAgent -> staleHostMap.put(hostAgent.getHost_id(), hostAgent));

        Set<String> terminatedHosts = getTerminatedHostsFromSource(new ArrayList<>(staleHostMap.keySet()));
        for (String staleId : staleHostMap.keySet()) {
            if (terminatedHosts.contains(staleId)) {
                removeStaleHost(staleId);
            } else {
                HostAgentBean hostAgent = staleHostMap.get(staleId);
                if (isHostStale(hostAgent)) {
                    LOG.warn("Agent ({}) is stale (not Pinning Teletraan), but the host termination state is unknown.",
                            hostAgent);
                }
            }
        }
    }

    /**
     * Clean up hosts without any agents
     *
     * If a host is directly added to Teletraan, there will be no agent associated
     * with it immediately.
     * Hosts may stuck in this state so we should clean up here.
     */
    private void cleanUpAgentlessHosts() {
        long current_time = System.currentTimeMillis();
        long noUpdateSince = current_time - absoluteThreshold;
        List<String> agentlessHosts;
        try {
            agentlessHosts = hostDAO.getStaleAgentlessHostIds(noUpdateSince, agentlessHostBatchSize);
        } catch (SQLException ex) {
            LOG.error("failed to get agentless hosts", ex);
            return;
        }

        Set<String> terminatedHosts = getTerminatedHostsFromSource(agentlessHosts);
        for (String hostId : agentlessHosts) {
            if (terminatedHosts.contains(hostId)) {
                removeStaleHost(hostId);
            } else {
                LOG.warn("Agentless host {} termination state is unknown", hostId);
            }
        }
    }

    @Override
    void processAllHosts() {
        processStaleHosts();
        determineStaleHostCandidates();
        cleanUpAgentlessHosts();
    }
}
