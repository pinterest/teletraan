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
import com.pinterest.arcee.dao.AlarmDAO;
import com.pinterest.arcee.dao.SpotAutoScalingDAO;
import com.pinterest.arcee.dao.GroupInfoDAO;
import com.pinterest.arcee.dao.HostInfoDAO;
import com.pinterest.clusterservice.bean.AwsVmBean;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.ASGStatus;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.common.CommonUtils;
import com.pinterest.deployservice.dao.GroupDAO;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.dao.UtilDAO;
import com.pinterest.deployservice.handler.CommonHandler;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.*;
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
    private UtilDAO utilDAO;
    private SpotAutoScalingDAO spotAutoScalingDAO;
    private ExecutorService jobPool;
    private CommonHandler commonHandler;

    public GroupHandler(ServiceContext serviceContext) {
        asgDAO = serviceContext.getAutoScalingManager();
        hostDAO = serviceContext.getHostDAO();
        groupDAO = serviceContext.getGroupDAO();
        groupInfoDAO = serviceContext.getGroupInfoDAO();
        alarmDAO = serviceContext.getAlarmDAO();
        alarmWatcher = serviceContext.getAlarmManager();
        jobPool = serviceContext.getJobPool();
        utilDAO = serviceContext.getUtilDAO();
        hostInfoDAO = serviceContext.getHostInfoDAO();
        spotAutoScalingDAO = serviceContext.getSpotAutoScalingDAO();
        commonHandler = new CommonHandler(serviceContext);
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
                asgDAO.deleteAutoScalingGroup(groupName, detachInstances);
                LOG.info(String.format("Delete autoscaling group: %s", groupName));

                // step 3 update the groups db
                if (!StringUtils.isEmpty(spotLaunchConfig)) {
                    LOG.info(String.format("Delete launch config for group: %s", groupName));
                    asgDAO.deleteLaunchConfig(spotLaunchConfig);

                } else {
                    GroupBean groupBean = groupInfoDAO.getGroupInfo(groupName);
                    if (groupBean != null) {
                        GroupBean newBean = new GroupBean();
                        newBean.setAsg_status(ASGStatus.UNKNOWN);
                        newBean.setLast_update(System.currentTimeMillis());
                        groupInfoDAO.updateGroupInfo(groupName, newBean);
                    }
                }
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


    // manage launch config
    private boolean needUpdateLaunchConfig(GroupBean oldBean, GroupBean newBean) {
        if (!newBean.getInstance_type().equals(oldBean.getInstance_type())) {
            return true;
        }

        if (!newBean.getImage_id().equals(oldBean.getImage_id())) {
            return true;
        }

        if (!newBean.getSecurity_group().equals(oldBean.getSecurity_group())) {
            return true;
        }

        if (!newBean.getUser_data().equals(oldBean.getUser_data())) {
            return true;
        }

        if (!newBean.getIam_role().equals(oldBean.getIam_role())) {
            return true;
        }

        return !newBean.getAssign_public_ip().equals(oldBean.getAssign_public_ip());
    }

    private String changeConfig(String groupName, GroupBean newGroupBean, String bidPrice) throws Exception {
        AwsVmBean awsVmBean = generateVmBean(groupName, newGroupBean);
        String newConfig;
        if (bidPrice == null) {
            newConfig = asgDAO.createLaunchConfig(groupName, awsVmBean);
        } else {
            newConfig = asgDAO.createSpotLaunchConfig(groupName, awsVmBean, bidPrice);
        }
        return newConfig;
    }

    private void updateLifeCycle(String groupName, GroupBean existingGroupBean, GroupBean newGroupBean) throws Exception {
        // Lifecycle management
        if (newGroupBean.getLifecycle_state()) {
            // Create Lifecycle hook
            if (!existingGroupBean.getLifecycle_state()) {
                LOG.info("Start to add a lifecycle to group {}", groupName);
                asgDAO.createLifecycleHook(groupName, newGroupBean.getLifecycle_timeout().intValue());
            } else {
                // Update Lifecycle hook only when timeout changes
                if (!newGroupBean.getLifecycle_timeout().equals(existingGroupBean.getLifecycle_timeout())) {
                    LOG.info("Start to update the lifecycle to group {}", groupName);
                    asgDAO.createLifecycleHook(groupName, newGroupBean.getLifecycle_timeout().intValue());
                }
            }
        } else if (existingGroupBean.getLifecycle_state()) {
            // Delete Lifecycle hook
            String lifecycleHookId = String.format("LIFECYCLEHOOK-%s", groupName);
            LOG.info("Start to delete lifecycle {} to group {}", lifecycleHookId, groupName);
            asgDAO.deleteLifecycleHook(groupName);

        }
    }

    public GroupBean getGroupInfoByClusterName(String clusterName) throws Exception {
        GroupBean oldBean = groupInfoDAO.getGroupInfo(clusterName);
        if (oldBean == null) {
            updateLaunchConfigInternal(null, clusterName);
            oldBean = groupInfoDAO.getGroupInfo(clusterName);
        }

        String launchConfigId = oldBean.getLaunch_config_id();
        // TODO use for migration
        if (launchConfigId == null) {
            launchConfigId = updateLaunchConfigInternal(oldBean, clusterName);
        }

        AwsVmBean awsVmBean = asgDAO.getLaunchConfigInfo(launchConfigId);
        if (awsVmBean == null) {
            launchConfigId = updateLaunchConfigInternal(oldBean, clusterName);
            awsVmBean = asgDAO.getLaunchConfigInfo(launchConfigId);
        }
        
        GroupBean groupBean = new GroupBean();
        if (oldBean.getChatroom() != null) {
            groupBean.setChatroom(oldBean.getChatroom());
        }

        if (oldBean.getWatch_recipients() != null) {
            groupBean.setWatch_recipients(oldBean.getWatch_recipients());
        }

        if (oldBean.getPager_recipients() != null) {
            groupBean.setPager_recipients(oldBean.getPager_recipients());
        }

        if (oldBean.getAsg_status() != null) {
            groupBean.setAsg_status(oldBean.getAsg_status());
        }

        if (oldBean.getSubnets() != null) {
            groupBean.setSubnets(oldBean.getSubnets());
        }

        groupBean.setGroup_name(clusterName);
        groupBean.setImage_id(awsVmBean.getImage());
        groupBean.setInstance_type(awsVmBean.getHostType());
        groupBean.setSecurity_group(awsVmBean.getSecurityZone());
        groupBean.setAssign_public_ip(awsVmBean.getAssignPublicIp());
        groupBean.setLaunch_config_id(awsVmBean.getLaunchConfigId());
        groupBean.setIam_role(awsVmBean.getRole());
        groupBean.setUser_data(awsVmBean.getRawUserDataString());
        groupBean.setLaunch_latency_th(oldBean.getLaunch_latency_th());
        groupBean.setHealthcheck_period(oldBean.getHealthcheck_period());
        groupBean.setHealthcheck_state(oldBean.getHealthcheck_state());
        groupBean.setLifecycle_state(oldBean.getLifecycle_state());
        groupBean.setLifecycle_timeout(oldBean.getLifecycle_timeout());
        groupBean.setLast_update(oldBean.getLast_update());
        return groupBean;
    }

    // TODO used for migration only
    private String updateLaunchConfigInternal(GroupBean oldBean, String clusterName) throws Exception {
        String processLockName = String.format("CREATE-%s", clusterName);
        Connection connection = utilDAO.getLock(processLockName);
        if (connection == null) {
            LOG.error("Failed to grab CREATE_GROUP_INFO_LOCK for group = {}. This means someone else is holding it, exiting", clusterName);
            return null;
        }
        try {
            if (oldBean != null) {
                if (oldBean.getUser_data() == null) {
                    oldBean.setUser_data(String.format("#cloud-config\nrole: %s\n", clusterName));
                } else if (!oldBean.getUser_data().contains("#cloud-config")) {
                    String userData = new String(Base64.decodeBase64(oldBean.getUser_data()));
                    oldBean.setUser_data(userData);
                }

                if (oldBean.getIam_role() == null) {
                    oldBean.setIam_role(AutoScalingConstants.DEFAULT_IAM_ROLE);
                } else if (oldBean.getIam_role().contains("/")) {
                    oldBean.setIam_role(oldBean.getIam_role().split("/")[1]);
                }

                String launchConfigId = changeConfig(clusterName, oldBean, null);
                GroupBean newBean = new GroupBean();
                newBean.setLaunch_config_id(launchConfigId);
                groupInfoDAO.updateGroupInfo(clusterName, newBean);
                return launchConfigId;
            } else {
                GroupBean groupBean = generateDefaultGroupBean(clusterName);
                String launchConfigId = changeConfig(clusterName, groupBean, null);
                groupBean.setLaunch_config_id(launchConfigId);
                groupInfoDAO.insertGroupInfo(groupBean);
                return launchConfigId;
            }
        } catch (Exception ex) {
            LOG.error("Failed to generate config for group", ex);
            return null;
        } finally {
            utilDAO.releaseLock(processLockName, connection);
        }
    }

    public void updateLaunchConfig(String groupName, GroupBean newGroupBean) throws Exception {
        GroupBean groupBean = getGroupInfoByClusterName(groupName);
        if (groupBean == null) {
            groupBean = generateDefaultGroupBean(groupName);
        }
        // patch missing field in the newGroupBean with existing/default value
        newGroupBean = generateUpdatedGroupBean(groupBean, newGroupBean);

        // check if we should update aws autoscaling launch config
        boolean needUpdateLaunchConfig = needUpdateLaunchConfig(newGroupBean, groupBean);
        boolean needUpdateSubnets = newGroupBean.getSubnets() != null && groupBean.getSubnets() != null && !newGroupBean.getSubnets().equals(groupBean.getSubnets());

        if (needUpdateLaunchConfig) {
            String newConfig = changeConfig(groupName, newGroupBean, null);
            newGroupBean.setLaunch_config_id(newConfig);
        }

        boolean enabledAutoScaling = (!asgDAO.getAutoScalingGroupStatus(groupName).equals(ASGStatus.UNKNOWN));
        if (enabledAutoScaling &&(needUpdateLaunchConfig || needUpdateSubnets)) {
            AwsVmBean updateBean = new AwsVmBean();
            updateBean.setLaunchConfigId(newGroupBean.getLaunch_config_id());
            updateBean.setSubnet(newGroupBean.getSubnets());
            asgDAO.updateAutoScalingGroup(groupName, updateBean);
            if (needUpdateLaunchConfig) {
                asgDAO.deleteLaunchConfig(groupBean.getLaunch_config_id());
            }
        }

        updateLifeCycle(groupName, groupBean, newGroupBean);
        groupInfoDAO.updateGroupInfo(groupName, newGroupBean);

        // handle spot auto scaling group
        List<SpotAutoScalingBean> spotAutoScalingGroups = spotAutoScalingDAO.getAutoScalingGroupsByCluster(groupName);
        for (SpotAutoScalingBean spotAutoScalingBean : spotAutoScalingGroups) {
            String spotGroupName = spotAutoScalingBean.getAsg_name();
            AwsVmBean updateBean = new AwsVmBean();
            if (needUpdateLaunchConfig) {
                String newConfig = changeConfig(spotGroupName, newGroupBean, spotAutoScalingBean.getBid_price());
                SpotAutoScalingBean newSpotAutoScalingBean = new SpotAutoScalingBean();
                newSpotAutoScalingBean.setLaunch_config_id(newConfig);
                updateBean.setLaunchConfigId(newConfig);
                spotAutoScalingDAO.updateSpotAutoScalingGroup(spotGroupName, newSpotAutoScalingBean);
            }
            if (needUpdateLaunchConfig || needUpdateSubnets) {
                updateBean.setSubnet(newGroupBean.getSubnets());
                asgDAO.updateAutoScalingGroup(spotGroupName, updateBean);
                if (needUpdateLaunchConfig) {
                    asgDAO.deleteLaunchConfig(spotAutoScalingBean.getLaunch_config_id());
                }
            }
            updateLifeCycle(spotGroupName, groupBean, newGroupBean);
        }
    }

    public void disableAutoScalingGroup(String groupName) throws Exception {
        asgDAO.disableAutoScalingGroup(groupName);
        GroupBean groupBean = new GroupBean();
        groupBean.setAsg_status(ASGStatus.DISABLED);
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
        groupBean.setAsg_status(ASGStatus.ENABLED);
        groupBean.setLast_update(System.currentTimeMillis());
        groupInfoDAO.updateGroupInfo(groupName, groupBean);

        List<SpotAutoScalingBean> spotAutoScalingBeans = spotAutoScalingDAO.getAutoScalingGroupsByCluster(groupName);
        for (SpotAutoScalingBean spotAutoScalingBean : spotAutoScalingBeans) {
            asgDAO.enableAutoScalingGroup(spotAutoScalingBean.getAsg_name());
        }
    }

    public void insertOrUpdateAutoScalingGroup(String groupName, AutoScalingRequestBean request) throws Exception {
        if (asgDAO.getAutoScalingGroupStatus(groupName).equals(ASGStatus.UNKNOWN)) {
            updateAutoScalingGroup(groupName, request);
        } else {
            createAutoScalingGroup(groupName, request);
        }
    }

    private void createSpotAutoScalingGroup(String clusterName, GroupBean groupBean, AutoScalingRequestBean request) throws Exception {
        String spotGroupName = String.format("%s-spot", clusterName);
        GroupBean newGroupBean = (GroupBean)groupBean.clone();
        newGroupBean.setGroup_name(spotGroupName);
        String bidPrice = request.getSpotPrice();
        // step 1 create launch config
        String newConfig = asgDAO.createSpotLaunchConfig(spotGroupName, generateVmBean(spotGroupName, groupBean), bidPrice);
        newGroupBean.setLaunch_config_id(newConfig);
        asgDAO.createAutoScalingGroup(spotGroupName, generateInternalAutoScalingRequest(newGroupBean, request, true));

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

    public void createAutoScalingGroup(String groupName, AutoScalingRequestBean request) throws Exception {
        GroupBean groupBean = getGroupInfoByClusterName(groupName);
        if (groupBean == null) {
            return;
        }

        if (groupBean.getSubnets() == null) {
            LOG.error(String.format("Failed to create auto scaling group for %s: empty subnets ", groupName));
            throw new Exception(String.format("Failed to create auto scaling group for %s: empty subnets ", groupName));
        }

        AwsVmBean updateBean = generateInternalAutoScalingRequest(groupBean, request, false);
        if (request.isAttachInstances()) {
            List<String> ids = hostDAO.getHostIdsByGroup(groupName);
            LOG.info(String.format("Fetched %d instances from host table.", ids.size()));
            if (ids.isEmpty()) {
                asgDAO.createAutoScalingGroup(groupName, updateBean);
            } else {
                try {
                    int minSize = request.getMinSize();
                    updateBean.setMinSize(Math.max(updateBean.getMinSize() - ids.size(), 0));
                    updateBean.setMaxSize(Math.max(ids.size(), updateBean.getMaxSize()));
                    LOG.debug(String.format("create auto scaling group with launch Id: %s", updateBean.getLaunchConfigId()));
                    asgDAO.createAutoScalingGroup(groupName, updateBean);
                    jobPool.submit(new AttachInstanceToGroupJob(groupName, request, ids, minSize, groupBean.getSubnets()));
                } catch (Exception ex) {
                    LOG.error("Failed to create auto scaling group {}.", groupName, ex);
                }
            }
        } else {
            asgDAO.createAutoScalingGroup(groupName, updateBean);
        }

        GroupBean newBean = new GroupBean();
        newBean.setAsg_status(ASGStatus.ENABLED);
        newBean.setLast_update(System.currentTimeMillis());
        groupInfoDAO.updateGroupInfo(groupName, newBean);

        if (request.getEnableSpot() != null && request.getEnableSpot()) {
            createSpotAutoScalingGroup(groupName, groupBean, request);
        }
    }


    private AwsVmBean generateInternalAutoScalingRequest(GroupBean groupBean, AutoScalingRequestBean requestBean, boolean spotFleet) throws Exception {
        String launchConfig = groupBean.getLaunch_config_id();
        String subnets = groupBean.getSubnets();
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
        GroupBean groupBean = getGroupInfoByClusterName(groupName);
        if (groupBean == null) {
            return;
        }
        asgDAO.updateAutoScalingGroup(groupName, generateInternalAutoScalingRequest(groupBean, request, false));

        // handle spot instance group
        List<SpotAutoScalingBean> autoScalingGroupBeans = spotAutoScalingDAO.getAutoScalingGroupsByCluster(groupName);
        if (autoScalingGroupBeans.isEmpty() && request.getEnableSpot()) { // if we want to create spot auto scaling group
            createSpotAutoScalingGroup(groupName, groupBean, request);
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
                    String newSpotConfig = changeConfig(spotAutoScalingName, groupBean, request.getSpotPrice());
                    autoScalingBean.setLaunch_config_id(newSpotConfig);
                    autoScalingBean.setBid_price(request.getSpotPrice());

                    // update aws auto scaling group
                    AwsVmBean awsVmBean = new AwsVmBean();
                    awsVmBean.setLaunchConfigId(newSpotConfig);
                    asgDAO.updateAutoScalingGroup(spotAutoScalingName, awsVmBean);
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

    private GroupBean generateDefaultGroupBean(String groupName) throws Exception {
        String defaultImageId = commonHandler.getDefaultImageId(groupName);
        GroupBean groupBean = new GroupBean();
        groupBean.setGroup_name(groupName);
        groupBean.setInstance_type(AutoScalingConstants.DEFAULT_INSTANCE_TYPE);
        groupBean.setImage_id(defaultImageId);
        groupBean.setSecurity_group(AutoScalingConstants.DEFAULT_SECURITY_GROUP);
        groupBean.setLaunch_latency_th(AutoScalingConstants.DEFAULT_LAUNCH_LATENCY_THRESHOLD);
        groupBean.setIam_role(AutoScalingConstants.DEFAULT_IAM_ROLE);
        groupBean.setAssign_public_ip(false);
        groupBean.setUser_data(String.format("#cloud-config\nrole: %s\n", groupName));
        groupBean.setAsg_status(ASGStatus.UNKNOWN);
        groupBean.setHealthcheck_state(false);
        groupBean.setHealthcheck_period(HealthCheckConstants.HEALTHCHECK_PERIOD);
        groupBean.setLifecycle_state(false);
        groupBean.setLifecycle_timeout(AutoScalingConstants.LIFECYCLE_TIMEOUT);
        return groupBean;
    }


    private GroupBean generateUpdatedGroupBean(GroupBean groupBean, GroupBean requestBean) throws Exception {
        if (requestBean.getGroup_name() == null) {
            requestBean.setGroup_name(groupBean.getGroup_name());
        }

        if (requestBean.getInstance_type() == null) {
            requestBean.setInstance_type(groupBean.getInstance_type());
        }

        if (requestBean.getImage_id() == null) {
            requestBean.setImage_id(groupBean.getImage_id());
        }

        if (requestBean.getSecurity_group() == null) {
            requestBean.setSecurity_group(groupBean.getSecurity_group());
        }

        if (requestBean.getLaunch_latency_th() == null) {
            requestBean.setLaunch_latency_th(groupBean.getLaunch_latency_th());
        }

        if (requestBean.getIam_role() == null) {
            requestBean.setIam_role(groupBean.getIam_role());
        }

        if (requestBean.getAssign_public_ip() == null) {
            requestBean.setAssign_public_ip(groupBean.getAssign_public_ip());
        }

        if (requestBean.getUser_data() == null) {
            requestBean.setUser_data(groupBean.getUser_data());
        } else {
            requestBean.setUser_data(requestBean.getUser_data());
        }

        if (requestBean.getAsg_status() == null) {
            requestBean.setAsg_status(ASGStatus.UNKNOWN);
        }

        if (requestBean.getHealthcheck_state() == null) {
            requestBean.setHealthcheck_state(groupBean.getHealthcheck_state());
        }

        if (requestBean.getHealthcheck_period() == null) {
            requestBean.setHealthcheck_period(groupBean.getHealthcheck_period());
        }

        if (requestBean.getLifecycle_state() == null) {
            requestBean.setLifecycle_state(groupBean.getLifecycle_state());
        }

        if (requestBean.getLifecycle_timeout() == null) {
            requestBean.setLifecycle_timeout(groupBean.getLifecycle_timeout());
        }

        return requestBean;
    }

    private AwsVmBean generateVmBean(String groupName, GroupBean groupBean) throws Exception {
        AwsVmBean awsVmBean = new AwsVmBean();
        awsVmBean.setRole(groupBean.getIam_role());
        awsVmBean.setImage(groupBean.getImage_id());
        awsVmBean.setAssignPublicIp(groupBean.getAssign_public_ip());
        awsVmBean.setRawUserDataString(groupBean.getUser_data());
        if (groupBean.getIam_role().contains("/")) {
            awsVmBean.setRole(groupBean.getIam_role().split("/")[1]);
        } else {
            awsVmBean.setRole(groupBean.getIam_role());
        }
        awsVmBean.setClusterName(groupName);
        awsVmBean.setHostType(groupBean.getInstance_type());
        awsVmBean.setSecurityZone(groupBean.getSecurity_group());
        awsVmBean.setSubnet(groupBean.getSubnets());
        return awsVmBean;
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
}
