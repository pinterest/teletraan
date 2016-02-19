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
import com.pinterest.clusterservice.bean.AwsVmBean;
import com.pinterest.clusterservice.bean.CloudProvider;
import com.pinterest.clusterservice.bean.ClusterBean;
import com.pinterest.clusterservice.cm.AwsVmManager;
import com.pinterest.clusterservice.cm.ClusterManager;
import com.pinterest.clusterservice.cm.DefaultClusterManager;
import com.pinterest.clusterservice.dao.ClusterDAO;
import com.pinterest.deployservice.ServiceContext;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;

public class ClusterHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterHandler.class);
    private final ClusterDAO clusterDAO;
    private final AwsConfigManager awsConfigManager;
    private final ClusterMappingHandler clusterMappingHandler;

    public ClusterHandler(ServiceContext serviceContext) {
        this.clusterDAO = serviceContext.getClusterDAO();
        this.awsConfigManager = serviceContext.getAwsConfigManager();
        this.clusterMappingHandler = new ClusterMappingHandler(serviceContext);
    }

    public ClusterManager createClusterManager(CloudProvider provider) {
        if (provider == CloudProvider.AWS && awsConfigManager != null) {
            return new AwsVmManager(awsConfigManager);
        } else {
            return new DefaultClusterManager();
        }
    }

    public void createCluster(ClusterBean clusterBean) throws Exception {
        String clusterName = clusterBean.getCluster_name();
        if (clusterBean.getProvider() == CloudProvider.AWS) {
            LOG.info(String.format("Start to create AWS VM cluster for %s", clusterName));
            AwsVmBean awsVmBean = clusterMappingHandler.mappingToDefaultAwsVmBean(clusterBean);
            ClusterManager clusterManager = createClusterManager(CloudProvider.AWS);
            clusterManager.createCluster(clusterName, awsVmBean);
        }

        clusterBean.setLast_update(System.currentTimeMillis());
        clusterDAO.insert(clusterBean);
    }

    public void updateCluster(String clusterName, ClusterBean clusterBean) throws Exception {
        if (clusterBean.getProvider() == CloudProvider.AWS) {
            LOG.info(String.format("Start to update AWS VM cluster for %s", clusterName));
            ClusterManager clusterManager = createClusterManager(CloudProvider.AWS);
            AwsVmBean patchBean = (AwsVmBean) clusterManager.getCluster(clusterName);
            AwsVmBean awsVmBean = clusterMappingHandler.mappingToAwsVmBean(clusterBean, patchBean);
            clusterManager.updateCluster(clusterName, awsVmBean);
        }

        clusterBean.setLast_update(System.currentTimeMillis());
        clusterDAO.update(clusterName, clusterBean);
    }

    public ClusterBean getCluster(String clusterName) throws Exception {
        return clusterDAO.getByClusterName(clusterName);
    }

    public void deleteCluster(String clusterName) throws Exception {
        ClusterBean clusterBean = clusterDAO.getByClusterName(clusterName);
        if (clusterBean.getProvider() == CloudProvider.AWS) {
            LOG.info(String.format("Start to delete AWS VM cluster %s", clusterName));
            ClusterManager clusterManager = createClusterManager(CloudProvider.AWS);
            clusterManager.deleteCluster(clusterName);
        }

        clusterDAO.delete(clusterName);
    }

    public void launchHosts(String clusterName, int num) throws Exception {
        ClusterBean clusterBean = clusterDAO.getByClusterName(clusterName);
        if (clusterBean.getProvider() == CloudProvider.AWS) {
            LOG.info(String.format("Start to launch %d AWS VM hosts to cluster %s", num, clusterName));
            ClusterManager clusterManager = createClusterManager(CloudProvider.AWS);
            clusterManager.launchHosts(clusterName, num);
        }

        ClusterBean newBean = new ClusterBean();
        newBean.setCapacity(clusterBean.getCapacity() + num);
        newBean.setLast_update(System.currentTimeMillis());
        clusterDAO.update(clusterName, newBean);
    }

    public void termianteHosts(String clusterName, Collection<String> hostIds, boolean replaceHost) throws Exception {
        ClusterBean clusterBean = clusterDAO.getByClusterName(clusterName);
        if (clusterBean.getProvider() == CloudProvider.AWS) {
            LOG.info(String.format("Start to termiante AWS VM hosts %s from cluster %s", hostIds.toString(), clusterName));
            ClusterManager clusterManager = createClusterManager(CloudProvider.AWS);
            if (replaceHost) {
                clusterManager.terminateHosts(clusterName, hostIds, true);
            } else {
                Collection<String> hostIdsInCluster = clusterManager.getHosts(clusterName, hostIds);
                int capacity = Math.min(clusterBean.getCapacity() - hostIdsInCluster.size(), 0);
                clusterManager.terminateHosts(clusterName, hostIdsInCluster, false);

                ClusterBean newBean = new ClusterBean();
                newBean.setCapacity(capacity);
                newBean.setLast_update(System.currentTimeMillis());
                clusterDAO.update(clusterName, newBean);
            }
        }
    }

    public Collection<String> getHosts(String clusterName, Collection<String> hostIds) throws Exception {
        ClusterBean clusterBean = clusterDAO.getByClusterName(clusterName);
        if (clusterBean.getProvider() == CloudProvider.AWS) {
            ClusterManager clusterManager = createClusterManager(CloudProvider.AWS);
            return clusterManager.getHosts(clusterName, hostIds);
        }
        return Collections.emptyList();
    }

    public String getProviderByClusterName(String clusterName) throws Exception {
        return clusterDAO.getProviderByClusterName(clusterName);
    }

    public void createAwsVmCluster(AwsVmBean advancedBean) throws Exception {
        String clusterName = advancedBean.getClusterName();
        ClusterBean clusterBean = new ClusterBean();
        clusterBean.setCluster_name(clusterName);
        clusterBean.setCapacity(advancedBean.getMaxSize());
        clusterBean.setBase_image_id(advancedBean.getImage());
        clusterBean.setHost_type_id(advancedBean.getHostType());
        clusterBean.setSecurity_zone_id(advancedBean.getSecurityZone());
        clusterBean.setPlacement_id(advancedBean.getSubnet());
        clusterBean.setProvider(CloudProvider.AWS);
        clusterBean.setLast_update(System.currentTimeMillis());

        LOG.info(String.format("Start to create advanced AWS VM cluster for %s", clusterName));
        AwsVmBean awsVmBean = clusterMappingHandler.mappingToDefaultAwsVmBean(clusterBean);
        if (StringUtils.isNotEmpty(advancedBean.getRole())) {
            awsVmBean.setRole(advancedBean.getRole());
        }

        if (advancedBean.getUserDataConfigs() != null && !advancedBean.getUserDataConfigs().isEmpty()) {
            awsVmBean.setUserDataConfigs(advancedBean.getUserDataConfigs());
        }

        if (advancedBean.getAssignPublicIp()) {
            awsVmBean.setAssignPublicIp(advancedBean.getAssignPublicIp());
        }

        ClusterManager clusterManager = createClusterManager(CloudProvider.AWS);
        clusterManager.createCluster(clusterName, awsVmBean);
        clusterDAO.insert(clusterBean);
    }

    public void updateAwsVmCluster(String clusterName, AwsVmBean advancedBean) throws Exception {
        ClusterBean clusterBean = new ClusterBean();
        clusterBean.setCluster_name(clusterName);
        clusterBean.setCapacity(advancedBean.getMaxSize());
        clusterBean.setBase_image_id(advancedBean.getImage());
        clusterBean.setHost_type_id(advancedBean.getHostType());
        clusterBean.setSecurity_zone_id(advancedBean.getSecurityZone());
        clusterBean.setPlacement_id(advancedBean.getSubnet());
        clusterBean.setProvider(CloudProvider.AWS);
        clusterBean.setLast_update(System.currentTimeMillis());

        LOG.info(String.format("Start to update advanced AWS VM cluster for %s", clusterName));
        ClusterManager clusterManager = createClusterManager(CloudProvider.AWS);
        AwsVmBean patchBean = (AwsVmBean) clusterManager.getCluster(clusterName);
        AwsVmBean awsVmBean = clusterMappingHandler.mappingToAwsVmBean(clusterBean, patchBean);
        if (StringUtils.isNotEmpty(advancedBean.getRole())) {
            awsVmBean.setRole(advancedBean.getRole());
        }

        if (advancedBean.getUserDataConfigs() != null && !advancedBean.getUserDataConfigs().isEmpty()) {
            awsVmBean.setUserDataConfigs(advancedBean.getUserDataConfigs());
        }

        if (advancedBean.getAssignPublicIp()) {
            awsVmBean.setAssignPublicIp(advancedBean.getAssignPublicIp());
        }

        clusterManager.updateCluster(clusterName, awsVmBean);
        clusterBean.setLast_update(System.currentTimeMillis());
        clusterDAO.update(clusterName, clusterBean);
    }

    public AwsVmBean getAwsVmCluster(String clusterName) throws Exception {
        ClusterManager clusterManager = createClusterManager(CloudProvider.AWS);
        Object clusterBean = clusterManager.getCluster(clusterName);
        if (clusterBean == null) {
            return null;
        }
        return (AwsVmBean) clusterBean;
    }
}
