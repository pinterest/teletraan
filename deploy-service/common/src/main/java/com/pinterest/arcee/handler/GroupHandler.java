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
package com.pinterest.arcee.handler;


import com.pinterest.arcee.autoscaling.AlarmManager;
import com.pinterest.arcee.autoscaling.AutoScalingManager;
import com.pinterest.arcee.bean.*;
import com.pinterest.arcee.common.AutoScalingConstants;
import com.pinterest.arcee.dao.*;
import com.pinterest.arcee.dao.AlarmDAO;
import com.pinterest.arcee.dao.GroupInfoDAO;
import com.pinterest.arcee.dao.HostInfoDAO;
import com.pinterest.arcee.dao.SpotAutoScalingDAO;
import com.pinterest.clusterservice.bean.AwsVmBean;
import com.pinterest.clusterservice.cm.AwsVmManager;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.ASGStatus;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.common.CommonUtils;
import com.pinterest.deployservice.dao.GroupDAO;
import com.pinterest.deployservice.dao.HostDAO;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class GroupHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GroupHandler.class);
    // http://docs.aws.amazon.com/AutoScaling/latest/APIReference/API_AttachInstances.html
    private static final int ATTACH_INSTANCE_SIZE = 16;

    private static final String SPOT_AUTO_SCALING_TERMINATION_POLICY = "OldestInstance";

    private static final int LENDING_INSTANCE_BATCH_SIZE = 10;
    private static final int LENDING_INSTANCE_COOL_DOWN_TIME = 10;
    private AutoScalingManager asgDAO;
    private AlarmDAO alarmDAO;
    private HostInfoDAO hostInfoDAO;
    private HostDAO hostDAO;
    private AlarmManager alarmWatcher;
    private GroupDAO groupDAO;
    private GroupInfoDAO groupInfoDAO;
    private SpotAutoScalingDAO spotAutoScalingDAO;
    private ExecutorService jobPool;
    private AwsVmManager awsVmManager;
    private PasConfigDAO pasConfigDAO;
    private ManagingGroupDAO managingGroupDAO;
    private GroupMappingDAO groupMappingDAO;

    public GroupHandler(ServiceContext serviceContext) {
        asgDAO = serviceContext.getAutoScalingManager();
        hostDAO = serviceContext.getHostDAO();
        groupDAO = serviceContext.getGroupDAO();
        groupInfoDAO = serviceContext.getGroupInfoDAO();
        alarmDAO = serviceContext.getAlarmDAO();
        alarmWatcher = serviceContext.getAlarmManager();
        jobPool = serviceContext.getJobPool();
        hostInfoDAO = serviceContext.getHostInfoDAO();
        spotAutoScalingDAO = serviceContext.getSpotAutoScalingDAO();
        pasConfigDAO = serviceContext.getPasConfigDAO();
        awsVmManager = new AwsVmManager(serviceContext);
        managingGroupDAO = serviceContext.getManagingGroupDAO();
        groupMappingDAO = serviceContext.getGroupMappingDAO();
    }

    private final class DeleteAutoScalingJob implements Callable<Void> {
        private String groupName;
        private boolean detachInstances;
        private String spotLaunchConfig;

        public DeleteAutoScalingJob(String groupName, boolean detachInstances, String spotLaunchConfig) {
            this.groupName = groupName;
            this.detachInstances = detachInstances;
            this.spotLaunchConfig = spotLaunchConfig;
        }

        public Void call() {
            try {
                // step 1 delete all alarms related to the autoscaling group
                LOG.info(String.format("Delete alarms for group %s", groupName));
                List<AsgAlarmBean> asgAlarmBeans = alarmDAO.getAlarmInfosByGroup(groupName);
                for (AsgAlarmBean asgAlarmBean : asgAlarmBeans) {
                    alarmWatcher.deleteAlarmFromPolicy(asgAlarmBean);
                    alarmDAO.deleteAlarmInfoById(asgAlarmBean.getAlarm_id());
                }
                // step 2 delete auto scaling group
                AwsVmBean awsVmBean = asgDAO.getAutoScalingGroupInfo(groupName);
                String launchConfig = awsVmBean.getLaunchConfigId();
                asgDAO.deleteAutoScalingGroup(groupName, detachInstances);
                asgDAO.deleteLaunchConfig(launchConfig);
                LOG.info(String.format("Delete autoscaling group: %s", groupName));

                // step 3 update the groups db
                if (!StringUtils.isEmpty(spotLaunchConfig)) {
                    LOG.info(String.format("Delete launch config for group: %s", groupName));
                    asgDAO.deleteLaunchConfig(spotLaunchConfig);

                }
                groupInfoDAO.removeGroup(groupName);
            } catch (Exception t) {
                LOG.error(String.format("Failed to delete auto scaling group %s", groupName), t);
            } finally {
                return null;
            }
        }
    }

    private final class AttachInstanceToGroupJob implements Callable<Void> {
        private String groupName;
        private AutoScalingRequestBean request;
        private List<String> ids;
        private int minSize;
        private String subnets;

        public AttachInstanceToGroupJob(String groupName, AutoScalingRequestBean request, List<String> ids, int minSize, String subnets) {
            this.groupName = groupName;
            this.request = request;
            this.ids = ids;
            this.minSize = minSize;
            this.subnets = subnets;
        }

        public Void call() {
            try {
                LOG.info("Start to attach instance {} to group {}", ids.toString(), groupName);
                for (int i = 0; i < ids.size(); i += ATTACH_INSTANCE_SIZE) {
                    try {
                        asgDAO.addInstancesToAutoScalingGroup(ids.subList(i, Math.min(ids.size(), i + ATTACH_INSTANCE_SIZE)), groupName);
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        LOG.error("Failed to attach instance to group {}", groupName, e);
                    }
                }

                if (request != null && !StringUtils.isEmpty(subnets)) {
                    AwsVmBean updateBean = new AwsVmBean();
                    updateBean.setMinSize(minSize);
                    LOG.info("Update auto scaling group {} with subnet {} and min size {}", groupName, subnets, minSize);
                    updateBean.setMinSize(minSize);
                    asgDAO.updateAutoScalingGroup(groupName, updateBean);
                }
            } catch (Exception ex) {
                LOG.error("Failed to process AttachInstanceToGroupJob for group {}", groupName, ex);
            } finally {
                return null;
            }
        }
    }

    private final class CreateSpotAutoScalingGroupJob implements Callable<Void> {

        private String clusterName;
        private String spotGroupName;
        private Double sensitivityRatio;

        public CreateSpotAutoScalingGroupJob(String clusterName, SpotAutoScalingBean spotAutoScalingBean) {
            this.clusterName = clusterName;
            spotGroupName = getSpotAutoScalingGroupName(clusterName);
            sensitivityRatio = spotAutoScalingBean.getSensitivity_ratio();
        }

        @Override
        public Void call() {
            try {
                createSpotAutoScalingGroup(clusterName);
            } catch (Exception ex) {
                LOG.error(String.format("Failed to create spot auto scaling group: %s", clusterName), ex);
            } finally {
                return null;
            }
        }

        private void createSpotAutoScalingGroup(String groupName) throws Exception {
            // step 1. update auto scaling policies
            Map<String, ScalingPolicyBean> scalingPolicies = asgDAO.getScalingPoliciesForGroup(groupName);
            if (scalingPolicies.isEmpty()) {
                return;
            }

            for (Map.Entry<String, ScalingPolicyBean> scalingPolicy : scalingPolicies.entrySet()) {
                asgDAO.addScalingPolicyToGroup(spotGroupName,  scalingPolicy.getValue());
            }


            // step 2. update alarms
            List<AsgAlarmBean> asgAlarmBeans = getAlarmInfoByGroup(groupName);
            if (asgAlarmBeans.isEmpty()) {
                return;
            }

            Map<String, ScalingPolicyBean> spotScalingPolicies = asgDAO.getScalingPoliciesForGroup(spotGroupName);
            List<AsgAlarmBean> spotAlarmInfos = new ArrayList<>();
            for (AsgAlarmBean asgAlarmBean : asgAlarmBeans) {
                spotAlarmInfos.add(generateSpotFleetAlarm(spotGroupName, sensitivityRatio, asgAlarmBean));
            }

            updateAlarmInfoInternal(groupName, spotScalingPolicies, spotAlarmInfos);
        }
    }

    private void updateLifeCycle(String groupName, GroupBean existingGroupBean, GroupBean newGroupBean) throws Exception {
        // Lifecycle management
        if (newGroupBean.getLifecycle_state() == null) {
            return;
        }

        if (newGroupBean.getLifecycle_state()) {
            // nothing to update
            if (newGroupBean.getLifecycle_timeout() == null) {
                return;
            }

            if (existingGroupBean != null &&  existingGroupBean.getLifecycle_state() &&
                existingGroupBean.getLifecycle_timeout().equals(newGroupBean.getLifecycle_timeout())) {
                return;
            }
            // Create Lifecycle hook
            LOG.info("Start to update lifecycle to group {}", groupName);
            asgDAO.createLifecycleHook(groupName, newGroupBean.getLifecycle_timeout().intValue());
        } else if (existingGroupBean != null && existingGroupBean.getLifecycle_state()) {
            // Delete Lifecycle hook
            String lifecycleHookId = String.format("LIFECYCLEHOOK-%s", groupName);
            LOG.info("Start to delete lifecycle {} to group {}", lifecycleHookId, groupName);
            asgDAO.deleteLifecycleHook(groupName);

        }
    }

    public GroupInfoBean getGroupInfoByClusterName(String clusterName) throws Exception {
        AwsVmBean awsVmBean = asgDAO.getAutoScalingGroupInfo(clusterName);
        GroupBean oldBean = groupInfoDAO.getGroupInfo(clusterName);
        if (awsVmBean == null && oldBean == null) {
            return null;
        } else if (awsVmBean == null) {
            awsVmBean = getGroupInfo(clusterName, oldBean);
        }

        GroupInfoBean groupInfoBean = new GroupInfoBean();
        groupInfoBean.setAwsVmBean(awsVmBean);
        groupInfoBean.setGroupBean(oldBean);
        return groupInfoBean;
    }

    public void updateCluster(String groupName, AwsVmBean awsVmBean) throws Exception {
        if (getCluster(groupName) == null) {
            updateLaunchConfig(groupName, awsVmBean);
            return;
        }

        awsVmManager.updateCluster(groupName, awsVmBean);
        // handle spot auto scaling group
        SpotAutoScalingBean spotAutoScalingBean = spotAutoScalingDAO.getAutoScalingGroupsByCluster(groupName);
        if (spotAutoScalingBean != null) {
            String spotGroupName = getSpotAutoScalingGroupName(groupName);
            awsVmManager.updateCluster(spotGroupName, awsVmBean);
            if (spotAutoScalingBean.getEnable_resource_lending()) {
                String lendingGroupName = getLendingAutoScalingGroupName(groupName);
                awsVmManager.updateCluster(lendingGroupName, awsVmBean);
            }
        }
    }

    public void createCluster(String groupName, AwsVmBean newAwsVmBean) throws Exception {
        awsVmManager.createCluster(groupName, newAwsVmBean);
        createNewPredictiveAutoScalingEntry(groupName);
    }

    public AwsVmBean getCluster(String clusterName) throws Exception {
        return asgDAO.getAutoScalingGroupInfo(clusterName);
    }


    public void updateGroupInfo(String clusterName, GroupBean groupbean) throws Exception {
        GroupBean existingGroupBean = groupInfoDAO.getGroupInfo(clusterName);
        groupInfoDAO.insertOrUpdateGroupInfo(clusterName, groupbean);
        updateLifeCycle(clusterName, existingGroupBean, groupbean);

        SpotAutoScalingBean spotAutoScalingBean = spotAutoScalingDAO.getAutoScalingGroupsByCluster(clusterName);
        if (spotAutoScalingBean == null) {
            return;
        }

        String spotGroupName = getSpotAutoScalingGroupName(clusterName);
        updateLifeCycle(spotGroupName, existingGroupBean,  groupbean);
        if (spotAutoScalingBean.getEnable_resource_lending()) {
            updateLifeCycle(getLendingAutoScalingGroupName(clusterName), existingGroupBean, groupbean);
        }

    }

    public void disableAutoScalingGroup(String clusterName) throws Exception {
        asgDAO.disableAutoScalingGroup(clusterName);
        GroupBean groupBean = new GroupBean();
        groupBean.setLast_update(System.currentTimeMillis());
        groupInfoDAO.updateGroupInfo(clusterName, groupBean);

        SpotAutoScalingBean spotAutoScalingBean = spotAutoScalingDAO.getAutoScalingGroupsByCluster(clusterName);
        if (spotAutoScalingBean == null) {
            return;
        }
        asgDAO.disableAutoScalingGroup(getSpotAutoScalingGroupName(clusterName));
    }

    public void enableAutoScalingGroup(String clusterName) throws Exception {
        asgDAO.enableAutoScalingGroup(clusterName);
        GroupBean groupBean = new GroupBean();
        groupBean.setLast_update(System.currentTimeMillis());
        groupInfoDAO.updateGroupInfo(clusterName, groupBean);

        SpotAutoScalingBean spotAutoScalingBean = spotAutoScalingDAO.getAutoScalingGroupsByCluster(clusterName);
        if (spotAutoScalingBean == null) {
            return;
        }
        asgDAO.enableAutoScalingGroup(getSpotAutoScalingGroupName(clusterName));
    }

    private void createSpotAutoScalingGroup(String clusterName, AwsVmBean awsVmBean, AutoScalingRequestBean request) throws Exception {
        String spotGroupName = getSpotAutoScalingGroupName(clusterName);
        String bidPrice = request.getSpotPrice();

        // step 1 create launch config

        AwsVmBean updatedVmBean = new AwsVmBean();

        updatedVmBean.setAssignPublicIp(awsVmBean.getAssignPublicIp());
        updatedVmBean.setClusterName(spotGroupName);
        updatedVmBean.setHostType(awsVmBean.getHostType());
        updatedVmBean.setImage(awsVmBean.getImage());
        updatedVmBean.setSecurityZone(awsVmBean.getSecurityZone());
        updatedVmBean.setRole(awsVmBean.getRole());
        updatedVmBean.setSubnet(awsVmBean.getSubnet());
        updatedVmBean.setRawUserDataString(awsVmBean.getRawUserDataString());

        updatedVmBean.setBidPrice(bidPrice);
        updatedVmBean.setTerminationPolicy(SPOT_AUTO_SCALING_TERMINATION_POLICY);
        updatedVmBean.setMinSize(0);
        updatedVmBean.setMaxSize((int)(request.getMaxSize() * request.getSpotRatio()));
        awsVmManager.createCluster(spotGroupName, updatedVmBean);

        // step 2 update datebase with the launch config
        SpotAutoScalingBean spotAutoScalingBean = new SpotAutoScalingBean();
        spotAutoScalingBean.setCluster_name(clusterName);
        spotAutoScalingBean.setBid_price(request.getSpotPrice());
        spotAutoScalingBean.setSpot_ratio(request.getSpotRatio());
        spotAutoScalingBean.setSensitivity_ratio(request.getSensitivityRatio());
        spotAutoScalingBean.setEnable_grow(false);
        spotAutoScalingBean.setEnable_resource_lending(request.getEnableResourceLending());

        spotAutoScalingDAO.insertAutoScalingGroupToCluster(spotGroupName, spotAutoScalingBean);

        GroupMappingBean groupMappingBean = new GroupMappingBean();
        groupMappingBean.setAsg_group_name(spotGroupName);
        groupMappingBean.setCluster_name(clusterName);

        groupMappingDAO.insertGroupMapping(spotGroupName, groupMappingBean);
        // make aws create auto scaling as an async job
        jobPool.submit(new CreateSpotAutoScalingGroupJob(clusterName, spotAutoScalingBean));
    }

    private void createLendingAutoScalingGroup(String clusterName, AwsVmBean awsVmBean, AutoScalingRequestBean request) throws Exception {
        AwsVmBean updatedVmBean = new AwsVmBean();
        String groupName = getLendingAutoScalingGroupName(clusterName);

        updatedVmBean.setAssignPublicIp(awsVmBean.getAssignPublicIp());
        updatedVmBean.setClusterName(groupName);
        updatedVmBean.setHostType(awsVmBean.getHostType());
        updatedVmBean.setImage(awsVmBean.getImage());
        updatedVmBean.setSecurityZone(awsVmBean.getSecurityZone());
        updatedVmBean.setRole(awsVmBean.getRole());
        updatedVmBean.setSubnet(awsVmBean.getSubnet());
        updatedVmBean.setRawUserDataString(awsVmBean.getRawUserDataString());

        updatedVmBean.setTerminationPolicy(SPOT_AUTO_SCALING_TERMINATION_POLICY);
        updatedVmBean.setMinSize(0);
        updatedVmBean.setMaxSize(0);
        awsVmManager.createCluster(groupName, updatedVmBean);

        // step 3 create managing group entry
        ManagingGroupsBean managingGroupsBean = new ManagingGroupsBean();
        managingGroupsBean.setMax_lending_size(0);
        managingGroupsBean.setGroup_name(clusterName);
        managingGroupsBean.setBatch_size(LENDING_INSTANCE_BATCH_SIZE);
        managingGroupsBean.setCool_down(LENDING_INSTANCE_COOL_DOWN_TIME);
        managingGroupsBean.setLent_size(0);
        managingGroupsBean.setLending_priority(0);
        managingGroupsBean.setInstance_type(awsVmBean.getHostType());
        managingGroupDAO.insertManagingGroup(clusterName, managingGroupsBean);

        GroupMappingBean groupMappingBean = new GroupMappingBean();
        groupMappingBean.setAsg_group_name(groupName);
        groupMappingBean.setCluster_name(clusterName);
        groupMappingDAO.insertGroupMapping(groupName, groupMappingBean);
    }

    private void updateAttachedAutoScalingGroups(String clusterName, AwsVmBean awsVmBean, AutoScalingRequestBean request) throws Exception {
        SpotAutoScalingBean autoScalingGroupBean = spotAutoScalingDAO.getAutoScalingGroupsByCluster(clusterName);
        String spotAutoScalingName = getSpotAutoScalingGroupName(clusterName);
        String lendingAutoScalingName = getLendingAutoScalingGroupName(clusterName);
        if (autoScalingGroupBean == null && request.getEnableSpot()) { // if we want to create spot auto scaling group
            createSpotAutoScalingGroup(clusterName, awsVmBean, request);
            if (request.getEnableResourceLending()) {
                createLendingAutoScalingGroup(clusterName, awsVmBean, request);
            }
        } else if (autoScalingGroupBean != null && !request.getEnableSpot()) {
            List<AsgAlarmBean> asgAlarmBeans = alarmDAO.getAlarmInfosByGroup(spotAutoScalingName);
            for (AsgAlarmBean asgAlarmBean : asgAlarmBeans) {
                alarmWatcher.deleteAlarmFromPolicy(asgAlarmBean);
                alarmDAO.deleteAlarmInfoById(asgAlarmBean.getAlarm_id());
            }
            awsVmManager.deleteCluster(spotAutoScalingName);
            groupMappingDAO.deleteGroupMapping(spotAutoScalingName);
            if (autoScalingGroupBean.getEnable_resource_lending()) {
                awsVmManager.deleteCluster(lendingAutoScalingName);
                groupMappingDAO.deleteGroupMapping(lendingAutoScalingName);
            }

            spotAutoScalingDAO.deleteAllAutoScalingGroupByCluster(spotAutoScalingName);
            managingGroupDAO.deleteManagingGroup(clusterName);
        } else if (autoScalingGroupBean != null) {
            if (autoScalingGroupBean.getBid_price() != null && !autoScalingGroupBean.getBid_price().equals(request.getSpotPrice())) {
                AwsVmBean spotAwsVmBean = new AwsVmBean();
                spotAwsVmBean.setClusterName(spotAutoScalingName);
                spotAwsVmBean.setBidPrice(request.getSpotPrice());
                awsVmManager.updateCluster(spotAutoScalingName, spotAwsVmBean);
                autoScalingGroupBean.setBid_price(request.getSpotPrice());
            }
            autoScalingGroupBean.setSpot_ratio(request.getSpotRatio());
            if (!autoScalingGroupBean.getSensitivity_ratio().equals(request.getSensitivityRatio())) {
                autoScalingGroupBean.setSensitivity_ratio(request.getSensitivityRatio());
                // update alarms.
                List<AsgAlarmBean> asgAlarmBeans = alarmDAO.getAlarmInfosByGroup(clusterName);
                List<AsgAlarmBean> spotAsgAlarmBeans = new ArrayList<>();
                Map<String, ScalingPolicyBean> spotPolicies = asgDAO.getScalingPoliciesForGroup(spotAutoScalingName);
                for (AsgAlarmBean asgAlarmBean : asgAlarmBeans) {
                    spotAsgAlarmBeans.add(generateSpotFleetAlarm(spotAutoScalingName, request.getSensitivityRatio(), asgAlarmBean));
                }
                updateAlarmInfoInternal(clusterName, spotPolicies, spotAsgAlarmBeans);
            }

            if (request.getEnableResourceLending() != null) {
                if (!autoScalingGroupBean.getEnable_resource_lending() && request
                    .getEnableResourceLending()) {
                    LOG.info(String.format("Creating lending auto scaling group for cluster %s", clusterName));
                    createLendingAutoScalingGroup(clusterName, awsVmBean, request);
                }

                if (autoScalingGroupBean.getEnable_resource_lending() && !request
                    .getEnableResourceLending()) {
                    LOG.info(String.format("Deleting lending auto scaling group for cluster %s", clusterName));
                    awsVmManager.deleteCluster(lendingAutoScalingName);
                    managingGroupDAO.deleteManagingGroup(clusterName);
                    groupMappingDAO.deleteGroupMapping(lendingAutoScalingName);
                }
                autoScalingGroupBean.setEnable_resource_lending(request.getEnableResourceLending());
            }
            spotAutoScalingDAO.updateSpotAutoScalingGroup(clusterName, autoScalingGroupBean);
            // We don't update the spot auto scaling group max size. The worker will do it async.
        }
    }

    public void createNewPredictiveAutoScalingEntry(String groupName) throws Exception {
        PasConfigBean pasConfigBean = new PasConfigBean();
        pasConfigBean.setGroup_name(groupName);
        pasConfigBean.setPas_state(PasState.DISABLED);
        pasConfigBean.setMetric("");
        pasConfigBean.setThroughput(0);
        pasConfigDAO.insertPasConfig(pasConfigBean);
    }

    private AwsVmBean generateInternalAutoScalingRequest(AwsVmBean groupBean, AutoScalingRequestBean requestBean, boolean spotFleet) throws Exception {
        String launchConfig = groupBean.getLaunchConfigId();
        String subnets = groupBean.getSubnet();
        AwsVmBean updateBean = new AwsVmBean();
        updateBean.setSubnet(subnets);
        updateBean.setLaunchConfigId(launchConfig);
        if (!spotFleet) {
            updateBean.setMaxSize(requestBean.getMaxSize());
            updateBean.setMinSize(requestBean.getMinSize());
            updateBean.setTerminationPolicy(requestBean.getTerminationPolicy());
        } else {
            updateBean.setTerminationPolicy(SPOT_AUTO_SCALING_TERMINATION_POLICY);
            updateBean.setMinSize(0);
            updateBean.setMaxSize((int)(requestBean.getMaxSize() * requestBean.getSpotRatio()));
        }
        return updateBean;
    }

    public void updateAutoScalingGroup(String groupName, AutoScalingRequestBean request) throws Exception {
        AwsVmBean awsVmBean = asgDAO.getAutoScalingGroupInfo(groupName);
        if (awsVmBean == null) {
            return;
        }

        asgDAO.updateAutoScalingGroup(groupName, generateInternalAutoScalingRequest(awsVmBean, request, false));

        // handle spot instance group
        updateAttachedAutoScalingGroups(groupName, awsVmBean, request);
    }

    public void deleteAutoScalingGroup(String clusterName, boolean detachInstance) throws Exception {
        // do it async
        jobPool.submit(new DeleteAutoScalingJob(clusterName, detachInstance, null));

        // handle spot auto scaling group case
        SpotAutoScalingBean
            spotAutoScalingBean =
            spotAutoScalingDAO.getAutoScalingGroupsByCluster(clusterName);
        if (spotAutoScalingBean == null) {
            return;
        }
        awsVmManager.deleteCluster(getSpotAutoScalingGroupName(clusterName));
        if (!spotAutoScalingBean.getEnable_resource_lending()) {
            return;
        }
        awsVmManager.deleteCluster(getLendingAutoScalingGroupName(clusterName));
    }

    public ASGStatus getAutoScalingGroupStatus(String groupName) throws Exception {
        return asgDAO.getAutoScalingGroupStatus(groupName);
    }

    public List<AutoScalingGroupBean> getAutoScalingGroupInfoByName(String clusterName) throws Exception {
        List<AutoScalingGroupBean> autoScalingGroupBeans = new ArrayList<>();
        AutoScalingGroupBean asgInfo = asgDAO.getAutoScalingGroupInfoByName(clusterName);
        asgInfo.setSpotGroup(false);

        if (asgInfo.getStatus() == ASGStatus.UNKNOWN) {
            // if the auto scaling is not even enabled for this group
            asgInfo.setMaxSize(AutoScalingConstants.DEFAULT_GROUP_CAPACITY);
            asgInfo.setMinSize(AutoScalingConstants.DEFAULT_GROUP_CAPACITY);
            autoScalingGroupBeans.add(asgInfo);
            return autoScalingGroupBeans;
        }

        autoScalingGroupBeans.add(asgInfo);
        // add spot instance
        SpotAutoScalingBean spotAutoScalingBean = spotAutoScalingDAO.getAutoScalingGroupsByCluster(clusterName);
        if (spotAutoScalingBean == null) {
            return autoScalingGroupBeans;
        }

        AutoScalingGroupBean spotAsgGroup = asgDAO.getAutoScalingGroupInfoByName(getSpotAutoScalingGroupName(clusterName));
        spotAsgGroup.setSpotGroup(true);
        autoScalingGroupBeans.add(spotAsgGroup);
        return autoScalingGroupBeans;
    }

    public AutoScalingSummaryBean getAutoScalingSummaryByName(String clusterName) throws Exception {
        AutoScalingGroupBean asgInfo = asgDAO.getAutoScalingGroupInfoByName(clusterName);
        AutoScalingSummaryBean autoScalingSummaryBean = new AutoScalingSummaryBean();
        autoScalingSummaryBean.setStatus(asgInfo.getStatus());
        autoScalingSummaryBean.setTerminationPolicy(asgInfo.getTerminationPolicy());
        autoScalingSummaryBean.setMinSize(asgInfo.getMinSize());
        autoScalingSummaryBean.setMaxSize(asgInfo.getMaxSize());
        autoScalingSummaryBean.setEnableSpot(false);
        autoScalingSummaryBean.setEnableResourceLending(false);
        autoScalingSummaryBean.setDesiredCapacity(asgInfo.getDesiredCapacity());

        if (asgInfo.getStatus() == ASGStatus.UNKNOWN) {
            return autoScalingSummaryBean;
        }

        SpotAutoScalingBean spotAutoScalingBean = spotAutoScalingDAO.getAutoScalingGroupsByCluster(clusterName);
        if (spotAutoScalingBean == null) {
            return autoScalingSummaryBean;
        }

        autoScalingSummaryBean.setEnableSpot(true);
        autoScalingSummaryBean.setBidPrice(spotAutoScalingBean.getBid_price());
        autoScalingSummaryBean.setSensitivityRatio(spotAutoScalingBean.getSensitivity_ratio());
        autoScalingSummaryBean.setSpotRatio(spotAutoScalingBean.getSpot_ratio());
        autoScalingSummaryBean.setEnableResourceLending(spotAutoScalingBean.getEnable_resource_lending());
        return autoScalingSummaryBean;
    }

    public ScalingPoliciesBean getScalingPolicyInfoByName(String groupName) throws Exception {
        ScalingPoliciesBean scalingPolicyInfo = new ScalingPoliciesBean();
        Map<String, ScalingPolicyBean> policies = asgDAO.getScalingPoliciesForGroup(groupName);

        if (policies.isEmpty()) {
            return scalingPolicyInfo;
        }

        for (Map.Entry<String, ScalingPolicyBean> policy : policies.entrySet()) {
            ScalingPolicyBean policyBean = policy.getValue();
            if (policyBean.getScaleSize() > 0) {
                scalingPolicyInfo.addScaleupPolicy(policyBean);

            } else {
                policyBean.setScaleSize(Math.abs(policyBean.getScaleSize()));
                scalingPolicyInfo.addScaleDownPolicy(policyBean);
            }
        }
        return scalingPolicyInfo;
    }

    public void putScalingPolicyToGroup(String groupName, ScalingPoliciesBean request) throws Exception {
        SpotAutoScalingBean spotAutoScalingBean = spotAutoScalingDAO.getAutoScalingGroupsByCluster(groupName);
        if (!request.getScaleUpPolicies().isEmpty()) {
            ScalingPolicyBean scaleUpPolicy = request.getScaleUpPolicies().get(0);
            scaleUpPolicy.setPolicyType(PolicyType.SCALEUP.toString());
            asgDAO.addScalingPolicyToGroup(groupName, scaleUpPolicy);

            if (spotAutoScalingBean != null) {
                asgDAO.addScalingPolicyToGroup(getSpotAutoScalingGroupName(groupName), scaleUpPolicy);
            }
        }

        if (!request.getScaleDownPolicies().isEmpty()) {
            ScalingPolicyBean scaleDownPolicy = request.getScaleDownPolicies().get(0);
            scaleDownPolicy.setPolicyType(PolicyType.SCALEDOWN.toString());
            scaleDownPolicy.setScaleSize(-1 * scaleDownPolicy.getScaleSize());
            asgDAO.addScalingPolicyToGroup(groupName, scaleDownPolicy);

            if (spotAutoScalingBean != null) {
                asgDAO.addScalingPolicyToGroup(getSpotAutoScalingGroupName(groupName), scaleDownPolicy);
            }
        }
    }

    public void addAlarmsToAutoScalingGroup(String groupName, List<AsgAlarmBean> alarmInfos) throws Exception {
        for (AsgAlarmBean asgAlarmBean : alarmInfos) {
            asgAlarmBean.setAlarm_id(CommonUtils.getBase64UUID());
        }

        Map<String, ScalingPolicyBean> policies = asgDAO.getScalingPoliciesForGroup(groupName);
        updateAlarmInfoInternal(groupName, policies, alarmInfos);

        SpotAutoScalingBean spotAutoScalingBean = spotAutoScalingDAO.getAutoScalingGroupsByCluster(groupName);
        if (spotAutoScalingBean == null) {
            return;
        }
        String spotGroupName = getSpotAutoScalingGroupName(groupName);
        Map<String, ScalingPolicyBean> spotPolicies = asgDAO.getScalingPoliciesForGroup(spotGroupName);

        List<AsgAlarmBean> spotAlarmInfos = new ArrayList<>();
        for (AsgAlarmBean asgAlarmBean : alarmInfos) {
            spotAlarmInfos.add(generateSpotFleetAlarm(spotGroupName, spotAutoScalingBean.getSensitivity_ratio(),  asgAlarmBean));
        }
        // the alarm should be set up to the reserved instance group space.
        updateAlarmInfoInternal(groupName, spotPolicies, spotAlarmInfos);
    }

    public void updateAlarmsToAutoScalingGroup(String groupName, List<AsgAlarmBean> alarmInfos) throws Exception {
        Map<String, ScalingPolicyBean> policies = asgDAO.getScalingPoliciesForGroup(groupName);
        updateAlarmInfoInternal(groupName, policies, alarmInfos);

        SpotAutoScalingBean spotAutoScalingBean = spotAutoScalingDAO.getAutoScalingGroupsByCluster(groupName);
        if (spotAutoScalingBean == null) {
            return;
        }

        String spotAutoScalingName = getSpotAutoScalingGroupName(groupName);
        List<AsgAlarmBean> spotAlarmInfos = new ArrayList<>();
        for (AsgAlarmBean asgAlarmBean : alarmInfos) {
            spotAlarmInfos.add(generateSpotFleetAlarm(spotAutoScalingName, spotAutoScalingBean.getSensitivity_ratio(), asgAlarmBean));
        }

        Map<String, ScalingPolicyBean> spotPolicies = asgDAO.getScalingPoliciesForGroup(spotAutoScalingName);
        updateAlarmInfoInternal(groupName, spotPolicies, spotAlarmInfos);

    }


    private AsgAlarmBean generateSpotFleetAlarm(String spotGroupName, Double sensitivity_ratio, AsgAlarmBean asgAlarmBean) throws Exception {
        AsgAlarmBean spotAlarmBean = new AsgAlarmBean();
        spotAlarmBean.setAlarm_id(String.format("%s-spot", asgAlarmBean.getAlarm_id()));
        spotAlarmBean.setAction_type(asgAlarmBean.getAction_type());
        spotAlarmBean.setComparator(asgAlarmBean.getComparator());
        spotAlarmBean.setEvaluation_time(asgAlarmBean.getEvaluation_time());
        spotAlarmBean.setFrom_aws_metric(asgAlarmBean.getFrom_aws_metric());
        spotAlarmBean.setGroup_name(spotGroupName);
        spotAlarmBean.setMetric_source(asgAlarmBean.getMetric_source());
        spotAlarmBean.setMetric_name(asgAlarmBean.getMetric_name());
        if (asgAlarmBean.getComparator().equals("LessThanOrEqualToThreshold")) { // makes spot auto scaling more sensitive
            spotAlarmBean.setThreshold(asgAlarmBean.getThreshold() * (1 + sensitivity_ratio));
        } else {
            spotAlarmBean.setThreshold(asgAlarmBean.getThreshold() * (1 - sensitivity_ratio));
        }
        return spotAlarmBean;
    }

    public void updateAlarmInfoInternal(String groupName, Map<String, ScalingPolicyBean> policies, List<AsgAlarmBean> alarmInfoRequests) throws Exception {
        String growPolicyARN = policies.get(PolicyType.SCALEUP.toString()).getARN();
        String shrinkPolicyARN = policies.get(PolicyType.SCALEDOWN.toString()).getARN();

        for (AsgAlarmBean asgAlarmBean : alarmInfoRequests) {
            if (asgAlarmBean.getFrom_aws_metric()) {
                asgAlarmBean.setMetric_name(asgAlarmBean.getMetric_source());
            } else {
                asgAlarmBean.setMetric_name(getMetricName(groupName, asgAlarmBean.getMetric_source()));
            }

            String autoScalingActionType = asgAlarmBean.getAction_type().toUpperCase();
            if (autoScalingActionType.equals(AutoScalingConstants.ASG_GROW)) {
                alarmWatcher.putAlarmToPolicy(growPolicyARN, groupName, asgAlarmBean);
            } else if (autoScalingActionType.equals(AutoScalingConstants.ASG_SHRINK)) {
                alarmWatcher.putAlarmToPolicy(shrinkPolicyARN, groupName, asgAlarmBean);
            } else {
                throw new Exception(String.format("Unsupported alarm action type: %s", autoScalingActionType));
            }

            alarmDAO.insertOrUpdateAlarmInfo(asgAlarmBean);
        }
    }

    public void deleteAlarmFromAutoScalingGroup(String alarmId) throws Exception {
        deleteAlarmFromAutoScalingGroupInternal(alarmId);
        String spotAlarmId = String.format("%s-spot", alarmId);
        deleteAlarmFromAutoScalingGroupInternal(spotAlarmId);
    }

    public void deleteAlarmFromAutoScalingGroupInternal(String alarmId) throws Exception {
        AsgAlarmBean asgAlarmBean = alarmDAO.getAlarmInfoById(alarmId);
        if (asgAlarmBean == null) {
            LOG.debug(String.format("Cannot find alarm with Id %s", alarmId));
            return;
        }
        alarmDAO.deleteAlarmInfoById(alarmId);
        alarmWatcher.deleteAlarmFromPolicy(asgAlarmBean);
    }

    public List<AsgAlarmBean> getAlarmInfoByGroup(String groupName) throws Exception {
        return alarmDAO.getAlarmInfosByGroup(groupName);
    }

    public List<String> listAwsMetricNames(String groupName) throws Exception {
        return alarmWatcher.listAwsMetrics(groupName);
    }


    public List<String> getEnvGroupNames(long pageIndex, int pageSize) throws Exception {
        return groupDAO.getExistingGroups(pageIndex, pageSize);
    }

    public ScalingActivitiesBean getScalingActivities(String groupName, int pageSize, String token) throws Exception {
        if (!asgDAO.getAutoScalingGroupStatus(groupName).equals(ASGStatus.UNKNOWN)) {
            return asgDAO.getScalingActivity(groupName, pageSize, token);
        } else {
            ScalingActivitiesBean scalingActivitiesInfo = new ScalingActivitiesBean();
            scalingActivitiesInfo.setActivities(new ArrayList<>());
            return scalingActivitiesInfo;
        }
    }

    public List<HostBean> getHostsByGroupName(String groupName) throws Exception {
        return hostDAO.getAllActiveHostsByGroup(groupName);
    }

    public void attachInstanceToAutoScalingGroup(List<String> instanceIds, String groupName) throws Exception {
        List<String> runningIds = hostInfoDAO.getRunningInstances(instanceIds);
        if (runningIds.isEmpty()) {
            LOG.info("Instances {} are not running. Cannot attach to group {}", instanceIds.toString(), groupName);
            return;
        }

        Collection<String> runningASGInstances = asgDAO.getAutoScalingInstances(groupName, runningIds);
        runningIds.removeAll(runningASGInstances);

        if (!runningIds.isEmpty()) {
            jobPool.submit(new AttachInstanceToGroupJob(groupName, null, runningIds, 0, null));
        }
    }

    public void protectInstancesInAutoScalingGroup(List<String> instanceIds, String groupName) throws Exception {
        List<String> runningIds = hostInfoDAO.getRunningInstances(instanceIds);
        if (runningIds.isEmpty()) {
            LOG.info("Instances {} are not running. Cannot attach to group {}", instanceIds.toString(), groupName);
            return;
        }
        asgDAO.protectInstanceInAutoScalingGroup(runningIds, groupName);
    }

    public void unprotectInstancesInAutoScalingGroup(List<String> instanceIds, String groupName) throws Exception {
        List<String> runningIds = hostInfoDAO.getRunningInstances(instanceIds);
        if (runningIds.isEmpty()) {
            LOG.info("Instances {} are not running. Cannot attach to group {}", instanceIds.toString(), groupName);
            return;
        }
        asgDAO.unprotectInstanceInAutoScalingGroup(runningIds, groupName);
    }

    public Boolean isInstanceProtected(String groupName, String instanceId) throws Exception {
        List<String> runningIds = hostInfoDAO.getRunningInstances(Collections.singletonList(instanceId));
        if (runningIds.isEmpty()) {
            LOG.info("Instances {} are not running. Cannot attach to group {}", instanceId, groupName);
            return false;
        }
        return asgDAO.isInstanceProtected(instanceId);
    }

    public void detachInstanceFromAutoScalingGroup(List<String> instanceIds, String groupName) throws Exception {
        Collection<String> runningIds = asgDAO.getAutoScalingInstances(groupName, instanceIds);

        // If detach it without decreasing the fleet size, ASG launches a new instance to replace it.
        if (!runningIds.isEmpty()) {
            LOG.info("Start to detach instance {} from group {}", instanceIds.toString(), groupName);
            asgDAO.detachInstancesFromAutoScalingGroup(runningIds, groupName, true);
        }
    }

    String getMetricName(String groupName, String metricResource) {
        String sha = CommonUtils.getShaHex(metricResource.getBytes()).substring(0, 9);
        return String.format("%s-metric-%s", groupName, sha);
    }


    // Health Check related
    public boolean isScalingDownEventEnabled(String groupName) throws Exception {
        return asgDAO.isScalingDownEventEnabled(groupName);
    }

    public void enableScalingDownEvent(String groupName) throws Exception {
        LOG.info("Start to enable scaling down for group {}", groupName);
        asgDAO.enableScalingDownEvent(groupName);
    }

    public void disableScalingDownEvent(String groupName) throws Exception {
        LOG.info("Start to disable scaling down for group {}", groupName);
        asgDAO.disableScalingDownEvent(groupName);
    }

    public void updateHealthCheckState(String groupName, boolean state) throws Exception {
        GroupBean bean = new GroupBean();
        bean.setHealthcheck_state(state);
        groupInfoDAO.updateGroupInfo(groupName, bean);
    }

    public void putScheduledActionsToAutoScalingGroup(String clusterName, Collection<AsgScheduleBean> asgScheduleBeans) throws Exception {
        for (AsgScheduleBean asgScheduleBean : asgScheduleBeans) {
            LOG.info(String.format("Start to add scheduled actions to %s: %s", clusterName, asgScheduleBean.toString()));
            asgDAO.putScheduledAction(clusterName, asgScheduleBean);
        }
    }

    public Collection<AsgScheduleBean> getScheduledActionsByAutoScalingGroup(String clusterName) throws Exception {
        return asgDAO.getScheduledActions(clusterName);
    }

    public void deleteScheduledActionFromAutoScalingGroup(String clusterName, String actionId) throws Exception {
        asgDAO.deleteScheduledAction(clusterName, actionId);
    }

    // the following functions is used for backward compatible only
    private void updateLaunchConfig(String clusterName, AwsVmBean awsVmBean) throws Exception {
        GroupBean groupBean = groupInfoDAO.getGroupInfo(clusterName);
        String launchConfig = groupBean.getLaunch_config_id();
        AwsVmBean oldAwsVmBean = asgDAO.getLaunchConfigInfo(launchConfig);
        String newLaunchConfig = awsVmManager.updateLaunchConfig(clusterName, oldAwsVmBean, awsVmBean);

        groupBean.setLaunch_config_id(newLaunchConfig);
        if (awsVmBean.getSubnet() != null) {
            groupBean.setSubnets(awsVmBean.getSubnet());
        }
        groupInfoDAO.updateGroupInfo(clusterName, groupBean);
    }

    public AwsVmBean getGroupInfo(String clusterName, GroupBean groupBean) throws Exception {
        AwsVmBean awsVmBean = asgDAO.getLaunchConfigInfo(groupBean.getLaunch_config_id());
        if (awsVmBean == null) {
            return null;
        }

        if (groupBean.getSubnets() != null) {
            awsVmBean.setSubnet(groupBean.getSubnets());
        }
        awsVmBean.setAsgStatus(ASGStatus.UNKNOWN);
        awsVmBean.setClusterName(clusterName);
        return awsVmBean;
    }

    public void createAutoScalingGroup(String clusterName, AwsVmBean awsVmBean) throws Exception {
        GroupBean groupBean = groupInfoDAO.getGroupInfo(clusterName);
        String launchConfig = groupBean.getLaunch_config_id();
        awsVmBean.setLaunchConfigId(launchConfig);
        awsVmBean.setSubnet(groupBean.getSubnets());
        asgDAO.createAutoScalingGroup(clusterName, awsVmBean);
    }

    private String getSpotAutoScalingGroupName(String clusterName) {
        return String.format("%s-spot", clusterName);
    }

    private String getLendingAutoScalingGroupName(String clusterName) {
        return String.format("%s-lending", clusterName);
    }
}
