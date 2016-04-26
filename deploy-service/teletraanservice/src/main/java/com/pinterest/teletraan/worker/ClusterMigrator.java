/*
 * Copyright 2016 Pinterest, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pinterest.teletraan.worker;


import com.pinterest.clusterservice.bean.ClusterBean;
import com.pinterest.clusterservice.dao.ClusterDAO;
import com.pinterest.clusterservice.handler.ClusterHandler;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.dao.AgentDAO;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.dao.UtilDAO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ClusterMigrator implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterMigrator.class);
    private final AgentDAO agentDAO;
    private final ClusterDAO clusterDAO;
    private final EnvironDAO environDAO;
    private final HostDAO hostDAO;
    private final UtilDAO utilDAO;
    private final ClusterHandler clusterHandler;

    public ClusterMigrator(ServiceContext serviceContext) {
        agentDAO = serviceContext.getAgentDAO();
        clusterDAO = serviceContext.getClusterDAO();
        environDAO = serviceContext.getEnvironDAO();
        hostDAO = serviceContext.getHostDAO();
        utilDAO = serviceContext.getUtilDAO();
        clusterHandler = new ClusterHandler(serviceContext);
    }

    private boolean canProceed(EnvironBean environBean) throws Exception {
        String envId = environBean.getEnv_id();
        long capacityTotal = environDAO.countTotalCapacity(envId, environBean.getEnv_name(), environBean.getStage_name());
        long totalAgents = agentDAO.getAllByEnv(envId).size();
        long totalNotServingAgents = agentDAO.countNotServingAgent(envId);
        LOG.debug(String.format("Number of newly provisioned hosts: %s, number of not serving agents: %s", capacityTotal - totalAgents, totalNotServingAgents));
        // Not serving agents + newly provisioned hosts
        return (totalNotServingAgents + capacityTotal - totalAgents) <= environBean.getMax_parallel();
    }

    private void replaceCluster(String source) throws Exception {
        List<EnvironBean> envs = environDAO.getEnvsByGroups(Collections.singletonList(source));
        EnvironBean environBean = null;
        for (EnvironBean env : envs) {
            // Find the migration destination
            ClusterBean clusterBean = clusterHandler.getCluster(env.getEnv_name(), env.getStage_name());
            if (clusterBean != null) {
                environBean = env;
            }
        }

        if (environBean == null) {
            LOG.error(String.format("Cannot find destination cluster for retired cluster %s", source));
            return;
        }

        if (!canProceed(environBean)) {
            LOG.debug(String.format("Cannot proceed to stop host in cluster %s", source));
            return;
        }

        ClusterBean clusterBean = clusterDAO.getByClusterName(source);
        if (clusterBean == null) {
            clusterHandler.launchHosts(environBean.getEnv_name(), environBean.getStage_name(), 1);
        }

        Collection<HostBean> hostBeans = hostDAO.getActiveAndRetiredHostsByGroup(source);
        if (hostBeans.isEmpty()) {
            return;
        }
        HostBean hostBean = hostBeans.iterator().next();
        clusterHandler.stopHosts(environBean.getEnv_name(), environBean.getStage_name(), Collections.singletonList(hostBean.getHost_id()));
    }


    private void processBatch() throws Exception {
        Collection<String> clusterNames = hostDAO.getRetiredGroupNames();
        for (String clusterName : clusterNames) {
            String lockName = String.format("CLUSTERMIGRATOR-%s", clusterName);
            Connection connection = utilDAO.getLock(lockName);
            if (connection != null) {
                try {
                    replaceCluster(clusterName);
                } catch (Exception e) {
                    LOG.error(String.format("Failed to process cluster %s", clusterName), e);
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
            LOG.info("Start to run ClusterMigrator");
            processBatch();
        } catch (Throwable t) {
            LOG.error("Faile to run ClusterMigrator", t);
        }
    }
}
