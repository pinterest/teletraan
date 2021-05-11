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
import com.pinterest.deployservice.bean.HostAgentBean;
import com.pinterest.deployservice.bean.HostState;
import com.pinterest.deployservice.common.Constants;
import com.pinterest.deployservice.dao.AgentDAO;
import com.pinterest.deployservice.dao.GroupDAO;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.dao.HostAgentDAO;
import com.pinterest.deployservice.handler.HostHandler;
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
    protected HostAgentDAO hostAgentDAO;
    private GroupDAO groupDAO;
    private HostHandler hostHandler;
    protected long maxStaleHostThreshold;
    protected long minStaleHostThreshold;

    public SimpleAgentJanitor(ServiceContext serviceContext, int minStaleHostThreshold,
        int maxStaleHostThreshold) {
        agentDAO = serviceContext.getAgentDAO();
        hostDAO = serviceContext.getHostDAO();
        hostAgentDAO = serviceContext.getHostAgentDAO();
        groupDAO = serviceContext.getGroupDAO();
        hostHandler = new HostHandler(serviceContext);
        this.maxStaleHostThreshold = maxStaleHostThreshold * 1000;
        this.minStaleHostThreshold = minStaleHostThreshold * 1000;
    }

    // remove the stale host from db
    void removeStaleHost(String id) throws Exception {
        LOG.info(String.format("Delete records of stale host {}", id));
        try {
            hostHandler.removeHost(id);
        } catch (Exception e) {
            LOG.error("Failed to delete all records for host {}. exception {}", id, e);
        }
    }

    void markUnreachableHost(String id) throws Exception {
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

    private void processStaleHosts(Collection<String> staleHostIds, boolean needToDelete) throws Exception {
        for (String staleId : staleHostIds) {
            if (needToDelete) {
                removeStaleHost(staleId);
            } else {
                markUnreachableHost(staleId);
            }
        }
    }

    void processAllHosts() throws Exception {
        LOG.info("Process explicite capacity hosts");
        // If a host fails to ping for longer than max stale threshold,
        // then just remove it
        long current_time = System.currentTimeMillis();
        long maxThreshold = current_time - maxStaleHostThreshold;
        List<HostAgentBean> maxStaleHosts = hostAgentDAO.getStaleEnvHosts(maxThreshold);
        Set<String> maxStaleHostIds = new HashSet<>();
        for (HostAgentBean host : maxStaleHosts) {
            maxStaleHostIds.add(host.getHost_id());
        }
        if (!maxStaleHostIds.isEmpty()) {
            LOG.info("Found the following hosts (Explicite capacity) exceeded maxStaleThreshold: ",
                maxStaleHostIds);
            processStaleHosts(maxStaleHostIds, true);
        }

        // If host fails to ping for longer than min stale threshold,
        // then mark them as UNREACHABLE
        current_time = System.currentTimeMillis();
        long minThreshold = current_time - minStaleHostThreshold;
        List<HostAgentBean> minStaleHosts = hostAgentDAO.getStaleEnvHosts(minThreshold);
        Set<String> minStaleHostIds = new HashSet<>();
        for (HostAgentBean host : minStaleHosts) {
            minStaleHostIds.add(host.getHost_id());
        }
        if (!minStaleHostIds.isEmpty()) {
            LOG.info("Found following hosts (Explicite capacity) excceeded minStaleThreshold: ",
                minStaleHostIds);
            processStaleHosts(minStaleHostIds, false);
        }
    }

    @Override
    public void run() {
        try {
            LOG.info("Start simple agent janitor process...");
            processAllHosts();
        } catch (Throwable t) {
            // Catch all throwable so that subsequent job not suppressed
            LOG.error("SimpleAgentJanitor Failed.", t);
        }
    }
}
