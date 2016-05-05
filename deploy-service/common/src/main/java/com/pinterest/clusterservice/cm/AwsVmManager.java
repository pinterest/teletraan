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
package com.pinterest.clusterservice.cm;

import com.google.common.base.Joiner;

import com.pinterest.arcee.autoscaling.AutoScalingManager;
import com.pinterest.arcee.dao.HostInfoDAO;
import com.pinterest.arcee.common.AutoScalingConstants;
import com.pinterest.clusterservice.bean.AwsVmBean;
import com.pinterest.clusterservice.bean.BaseImageBean;
import com.pinterest.clusterservice.bean.ClusterBean;
import com.pinterest.clusterservice.bean.HostTypeBean;
import com.pinterest.clusterservice.bean.PlacementBean;
import com.pinterest.clusterservice.bean.SecurityZoneBean;
import com.pinterest.clusterservice.dao.BaseImageDAO;
import com.pinterest.clusterservice.dao.ClusterDAO;
import com.pinterest.clusterservice.dao.HostTypeDAO;
import com.pinterest.clusterservice.dao.PlacementDAO;
import com.pinterest.clusterservice.dao.SecurityZoneDAO;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.common.Constants;
import com.pinterest.deployservice.handler.DataHandler;

import com.amazonaws.AmazonClientException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AwsVmManager implements ClusterManager {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(AwsVmManager.class);
    private static final String DEFAULT_TERMINATION_POLICY = "Default";
    private static final String ROLE_KEY = "cmp_role";
    private static final String PUBLIC_KEY = "cmp_public_ip";
    private final BaseImageDAO baseImageDAO;
    private final ClusterDAO clusterDAO;
    private final HostTypeDAO hostTypeDAO;
    private final HostInfoDAO hostInfoDAO;
    private final PlacementDAO placementDAO;
    private final SecurityZoneDAO securityZoneDAO;
    private AutoScalingManager autoScalingManager;
    private final DataHandler dataHandler;

    public AwsVmManager(ServiceContext serviceContext) {
        baseImageDAO = serviceContext.getBaseImageDAO();
        clusterDAO = serviceContext.getClusterDAO();
        hostTypeDAO = serviceContext.getHostTypeDAO();
        hostInfoDAO = serviceContext.getHostInfoDAO();
        placementDAO = serviceContext.getPlacementDAO();
        securityZoneDAO = serviceContext.getSecurityZoneDAO();
        autoScalingManager = serviceContext.getAutoScalingManager();
        dataHandler = new DataHandler(serviceContext);
    }

    @Override
    public void createCluster(String clusterName, ClusterBean clusterBean) throws Exception {
        AwsVmBean awsVmBean = mappingToAwsVmBean(clusterBean);
        createCluster(clusterName, awsVmBean);
    }

    @Override
    public void updateCluster(String clusterName, ClusterBean clusterBean) throws Exception {
        AwsVmBean newBean = mappingToAwsVmBean(clusterBean);
        updateCluster(clusterName, newBean);
    }

    public void createCluster(String clusterName, AwsVmBean awsVmBean) throws Exception {
        LOG.info(String.format("Start to create AWS cluster for %s", clusterName));
        String launchConfig = autoScalingManager.createLaunchConfig(clusterName, awsVmBean);
        awsVmBean.setTerminationPolicy(DEFAULT_TERMINATION_POLICY);
        awsVmBean.setLaunchConfigId(launchConfig);
        autoScalingManager.createAutoScalingGroup(clusterName, awsVmBean);
    }

    public void updateCluster(String clusterName, AwsVmBean newBean) throws Exception {
        LOG.info(String.format("Start to update AWS cluster for %s", clusterName));
        AwsVmBean oldBean = autoScalingManager.getAutoScalingGroupInfo(clusterName);
        if (oldBean == null) {
            LOG.error(String.format("Autoscaling group %s does not exist. Cannot update cluster", clusterName));
            throw new Exception(String.format("Autoscaling group %s does not exist. Cannot update cluster", clusterName));
        }

        String newLaunchConfigId = updateLaunchConfig(clusterName, oldBean, newBean);
        newBean.setLaunchConfigId(newLaunchConfigId);
        updateAutoScalingGroup(clusterName, newBean);
        if (newLaunchConfigId != null) {
            autoScalingManager.deleteLaunchConfig(oldBean.getLaunchConfigId());
        }
    }

    @Override
    public ClusterBean getCluster(String clusterName) throws Exception {
        ClusterBean clusterBean = clusterDAO.getByClusterName(clusterName);
        if (StringUtils.isEmpty(clusterBean.getConfig_id())) {
            AwsVmBean awsVmBean = autoScalingManager.getAutoScalingGroupInfo(clusterName);
            Map<String, String> resultMap = new HashMap<>(awsVmBean.getUserDataConfigs());
            resultMap.put(ROLE_KEY, awsVmBean.getRole());
            if (awsVmBean.getAssignPublicIp()) {
                resultMap.put(PUBLIC_KEY, "yes");
            }

            String configId = dataHandler.insertMap(resultMap, Constants.SYSTEM_OPERATOR);
            clusterBean.setConfig_id(configId);
            clusterBean.setLast_update(System.currentTimeMillis());
            clusterDAO.update(clusterName, clusterBean);
            LOG.info(String.format("Update configs for cluster %s", clusterName));
        }

        return clusterBean;
    }

    @Override
    public void deleteCluster(String clusterName) throws Exception {
        LOG.info(String.format("Start to delete AWS cluster %s", clusterName));
        AwsVmBean awsVmBean = autoScalingManager.getAutoScalingGroupInfo(clusterName);
        autoScalingManager.deleteAutoScalingGroup(clusterName, false);
        autoScalingManager.deleteLaunchConfig(awsVmBean.getLaunchConfigId());
    }

    @Override
    public Collection<HostBean> launchHosts(String clusterName, int num, boolean launchInAsg) throws Exception {
        LOG.info(String.format("Start to launch %d AWS hosts to cluster %s", num, clusterName));
        if (launchInAsg) {
            autoScalingManager.increaseGroupCapacity(clusterName, num);
            return Collections.emptyList();
        } else {
            AwsVmBean awsVmBean = autoScalingManager.getAutoScalingGroupInfo(clusterName);
            return hostInfoDAO.launchHosts(awsVmBean, num, null);
        }
    }

    @Override
    public void terminateHosts(String clusterName, Collection<String> hostIds, boolean replaceHost) throws Exception {
        LOG.info(String.format("Start to terminate AWS hosts %s in cluster %s", hostIds.toString(), clusterName));
        if (replaceHost) {
            hostInfoDAO.terminateHosts(hostIds);
        } else {
            autoScalingManager.disableAutoScalingActions(clusterName,
                Collections.singletonList(AutoScalingConstants.PROCESS_LAUNCH));
            hostInfoDAO.terminateHosts(hostIds);
            autoScalingManager.decreaseGroupCapacity(clusterName, hostIds.size());
            autoScalingManager.enableAutoScalingActions(clusterName,
                Collections.singletonList(AutoScalingConstants.PROCESS_LAUNCH));
        }
    }

    @Override
    public Collection<String> getHosts(String clusterName, Collection<String> hostIds) throws Exception {
        if (hostIds == null || hostIds.isEmpty()) {
           return autoScalingManager.getAutoScalingGroupInfoByName(clusterName).getInstances();
        } else {
            return autoScalingManager.getAutoScalingInstances(clusterName, hostIds);
        }
    }

    private String updateLaunchConfig(String clusterName, AwsVmBean oldBean, AwsVmBean newBean) throws Exception {
        try {
            if (newBean.getImage() == null) {
                newBean.setImage(oldBean.getImage());
            }

            if (newBean.getHostType() == null) {
                newBean.setHostType(oldBean.getHostType());
            }

            if (newBean.getSecurityZone() == null) {
                newBean.setSecurityZone(oldBean.getSecurityZone());
            }

            if (newBean.getAssignPublicIp() == null) {
                newBean.setAssignPublicIp(oldBean.getAssignPublicIp());
            }

            if (newBean.getUserDataConfigs() == null) {
                newBean.setUserDataConfigs(oldBean.getUserDataConfigs());
            }

            if (newBean.getRole() == null) {
                newBean.setRole(oldBean.getRole());
            }

            return autoScalingManager.createLaunchConfig(clusterName, newBean);
        } catch (AmazonClientException e) {
            LOG.error(String.format("Failed to update launch config for %s: %s", clusterName, e.getMessage()));
            throw new Exception(String.format("Failed to udpate launch config for %s: %s", clusterName, e.getMessage()));
        }
    }

    private void updateAutoScalingGroup(String clusterName, AwsVmBean newBean) throws Exception {
        try {
            AwsVmBean updateAsgRequest = new AwsVmBean();
            updateAsgRequest.setSubnet(newBean.getSubnet());
            updateAsgRequest.setMinSize(newBean.getMinSize());
            updateAsgRequest.setMaxSize(newBean.getMaxSize());
            updateAsgRequest.setLaunchConfigId(newBean.getLaunchConfigId());
            autoScalingManager.updateAutoScalingGroup(clusterName, updateAsgRequest);
        } catch (AmazonClientException e) {
            LOG.error(String.format("Failed to update auto scaling group %s: %s", clusterName, e.getMessage()));
            throw new Exception(String.format("Failed to update auto scaling group %s: %s", clusterName, e.getMessage()));
        }
    }

    private AwsVmBean mappingToAwsVmBean(ClusterBean clusterBean) throws Exception {
        String clusterName = clusterBean.getCluster_name();
        ClusterBean oldBean = clusterDAO.getByClusterName(clusterName);
        AwsVmBean awsVmBean = new AwsVmBean();

        if (clusterBean.getBase_image_id() != null) {
            BaseImageBean baseImageBean = baseImageDAO.getById(clusterBean.getBase_image_id());
            if (baseImageBean == null) {
                LOG.error(String.format("Failed to create cluster %s: invalid image %s", clusterName, clusterBean.getBase_image_id()));
                throw new Exception(String.format("Failed to create cluster %s: invalid image %s", clusterName, clusterBean.getBase_image_id()));
            }
            awsVmBean.setImage(baseImageBean.getProvider_name());
        } else if (oldBean == null) {
            LOG.error(String.format("Failed to create cluster %s: null image", clusterName));
            throw new Exception(String.format("Failed to create cluster %s: null image", clusterName));
        }

        if (clusterBean.getHost_type_id() != null) {
            HostTypeBean hostTypeBean = hostTypeDAO.getById(clusterBean.getHost_type_id());
            if (hostTypeBean == null) {
                LOG.error(String.format("Failed to create cluster %s: invalid host type %s", clusterName, clusterBean.getHost_type_id()));
                throw new Exception(String.format("Failed to create cluster %s: invalid host type %s", clusterName, clusterBean.getHost_type_id()));
            }
            awsVmBean.setHostType(hostTypeBean.getProvider_name());
        } else if (oldBean == null) {
            LOG.error(String.format("Failed to create cluster %s: null host type", clusterName));
            throw new Exception(String.format("Failed to create cluster %s: null host type", clusterName));
        }

        if (clusterBean.getSecurity_zone_id() != null) {
            SecurityZoneBean securityZoneBean = securityZoneDAO.getById(clusterBean.getSecurity_zone_id());
            if (securityZoneBean == null) {
                LOG.error(String.format("Failed to create cluster %s: invalid security zone %s", clusterName, clusterBean.getSecurity_zone_id()));
                throw new Exception(String.format("Failed to create cluster %s: invalid security zone %s", clusterName, clusterBean.getSecurity_zone_id()));
            }
            awsVmBean.setSecurityZone(securityZoneBean.getProvider_name());
        } else if (oldBean == null) {
            LOG.error(String.format("Failed to create cluster %s: null security zone", clusterName));
            throw new Exception(String.format("Failed to create cluster %s: null security zone", clusterName));
        }

        if (clusterBean.getPlacement_id() != null) {
            List<String> placementIds = Arrays.asList(clusterBean.getPlacement_id().split(","));
            List<String> placementNames = new ArrayList<>();
            for (String placementId : placementIds) {
                PlacementBean placementBean = placementDAO.getById(placementId);
                if (placementBean != null) {
                    placementNames.add(placementBean.getProvider_name());
                }
            }
            if (placementNames.isEmpty()) {
                LOG.error(String.format("Failed to create cluster %s: invalid placement %s", clusterName, clusterBean.getSecurity_zone_id()));
                throw new Exception(String.format("Failed to create cluster %s: invalid placement %s", clusterName, clusterBean.getSecurity_zone_id()));
            }
            awsVmBean.setSubnet(Joiner.on(",").join(placementNames));
        } else if (oldBean == null) {
            LOG.error(String.format("Failed to create cluster %s: null placement", clusterName));
            throw new Exception(String.format("Failed to create cluster %s: null placement", clusterName));
        }

        String configId = clusterBean.getConfig_id();
        if (configId != null) {
            Map<String, String> configMaps = dataHandler.getMapById(configId);
            if (configMaps.containsKey(ROLE_KEY)) {
                awsVmBean.setRole(configMaps.get(ROLE_KEY));
            }

            if (configMaps.containsKey(PUBLIC_KEY)) {
                awsVmBean.setAssignPublicIp(true);
            } else {
                awsVmBean.setAssignPublicIp(false);
            }

            Map<String, String> resultMap = new HashMap<>();
            for (Map.Entry<String, String> entry : configMaps.entrySet()) {
                if (!entry.getKey().equals(ROLE_KEY) && !entry.getKey().equals(PUBLIC_KEY)) {
                    resultMap.put(entry.getKey(), entry.getValue());
                }
            }

            if (!resultMap.isEmpty()) {
                awsVmBean.setUserDataConfigs(resultMap);
            }
        } else if (oldBean == null) {
            LOG.error(String.format("Failed to create cluster %s: empty config id", clusterName));
            throw new Exception(String.format("Failed to create cluster %s: empty config id", clusterName));
        }

        if (clusterBean.getCapacity() != null) {
            awsVmBean.setMinSize(clusterBean.getCapacity());
            awsVmBean.setMaxSize(clusterBean.getCapacity());
        }

        awsVmBean.setClusterName(clusterName);
        LOG.debug(String.format("Mapping %s to %s", clusterBean.toString(), awsVmBean.toString()));
        return awsVmBean;
    }
}
