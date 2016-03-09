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


import com.pinterest.arcee.autoscaling.AutoScaleGroupManager;
import com.pinterest.arcee.bean.AutoScalingGroupBean;
import com.pinterest.arcee.bean.AutoScalingRequestBean;
import com.pinterest.arcee.bean.GroupBean;
import com.pinterest.arcee.bean.SpotAutoScalingBean;
import com.pinterest.arcee.dao.GroupInfoDAO;
import com.pinterest.arcee.dao.ReservedInstanceInfoDAO;
import com.pinterest.arcee.dao.SpotAutoScalingDAO;
import com.pinterest.deployservice.ServiceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class SpotAutoScalingScheduler implements Runnable {
    private static Logger LOG = LoggerFactory.getLogger(SpotAutoScalingScheduler.class);
    private SpotAutoScalingDAO spotAutoScalingDAO;
    private GroupInfoDAO groupInfoDAO;
    private ReservedInstanceInfoDAO reservedInstanceInfoDAO;
    private AutoScaleGroupManager autoScaleGroupManager;
    private static int FREE_RESERVED_INSTANCE_THRESHOLD = 150;

    public SpotAutoScalingScheduler(ServiceContext serviceContext) {
        spotAutoScalingDAO = serviceContext.getSpotAutoScalingDAO();
        autoScaleGroupManager = serviceContext.getAutoScaleGroupManager();
        groupInfoDAO = serviceContext.getGroupInfoDAO();
        reservedInstanceInfoDAO = serviceContext.getReservedInstanceInfoDAO();
        autoScaleGroupManager = serviceContext.getAutoScaleGroupManager();
    }

    private void processSpotAutoScaling(String clusterName, String spotAutoScalingGroupName) throws Exception {
        GroupBean groupBean = groupInfoDAO.getGroupInfo(clusterName);
        if (groupBean == null) {
            return;
        }

        String instanceType = groupBean.getInstance_type();
        int reservedInstanceCount = reservedInstanceInfoDAO.getReservedInstanceCount(instanceType);
        int runningReservedInstanceCount = reservedInstanceInfoDAO.getRunningReservedInstanceCount(instanceType);
        int freeInstance = reservedInstanceCount - runningReservedInstanceCount;

        if (freeInstance < FREE_RESERVED_INSTANCE_THRESHOLD) {
            LOG.info(String.format("Enable scaling up for spot auto scaling group %s. Free RI is: %d", clusterName, freeInstance));
            autoScaleGroupManager.enableScalingUpEvent(spotAutoScalingGroupName);
        } else {
            LOG.info(String.format("Disable scaling up for spot auto scaling group %s. Free RI is: %d", clusterName, freeInstance));
            autoScaleGroupManager.disableScalingUpEvent(spotAutoScalingGroupName);
        }
    }

    private void processOne(String clusterName, SpotAutoScalingBean spotAutoScalingBean)  throws Exception {
        AutoScalingGroupBean autoScalingGroupBean = autoScaleGroupManager.getAutoScalingGroupInfoByName(clusterName);
        List<String> instances = autoScalingGroupBean.getInstances();
        int instanceCount = instances.size();
        String spotAutoScalingGroupName = spotAutoScalingBean.getAsg_name();
        AutoScalingGroupBean spotAutoScalingGroup = autoScaleGroupManager.getAutoScalingGroupInfoByName(
            spotAutoScalingGroupName);

        int targetSpotAutoScalingGroupMaxSize = (int)(instanceCount * spotAutoScalingBean.getSpot_ratio());
        if (targetSpotAutoScalingGroupMaxSize == spotAutoScalingGroup.getMaxSize()) {
            LOG.info(String.format("Auto Scaling group: %s current running: %d, target spot max size: %d, current max size: %d",
                                   clusterName, instanceCount, targetSpotAutoScalingGroupMaxSize, spotAutoScalingGroup.getMaxSize()));
            return;
        }

        LOG.info(String.format("Auto Scaling group: %s current running: %d, current max size: %d, change to target spot max size: %d,  ",
                               clusterName, instanceCount, spotAutoScalingGroup.getMaxSize(), targetSpotAutoScalingGroupMaxSize));

        AutoScalingRequestBean requestBean = new AutoScalingRequestBean();
        requestBean.setMinSize(0);
        requestBean.setMaxSize(targetSpotAutoScalingGroupMaxSize);
        requestBean.setGroupName(spotAutoScalingGroupName);
        requestBean.setTerminationPolicy(spotAutoScalingGroup.getTerminationPolicy().toString());
        autoScaleGroupManager.updateAutoScalingGroup(requestBean, null);

        processSpotAutoScaling(clusterName, spotAutoScalingGroupName);
    }

    public void processBatch() throws Exception {
        List<SpotAutoScalingBean> spotAutoScalingBeans = spotAutoScalingDAO.getAllSpotAutoScalingGroups();
        Collections.shuffle(spotAutoScalingBeans);
        for (SpotAutoScalingBean spotAutoScalingBean : spotAutoScalingBeans) {
            try {
                LOG.info(String.format("Processing spot auto scaling group: %s",
                                       spotAutoScalingBean.getCluster_name()));
                processOne(spotAutoScalingBean.getCluster_name(), spotAutoScalingBean);
            } catch (Exception ex) {
                LOG.error(String.format("Failed to process auto scaling group: %s", spotAutoScalingBean.getCluster_name()), ex);
            }
        }
    }

    public void run() {
        try {
            LOG.info("SpotAutoScalingScheduler Start spot auto scaling group scheduling process...");
            processBatch();
        } catch (Throwable t) {
            // Catch all throwable so that subsequent job not suppressed
            LOG.error("SpotAutoScalingScheduler Failed.", t);
        }
    }
}
