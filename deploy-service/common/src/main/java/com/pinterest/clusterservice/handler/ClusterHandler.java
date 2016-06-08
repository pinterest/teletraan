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


import com.google.common.base.Joiner;

import com.pinterest.arcee.aws.AwsConfigManager;
import com.pinterest.arcee.bean.GroupBean;
import com.pinterest.arcee.dao.GroupInfoDAO;
import com.pinterest.clusterservice.bean.BaseImageBean;
import com.pinterest.clusterservice.bean.CloudProvider;
import com.pinterest.clusterservice.bean.ClusterBean;
import com.pinterest.clusterservice.bean.ClusterInfoBean;
import com.pinterest.clusterservice.bean.ClusterUpgradeEventBean;
import com.pinterest.clusterservice.bean.HostTypeBean;
import com.pinterest.clusterservice.bean.PlacementBean;
import com.pinterest.clusterservice.bean.SecurityZoneBean;
import com.pinterest.clusterservice.cm.AwsVmManager;
import com.pinterest.clusterservice.cm.ClusterManager;
import com.pinterest.clusterservice.cm.DefaultClusterManager;
import com.pinterest.clusterservice.dao.BaseImageDAO;
import com.pinterest.clusterservice.dao.ClusterDAO;
import com.pinterest.clusterservice.dao.HostTypeDAO;
import com.pinterest.clusterservice.dao.PlacementDAO;
import com.pinterest.clusterservice.dao.SecurityZoneDAO;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.AgentBean;
import com.pinterest.deployservice.bean.AgentState;
import com.pinterest.clusterservice.bean.ClusterState;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.bean.HostState;
import com.pinterest.deployservice.common.Constants;
import com.pinterest.deployservice.dao.AgentDAO;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.GroupDAO;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.handler.DataHandler;
import com.pinterest.deployservice.handler.EnvironHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ClusterHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterHandler.class);
    private final AgentDAO agentDAO;
    private final BaseImageDAO baseImageDAO;
    private final ClusterDAO clusterDAO;
    private final EnvironDAO environDAO;
    private final GroupDAO groupDAO;
    private final GroupInfoDAO groupInfoDAO;
    private final HostDAO hostDAO;
    private final HostTypeDAO hostTypeDAO;
    private final PlacementDAO placementDAO;
    private final SecurityZoneDAO securityZoneDAO;
    private final EnvironHandler environHandler;
    private final DataHandler dataHandler;
    private final ClusterUpgradeEventHandler clusterUpgradeEventHandler;
    private final AwsConfigManager awsConfigManager;
    private final ServiceContext serviceContext;

    public ClusterHandler(ServiceContext serviceContext) {
        this.agentDAO = serviceContext.getAgentDAO();
        this.baseImageDAO = serviceContext.getBaseImageDAO();
        this.clusterDAO = serviceContext.getClusterDAO();
        this.environDAO = serviceContext.getEnvironDAO();
        this.groupDAO = serviceContext.getGroupDAO();
        this.groupInfoDAO = serviceContext.getGroupInfoDAO();
        this.hostDAO = serviceContext.getHostDAO();
        this.hostTypeDAO = serviceContext.getHostTypeDAO();
        this.placementDAO = serviceContext.getPlacementDAO();
        this.securityZoneDAO = serviceContext.getSecurityZoneDAO();
        this.environHandler = new EnvironHandler(serviceContext);
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

    public void createCluster(String envName, String stageName, ClusterInfoBean clusterInfoBean, String operator) throws Exception {
        //1. Create cluster
        String clusterName = String.format("%s-%s", envName, stageName);
        clusterInfoBean.setClusterName(clusterName);
        clusterInfoBean.setState(ClusterState.NORMAL);
        ClusterBean updateClusterBean = mappingToClusterBean(clusterName, clusterInfoBean, operator);
        ClusterManager clusterManager = createClusterManager(clusterInfoBean.getProvider());
        clusterManager.createCluster(clusterName, updateClusterBean);

        //2. Save cluster name and isDocker
        EnvironBean environBean = environDAO.getByStage(envName, stageName);
        EnvironBean updateEnvBean = new EnvironBean();
        if (clusterInfoBean.getIsDocker() != null) {
            updateEnvBean.setIs_docker(clusterInfoBean.getIsDocker());
        }
        updateEnvBean.setCluster_name(clusterName);
        environHandler.updateStage(environBean, updateEnvBean, operator);

        //3. Create a default advanced group bean and save it to capacity
        groupDAO.addGroupCapacity(environBean.getEnv_id(), clusterName);
        GroupBean groupBean = new GroupBean();
        groupBean.setGroup_name(clusterName);
        groupBean.setLast_update(System.currentTimeMillis());
        groupInfoDAO.insertOrUpdateGroupInfo(clusterName, groupBean);
    }

    public void updateCluster(String envName, String stageName, ClusterInfoBean clusterInfoBean, String operator) throws Exception {
        //1. Update Cluster
        String clusterName = getClusterName(envName, stageName);
        ClusterBean updateClusterBean = mappingToClusterBean(clusterName, clusterInfoBean, operator);
        ClusterManager clusterManager = createClusterManager(clusterInfoBean.getProvider());
        clusterManager.updateCluster(clusterName, updateClusterBean);

        //2. update isDocker
        EnvironBean environBean = environDAO.getByStage(envName, stageName);
        EnvironBean updateEnvBean = new EnvironBean();
        if (clusterInfoBean.getIsDocker() != null) {
            updateEnvBean.setIs_docker(clusterInfoBean.getIsDocker());
        }
        environHandler.updateStage(environBean, updateEnvBean, operator);
    }

    public ClusterInfoBean getCluster(String envName, String stageName) throws Exception {
        String clusterName = getClusterName(envName, stageName);
        return mappingToClusterInfoBean(clusterName);
    }

    public void deleteCluster(String envName, String stageName) throws Exception {
        //1. Remove advanced group bean and remove it from capacity
        EnvironBean environBean = environDAO.getByStage(envName, stageName);
        String clusterName = environBean.getCluster_name();
        groupDAO.removeGroupCapacity(environBean.getEnv_id(), clusterName);
        groupInfoDAO.removeGroup(clusterName);

        //2. Remove cluster name and update isDocker to false
        EnvironBean updateEnvBean = new EnvironBean();
        updateEnvBean.setCluster_name("");
        updateEnvBean.setIs_docker(false);
        environHandler.updateStage(environBean, updateEnvBean, Constants.SYSTEM_OPERATOR);

        //3. Delete cluster
        ClusterBean clusterBean = clusterDAO.getByClusterName(clusterName);
        ClusterManager clusterManager = createClusterManager(clusterBean.getProvider());
        clusterManager.deleteCluster(clusterName);
        clusterDAO.delete(clusterName);
        dataHandler.deleteData(clusterBean.getConfig_id());
    }

    public void replaceCluster(String envName, String stageName) throws Exception {
        // Step 1. Create one cluster upgrade event
        EnvironBean environBean = environDAO.getByStage(envName, stageName);
        String clusterName = environBean.getCluster_name();
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

    private String getClusterName(String envName, String stageName) throws Exception {
        EnvironBean environBean = environDAO.getByStage(envName, stageName);
        return environBean.getCluster_name();
    }

    private void updateClusterInternal(String envName, String stageName, ClusterBean updateBean) throws Exception {
        String clusterName = getClusterName(envName, stageName);
        updateBean.setCluster_name(clusterName);
        updateBean.setLast_update(System.currentTimeMillis());
        clusterDAO.update(clusterName, updateBean);
    }

    private ClusterBean mappingToClusterBean(String clusterName, ClusterInfoBean clusterInfoBean, String operator) throws Exception {
        ClusterBean oldClusterBean = clusterDAO.getByClusterName(clusterName);
        CloudProvider provider = clusterInfoBean.getProvider();
        ClusterBean updateClusterBean = new ClusterBean();
        if (clusterInfoBean.getBaseImageId() != null) {
            updateClusterBean.setBase_image_id(clusterInfoBean.getBaseImageId());
        } else if (oldClusterBean == null) {
            LOG.error(String.format("Failed to create cluster %s: null image", clusterName));
            throw new Exception(String.format("Failed to create cluster %s: null image", clusterName));
        }

        if (clusterInfoBean.getHostType() != null) {
            String hostType = clusterInfoBean.getHostType();
            HostTypeBean hostTypeBean = hostTypeDAO.getByProviderAndAbstractName(provider.toString(), hostType);
            if (hostTypeBean == null) {
                LOG.error(String.format("Failed to create cluster %s: invalid host type %s", clusterName, hostType));
                throw new Exception(String.format("Failed to create cluster %s: invalid host type %s", clusterName, hostType));
            }
            updateClusterBean.setHost_type_id(hostTypeBean.getId());
        } else if (oldClusterBean == null) {
            LOG.error(String.format("Failed to create cluster %s: null host type", clusterName));
            throw new Exception(String.format("Failed to create cluster %s: null host type", clusterName));
        }

        if (clusterInfoBean.getSecurityZone() != null) {
            String securityZone = clusterInfoBean.getSecurityZone();
            SecurityZoneBean securityZoneBean = securityZoneDAO.getByProviderAndAbstractName(provider.toString(), securityZone);
            if (securityZoneBean == null) {
                LOG.error(String.format("Failed to create cluster %s: invalid security zone %s", clusterName, securityZone));
                throw new Exception(String.format("Failed to create cluster %s: invalid security zone %s", clusterName, securityZone));
            }
            updateClusterBean.setSecurity_zone_id(securityZoneBean.getId());
        } else if (oldClusterBean == null) {
            LOG.error(String.format("Failed to create cluster %s: null security zone", clusterName));
            throw new Exception(String.format("Failed to create cluster %s: null security zone", clusterName));
        }

        if (clusterInfoBean.getPlacement() != null) {
            List<String> placements = Arrays.asList(clusterInfoBean.getPlacement().split(","));
            List<String> placementIds = new ArrayList<>();
            List<String> placementNames = new ArrayList<>();
            for (String placement : placements) {
                PlacementBean placementBean = placementDAO.getByProviderAndAbstractName(provider.toString(), placement);
                if (placementBean != null) {
                    placementIds.add(placementBean.getId());
                    placementNames.add(placementBean.getProvider_name());
                }
            }
            if (placementNames.isEmpty()) {
                LOG.error(String.format("Failed to create cluster %s: invalid placement %s", clusterName, clusterInfoBean.getPlacement()));
                throw new Exception(String.format("Failed to create cluster %s: invalid placement %s", clusterName, clusterInfoBean.getPlacement()));
            }
            updateClusterBean.setPlacement_id(Joiner.on(",").join(placementIds));
        } else if (oldClusterBean == null) {
            LOG.error(String.format("Failed to create cluster %s: null placement", clusterName));
            throw new Exception(String.format("Failed to create cluster %s: null placement", clusterName));
        }

        Map<String, String> configMaps = clusterInfoBean.getConfigs();
        if (!configMaps.isEmpty()) {
            // update configs
            if (oldClusterBean == null || oldClusterBean.getConfig_id() == null) {
                String configId = dataHandler.insertMap(configMaps, operator);
                updateClusterBean.setConfig_id(configId);
            } else {
                dataHandler.updateMap(oldClusterBean.getConfig_id(), configMaps, operator);
                updateClusterBean.setConfig_id(oldClusterBean.getConfig_id());
            }
        } else if (oldClusterBean == null) {
            LOG.error(String.format("Failed to create cluster %s: empty config id", clusterName));
            throw new Exception(String.format("Failed to create cluster %s: empty config id", clusterName));
        }

        if (clusterInfoBean.getCapacity() != null) {
            updateClusterBean.setCapacity(clusterInfoBean.getCapacity());
        } else if (oldClusterBean == null) {
            LOG.error(String.format("Failed to create cluster %s: empty capacity", clusterName));
            throw new Exception(String.format("Failed to create cluster %s: empty capacity", clusterName));
        }

        if (clusterInfoBean.getState() != null) {
            updateClusterBean.setState(clusterInfoBean.getState());
        }

        updateClusterBean.setCluster_name(clusterName);
        updateClusterBean.setLast_update(System.currentTimeMillis());
        updateClusterBean.setProvider(provider);
        if (oldClusterBean == null) {
            clusterDAO.insert(updateClusterBean);
        } else {
            clusterDAO.update(clusterName, updateClusterBean);
        }

        LOG.debug(String.format("Mapping %s to %s", clusterInfoBean.toString(), updateClusterBean.toString()));
        return updateClusterBean;
    }

    private ClusterInfoBean mappingToClusterInfoBean(String clusterName) throws Exception {
        ClusterBean clusterBean = clusterDAO.getByClusterName(clusterName);
        if (clusterBean == null) {
            return null;
        }

        ClusterInfoBean clusterInfoBean = new ClusterInfoBean();
        clusterInfoBean.setClusterName(clusterName);
        clusterInfoBean.setCapacity(clusterBean.getCapacity());
        clusterInfoBean.setProvider(clusterBean.getProvider());
        clusterInfoBean.setState(clusterBean.getState());
        clusterInfoBean.setBaseImageId(clusterBean.getBase_image_id());

        HostTypeBean hostTypeBean = hostTypeDAO.getById(clusterBean.getHost_type_id());
        clusterInfoBean.setHostType(hostTypeBean.getAbstract_name());

        SecurityZoneBean securityZoneBean = securityZoneDAO.getById(clusterBean.getSecurity_zone_id());
        clusterInfoBean.setSecurityZone(securityZoneBean.getAbstract_name());

        List<String> placementIds = Arrays.asList(clusterBean.getPlacement_id().split(","));
        List<String> placementNames = new ArrayList<>();
        for (String placementId : placementIds) {
            PlacementBean placementBean = placementDAO.getById(placementId);
            if (placementBean != null) {
                placementNames.add(placementBean.getAbstract_name());
            }
        }
        clusterInfoBean.setPlacement(Joiner.on(",").join(placementNames));

        EnvironBean environBean = environDAO.getByCluster(clusterName);
        clusterInfoBean.setIsDocker(environBean.getIs_docker());

        Map<String, String> configMaps = dataHandler.getMapById(clusterBean.getConfig_id());
        clusterInfoBean.setConfigs(configMaps);
        return clusterInfoBean;
    }
}
