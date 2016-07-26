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
import com.pinterest.deployservice.bean.HostState;
import com.pinterest.deployservice.group.HostGroupManager;
import com.pinterest.deployservice.rodimus.RodimusManager;

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

    private void processStaleHosts(Collection<String> staleHostIds, boolean isMarkedUnreachable) throws Exception {
        if (staleHostIds.isEmpty()) {
            return;
        }

        Collection<String> terminatedHosts = new ArrayList<>(staleHostIds);
        for (String hostId : staleHostIds) {
            Collection<String> resultIds = rodimusManager.getTerminatedHosts(Collections.singletonList(hostId));
            if (resultIds.isEmpty()) {
                terminatedHosts.remove(hostId);
            }
        }

        if (isMarkedUnreachable) {
            for (String staleId : staleHostIds) {
                if (terminatedHosts.contains(staleId)) {
                    removeStaleHost(staleId);
                } else {
                    // TODO this lock can be removed, once we move the terminating logic from state to status
                    HostBean host = hostDAO.getHostsByHostId(staleId).get(0);
                    if (host.getState() != HostState.TERMINATING && host.getState() != HostState.PENDING_TERMINATE) {
                        try {
                            markUnreachableHost(staleId);
                        } catch (Exception e) {
                            LOG.error("AgentJanitor Failed to mark host {} as UNREACHABLE", staleId,
                                      e);
                        }
                    }
                }
            }
        } else {
            for (String removedId : terminatedHosts) {
                removeStaleHost(removedId);
            }
        }
    }

    private Long getInstanceLaunchGracePeriod(String clusterName) throws Exception {
        Long launchGracePeriod = rodimusManager.getClusterInstanceLaunchGracePeriod(clusterName);
        return launchGracePeriod == null ? maxLaunchLatencyThreshold : launchGracePeriod * 1000;
    }

    @Override
    void processIndividualHosts() throws Exception {
        long current_time = System.currentTimeMillis();

        // If host fails to ping for longer than min stale threshold,
        // either mark them as UNREACHABLE, or remove if confirmed with source of truth
        long minThreshold = current_time - minStaleHostThreshold;
        List<HostBean> minStaleHosts = hostDAO.getStaleEnvHosts(minThreshold);
        Set<String> minStaleHostIds = new HashSet<>();
        for (HostBean host : minStaleHosts) {
            minStaleHostIds.add(host.getHost_id());
        }
        processStaleHosts(minStaleHostIds, false);

        current_time = System.currentTimeMillis();
        long maxThreshold = current_time - Math.min(maxStaleHostThreshold, maxLaunchLatencyThreshold);
        List<HostBean> maxStaleHosts = hostDAO.getStaleEnvHosts(maxThreshold);
        Set<String> staleHostIds = new HashSet<>();
        for (HostBean host : maxStaleHosts) {
            if (host.getState() == HostState.PROVISIONED) {
                if (current_time - host.getLast_update() >= maxLaunchLatencyThreshold) {
                    staleHostIds.add(host.getHost_id());
                }
            } else if (host.getState() != HostState.TERMINATING && host.getState() != HostState.PENDING_TERMINATE) {
                if (current_time - host.getLast_update() >= maxStaleHostThreshold) {
                    staleHostIds.add(host.getHost_id());
                }
            }
        }
        processStaleHosts(staleHostIds, true);
    }

    @Override
    void processEachGroup(String groupName) throws Exception {
        // TODO the following call could be slow or fail if there are lots of host in one group
        Map<String, HostBean> cmdbReportedHosts = hostGroupDAO.getHostIdsByGroup(groupName);
        long page_index = 1;
        int page_size = 1000;
        Set<String> groups = new HashSet<>();
        groups.add(groupName);

        Set<String> maxStaleHostIds = new HashSet<>();
        Set<String> minStaleHostIds = new HashSet<>();
        Long launchGracePeriod = getInstanceLaunchGracePeriod(groupName);
        long current_time = System.currentTimeMillis();
        while (true) {
            List<HostBean> hostBeans = hostDAO.getHostsByGroup(groupName, page_index, page_size);
            if (hostBeans.isEmpty()) {
                break;
            }

            for (HostBean host : hostBeans) {
                long last_update = host.getLast_update();
                long launchLatency = current_time - last_update;
                String id = host.getHost_id();
                if (host.getState() == HostState.PROVISIONED) {
                    if (launchLatency >= launchGracePeriod) {
                        maxStaleHostIds.add(id);
                    }
                } else if (host.getState() != HostState.TERMINATING && host.getState() != HostState.PENDING_TERMINATE) {
                    if (launchLatency >= maxStaleHostThreshold) {
                        maxStaleHostIds.add(id);
                    } else if (launchLatency >= minStaleHostThreshold) {
                        minStaleHostIds.add(id);
                    }
                }

                // remove the existing host from hostSet got from group
                cmdbReportedHosts.remove(id);
            }
            page_index++;
        }

        // step 2: check ec2 if stale hosts ips are terminated
        LOG.info("Process max stale hosts: {}, and min stale hosts: {}",
            maxStaleHostIds.toString(), minStaleHostIds.toString());
        processStaleHosts(minStaleHostIds, false);
        processStaleHosts(maxStaleHostIds, true);

        // Check the instance is not launched from Teletraan/Autoscaling
        Set<String> ids = cmdbReportedHosts.keySet();
        Collection<String> terminatedIds = new ArrayList<>(ids);
        for (String hostId : ids) {
            Collection<String> resultIds = rodimusManager.getTerminatedHosts(Collections.singletonList(hostId));
            if (resultIds.isEmpty()) {
                terminatedIds.remove(hostId);
            }
        }

        for (Map.Entry<String, HostBean> host : cmdbReportedHosts.entrySet()) {
            try {
                String id = host.getKey();
                if (terminatedIds.contains(id)) {
                    continue;
                }
                
                String hostName = host.getValue().getHost_name();
                String ip = host.getValue().getIp();
                hostDAO.insertOrUpdate(hostName, ip, id, HostState.PROVISIONED.toString(), groups);
            } catch (Exception ex) {
                // TODO we should find a better way to handle this.
                LOG.error("Failed to get information for {}", host, ex);
            }
        }
    }
}
