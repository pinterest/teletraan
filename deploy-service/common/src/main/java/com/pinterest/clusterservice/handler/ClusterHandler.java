/*
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
package com.pinterest.clusterservice.handler;


import com.pinterest.arcee.aws.AwsConfigManager;
import com.pinterest.arcee.bean.GroupBean;
import com.pinterest.arcee.dao.GroupInfoDAO;
import com.pinterest.clusterservice.bean.CloudProvider;
import com.pinterest.clusterservice.bean.ClusterBean;
import com.pinterest.clusterservice.bean.ClusterUpgradeEventBean;
import com.pinterest.clusterservice.cm.AwsVmManager;
import com.pinterest.clusterservice.cm.ClusterManager;
import com.pinterest.clusterservice.cm.DefaultClusterManager;
import com.pinterest.clusterservice.dao.ClusterDAO;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.AgentBean;
import com.pinterest.deployservice.bean.AgentState;
import com.pinterest.clusterservice.bean.ClusterState;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.bean.HostState;
import com.pinterest.deployservice.dao.AgentDAO;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.GroupDAO;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.handler.DataHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class ClusterHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterHandler.class);
    private final AgentDAO agentDAO;
    private final ClusterDAO clusterDAO;
    private final EnvironDAO environDAO;
    private final GroupDAO groupDAO;
    private final GroupInfoDAO groupInfoDAO;
    private final HostDAO hostDAO;
    private final DataHandler dataHandler;
    private final ClusterUpgradeEventHandler clusterUpgradeEventHandler;
    private final AwsConfigManager awsConfigManager;
    private final ServiceContext serviceContext;

    public ClusterHandler(ServiceContext serviceContext) {
        this.agentDAO = serviceContext.getAgentDAO();
        this.clusterDAO = serviceContext.getClusterDAO();
        this.environDAO = serviceContext.getEnvironDAO();
        this.groupDAO = serviceContext.getGroupDAO();
        this.groupInfoDAO = serviceContext.getGroupInfoDAO();
        this.hostDAO = serviceContext.getHostDAO();
        this.dataHandler = new DataHandler(serviceContext);
        this.clusterUpgradeEventHandler = new ClusterUpgradeEventHandler(serviceContext);
        this.awsConfigManager = serviceContext.getAwsConfigManager();
        this.serviceContext = serviceContext;
    }

    private ClusterManager createClusterManager(CloudProvider provider) {
        if (provider == CloudProvider.AWS && awsConfigManager != null) {
            return new AwsVmManager(serviceContext);
        } else {
            return new DefaultClusterManager();
        }
    }

    public void createCluster(String envName, String stageName, ClusterBean clusterBean) throws Exception {
        String clusterName = getClusterName(envName, stageName);
        clusterBean.setCluster_name(clusterName);
        clusterBean.setState(ClusterState.NORMAL);
        clusterBean.setLast_update(System.currentTimeMillis());

        ClusterManager clusterManager = createClusterManager(clusterBean.getProvider());
        clusterManager.createCluster(clusterName, clusterBean);

        clusterDAO.insert(clusterBean);
        EnvironBean environBean = environDAO.getByStage(envName, stageName);
        groupDAO.addGroupCapacity(environBean.getEnv_id(), clusterName);

        // Create a default advanced group bean
        GroupBean groupBean = new GroupBean();
        groupBean.setGroup_name(clusterName);
        groupBean.setLast_update(System.currentTimeMillis());
        groupInfoDAO.insertGroupInfo(groupBean);
    }

    public void updateCluster(String envName, String stageName, ClusterBean clusterBean) throws Exception {
        String clusterName = getClusterName(envName, stageName);
        ClusterManager clusterManager = createClusterManager(clusterBean.getProvider());
        clusterManager.updateCluster(clusterName, clusterBean);
        updateClusterInternal(envName, stageName, clusterBean);
    }

    public ClusterBean getCluster(String envName, String stageName) throws Exception {
        String clusterName = getClusterName(envName, stageName);
        return clusterDAO.getByClusterName(clusterName);
    }

    public void deleteCluster(String envName, String stageName) throws Exception {
        String clusterName = getClusterName(envName, stageName);
        ClusterBean clusterBean = clusterDAO.getByClusterName(clusterName);

        ClusterManager clusterManager = createClusterManager(clusterBean.getProvider());
        clusterManager.deleteCluster(clusterName);

        clusterDAO.delete(clusterName);
        dataHandler.deleteData(clusterBean.getConfig_id());

        EnvironBean environBean = environDAO.getByStage(envName, stageName);
        groupDAO.removeGroupCapacity(environBean.getEnv_id(), clusterName);
    }

    public void replaceCluster(String envName, String stageName) throws Exception {
        String clusterName = getClusterName(envName, stageName);
        // Step 1. Create one cluster upgrade event
        EnvironBean environBean = environDAO.getByStage(envName, stageName);
        ClusterUpgradeEventBean eventBean = new ClusterUpgradeEventBean();
        eventBean.setCluster_name(clusterName);
        eventBean.setEnv_id(environBean.getEnv_id());
        clusterUpgradeEventHandler.createClusterUpgradeEvent(eventBean);

        // Step 2. Update host can_retire
        HostBean hostBean = new HostBean();
        hostBean.setCan_retire(true);
        hostBean.setLast_update(System.currentTimeMillis());
        hostDAO.updateHostByGroup(clusterName, hostBean);

        // Step 3. Update cluster state
        enableReplace(envName, stageName);
    }

    public String updateAdvancedConfigs(String envName, String stageName, Map<String, String> configs, String operator) throws Exception {
        String clusterName = getClusterName(envName, stageName);
        ClusterBean clusterBean = clusterDAO.getByClusterName(clusterName);
        if (clusterBean == null || clusterBean.getConfig_id() == null) {
            return dataHandler.insertMap(configs, operator);
        }

        String configId = clusterBean.getConfig_id();
        if (configId != null) {
            dataHandler.updateMap(configId, configs, operator);
        }
        return configId;
    }

    public Map<String, String> getAdvancedConfigs(String envName, String stageName) throws Exception {
        String clusterName = getClusterName(envName, stageName);
        ClusterBean clusterBean = clusterDAO.getByClusterName(clusterName);
        if (clusterBean == null) {
            return Collections.emptyMap();
        }
        return dataHandler.getMapById(clusterBean.getConfig_id());
    }

    public void launchHosts(String envName, String stageName, int num) throws Exception {
        String clusterName = getClusterName(envName, stageName);
        if (num <= 0) {
            LOG.error(String.format("Failed to launch %d hosts to auto scaling group %s: number cannot be negative", num, clusterName));
            throw new Exception(String.format("Failed to launch %d hosts to auto scaling group %s: number cannot be negative", num, clusterName));
        }

        ClusterBean clusterBean = clusterDAO.getByClusterName(clusterName);
        ClusterManager clusterManager = createClusterManager(clusterBean.getProvider());
        clusterManager.launchHosts(clusterName, num, true);

        ClusterBean updateBean = new ClusterBean();
        updateBean.setCapacity(clusterBean.getCapacity() + num);
        updateClusterInternal(envName, stageName, updateBean);
    }

    public void stopHosts(String envName, String stageName, Collection<String> hostIds) throws Exception {
        String clusterName = getClusterName(envName, stageName);
        stopHostsByClusterName(clusterName, hostIds);
    }

    public void stopHostsByClusterName(String clusterName, Collection<String> hostIds) throws Exception {
        LOG.info(String.format("Start to gracefully shutdown hosts %s in cluster %s", hostIds.toString(), clusterName));
        for (String hostId : hostIds) {
            AgentBean agentBean = new AgentBean();
            agentBean.setState(AgentState.STOP);
            agentBean.setLast_update(System.currentTimeMillis());
            agentDAO.updateAgentById(hostId, agentBean);

            HostBean hostBean = new HostBean();
            hostBean.setState(HostState.PENDING_TERMINATE);
            hostBean.setLast_update(System.currentTimeMillis());
            hostDAO.updateHostById(hostId, hostBean);
        }
    }

    public void terminateHosts(String envName, String stageName, Collection<String> hostIds) throws Exception {
        String clusterName = getClusterName(envName, stageName);
        terminateHostsByClusterName(clusterName, hostIds);
    }

    public void terminateHostsByClusterName(String clusterName, Collection<String> hostIds) throws Exception {
        // TODO based on provider to create different factory
        ClusterManager clusterManager = createClusterManager(CloudProvider.AWS);
        clusterManager.terminateHosts(clusterName, hostIds, true);

        for (String hostId : hostIds) {
            HostBean hostBean = new HostBean();
            hostBean.setState(HostState.TERMINATING);
            hostBean.setLast_update(System.currentTimeMillis());
            hostDAO.updateHostById(hostId, hostBean);
        }
    }

    public Collection<String> getHostNames(String envName, String stageName) throws Exception {
        String clusterName = getClusterName(envName, stageName);
        return hostDAO.getHostNamesByGroup(clusterName);
    }

    public void pauseReplace(String envName, String stageName) throws Exception {
        ClusterBean clusterBean = new ClusterBean();
        clusterBean.setState(ClusterState.PAUSE);
        updateClusterInternal(envName, stageName, clusterBean);
    }

    public void enableReplace(String envName, String stageName) throws Exception {
        ClusterBean clusterBean = new ClusterBean();
        clusterBean.setState(ClusterState.REPLACE);
        updateClusterInternal(envName, stageName, clusterBean);
    }

    public void cancelReplace(String envName, String stageName) throws Exception {
        ClusterBean updateBean = new ClusterBean();
        updateBean.setState(ClusterState.NORMAL);
        updateClusterInternal(envName, stageName, updateBean);
        clusterUpgradeEventHandler.abortClusterUpgradeEvents(getClusterName(envName, stageName));
    }

    private String getClusterName(String envName, String stageName) {
        return String.format("%s-%s", envName, stageName);
    }

    private void updateClusterInternal(String envName, String stageName, ClusterBean updateBean) throws Exception {
        String clusterName = getClusterName(envName, stageName);
        updateBean.setCluster_name(clusterName);
        updateBean.setLast_update(System.currentTimeMillis());
        clusterDAO.update(clusterName, updateBean);
    }
}
