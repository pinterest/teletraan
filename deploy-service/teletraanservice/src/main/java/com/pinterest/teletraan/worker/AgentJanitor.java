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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.HostAgentBean;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.bean.HostState;
import com.pinterest.deployservice.rodimus.RodimusManager;

/**
 * Housekeeping on stuck and dead agents and hosts
 *
 * If an agent has not ping server for certain time, we will cross check with
 * authoritative source to confirm if the host is terminated, and handle the
 * agent status accordingly.
 *
 * If a host doesn't have any agent for a while, we will handle the host
 * accordingly.
 */
public class AgentJanitor extends SimpleAgentJanitor {
    private static final Logger LOG = LoggerFactory.getLogger(AgentJanitor.class);
    private final RodimusManager rodimusManager;
    private final long maxLaunchLatencyThreshold;
    private final long absoluteThreshold = TimeUnit.DAYS.toMillis(7);
    private final int agentlessHostBatchSize = 300;
    private long janitorStartTime;

    public AgentJanitor(ServiceContext serviceContext, int minStaleHostThresholdSeconds,
            int maxStaleHostThresholdSeconds, int maxLaunchLatencyThresholdSeconds) {
        super(serviceContext, minStaleHostThresholdSeconds, maxStaleHostThresholdSeconds);
        rodimusManager = serviceContext.getRodimusManager();
        maxLaunchLatencyThreshold = TimeUnit.SECONDS.toMillis(maxLaunchLatencyThresholdSeconds);
    }

    private Set<String> getTerminatedHostsFromSource(List<String> staleHostIds) {
        int batchSize = 10;
        Set<String> terminatedHosts = new HashSet<>();
        for (int i = 0; i < staleHostIds.size(); i += batchSize) {
            try {
                terminatedHosts.addAll(rodimusManager
                        .getTerminatedHosts(staleHostIds.subList(i, Math.min(i + batchSize, staleHostIds.size()))));
            } catch (Exception ex) {
                //Report failure
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
                //Report success
            } catch (Exception ex) {
                //Report failure
                LOG.error("failed to get launch grace period for cluster {}, exception: {}", clusterName, ex);
            }
        }
        return launchGracePeriod == null ? maxLaunchLatencyThreshold : TimeUnit.SECONDS.toMillis(launchGracePeriod);
    }

    private boolean isHostStale(HostAgentBean hostAgentBean) {
        if (hostAgentBean == null || hostAgentBean.getLast_update() == null) {
            return false;
        }

        if (janitorStartTime - hostAgentBean.getLast_update() >= absoluteThreshold) {
            return true;
        }

        HostBean hostBean;
        try {
            hostBean = hostDAO.getHostsByHostId(hostAgentBean.getHost_id()).get(0);
            //Report success
        } catch (Exception ex) {
            LOG.error("failed to get host bean for ({}), {}", hostAgentBean, ex);
            //Report failure
            return false;
        }

        Long launchGracePeriod = getInstanceLaunchGracePeriod(hostAgentBean.getAuto_scaling_group());
        if ((hostBean.getState() == HostState.PROVISIONED)
                && (janitorStartTime - hostAgentBean.getLast_update() >= launchGracePeriod)) {
            return true;
        }
        if (hostBean.getState() != HostState.TERMINATING && !hostBean.isPendingTerminate() &&
                (janitorStartTime - hostAgentBean.getLast_update() >= maxStaleHostThreshold)) {
            return true;
        }
        return false;
    }

    /**
     * Process stale hosts which have not pinged since
     * janitorStartTime - minStaleHostThreshold
     * They will be candidates for stale hosts which will be removed in future
     * executions.
     * Either mark them as UNREACHABLE, or remove if confirmed with source of truth.
     */
    private void determineStaleHostCandidates() {
        long minThreshold = janitorStartTime - minStaleHostThreshold;
        long maxThreshold = janitorStartTime - maxStaleHostThreshold;
        List<HostAgentBean> unreachableHosts;
        try {
            LOG.debug("getting hosts between {}, {}", maxThreshold, minThreshold);
            unreachableHosts = hostAgentDAO.getStaleHosts(maxThreshold, minThreshold);
        } catch (Exception ex) {
            LOG.error("failed to get unreachable hosts", ex);
            //Report failure
            return;
        }
        List<String> unreachableHostIds = unreachableHosts.stream().map(HostAgentBean::getHost_id)
                .collect(Collectors.toList());
        LOG.debug("fetched {} unreachable hosts", unreachableHostIds.size());

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
     * janitorStartTime - maxStaleHostThreshold
     * They are confirmed stale hosts, should be removed from Teletraan
     */
    private void processStaleHosts() {
        long maxThreshold = janitorStartTime - maxStaleHostThreshold;
        List<HostAgentBean> staleHosts;
        try {
            LOG.debug("getting hosts before {}", maxThreshold);
            staleHosts = hostAgentDAO.getStaleHosts(maxThreshold);
        } catch (Exception ex) {
            LOG.error("failed to get stale hosts", ex);
            //Report failure
            return;
        }

        Map<String, HostAgentBean> staleHostMap = new HashMap<>();
        staleHosts.stream().forEach(hostAgent -> staleHostMap.put(hostAgent.getHost_id(), hostAgent));
        LOG.debug("fetched {} unreachable hosts", staleHostMap.values().size());

        Set<String> terminatedHosts = getTerminatedHostsFromSource(new ArrayList<>(staleHostMap.keySet()));
        for (String staleId : staleHostMap.keySet()) {
            if (terminatedHosts.contains(staleId)) {
                removeStaleHost(staleId);
            } else {
                HostAgentBean hostAgent = staleHostMap.get(staleId);
                if (isHostStale(hostAgent)) {
                    LOG.warn("Agent ({}) is stale (not Pinging Teletraan), but might be running.",
                            hostAgent);
                } else {
                    LOG.debug("host {} is not stale", staleId);
                }
            }
            //report success
        }
    }

    /**
     * Clean up hosts without any agents
     *
     * If a host is directly added to Teletraan, there will be no agent associated
     * with it immediately. Hosts may stuck in this state so we should clean up
     * here. We wait 10x maxLaunchLatencyThreshold before doing cleanup.
     */
    private void cleanUpAgentlessHosts() {
        long noUpdateSince = janitorStartTime - 10 * maxLaunchLatencyThreshold;
        List<String> agentlessHosts;
        try {
            agentlessHosts = hostDAO.getStaleAgentlessHostIds(noUpdateSince, agentlessHostBatchSize);
        } catch (SQLException ex) {
            LOG.error("failed to get agentless hosts", ex);
            //Report failure
            return;
        }

        Set<String> terminatedHosts = getTerminatedHostsFromSource(agentlessHosts);
        for (String hostId : agentlessHosts) {
            if (terminatedHosts.contains(hostId)) {
                removeStaleHost(hostId);
            } else {
                LOG.warn("Agentless host {} is stale but might be running", hostId);
            }
        }
    }

    @Override
    void processAllHosts() {
        janitorStartTime = System.currentTimeMillis();
        processStaleHosts();
        determineStaleHostCandidates();
        cleanUpAgentlessHosts();
        //report success
    }
}
