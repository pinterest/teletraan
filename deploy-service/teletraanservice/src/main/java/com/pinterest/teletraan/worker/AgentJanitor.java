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
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.bean.HostAgentBean;
import com.pinterest.deployservice.bean.HostState;
import com.pinterest.deployservice.group.HostGroupManager;
import com.pinterest.deployservice.rodimus.RodimusManager;
import com.pinterest.deployservice.handler.HostHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Housekeeping on stuck and dead agents
 * <p>
 * if an agent has not ping server for certain time, we will cross check with
 * authoritive source to confirm if the host is terminated, and handle the agent
 * status accordingly
 */
public class AgentJanitor extends SimpleAgentJanitor {
    private static final Logger LOG = LoggerFactory.getLogger(AgentJanitor.class);
    private HostGroupManager hostGroupDAO;
    private final RodimusManager rodimusManager;
    private long maxLaunchLatencyThreshold;

    public AgentJanitor(ServiceContext serviceContext, int minStaleHostThreshold,
        int maxStaleHostThreshold, int maxLaunchLatencyThreshold) {
        super(serviceContext, minStaleHostThreshold, maxStaleHostThreshold);
        hostGroupDAO = serviceContext.getHostGroupDAO();
        rodimusManager = serviceContext.getRodimusManager();
        this.maxLaunchLatencyThreshold = maxLaunchLatencyThreshold * 1000;
    }

    private Collection<String> getTerminatedHostsFromSource(Collection<String> staleHostIds) throws Exception {
        Collection<String> terminatedHosts = new ArrayList<>(staleHostIds);
        for (String hostId : staleHostIds) {
            Collection<String> resultIds = rodimusManager.getTerminatedHosts(Collections.singletonList(hostId));
            if (resultIds.isEmpty()) {
                terminatedHosts.remove(hostId);
            }
        }
        return terminatedHosts;
    }

    private Long getInstanceLaunchGracePeriod(String clusterName) throws Exception {
        Long launchGracePeriod = (clusterName != null) ? rodimusManager.getClusterInstanceLaunchGracePeriod(clusterName) : null;
        return launchGracePeriod == null ? maxLaunchLatencyThreshold : launchGracePeriod * 1000;
    }

    // Process stale hosts (hosts which have not pinged for more than min threshold period)
    // Removes hosts once confirmed with source
    private void processLowWatermarkHosts() throws Exception {
        long current_time = System.currentTimeMillis();
        // If host fails to ping for longer than min stale threshold,
        // either mark them as UNREACHABLE, or remove if confirmed with source of truth
        long minThreshold = current_time - minStaleHostThreshold;
        List<HostAgentBean> minStaleHosts = hostAgentDAO.getStaleHosts(minThreshold);
        Set<String> minStaleHostIds = new HashSet<>();
        for (HostAgentBean hostAgentBean: minStaleHosts) {
            minStaleHostIds.add(hostAgentBean.getHost_id());
        }
        Collection<String> terminatedHosts = getTerminatedHostsFromSource(minStaleHostIds);
        for (String removedId: terminatedHosts) {
            removeStaleHost(removedId);
        }
    }

    private boolean isHostStale(HostAgentBean hostAgentBean) throws Exception {
        if (hostAgentBean == null || hostAgentBean.getLast_update() == null) {
            return false;
        }
        HostBean hostBean = hostDAO.getHostsByHostId(hostAgentBean.getHost_id()).get(0);
        long current_time = System.currentTimeMillis();
        Long launchGracePeriod = getInstanceLaunchGracePeriod(hostAgentBean.getAuto_scaling_group());
        if ((hostBean.getState() == HostState.PROVISIONED) && (current_time - hostAgentBean.getLast_update() >= launchGracePeriod)) {
            return true;
        }
        if (hostBean.getState() != HostState.TERMINATING && hostBean.getState() != HostState.PENDING_TERMINATE && 
            (current_time - hostAgentBean.getLast_update() >= maxStaleHostThreshold)) {
                return true;
        }
        return false;
    }

    // Process stale hosts (hosts which have not pinged for more than max threshold period)
    // Marks hosts unreachable if it's stale for max threshold 
    // Removes hosts once confirmed with source
    private void processHighWatermarkHosts() throws Exception {
        long current_time = System.currentTimeMillis();
        long maxThreshold = current_time - Math.min(maxStaleHostThreshold, maxLaunchLatencyThreshold);
        List<HostAgentBean> maxStaleHosts = hostAgentDAO.getStaleHosts(maxThreshold);
        Set<String> staleHostIds = new HashSet<>();
        
        for (HostAgentBean hostAgentBean : maxStaleHosts) {
            if (isHostStale(hostAgentBean)) {
                staleHostIds.add(hostAgentBean.getHost_id());
            }
        }
        Collection<String> terminatedHosts = getTerminatedHostsFromSource(staleHostIds);
        for (String staleId : staleHostIds) {
            if (terminatedHosts.contains(staleId)) {
                removeStaleHost(staleId);
            } else {
                markUnreachableHost(staleId);
            }
        }
    }

    @Override
    void processAllHosts() throws Exception {
        processLowWatermarkHosts();
        processHighWatermarkHosts();
    }
}
