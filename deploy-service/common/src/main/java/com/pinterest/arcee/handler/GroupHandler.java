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
import com.pinterest.arcee.common.HealthCheckConstants;
import com.pinterest.arcee.dao.*;
import com.pinterest.arcee.dao.AlarmDAO;
import com.pinterest.arcee.dao.GroupInfoDAO;
import com.pinterest.arcee.dao.HostInfoDAO;
import com.pinterest.arcee.dao.SpotAutoScalingDAO;
import com.pinterest.arcee.common.HealthCheckConstants;
import com.pinterest.arcee.dao.*;
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
        pasConfigDAO = serviceContext.getPasConfigDAO();
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
            spotGroupName = spotAutoScalingBean.getAsg_name();
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
        if (awsVmBean == null) {
            return null;
        }

        GroupBean oldBean = groupInfoDAO.getGroupInfo(clusterName);
        GroupInfoBean groupInfoBean = new GroupInfoBean();
        groupInfoBean.setAwsVmBean(awsVmBean);
        groupInfoBean.setGroupBean(oldBean);
        return groupInfoBean;
    }

    public void updateCluster(String groupName, AwsVmBean awsVmBean) throws Exception {
        awsVmManager.updateCluster(groupName, awsVmBean);
        // handle spot auto scaling group
        List<SpotAutoScalingBean> spotAutoScalingGroups = spotAutoScalingDAO.getAutoScalingGroupsByCluster(groupName);
        for (SpotAutoScalingBean spotAutoScalingBean : spotAutoScalingGroups) {
            String spotGroupName = spotAutoScalingBean.getAsg_name();
            awsVmManager.updateCluster(spotGroupName, awsVmBean);
        }
    }

    public void createCluster(String groupName, AwsVmBean newAwsVmBean) throws Exception {
        awsVmManager.createCluster(groupName, newAwsVmBean);
    }

    public AwsVmBean getCluster(String clusterName) throws Exception {
        return asgDAO.getAutoScalingGroupInfo(clusterName);
    }


    public void updateGroupInfo(String clusterName, GroupBean groupbean) throws Exception {
        GroupBean existingGroupBean = groupInfoDAO.getGroupInfo(clusterName);
        groupInfoDAO.insertOrUpdateGroupInfo(clusterName, groupbean);
        updateLifeCycle(clusterName, existingGroupBean, groupbean);

        List<SpotAutoScalingBean> spotAutoScalingBeans = spotAutoScalingDAO.getAutoScalingGroupsByCluster(clusterName);
        for (SpotAutoScalingBean spotAutoScalingBean : spotAutoScalingBeans) {
            String spotGroupName = spotAutoScalingBean.getAsg_name();
            updateLifeCycle(spotGroupName, existingGroupBean, groupbean);
        }
    }

    public void createGroupInfo(String clusterName, GroupBean groupBean) throws Exception {
        groupBean.setGroup_name(clusterName);
        groupInfoDAO.insertOrUpdateGroupInfo(clusterName, groupBean);
        if (groupBean.getLifecycle_state()) {
            updateLifeCycle(clusterName, null, groupBean);
        }
    }

    public GroupBean getGroupInfo(String clusterName) throws Exception {
        return groupInfoDAO.getGroupInfo(clusterName);
    }

    public void disableAutoScalingGroup(String groupName) throws Exception {
        asgDAO.disableAutoScalingGroup(groupName);
        GroupBean groupBean = new GroupBean();
        groupBean.setLast_update(System.currentTimeMillis());
        groupInfoDAO.updateGroupInfo(groupName, groupBean);

        List<SpotAutoScalingBean> spotAutoScalingBeans = spotAutoScalingDAO.getAutoScalingGroupsByCluster(groupName);
        for (SpotAutoScalingBean spotAutoScalingBean : spotAutoScalingBeans) {
            asgDAO.disableAutoScalingGroup(spotAutoScalingBean.getAsg_name());
        }
    }

    public void enableAutoScalingGroup(String groupName) throws Exception {
        asgDAO.enableAutoScalingGroup(groupName);
        GroupBean groupBean = new GroupBean();
        groupBean.setLast_update(System.currentTimeMillis());
        groupInfoDAO.updateGroupInfo(groupName, groupBean);

        List<SpotAutoScalingBean> spotAutoScalingBeans = spotAutoScalingDAO.getAutoScalingGroupsByCluster(groupName);
        for (SpotAutoScalingBean spotAutoScalingBean : spotAutoScalingBeans) {
            asgDAO.enableAutoScalingGroup(spotAutoScalingBean.getAsg_name());
        }
    }

    private void createSpotAutoScalingGroup(String clusterName, AwsVmBean awsVmBean, AutoScalingRequestBean request) throws Exception {
        String spotGroupName = String.format("%s-spot", clusterName);
        String bidPrice = request.getSpotPrice();

        // step 1 create launch config
        String newConfig = asgDAO.createSpotLaunchConfig(spotGroupName, awsVmBean, bidPrice);

        AwsVmBean updatedVmBean = new AwsVmBean();
        updatedVmBean.setLaunchConfigId(newConfig);
        updatedVmBean.setSubnet(awsVmBean.getSubnet());
        updatedVmBean.setTerminationPolicy(SPOT_AUTO_SCALING_TERMINATION_POLICY);
        updatedVmBean.setMinSize(0);
        updatedVmBean.setMaxSize((int)(request.getMaxSize() * request.getSpotRatio()));

        asgDAO.createAutoScalingGroup(spotGroupName, updatedVmBean);
        createNewPredictiveAutoScalingEntry(clusterName);

        // step 2 update datebase with the launch config
        SpotAutoScalingBean spotAutoScalingBean = new SpotAutoScalingBean();
        spotAutoScalingBean.setAsg_name(spotGroupName);
        spotAutoScalingBean.setCluster_name(clusterName);
        spotAutoScalingBean.setBid_price(request.getSpotPrice());
        spotAutoScalingBean.setSpot_ratio(request.getSpotRatio());
        spotAutoScalingBean.setLaunch_config_id(newConfig);
        spotAutoScalingBean.setSensitivity_ratio(request.getSensitivityRatio());
        spotAutoScalingBean.setEnable_grow(false);
        spotAutoScalingDAO.insertAutoScalingGroupToCluster(spotGroupName, spotAutoScalingBean);
        // make aws create auto scaling as an async job
        jobPool.submit(new CreateSpotAutoScalingGroupJob(clusterName, spotAutoScalingBean));
    }

    public void createNewPredictiveAutoScalingEntry(String groupName) throws Exception {
        PasConfigBean pasConfigBean = new PasConfigBean();
        pasConfigBean.setGroup_name(groupName);
        pasConfigBean.setPas_state("DISABLED");
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
        List<SpotAutoScalingBean> autoScalingGroupBeans = spotAutoScalingDAO.getAutoScalingGroupsByCluster(groupName);
        if (autoScalingGroupBeans.isEmpty() && request.getEnableSpot()) { // if we want to create spot auto scaling group
            createSpotAutoScalingGroup(groupName, awsVmBean, request);
        } else if (!autoScalingGroupBeans.isEmpty() && !request.getEnableSpot()) {
            for (SpotAutoScalingBean autoScalingBean : autoScalingGroupBeans) {
                spotAutoScalingDAO.deleteAutoScalingGroupFromCluster(autoScalingBean.getAsg_name());
                String LaunchConfig = autoScalingBean.getLaunch_config_id();
                jobPool.submit(
                    new DeleteAutoScalingJob(autoScalingBean.getAsg_name(), true, LaunchConfig));
            }
        } else {
            for (SpotAutoScalingBean autoScalingBean : autoScalingGroupBeans) {
                if (!autoScalingBean.getBid_price().equals(request.getSpotPrice())) {

                    String existingConfig = autoScalingBean.getLaunch_config_id();
                    String spotAutoScalingName = autoScalingBean.getAsg_name();
                    String newSpotConfig = asgDAO.createSpotLaunchConfig(spotAutoScalingName, awsVmBean, request.getSpotPrice());
                    autoScalingBean.setLaunch_config_id(newSpotConfig);
                    autoScalingBean.setBid_price(request.getSpotPrice());

                    // update aws auto scaling group
                    AwsVmBean newAwsVmBean = new AwsVmBean();
                    newAwsVmBean.setLaunchConfigId(newSpotConfig);
                    asgDAO.updateAutoScalingGroup(spotAutoScalingName, newAwsVmBean);
                    asgDAO.deleteLaunchConfig(existingConfig);
                }
                autoScalingBean.setSpot_ratio(request.getSpotRatio());
                if (!autoScalingBean.getSensitivity_ratio().equals(request.getSensitivityRatio())) {
                    autoScalingBean.setSensitivity_ratio(request.getSensitivityRatio());

                    // update alarms.
                    List<AsgAlarmBean> asgAlarmBeans = alarmDAO.getAlarmInfosByGroup(groupName);
                    List<AsgAlarmBean> spotAsgAlarmBeans = new ArrayList<>();
                    Map<String, ScalingPolicyBean> spotPolicies = asgDAO.getScalingPoliciesForGroup(autoScalingBean.getAsg_name());
                    for (AsgAlarmBean asgAlarmBean : asgAlarmBeans) {
                        spotAsgAlarmBeans.add(generateSpotFleetAlarm(autoScalingBean.getAsg_name(), request.getSensitivityRatio(), asgAlarmBean));
                    }

                    updateAlarmInfoInternal(groupName, spotPolicies, spotAsgAlarmBeans);
                }
                spotAutoScalingDAO.updateSpotAutoScalingGroup(autoScalingBean.getAsg_name(), autoScalingBean);
                // We don't update the spot auto scaling group max size. The worker will do it async.
            }
        }
    }

    public void deleteAutoScalingGroup(String groupName, boolean detachInstance) throws Exception {
        // do it async
        jobPool.submit(new DeleteAutoScalingJob(groupName, detachInstance, null));

        // handle spot auto scaling group case
        List<SpotAutoScalingBean> spotAutoScalingBeans = spotAutoScalingDAO.getAutoScalingGroupsByCluster(
            groupName);
        for (SpotAutoScalingBean spotAutoScalingBean : spotAutoScalingBeans) {
            String spotLaunchConfig = spotAutoScalingBean.getLaunch_config_id();
            spotAutoScalingDAO.deleteAutoScalingGroupFromCluster(spotAutoScalingBean.getAsg_name());
            jobPool.submit(new DeleteAutoScalingJob(spotAutoScalingBean.getAsg_name(), detachInstance, spotLaunchConfig));
        }
    }

    public ASGStatus getAutoScalingGroupStatus(String groupName) throws Exception {
        return asgDAO.getAutoScalingGroupStatus(groupName);
    }

    public List<AutoScalingGroupBean> getAutoScalingGroupInfoByName(String groupName) throws Exception {
        List<AutoScalingGroupBean> autoScalingGroupBeans = new ArrayList<>();
        AutoScalingGroupBean asgInfo = asgDAO.getAutoScalingGroupInfoByName(groupName);
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
        List<SpotAutoScalingBean> spotAutoScalingBeans = spotAutoScalingDAO.getAutoScalingGroupsByCluster(groupName);
        for (SpotAutoScalingBean spotAutoScalingBean : spotAutoScalingBeans) {
            AutoScalingGroupBean spotAsgGroup = asgDAO.getAutoScalingGroupInfoByName(spotAutoScalingBean.getAsg_name());
            spotAsgGroup.setSpotGroup(true);
            autoScalingGroupBeans.add(spotAsgGroup);
        }
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
        autoScalingSummaryBean.setDesiredCapacity(asgInfo.getDesiredCapacity());
        if (asgInfo.getStatus() == ASGStatus.UNKNOWN) {
            return autoScalingSummaryBean;
        }

        List<SpotAutoScalingBean> spotAutoScalingBeans = spotAutoScalingDAO.getAutoScalingGroupsByCluster(clusterName);
        if (spotAutoScalingBeans.isEmpty()) {
            return autoScalingSummaryBean;
        }

        autoScalingSummaryBean.setEnableSpot(true);
        SpotAutoScalingBean spotAutoScalingBean = spotAutoScalingBeans.get(0);
        autoScalingSummaryBean.setBidPrice(spotAutoScalingBean.getBid_price());
        autoScalingSummaryBean.setSensitivityRatio(spotAutoScalingBean.getSensitivity_ratio());
        autoScalingSummaryBean.setSpotRatio(spotAutoScalingBean.getSpot_ratio());
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
        List<SpotAutoScalingBean> spotAutoScalingBeans = spotAutoScalingDAO.getAutoScalingGroupsByCluster(groupName);
        if (!request.getScaleUpPolicies().isEmpty()) {
            ScalingPolicyBean scaleUpPolicy = request.getScaleUpPolicies().get(0);
            scaleUpPolicy.setPolicyType(PolicyType.SCALEUP.toString());
            asgDAO.addScalingPolicyToGroup(groupName, scaleUpPolicy);


            for (SpotAutoScalingBean mappingBean : spotAutoScalingBeans) {
                asgDAO.addScalingPolicyToGroup(mappingBean.getAsg_name(), scaleUpPolicy);
            }
        }

        if (!request.getScaleDownPolicies().isEmpty()) {
            ScalingPolicyBean scaleDownPolicy = request.getScaleDownPolicies().get(0);
            scaleDownPolicy.setPolicyType(PolicyType.SCALEDOWN.toString());
            scaleDownPolicy.setScaleSize(-1 * scaleDownPolicy.getScaleSize());
            asgDAO.addScalingPolicyToGroup(groupName, scaleDownPolicy);

            for (SpotAutoScalingBean mappingBean : spotAutoScalingBeans) {
                asgDAO.addScalingPolicyToGroup(mappingBean.getAsg_name(), scaleDownPolicy);
            }
        }
    }

    public void addAlarmsToAutoScalingGroup(String groupName, List<AsgAlarmBean> alarmInfos) throws Exception {
        for (AsgAlarmBean asgAlarmBean : alarmInfos) {
            asgAlarmBean.setAlarm_id(CommonUtils.getBase64UUID());
        }

        Map<String, ScalingPolicyBean> policies = asgDAO.getScalingPoliciesForGroup(groupName);
        updateAlarmInfoInternal(groupName, policies, alarmInfos);

        List<SpotAutoScalingBean> spotAutoScalingBeans = spotAutoScalingDAO.getAutoScalingGroupsByCluster(groupName);
        for (SpotAutoScalingBean spotAutoScalingBean : spotAutoScalingBeans) {
            String spotGroupName = spotAutoScalingBean.getAsg_name();
            Map<String, ScalingPolicyBean> spotPolicies = asgDAO.getScalingPoliciesForGroup(spotGroupName);

            List<AsgAlarmBean> spotAlarmInfos = new ArrayList<>();
            for (AsgAlarmBean asgAlarmBean : alarmInfos) {
                spotAlarmInfos.add(
                    generateSpotFleetAlarm(spotAutoScalingBean.getAsg_name(), spotAutoScalingBean.getSensitivity_ratio(),  asgAlarmBean));
            }
            // the alarm should be set up to the reserved instance group space.
            updateAlarmInfoInternal(groupName, spotPolicies, spotAlarmInfos);
        }

    }

    public void updateAlarmsToAutoScalingGroup(String groupName, List<AsgAlarmBean> alarmInfos) throws Exception {
        Map<String, ScalingPolicyBean> policies = asgDAO.getScalingPoliciesForGroup(groupName);
        updateAlarmInfoInternal(groupName, policies, alarmInfos);

        List<SpotAutoScalingBean> spotAutoScalingBeans = spotAutoScalingDAO.getAutoScalingGroupsByCluster(groupName);
        for (SpotAutoScalingBean spotAutoScalingBean : spotAutoScalingBeans) {
            List<AsgAlarmBean> spotAlarmInfos = new ArrayList<>();
            for (AsgAlarmBean asgAlarmBean : alarmInfos) {
                spotAlarmInfos.add(generateSpotFleetAlarm(spotAutoScalingBean.getAsg_name(), spotAutoScalingBean.getSensitivity_ratio(), asgAlarmBean));
            }

            String spotGroupName = spotAutoScalingBean.getAsg_name();
            Map<String, ScalingPolicyBean> spotPolicies = asgDAO.getScalingPoliciesForGroup(spotGroupName);
            updateAlarmInfoInternal(groupName, spotPolicies, spotAlarmInfos);
        }
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
}
