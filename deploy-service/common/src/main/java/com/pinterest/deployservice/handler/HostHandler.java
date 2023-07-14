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
        try {
            hostDAO.deleteAllById(hostId);
            agentDAO.deleteAllById(hostId);
            hostTagDAO.deleteByHostId(hostId);
            hostAgentDAO.delete(hostId);
            LOG.info("Removed all records for the host {}", hostId);
        } catch (Exception e) {
            LOG.error("Failed to remove all records for the host {}, exception: {}", hostId, e);
        }
    }
}
