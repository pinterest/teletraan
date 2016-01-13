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

import com.pinterest.arcee.dao.HostInfoDAO;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.bean.HostState;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.dao.UtilDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.*;

public class HostTerminator implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(HostTerminator.class);
    private static final long timeToRetain = 2 * 60 * 60 * 1000;
    private final HostDAO hostDAO;
    private final HostInfoDAO hostInfoDAO;
    private final UtilDAO utilDAO;

    public HostTerminator(ServiceContext serviceContext) {
        hostDAO = serviceContext.getHostDAO();
        hostInfoDAO = serviceContext.getHostInfoDAO();
        utilDAO = serviceContext.getUtilDAO();
    }

    void terminateHost(HostBean host) throws Exception {
        if ((System.currentTimeMillis() - host.getLast_update()) > timeToRetain) {
            String hostId = host.getHost_id();
            LOG.info(String.format("This host %s has been retained for more than %d milliseconds. Terminate it", hostId, timeToRetain));
            hostInfoDAO.terminateHost(hostId);

            HostBean hostBean = new HostBean();
            hostBean.setState(HostState.TERMINATING);
            hostBean.setLast_update(System.currentTimeMillis());
            hostDAO.updateHostById(hostId, hostBean);
        }
    }

    void removeTerminatedHost(HostBean host) throws Exception {
        // Check whether the host state is TERMINATED_CODE on AWS
        String hostId = host.getHost_id();
        Set<String> terminatedHosts = hostInfoDAO.getTerminatedHosts(new HashSet<>(Arrays.asList(hostId)));
        if (terminatedHosts.contains(hostId)) {
            LOG.info(String.format("Delete %s in host and agent table", hostId));
            hostDAO.deleteAllById(hostId);
        }
    }

    public void processBatch() throws Exception {
        List<HostBean> hosts = hostDAO.getTerminatingHosts();
        Collections.shuffle(hosts);
        for (HostBean host : hosts) {
            String lockName = String.format("HOSTTERMINATOR-%s", host.getHost_id());
            Connection connection = utilDAO.getLock(lockName);
            if (connection != null) {
                try {
                    if (host.getState() == HostState.PENDING_TERMINATE) {
                        terminateHost(host);
                    } else if (host.getState() == HostState.TERMINATING) {
                        removeTerminatedHost(host);
                    }
                } catch (Exception e) {
                    LOG.error("Failed to process {} host {}", host.getState().toString(), host.getHost_id(), e);
                } finally {
                    utilDAO.releaseLock(lockName, connection);
                }
            } else {
                LOG.warn(String.format("Failed to get lock: %s", lockName));
            }
        }
    }

    @Override
    public void run() {
        try {
            LOG.info("Start to run HostTerminator");
            processBatch();
        } catch (Throwable t) {
            LOG.error("Faile to run HostTerminator", t);
        }
    }
}
