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
import com.pinterest.arcee.bean.ManagingGroupsBean;
import com.pinterest.arcee.bean.SpotAutoScalingBean;
import com.pinterest.arcee.common.AutoScalingConstants;
import com.pinterest.arcee.dao.AlarmDAO;
import com.pinterest.arcee.dao.ManagingGroupDAO;
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
    private ReservedInstanceInfoDAO reservedInstanceInfoDAO;
    private AutoScalingManager autoScalingManager;
    private AlarmDAO asgAlarmDAO;
    private int spotAutoScalingThreshold;
    private AlarmManager alarmManager;
    private GroupHandler groupHandler;

    private ManagingGroupDAO managingGroupDAO;

    public SpotAutoScalingScheduler(ServiceContext serviceContext) {
        spotAutoScalingDAO = serviceContext.getSpotAutoScalingDAO();
        autoScalingManager = serviceContext.getAutoScalingManager();
        reservedInstanceInfoDAO = serviceContext.getReservedInstanceInfoDAO();
        autoScalingManager = serviceContext.getAutoScalingManager();
        spotAutoScalingThreshold = serviceContext.getSpotAutoScalingThreshold();
        asgAlarmDAO = serviceContext.getAlarmDAO();
        alarmManager = serviceContext.getAlarmManager();
        groupHandler = new GroupHandler(serviceContext);
        managingGroupDAO = serviceContext.getManagingGroupDAO();
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

    private void processSpotAutoScaling(String clusterName, SpotAutoScalingBean spotAutoScalingBean, int targetGroupSize) throws Exception {
        AwsVmBean awsVmBean = groupHandler.getCluster(clusterName);
        if (awsVmBean == null) {
            return;
        }

        String instanceType = awsVmBean.getHostType();
        int reservedInstanceCount = reservedInstanceInfoDAO.getReservedInstanceCount(instanceType);
        int runningReservedInstanceCount = reservedInstanceInfoDAO.getRunningReservedInstanceCount(instanceType);
        int freeInstance = reservedInstanceCount - runningReservedInstanceCount;

        String spotAutoScalingGroupName = String.format("%s-spot", clusterName);
        AwsVmBean spotAwsVmBean = autoScalingManager.getAutoScalingGroupInfo(spotAutoScalingGroupName);

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

        if (!spotAutoScalingBean.getEnable_resource_lending()) {
            if (spotAwsVmBean.getMaxSize() != targetGroupSize) {
                LOG.info(String.format("Auto scaling group %s current running %d, target spot max: %d, spot auto scaling max size: %d, "
                                       + "Adjusting max size", clusterName, spotAwsVmBean.getCurSize(), targetGroupSize, spotAwsVmBean.getMaxSize()));
                AwsVmBean updatedSpotAwsBean = new AwsVmBean();
                updatedSpotAwsBean.setMaxSize(targetGroupSize);
                autoScalingManager.updateAutoScalingGroup(spotAutoScalingGroupName, updatedSpotAwsBean);
            } else {

                LOG.info(String.format("Auto scaling group %s current running %d, target spot max: %d, spot auto scaling max size: %d, stay unchanged. ",
                                       clusterName, spotAwsVmBean.getCurSize(), targetGroupSize, spotAwsVmBean.getMaxSize()));
            }
            return;
        }

        String attachedAutoScalingGroupName = String.format("%s-lending", clusterName);
        AwsVmBean lendingAwsVmBean = autoScalingManager.getAutoScalingGroupInfo(attachedAutoScalingGroupName);

        if (spotAwsVmBean.getMaxSize() + lendingAwsVmBean.getCurSize() != targetGroupSize) {
            AwsVmBean updatedSpotAwsBean = new AwsVmBean();
            updatedSpotAwsBean.setMaxSize((targetGroupSize - lendingAwsVmBean.getCurSize()));
            autoScalingManager.updateAutoScalingGroup(spotAutoScalingGroupName, updatedSpotAwsBean);
        }

        if (targetGroupSize == lendingAwsVmBean.getMaxSize()) {
            LOG.info(String.format("Auto Scaling group: %s current running: %d, target spot max size: %d, current max size: %d",
                                   clusterName, lendingAwsVmBean.getCurSize(), targetGroupSize, lendingAwsVmBean.getMaxSize()));
            return;
        }

        LOG.info(String.format("Auto Scaling group: %s current running: %d, current max size: %d, change to target spot max size: %d,  ",
                               clusterName, lendingAwsVmBean.getCurSize(), lendingAwsVmBean.getMaxSize(), targetGroupSize));

        AwsVmBean updatedLendingAwsBean = new AwsVmBean();
        updatedLendingAwsBean.setMaxSize(targetGroupSize);
        autoScalingManager.updateAutoScalingGroup(attachedAutoScalingGroupName, updatedLendingAwsBean);

        ManagingGroupsBean managingGroupsBean = new ManagingGroupsBean();
        managingGroupsBean.setMax_lending_size(targetGroupSize);
        managingGroupDAO.updateManagingGroup(clusterName, managingGroupsBean);
    }



    private void processOne(String clusterName, SpotAutoScalingBean spotAutoScalingBean)  throws Exception {
        AutoScalingGroupBean autoScalingGroupBean = autoScalingManager
            .getAutoScalingGroupInfoByName(clusterName);
        List<String> instances = autoScalingGroupBean.getInstances();
        int instanceCount = instances.size();


        int targetSpotAutoScalingGroupMaxSize = (int)(instanceCount * spotAutoScalingBean.getSpot_ratio());
        processSpotAutoScaling(clusterName, spotAutoScalingBean, targetSpotAutoScalingGroupMaxSize);

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
