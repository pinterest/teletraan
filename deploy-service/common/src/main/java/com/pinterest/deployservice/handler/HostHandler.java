/**
 * Copyright (c) 2021-2023 Pinterest, Inc.
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
package com.pinterest.deployservice.handler;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.dao.AgentDAO;
import com.pinterest.deployservice.dao.HostAgentDAO;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.dao.HostTagDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostHandler {
    private static final Logger LOG = LoggerFactory.getLogger(HostHandler.class);
    private final AgentDAO agentDAO;
    private final HostDAO hostDAO;
    private final HostAgentDAO hostAgentDAO;
    private final HostTagDAO hostTagDAO;

    public HostHandler(ServiceContext serviceContext) {
        agentDAO = serviceContext.getAgentDAO();
        hostDAO = serviceContext.getHostDAO();
        hostAgentDAO = serviceContext.getHostAgentDAO();
        hostTagDAO = serviceContext.getHostTagDAO();
    }

    public void removeHost(String hostId) {
        boolean hasException = false;
        try {
            agentDAO.deleteAllById(hostId);
        } catch (Exception e) {
            hasException = true;
            LOG.error("Failed to remove host record from agent - " + hostId, e);
        }
        try {
            hostTagDAO.deleteByHostId(hostId);
        } catch (Exception e) {
            hasException = true;
            LOG.error("Failed to remove host record from hostTag - " + hostId, e);
        }
        try {
            hostAgentDAO.delete(hostId);
        } catch (Exception e) {
            hasException = true;
            LOG.error("Failed to remove host record from hostAgent - " + hostId, e);
        }
        try {
            hostDAO.deleteAllById(hostId);
        } catch (Exception e) {
            hasException = true;
            LOG.error("Failed to remove host record from host - " + hostId, e);
        }

        if (!hasException) {
            LOG.info("Removed all records for host {}", hostId);
        }
    }
}
