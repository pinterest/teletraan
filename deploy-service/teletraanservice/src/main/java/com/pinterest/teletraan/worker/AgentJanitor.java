/**
 * Copyright (c) 2016-2025 Pinterest, Inc.
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
import com.pinterest.deployservice.bean.HostAgentBean;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.bean.HostState;
import com.pinterest.deployservice.dao.AgentDAO;
import com.pinterest.deployservice.dao.HostAgentDAO;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.handler.HostHandler;
import com.pinterest.deployservice.rodimus.RodimusManager;
import com.pinterest.teletraan.universal.metrics.ErrorBudgetCounterFactory;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Housekeeping on stuck and dead agents and hosts
 *
 * <p>If an agent has not ping server for certain time, we will cross check with authoritative
 * source to confirm if the host is terminated, and handle the agent status accordingly.
 *
 * <p>If a host doesn't have any agent for a while, we will handle the host accordingly.
 */
public class AgentJanitor implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(AgentJanitor.class);
    private final RodimusManager rodimusManager;
    private final long maxLaunchLatencyThreshold;
    private final long absoluteThreshold = TimeUnit.DAYS.toMillis(7);
    private final int agentlessHostBatchSize = 300;
    private final AtomicInteger unreachableHostsCount;
    private final AtomicInteger staleHostsCount;
    private final Counter errorBudgetSuccess;
    private final Counter errorBudgetFailure;
    private long janitorStartTime;
    private final AgentDAO agentDAO;
    private final HostDAO hostDAO;
    private final HostAgentDAO hostAgentDAO;
    private final HostHandler hostHandler;
    private final long maxStaleHostThreshold;
    private final long minStaleHostThreshold;

    public AgentJanitor(
            ServiceContext serviceContext,
            int minStaleHostThresholdSeconds,
            int maxStaleHostThresholdSeconds,
            int maxLaunchLatencyThresholdSeconds) {
        agentDAO = serviceContext.getAgentDAO();
        hostDAO = serviceContext.getHostDAO();
        hostAgentDAO = serviceContext.getHostAgentDAO();
        rodimusManager = serviceContext.getRodimusManager();
        hostHandler = new HostHandler(serviceContext);
        this.maxStaleHostThreshold = maxStaleHostThresholdSeconds * 1000;
        this.minStaleHostThreshold = minStaleHostThresholdSeconds * 1000;
        maxLaunchLatencyThreshold = TimeUnit.SECONDS.toMillis(maxLaunchLatencyThresholdSeconds);
        unreachableHostsCount = Metrics.gauge("unreachable_hosts", new AtomicInteger(0));
        staleHostsCount = Metrics.gauge("stale_hosts", new AtomicInteger(0));

        errorBudgetSuccess =
                ErrorBudgetCounterFactory.createSuccessCounter(this.getClass().getSimpleName());
        errorBudgetFailure =
                ErrorBudgetCounterFactory.createFailureCounter(this.getClass().getSimpleName());
    }

    void processAllHosts() {
        janitorStartTime = System.currentTimeMillis();
        processStaleHosts();
        determineStaleHostCandidates();
        cleanUpAgentlessHosts();
        errorBudgetSuccess.increment();
    }

    private Set<String> getTerminatedHostsFromSource(List<String> staleHostIds) {
        int batchSize = 3;
        Set<String> terminatedHosts = new HashSet<>();
        for (int i = 0; i < staleHostIds.size(); i += batchSize) {
            try {
                terminatedHosts.addAll(
                        rodimusManager.getTerminatedHosts(
                                staleHostIds.subList(
                                        i, Math.min(i + batchSize, staleHostIds.size()))));
            } catch (Exception ex) {
                LOG.error("Failed to get terminated hosts", ex);
                errorBudgetFailure.increment();
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
                LOG.error(
                        "failed to get launch grace period for cluster {}, exception: {}",
                        clusterName,
                        ex);
                errorBudgetFailure.increment();
            }
        }
        return launchGracePeriod == null
                ? maxLaunchLatencyThreshold
                : TimeUnit.SECONDS.toMillis(launchGracePeriod);
    }

    private boolean isHostStale(HostAgentBean hostAgentBean) {
        if (hostAgentBean == null || hostAgentBean.getLast_update() == null) {
            return false;
        }

        if (janitorStartTime - hostAgentBean.getLast_update() >= absoluteThreshold) {
            LOG.debug(
                    "exceeded absolute stale threshold ({}) for host ({})",
                    absoluteThreshold,
                    hostAgentBean);
            return true;
        }

        HostBean hostBean;
        try {
            List<HostBean> hostBeans = hostDAO.getHostsByHostId(hostAgentBean.getHost_id());
            if (hostBeans.isEmpty()) {
                // Usually the host being checked is not terminated. However there might be some
                // synchronization latency.
                // Mark it as not stale and we will handle it in the next run.
                return false;
            }
            hostBean = hostBeans.get(0);
        } catch (Exception ex) {
            LOG.error("failed to get host bean for ({}), {}", hostAgentBean, ex);
            errorBudgetFailure.increment();
            return false;
        }

        Long launchGracePeriod =
                getInstanceLaunchGracePeriod(hostAgentBean.getAuto_scaling_group());
        if ((hostBean.getState() == HostState.PROVISIONED)
                && (janitorStartTime - hostAgentBean.getLast_update() >= launchGracePeriod)) {
            LOG.debug(
                    "exceeded launch grace period ({}) for provisioned host ({})",
                    launchGracePeriod,
                    hostAgentBean);
            return true;
        }
        if (hostBean.getState() != HostState.TERMINATING
                && !hostBean.isPendingTerminate()
                && (janitorStartTime - hostAgentBean.getLast_update() >= maxStaleHostThreshold)) {
            LOG.debug(
                    "exceeded max stale threshold ({}) for host ({})",
                    maxStaleHostThreshold,
                    hostAgentBean);
            return true;
        }
        return false;
    }

    private Map<String, HostAgentBean> getStaleHostsMap(long minThreshold, long maxThreshold) {
        List<HostAgentBean> staleHosts;
        Map<String, HostAgentBean> staleHostMap = new HashMap<>();
        try {
            LOG.debug("getting hosts between {}, {}", maxThreshold, minThreshold);
            if (minThreshold > 0) {
                staleHosts = hostAgentDAO.getStaleHosts(maxThreshold, minThreshold);
            } else {
                staleHosts = hostAgentDAO.getStaleHosts(maxThreshold);
            }
        } catch (Exception ex) {
            LOG.error("failed to get stale hosts", ex);
            errorBudgetFailure.increment();
            return staleHostMap;
        }

        staleHosts.stream()
                .forEach(hostAgent -> staleHostMap.put(hostAgent.getHost_id(), hostAgent));
        LOG.debug("fetched {} unreachable hosts", staleHostMap.size());
        return staleHostMap;
    }

    /**
     * Process stale hosts which have not pinged since janitorStartTime - minStaleHostThreshold They
     * will be candidates for stale hosts which will be removed in future executions. Either mark
     * them as UNREACHABLE, or remove if confirmed with source of truth.
     */
    private void determineStaleHostCandidates() {
        long minThreshold = janitorStartTime - minStaleHostThreshold;
        long maxThreshold = janitorStartTime - maxStaleHostThreshold;
        int unreachableHostCount = 0;
        Map<String, HostAgentBean> unreachableHostsMap =
                getStaleHostsMap(minThreshold, maxThreshold);

        Set<String> terminatedHosts =
                getTerminatedHostsFromSource(new ArrayList<>(unreachableHostsMap.keySet()));
        for (String unreachableId : unreachableHostsMap.keySet()) {
            if (terminatedHosts.contains(unreachableId)) {
                removeStaleHost(unreachableId);
            } else {
                markUnreachableHost(unreachableId);
                unreachableHostCount++;
                HostAgentBean host = unreachableHostsMap.get(unreachableId);
                LOG.info(
                        "{} has unreachable host {}",
                        host.getAuto_scaling_group(),
                        host.getHost_id());
            }
            errorBudgetSuccess.increment();
        }
        this.unreachableHostsCount.set(unreachableHostCount);
    }

    /**
     * Process stale hosts which have not pinged since janitorStartTime - maxStaleHostThreshold They
     * are confirmed stale hosts, should be removed from Teletraan
     */
    private void processStaleHosts() {
        long maxThreshold = janitorStartTime - maxStaleHostThreshold;
        int staleHostCount = 0;
        Map<String, HostAgentBean> staleHostMap = getStaleHostsMap(0, maxThreshold);

        Set<String> terminatedHosts =
                getTerminatedHostsFromSource(new ArrayList<>(staleHostMap.keySet()));
        for (String staleId : staleHostMap.keySet()) {
            if (terminatedHosts.contains(staleId)) {
                removeStaleHost(staleId);
            } else {
                HostAgentBean hostAgent = staleHostMap.get(staleId);
                if (isHostStale(hostAgent)) {
                    markUnreachableHost(staleId);
                    LOG.warn(
                            "{}:{} is stale (not Pinging Teletraan), but might be running.",
                            hostAgent.getAuto_scaling_group(),
                            hostAgent.getHost_id());
                    staleHostCount++;
                    errorBudgetSuccess.increment();
                } else {
                    LOG.debug("host {} is not stale", staleId);
                }
            }
        }
        this.staleHostsCount.set(staleHostCount);
    }

    /**
     * Clean up hosts without any agents
     *
     * <p>If a host is directly added to Teletraan, there will be no agent associated with it
     * immediately. Hosts may stuck in this state so we should clean up here. We wait 10x
     * maxLaunchLatencyThreshold before doing cleanup.
     */
    private void cleanUpAgentlessHosts() {
        long noUpdateSince = janitorStartTime - 10 * maxLaunchLatencyThreshold;
        List<String> agentlessHosts;
        try {
            agentlessHosts =
                    hostDAO.getStaleAgentlessHostIds(noUpdateSince, agentlessHostBatchSize);
        } catch (SQLException ex) {
            LOG.error("failed to get agentless hosts", ex);
            errorBudgetFailure.increment();
            return;
        }

        Set<String> terminatedHosts = getTerminatedHostsFromSource(agentlessHosts);
        for (String hostId : agentlessHosts) {
            if (terminatedHosts.contains(hostId)) {
                removeStaleHost(hostId);
            } else {
                LOG.warn("Agentless host {} is stale but might be running", hostId);
                errorBudgetSuccess.increment();
            }
        }
    }

    // remove the stale host from db
    void removeStaleHost(String id) {
        LOG.info("Delete records of stale host {}", id);
        hostHandler.removeHost(id);
    }

    void markUnreachableHost(String id) {
        try {
            // mark the agent as unreachable
            AgentBean updateBean = new AgentBean();
            updateBean.setState(AgentState.UNREACHABLE);
            agentDAO.updateAgentById(id, updateBean);
            LOG.info("Marked agent {} as UNREACHABLE.", id);
        } catch (Exception e) {
            LOG.error("Failed to mark host {} as UNREACHABLE. exception {}", id, e);
        }
    }

    @Override
    public void run() {
        try {
            LOG.info("Start agent janitor process...");
            processAllHosts();
        } catch (Throwable t) {
            // Catch all throwable so that subsequent job not suppressed
            LOG.error("AgentJanitor Failed.", t);
        }
    }
}
