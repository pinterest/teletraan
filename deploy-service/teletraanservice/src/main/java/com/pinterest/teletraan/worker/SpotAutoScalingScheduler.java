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


import com.pinterest.arcee.autoscaling.AlarmManager;
import com.pinterest.arcee.autoscaling.AutoScalingManager;
import com.pinterest.arcee.bean.AsgAlarmBean;
import com.pinterest.arcee.bean.AutoScalingGroupBean;
import com.pinterest.arcee.bean.GroupBean;
import com.pinterest.arcee.bean.SpotAutoScalingBean;
import com.pinterest.arcee.common.AutoScalingConstants;
import com.pinterest.arcee.dao.AlarmDAO;
import com.pinterest.arcee.dao.GroupInfoDAO;
import com.pinterest.arcee.dao.ReservedInstanceInfoDAO;
import com.pinterest.arcee.dao.SpotAutoScalingDAO;
import com.pinterest.arcee.handler.GroupHandler;
import com.pinterest.clusterservice.bean.AwsVmBean;
import com.pinterest.deployservice.ServiceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SpotAutoScalingScheduler implements Runnable {
    private static Logger LOG = LoggerFactory.getLogger(SpotAutoScalingScheduler.class);
    private SpotAutoScalingDAO spotAutoScalingDAO;
    private GroupInfoDAO groupInfoDAO;
    private ReservedInstanceInfoDAO reservedInstanceInfoDAO;
    private AutoScalingManager autoScalingManager;
    private AlarmDAO asgAlarmDAO;
    private int spotAutoScalingThreshold;
    private AlarmManager alarmManager;
    private GroupHandler groupHandler;

    public SpotAutoScalingScheduler(ServiceContext serviceContext) {
        spotAutoScalingDAO = serviceContext.getSpotAutoScalingDAO();
        autoScalingManager = serviceContext.getAutoScalingManager();
        groupInfoDAO = serviceContext.getGroupInfoDAO();
        reservedInstanceInfoDAO = serviceContext.getReservedInstanceInfoDAO();
        autoScalingManager = serviceContext.getAutoScalingManager();
        spotAutoScalingThreshold = serviceContext.getSpotAutoScalingThreshold();
        asgAlarmDAO = serviceContext.getAlarmDAO();
        alarmManager = serviceContext.getAlarmManager();
        groupHandler = new GroupHandler(serviceContext);
        LOG.info(String.format("Set spot auto scaling threshold to %d", spotAutoScalingThreshold));
    }

    private void processAlarms(String groupName, boolean enableGrow) throws Exception {
        List<AsgAlarmBean> asgAlarmBeanList = asgAlarmDAO.getAlarmInfosByGroup(groupName);
        List<String> growAlarmIds = new ArrayList<>();
        List<String> shrinkAlarmIds = new ArrayList<>();
        for (AsgAlarmBean asgAlarmBean : asgAlarmBeanList) {
            if (asgAlarmBean.getAction_type().equals(AutoScalingConstants.ASG_SHRINK)) {
                shrinkAlarmIds.add(asgAlarmBean.getAlarm_id());
            } else {
                growAlarmIds.add(asgAlarmBean.getAlarm_id());
            }
        }

        if (enableGrow) {
            alarmManager.enableAlarm(growAlarmIds, groupName);
            alarmManager.disableAlarm(shrinkAlarmIds, groupName);
        } else {
            alarmManager.enableAlarm(shrinkAlarmIds, groupName);
            alarmManager.disableAlarm(growAlarmIds, groupName);
        }
    }

    private void processSpotAutoScaling(String clusterName, SpotAutoScalingBean spotAutoScalingBean) throws Exception {
        AwsVmBean awsVmBean = groupHandler.getCluster(clusterName);
        if (awsVmBean == null) {
            return;
        }

        String instanceType = awsVmBean.getHostType();
        int reservedInstanceCount = reservedInstanceInfoDAO.getReservedInstanceCount(instanceType);
        int runningReservedInstanceCount = reservedInstanceInfoDAO.getRunningReservedInstanceCount(instanceType);
        int freeInstance = reservedInstanceCount - runningReservedInstanceCount;
        String spotAutoScalingGroupName = spotAutoScalingBean.getAsg_name();
        if (freeInstance < spotAutoScalingThreshold) {
            LOG.info(String.format("Enable scaling up for spot auto scaling group %s. Free RI is: %d (Threshold: %d)", clusterName, freeInstance,
                                   spotAutoScalingThreshold));
            if (!spotAutoScalingBean.getEnable_grow()) {
                processAlarms(spotAutoScalingGroupName, true);
                SpotAutoScalingBean updatedBean = new SpotAutoScalingBean();
                updatedBean.setEnable_grow(true);
                spotAutoScalingDAO.updateSpotAutoScalingGroup(spotAutoScalingGroupName, updatedBean);
            }
        } else {
            LOG.info(String.format("Disable scaling up for spot auto scaling group %s. Free RI is: %d (Threshold: %d)", clusterName, freeInstance,
                                   spotAutoScalingThreshold));
            if (spotAutoScalingBean.getEnable_grow()) {
                processAlarms(spotAutoScalingGroupName, false);
                SpotAutoScalingBean updatedBean = new SpotAutoScalingBean();
                updatedBean.setEnable_grow(false);
                spotAutoScalingDAO.updateSpotAutoScalingGroup(spotAutoScalingGroupName, updatedBean);
            }
        }
    }

    private void processOne(String clusterName, SpotAutoScalingBean spotAutoScalingBean)  throws Exception {
        AutoScalingGroupBean autoScalingGroupBean = autoScalingManager
            .getAutoScalingGroupInfoByName(clusterName);
        List<String> instances = autoScalingGroupBean.getInstances();
        int instanceCount = instances.size();
        String spotAutoScalingGroupName = spotAutoScalingBean.getAsg_name();
        AutoScalingGroupBean spotAutoScalingGroup = autoScalingManager.getAutoScalingGroupInfoByName(
            spotAutoScalingGroupName);

        processSpotAutoScaling(clusterName, spotAutoScalingBean);

        int targetSpotAutoScalingGroupMaxSize = (int)(instanceCount * spotAutoScalingBean.getSpot_ratio());
        if (targetSpotAutoScalingGroupMaxSize == spotAutoScalingGroup.getMaxSize()) {
            LOG.info(String.format("Auto Scaling group: %s current running: %d, target spot max size: %d, current max size: %d",
                                   clusterName, instanceCount, targetSpotAutoScalingGroupMaxSize, spotAutoScalingGroup.getMaxSize()));
            return;
        }

        LOG.info(String.format("Auto Scaling group: %s current running: %d, current max size: %d, change to target spot max size: %d,  ",
                               clusterName, instanceCount, spotAutoScalingGroup.getMaxSize(), targetSpotAutoScalingGroupMaxSize));

        AwsVmBean updateBean = new AwsVmBean();
        updateBean.setMinSize(0);
        updateBean.setMaxSize(targetSpotAutoScalingGroupMaxSize);
        autoScalingManager.updateAutoScalingGroup(spotAutoScalingGroupName, updateBean);
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
