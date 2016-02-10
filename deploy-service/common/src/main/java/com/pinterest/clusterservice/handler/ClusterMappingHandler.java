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

import com.pinterest.clusterservice.bean.AwsVmBean;
import com.pinterest.clusterservice.bean.BaseImageBean;
import com.pinterest.clusterservice.bean.ClusterBean;
import com.pinterest.clusterservice.bean.HostTypeBean;
import com.pinterest.clusterservice.bean.PlacementBean;
import com.pinterest.clusterservice.bean.SecurityZoneBean;
import com.pinterest.clusterservice.dao.BaseImageDAO;
import com.pinterest.clusterservice.dao.HostTypeDAO;
import com.pinterest.clusterservice.dao.PlacementDAO;
import com.pinterest.clusterservice.dao.SecurityZoneDAO;
import com.pinterest.deployservice.ServiceContext;

import com.google.common.base.Joiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ClusterMappingHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterMappingHandler.class);
    private final BaseImageDAO baseImageDAO;
    private final HostTypeDAO hostTypeDAO;
    private final PlacementDAO placementDAO;
    private final SecurityZoneDAO securityZoneDAO;

    public ClusterMappingHandler(ServiceContext serviceContext) {
        this.baseImageDAO = serviceContext.getBaseImageDAO();
        this.hostTypeDAO = serviceContext.getHostTypeDAO();
        this.placementDAO = serviceContext.getPlacementDAO();
        this.securityZoneDAO = serviceContext.getSecurityZoneDAO();
    }

    public AwsVmBean mappingToDefaultAwsVmBean(ClusterBean clusterBean) throws Exception {
        String clusterName = clusterBean.getCluster_name();
        AwsVmBean awsVmBean = new AwsVmBean();

        if (clusterBean.getBase_image_id() == null) {
            LOG.error(String.format("Failed to create cluster %s: null image", clusterName));
            throw new Exception(String.format("Failed to create cluster %s: null image", clusterName));
        } else {
            BaseImageBean baseImageBean = baseImageDAO.getById(clusterBean.getBase_image_id());
            if (baseImageBean == null) {
                LOG.error(String.format("Failed to create cluster %s: invalid image %s", clusterName, clusterBean.getBase_image_id()));
                throw new Exception(String.format("Failed to create cluster %s: invalid image %s", clusterName, clusterBean.getBase_image_id()));
            }
            awsVmBean.setImage(baseImageBean.getProvider_name());
        }

        if (clusterBean.getHost_type_id() == null) {
            LOG.error(String.format("Failed to create cluster %s: null host type", clusterName));
            throw new Exception(String.format("Failed to create cluster %s: null host type", clusterName));
        } else {
            HostTypeBean hostTypeBean = hostTypeDAO.getById(clusterBean.getHost_type_id());
            if (hostTypeBean == null) {
                LOG.error(String.format("Failed to create cluster %s: invalid host type %s", clusterName, clusterBean.getHost_type_id()));
                throw new Exception(String.format("Failed to create cluster %s: invalid host type %s", clusterName, clusterBean.getHost_type_id()));
            }
            awsVmBean.setHostType(hostTypeBean.getProvider_name());
        }

        if (clusterBean.getSecurity_zone_id() == null) {
            LOG.error(String.format("Failed to create cluster %s: null security zone", clusterName));
            throw new Exception(String.format("Failed to create cluster %s: null security zone", clusterName));
        } else {
            SecurityZoneBean securityZoneBean = securityZoneDAO.getById(clusterBean.getSecurity_zone_id());
            if (securityZoneBean == null) {
                LOG.error(String.format("Failed to create cluster %s: invalid security zone %s", clusterName, clusterBean.getSecurity_zone_id()));
                throw new Exception(String.format("Failed to create cluster %s: invalid security zone %s", clusterName, clusterBean.getSecurity_zone_id()));
            }
            awsVmBean.setSecurityZone(securityZoneBean.getProvider_name());
        }

        if (clusterBean.getPlacement_id() == null) {
            LOG.error(String.format("Failed to create cluster %s: null placement", clusterName));
            throw new Exception(String.format("Failed to create cluster %s: null placement", clusterName));
        } else {
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
        }

        awsVmBean.setClusterName(clusterName);
        awsVmBean.setAssignPublicIp(false);
        awsVmBean.setMinSize(clusterBean.getCapacity() == null ? 0 : clusterBean.getCapacity());
        awsVmBean.setMaxSize(clusterBean.getCapacity() == null ? 0 : clusterBean.getCapacity());
        LOG.debug(String.format("Mapping %s to %s", clusterBean.toString(), awsVmBean.toString()));
        return awsVmBean;
    }

    public AwsVmBean mappingToAwsVmBean(ClusterBean clusterBean, AwsVmBean patchBean) throws Exception {
        String clusterName = clusterBean.getCluster_name();
        AwsVmBean awsVmBean = new AwsVmBean();
        awsVmBean.setClusterName(clusterBean.getCluster_name());
        if (clusterBean.getBase_image_id() != null) {
            BaseImageBean baseImageBean = baseImageDAO.getById(clusterBean.getBase_image_id());
            if (baseImageBean == null) {
                LOG.error(String.format("Failed to update cluster %s: invalid image %s", clusterName, clusterBean.getBase_image_id()));
                throw new Exception(String.format("Failed to update cluster %s: invalid image %s", clusterName, clusterBean.getBase_image_id()));
            }
            awsVmBean.setImage(baseImageBean.getProvider_name());
        }

        if (clusterBean.getHost_type_id() != null) {
            HostTypeBean hostTypeBean = hostTypeDAO.getById(clusterBean.getHost_type_id());
            if (hostTypeBean == null) {
                LOG.error(String.format("Failed to update cluster %s: invalid host type %s", clusterName, clusterBean.getHost_type_id()));
                throw new Exception(String.format("Failed to update cluster %s: invalid host type %s", clusterName, clusterBean.getHost_type_id()));
            }
            awsVmBean.setHostType(hostTypeBean.getProvider_name());
        }

        if (clusterBean.getSecurity_zone_id() != null) {
            SecurityZoneBean securityZoneBean = securityZoneDAO.getById(clusterBean.getSecurity_zone_id());
            if (securityZoneBean == null) {
                LOG.error(String.format("Failed to update cluster %s: invalid security zone %s", clusterName, clusterBean.getSecurity_zone_id()));
                throw new Exception(String.format("Failed to update cluster %s: invalid security zone %s", clusterName, clusterBean.getSecurity_zone_id()));
            }
            awsVmBean.setSecurityZone(securityZoneBean.getProvider_name());
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
                LOG.error(String.format("Failed to update cluster %s: invalid placement %s", clusterName, clusterBean.getPlacement_id()));
                throw new Exception(String.format("Failed to update cluster %s: invalid placement %s", clusterName, clusterBean.getPlacement_id()));
            }
            awsVmBean.setSubnet(Joiner.on(",").join(placementNames));
        }

        if (clusterBean.getCapacity() != null) {
            awsVmBean.setMinSize(clusterBean.getCapacity());
            awsVmBean.setMinSize(patchBean.getMaxSize());
        }
        return awsVmBean;
    }
}
