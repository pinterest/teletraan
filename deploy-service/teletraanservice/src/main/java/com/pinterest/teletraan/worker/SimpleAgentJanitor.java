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
import com.pinterest.deployservice.bean.AgentState;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.bean.HostState;
import com.pinterest.deployservice.common.Constants;
import com.pinterest.deployservice.dao.AgentDAO;
import com.pinterest.deployservice.dao.GroupDAO;
import com.pinterest.deployservice.dao.HostDAO;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Housekeeping on stuck and dead agents
 * <p>
 * if an agent has not ping server for certain time, we will cross check with
 * authoritive source to confirm if the host is terminated, and set agent status
 * accordingly
 */
public class SimpleAgentJanitor implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleAgentJanitor.class);
    private AgentDAO agentDAO;
    protected HostDAO hostDAO;
    private GroupDAO groupDAO;
    protected long maxStaleHostThreshold;
    protected long minStaleHostThreshold;

    public SimpleAgentJanitor(ServiceContext serviceContext, int minStaleHostThreshold,
        int maxStaleHostThreshold) {
        agentDAO = serviceContext.getAgentDAO();
        hostDAO = serviceContext.getHostDAO();
        groupDAO = serviceContext.getGroupDAO();
        this.maxStaleHostThreshold = maxStaleHostThreshold * 1000;
        this.minStaleHostThreshold = minStaleHostThreshold * 1000;
    }

    // remove the stale host from db
    void removeStaleHost(String id) throws Exception {
        try {
            hostDAO.deleteAllById(id);
            LOG.info("AgentJanitor delete all records for host {}.", id);
        } catch (Exception e) {
            LOG.error("AgentJanitor Failed to delete all records for host {}", id, e);
        }
    }

    void markUnreachableHost(String id) throws Exception {
        try {
            // mark the agent as unreachable
            AgentBean updateBean = new AgentBean();
            updateBean.setState(AgentState.UNREACHABLE);
            agentDAO.updateAgentById(id, updateBean);
            LOG.info("AgentJanitor marked agent {} as UNREACHABLE.", id);
        } catch (Exception e) {
            LOG.error("SimpleAgentJanitor Failed to mark host {} as UNREACHABLE", id, e);
        }
    }

    private void processStaleHosts(Collection<String> staleHostIds, boolean needToDelete) throws Exception {
        for (String staleId : staleHostIds) {
            if (needToDelete) {
                removeStaleHost(staleId);
            } else {
                markUnreachableHost(staleId);
            }
        }
    }

    void processIndividualHosts() throws Exception {
        // If a host fails to ping for longer than max stale threshold,
        // then just remove it
        long current_time = System.currentTimeMillis();
        long maxThreshold = current_time - maxStaleHostThreshold;
        List<HostBean> maxStaleHosts = hostDAO.getStaleEnvHosts(maxThreshold);
        Set<String> maxStaleHostIds = new HashSet<>();
        for (HostBean host : maxStaleHosts) {
            maxStaleHostIds.add(host.getHost_id());
        }
        if (!maxStaleHostIds.isEmpty()) {
            LOG.info("AgentJanitor found the following hosts (Explicite capacity) exceeded maxStaleThreshold: ",
                maxStaleHostIds);
            processStaleHosts(maxStaleHostIds, true);
        }

        // If host fails to ping for longer than min stale threshold,
        // then mark them as UNREACHABLE
        current_time = System.currentTimeMillis();
        long minThreshold = current_time - minStaleHostThreshold;
        List<HostBean> minStaleHosts = hostDAO.getStaleEnvHosts(minThreshold);
        Set<String> minStaleHostIds = new HashSet<>();
        for (HostBean host : minStaleHosts) {
            minStaleHostIds.add(host.getHost_id());
        }
        if (!minStaleHostIds.isEmpty()) {
            LOG.info("AgentJanitor found following hosts (Explicite capacity) excceeded minStaleThreshold: ",
                minStaleHostIds);
            processStaleHosts(minStaleHostIds, false);
        }
    }

    void processEachGroup(String groupName) throws Exception {
        Set<String> maxStaleHostIds = new HashSet<>();
        Set<String> minStaleHostIds = new HashSet<>();
        int size = 1000;
        for (int i = 1; ; i++) {
            List<HostBean> hostBeans = hostDAO.getHostsByGroup(groupName, i, size);
            if (CollectionUtils.isEmpty(hostBeans)) {
                break;
            }
            long current_time = System.currentTimeMillis();
            for (HostBean host : hostBeans) {
                long last_update = host.getLast_update();
                String id = host.getHost_id();
                if (current_time - last_update >= maxStaleHostThreshold) {
                    maxStaleHostIds.add(id);
                } else if (current_time - last_update >= minStaleHostThreshold) {
                    minStaleHostIds.add(id);
                }
            }
            if (hostBeans.size() < size) {
                break;
            }
        }

        if (!maxStaleHostIds.isEmpty()) {
            LOG.info("AgentJanitor found the following hosts excceeded maxStaleThreshold: ",
                maxStaleHostIds);
            processStaleHosts(maxStaleHostIds, true);
        }
        if (!minStaleHostIds.isEmpty()) {
            LOG.info("AgentJanitor found the following hosts excceeded minStaleThreshold: ",
                minStaleHostIds);
            processStaleHosts(minStaleHostIds, false);
        }
    }

    void processBatch() throws Exception {
        // First, check those groups associates with certain envs
        List<String> groups = groupDAO.getAllEnvGroups();
        // Manually inject the NULL group
        groups.add(Constants.NULL_HOST_GROUP);
        Collections.shuffle(groups);
        for (String group : groups) {
            try {
                LOG.info("AgentJanitor process group: {}", group);
                processEachGroup(group);
            } catch (Exception e) {
                LOG.error("SimpleAgentJanitor failed to process group: {}", group, e);
            }
        }

        // For those hosts do not belong to any host, but still associate with certain envs
        LOG.info("AgentJanitor process explicite capacity hosts");
        processIndividualHosts();
    }

    @Override
    public void run() {
        try {
            LOG.info("AgentJanitor Start simple agent janitor process...");
            processBatch();
        } catch (Throwable t) {
            // Catch all throwable so that subsequent job not suppressed
            LOG.error("SimpleAgentJanitor Failed.", t);
        }
    }
}
