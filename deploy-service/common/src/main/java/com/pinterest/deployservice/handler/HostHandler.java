package com.pinterest.deployservice.handler;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.dao.AgentDAO;
import com.pinterest.deployservice.dao.HostAgentDAO;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.dao.HostTagDAO;

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
            hostDAO.deleteAllById(hostId);
        } catch (Exception e) {
            hasException = true;
            LOG.error("Failed to remove host record from host - " + hostId, e);
        }
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

        if (!hasException) {
            LOG.info("Removed all records for host {}", hostId);
        }
    }

}
