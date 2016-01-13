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


import com.pinterest.deployservice.bean.ASGStatus;
import com.pinterest.arcee.bean.GroupBean;
import com.pinterest.arcee.dao.GroupInfoDAO;
import com.pinterest.arcee.dao.HostInfoDAO;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.bean.HostState;
import com.pinterest.deployservice.group.HostGroupManager;
import com.pinterest.deployservice.dao.UtilDAO;
import com.pinterest.deployservice.handler.CommonHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
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
    private HostInfoDAO hostInfoDAO;
    private HostGroupManager hostGroupDAO;
    private GroupInfoDAO groupInfoDAO;
    private UtilDAO utilDAO;
    private long maxLaunchLatencyThreshold;

    public AgentJanitor(ServiceContext serviceContext, int minStaleHostThreshold,
        int maxStaleHostThreshold, int maxLaunchLatencyThreshold) {
        super(serviceContext, minStaleHostThreshold, maxStaleHostThreshold);
        hostInfoDAO = serviceContext.getHostInfoDAO();
        hostGroupDAO = serviceContext.getHostGroupDAO();
        groupInfoDAO = serviceContext.getGroupInfoDAO();
        utilDAO = serviceContext.getUtilDAO();
        this.maxLaunchLatencyThreshold = maxLaunchLatencyThreshold * 1000;
    }

    private void processStaleHosts(Collection<String> staleHostIds, boolean isMarkedUnreachable) throws Exception {
        if (staleHostIds.isEmpty()) {
            return;
        }
        Set<String> terminatedHosts = hostInfoDAO.getTerminatedHosts(new HashSet<>(staleHostIds));
        if (isMarkedUnreachable) {
            for (String staleId : staleHostIds) {
                if (terminatedHosts.contains(staleId)) {
                    removeStaleHost(staleId);
                } else {
                    // TODO this lock can be removed, once we move the terminating logic from state to status
                    HostBean host = hostDAO.getHostsByHostId(staleId).get(0);
                    if (host.getState() != HostState.TERMINATING) {
                        String lockName = String.format("PROCESS-HOSTID-%s", staleId);
                        Connection connection = utilDAO.getLock(lockName);
                        if (connection != null) {
                            try {
                                markUnreachableHost(staleId);
                            } catch (Exception e) {
                                LOG.error("AgentJanitor Failed to mark host {} as UNREACHABLE", staleId, e);
                            } finally {
                                utilDAO.releaseLock(lockName, connection);
                            }
                        } else {
                            LOG.warn(String.format("Failed to get lock: %s", lockName));
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
            } else if (host.getState() != HostState.TERMINATING) {
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
        GroupBean groupBean = groupInfoDAO.getGroupInfo(groupName);
        if (groupBean == null) {
            LOG.debug("Group Name {} does not exist in groups table.", groupName);
        }

        long current_time = System.currentTimeMillis();
        while (true) {
            List<HostBean> hostBeans = hostDAO.getHostsByGroup(groupName, page_index, page_size);
            if (hostBeans.isEmpty()) {
                break;
            }

            for (HostBean host : hostBeans) {
                long last_update = host.getLast_update();
                String id = host.getHost_id();
                if (host.getState() == HostState.PROVISIONED) {
                    if (groupBean == null) {
                        // Not in autoscaling group
                        if (current_time - last_update >= maxStaleHostThreshold) {
                            maxStaleHostIds.add(id);
                        }
                    } else {
                        // If it is in autoscaling group, use launch latency threshold
                        if (current_time - last_update >= (long) groupBean.getLaunch_latency_th() * 1000) {
                            maxStaleHostIds.add(id);
                        }
                    }
                } else if (host.getState() != HostState.TERMINATING) {
                    if (current_time - last_update >= maxStaleHostThreshold) {
                        maxStaleHostIds.add(id);
                    } else if (current_time - last_update >= minStaleHostThreshold) {
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

        // we don't check CMDB for auto scaling group
        if (groupBean != null && groupBean.getAsg_status() != null && groupBean.getAsg_status() == ASGStatus.ENABLED) {
            return;
        }

        // Check the instance is not launched from Teletraan/Autoscaling
        Set<String> ids = cmdbReportedHosts.keySet();
        Set<String> terminatedIds = hostInfoDAO.getTerminatedHosts(ids);

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
