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
package com.pinterest.arcee.autoscaling;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.*;
import com.pinterest.arcee.bean.*;
import com.pinterest.arcee.common.AutoScalingConstants;
import com.pinterest.deployservice.bean.ASGStatus;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;

public class AwsAutoScaleGroupManager implements AutoScaleGroupManager {
    private static final Logger LOG = LoggerFactory.getLogger(AwsAutoScaleGroupManager.class);
    private static final String PROCESS_ALARMNOTIFICATION = "AlarmNotification";
    private static final String PROCESS_SCHEDULEDACTIONS = "ScheduledActions";
    private static final String PROCESS_LAUNCH = "Launch";
    private static final String PROCESS_TERMINATE = "Terminate";
    private static final String PROCESS_HEALTHCHECK = "HealthCheck";
    private static final String PROCESS_REPLACEUNHEALTHY = "ReplaceUnhealthy";
    private static final String PROCESS_ADDTOLOADBALANCER = "AddToLoadBalancer";
    private static final String PROCESS_AZREBALANCE = "AZRebalance";
    private String SNS_TOPIC_ARN;
    private String ROLE_ARN;
    private AmazonAutoScalingClient aasClient;
    private static final  String[] NOTIFICATION_TYPE = {
        "autoscaling:EC2_INSTANCE_LAUNCH",
        "autoscaling:EC2_INSTANCE_LAUNCH_ERROR",
        "autoscaling:EC2_INSTANCE_TERMINATE",
        "autoscaling:EC2_INSTANCE_TERMINATE_ERROR"};
    // http://docs.aws.amazon.com/AutoScaling/latest/APIReference/API_DetachInstances.html
    private static final int MAX_DETACH_INSTANCE_LENGTH = 16;

    public AwsAutoScaleGroupManager(String snsArn, String roleArn, AmazonAutoScalingClient aasClient) {
        SNS_TOPIC_ARN = snsArn;
        ROLE_ARN = roleArn;
        this.aasClient = aasClient;
    }

    //------ Launch Config
    @Override
    public String createLaunchConfig(GroupBean request) throws Exception {
        CreateLaunchConfigurationRequest configurationRequest = new CreateLaunchConfigurationRequest();
        configurationRequest.setImageId(request.getImage_id());
        configurationRequest.setKeyName("ops");
        String configId = genLaunchConfigId(request.getGroup_name());
        configurationRequest.setLaunchConfigurationName(configId);
        configurationRequest.setAssociatePublicIpAddress(request.getAssign_public_ip() != null && request.getAssign_public_ip());

        if (request.getSecurity_group() != null) {
            configurationRequest.setSecurityGroups(Arrays.asList(request.getSecurity_group()));
        }

        configurationRequest.setIamInstanceProfile(request.getIam_role());
        configurationRequest.setInstanceType(request.getInstance_type());
        InstanceMonitoring monitoring = new InstanceMonitoring();
        // DO NOT enable detailed instance monitoring
        monitoring.setEnabled(false);
        configurationRequest.setInstanceMonitoring(monitoring);
        configurationRequest.setUserData(request.getUser_data());
        aasClient.createLaunchConfiguration(configurationRequest);
        return configId;
    }

    @Override
    public String createLaunchConfig(String groupName, String instanceId, String imageId) throws Exception {
        if (instanceId == null) {
            throw new Exception(String.format("Cannot get instance id when creating launch config for %s", groupName));
        }
        String configId = genLaunchConfigId(groupName);
        CreateLaunchConfigurationRequest configurationRequest = new CreateLaunchConfigurationRequest();
        configurationRequest.setInstanceId(instanceId);
        configurationRequest.setImageId(imageId);
        configurationRequest.setLaunchConfigurationName(configId);
        aasClient.createLaunchConfiguration(configurationRequest);
        return configId;
    }

    @Override
    public void deleteLaunchConfig(String ConfigId) throws Exception {
        try {
            if (ConfigId == null) {
                return;
            }

            DeleteLaunchConfigurationRequest request = new DeleteLaunchConfigurationRequest();
            request.setLaunchConfigurationName(ConfigId);
            aasClient.deleteLaunchConfiguration(request);
        } catch (AmazonClientException e) {
            LOG.error(e.getMessage());
        }
    }

    @Override
    public void disableAutoScalingGroup(String groupName) throws Exception {
        SuspendProcessesRequest request = new SuspendProcessesRequest();
        // We only disable alarm notification and scheduled actions
        request.setScalingProcesses(Arrays.asList(PROCESS_ALARMNOTIFICATION, PROCESS_SCHEDULEDACTIONS,
                PROCESS_LAUNCH, PROCESS_TERMINATE, PROCESS_REPLACEUNHEALTHY));
        request.setAutoScalingGroupName(groupName);
        aasClient.suspendProcesses(request);
    }

    @Override
    public void enableAutoScalingGroup(String groupName) throws Exception {
        ResumeProcessesRequest request = new ResumeProcessesRequest();
        request.setAutoScalingGroupName(groupName);
        request.setScalingProcesses(Arrays
            .asList(PROCESS_LAUNCH, PROCESS_TERMINATE, PROCESS_HEALTHCHECK,
                PROCESS_REPLACEUNHEALTHY,
                PROCESS_ALARMNOTIFICATION, PROCESS_SCHEDULEDACTIONS, PROCESS_ADDTOLOADBALANCER));
        aasClient.resumeProcesses(request);
    }

    @Override
    public void deleteAutoScalingGroup(String groupName, boolean detachInstances) throws Exception {
        // step 1: get the auto scaling instances information
        if (detachInstances) {
            DescribeAutoScalingGroupsRequest request = new DescribeAutoScalingGroupsRequest();
            request.setAutoScalingGroupNames(Arrays.asList(groupName));
            DescribeAutoScalingGroupsResult result = aasClient.describeAutoScalingGroups(request);
            List<AutoScalingGroup> groups = result.getAutoScalingGroups();
            if (groups.isEmpty()) {
                return;
            }
            AutoScalingGroup group = groups.get(0);

            List<Instance> instances = group.getInstances();
            List<String> ids = new LinkedList<>();
            for (Instance instance : instances) {
                ids.add(instance.getInstanceId());
            }

            // step 2: update the autoscaling min size to 0
            UpdateAutoScalingGroupRequest updateAutoScalingGroupRequest = new UpdateAutoScalingGroupRequest();
            updateAutoScalingGroupRequest.setAutoScalingGroupName(groupName);
            updateAutoScalingGroupRequest.setMinSize(0);
            aasClient.updateAutoScalingGroup(updateAutoScalingGroupRequest);


            // step 3: detach instances from auto scaling group
            if (!ids.isEmpty()) {
                for (int i = 0; i < ids.size(); i += MAX_DETACH_INSTANCE_LENGTH) {
                    DetachInstancesRequest detachInstancesRequest = new DetachInstancesRequest();
                    detachInstancesRequest.setAutoScalingGroupName(groupName);
                    detachInstancesRequest.setShouldDecrementDesiredCapacity(true);
                    detachInstancesRequest.setInstanceIds(ids.subList(i, Math.min(i+ MAX_DETACH_INSTANCE_LENGTH, ids.size())));
                    aasClient.detachInstances(detachInstancesRequest);
                }
            }
        }

        // step 4 delete auto scaling group
        DeleteAutoScalingGroupRequest deleteAutoScalingGroupRequest = new DeleteAutoScalingGroupRequest();
        deleteAutoScalingGroupRequest.setAutoScalingGroupName(groupName);
        deleteAutoScalingGroupRequest.setForceDelete(true);
        aasClient.deleteAutoScalingGroup(deleteAutoScalingGroupRequest);
    }

    @Override
    public void updateSubnet(String groupName, String subnets) throws Exception {
        UpdateAutoScalingGroupRequest updateAutoScalingGroupRequest = new UpdateAutoScalingGroupRequest();
        updateAutoScalingGroupRequest.setAutoScalingGroupName(groupName);
        updateAutoScalingGroupRequest.setVPCZoneIdentifier(subnets);
        aasClient.updateAutoScalingGroup(updateAutoScalingGroupRequest);
    }

    @Override
    public void createAutoScalingGroup(String ConfigId, AutoScalingRequestBean request, String subnets) throws Exception {
        CreateAutoScalingGroupRequest autoScalingGroupRequest = new CreateAutoScalingGroupRequest();
        autoScalingGroupRequest.setAutoScalingGroupName(request.getGroupName());
        autoScalingGroupRequest.setVPCZoneIdentifier(subnets);
        autoScalingGroupRequest.withMinSize(request.getMinSize()).withMaxSize(request.getMaxSize());
        autoScalingGroupRequest.setLaunchConfigurationName(ConfigId);
        autoScalingGroupRequest.setTerminationPolicies(
            Arrays.asList(request.getTerminationPolicy()));
        aasClient.createAutoScalingGroup(autoScalingGroupRequest);

        SuspendProcessesRequest suspendProcessesRequest = new SuspendProcessesRequest();
        suspendProcessesRequest.setScalingProcesses(Arrays.asList(PROCESS_AZREBALANCE));
        suspendProcessesRequest.setAutoScalingGroupName(request.getGroupName());
        aasClient.suspendProcesses(suspendProcessesRequest);

        // setup notificaiton
        if (!StringUtils.isEmpty(SNS_TOPIC_ARN)) {
            PutNotificationConfigurationRequest notifRequest = new PutNotificationConfigurationRequest();
            notifRequest.setAutoScalingGroupName(request.getGroupName());
            notifRequest.setTopicARN(SNS_TOPIC_ARN);
            notifRequest.setNotificationTypes(Arrays.asList(NOTIFICATION_TYPE));
            aasClient.putNotificationConfiguration(notifRequest);
        }
    }

    @Override
    public void updateAutoScalingGroup(AutoScalingRequestBean request, String subnets) throws Exception {
        UpdateAutoScalingGroupRequest updateAutoScalingGroupRequest = new UpdateAutoScalingGroupRequest();
        updateAutoScalingGroupRequest.setAutoScalingGroupName(request.getGroupName());
        updateAutoScalingGroupRequest.setVPCZoneIdentifier(subnets);
        updateAutoScalingGroupRequest.setTerminationPolicies(
            Arrays.asList(request.getTerminationPolicy()));
        updateAutoScalingGroupRequest.withMinSize(request.getMinSize()).withMaxSize(
            request.getMaxSize());
        aasClient.updateAutoScalingGroup(updateAutoScalingGroupRequest);
    }

    @Override
    public void changeAutoScalingGroupLaunchConfig(String groupName, String configId) throws Exception {
        UpdateAutoScalingGroupRequest updateAutoScalingGroupRequest = new UpdateAutoScalingGroupRequest();
        updateAutoScalingGroupRequest.setAutoScalingGroupName(groupName);
        updateAutoScalingGroupRequest.setLaunchConfigurationName(configId);
        aasClient.updateAutoScalingGroup(updateAutoScalingGroupRequest);
    }

    @Override
    public GroupBean getLaunchConfigByName(String configId) throws Exception {
        GroupBean groupBean = new GroupBean();
        if (configId == null) {
            return groupBean;
        }

        DescribeLaunchConfigurationsRequest configurationsRequest = new DescribeLaunchConfigurationsRequest();
        configurationsRequest.setLaunchConfigurationNames(Arrays.asList(configId));
        DescribeLaunchConfigurationsResult configurationsResult = aasClient.describeLaunchConfigurations(configurationsRequest);
        List<LaunchConfiguration> configs = configurationsResult.getLaunchConfigurations();
        if (configs.isEmpty()) {
            return groupBean;
        }

        LaunchConfiguration config = configs.get(0);
        groupBean.setInstance_type(config.getInstanceType());
        groupBean.setImage_id(config.getImageId());
        if (!config.getSecurityGroups().isEmpty()) {
            groupBean.setSecurity_group(config.getSecurityGroups().get(0));
        }

        groupBean.setUser_data(new String(Base64.decodeBase64(config.getUserData())));
        groupBean.setIam_role(config.getIamInstanceProfile());

        if (config.isAssociatePublicIpAddress() != null) {
            groupBean.setAssign_public_ip(config.getAssociatePublicIpAddress());
        } else {
            // default value to false
            groupBean.setAssign_public_ip(false);
        }
        return groupBean;
    }

    @Override
    public AutoScalingGroupBean getAutoScalingGroupInfoByName(String groupName) throws Exception {
        AutoScalingGroupBean asgInfo = generateDefaultASGInfo();
        DescribeAutoScalingGroupsRequest request = new DescribeAutoScalingGroupsRequest();
        request.setAutoScalingGroupNames(Arrays.asList(groupName));
        DescribeAutoScalingGroupsResult result = aasClient.describeAutoScalingGroups(request);
        List<AutoScalingGroup> asgroups = result.getAutoScalingGroups();

        if (asgroups.isEmpty()) {
            return asgInfo;
        }

        AutoScalingGroup asgroup = asgroups.get(0);
        // set autoscaling group status
        List<SuspendedProcess> suspendedProcesses = asgroup.getSuspendedProcesses();
        HashSet<String> processName = new HashSet<>();
        for (SuspendedProcess process : suspendedProcesses) {
            processName.add(process.getProcessName());
        }
        if (processName.contains(PROCESS_ALARMNOTIFICATION) && processName.contains(PROCESS_SCHEDULEDACTIONS)) {
            asgInfo.setStatus(ASGStatus.DISABLED);
        } else {
            asgInfo.setStatus(ASGStatus.ENABLED);
        }

        asgInfo.setMinSize(asgroup.getMinSize());
        asgInfo.setMaxSize(asgroup.getMaxSize());
        // TODO this is dangerous that we are using the same value of TerminationPolicy
        String policy = asgroup.getTerminationPolicies().isEmpty() ? "Default" : new String(asgroup.getTerminationPolicies().get(0).getBytes());
        asgInfo.setTerminationPolicy(AutoScalingTerminationPolicy.valueOf(AutoScalingTerminationPolicy.class, policy));

        List<Instance> instances = asgroup.getInstances();
        for (Instance instance : instances) {
            if (instance.getInstanceId() != null) {
                asgInfo.addToInstances(instance.getInstanceId());
            }
        }
        return asgInfo;
    }

    //------ Instances
    @Override
    public void addInstancesToAutoScalingGroup(Collection<String> instances, String groupName) throws Exception {
        // Already make sure that do not describe instances more than 50 records
        instances.removeAll(instancesInAutoScalingGroup(instances));
        if (instances.isEmpty()) {
            return;
        }

        AttachInstancesRequest attachInstancesRequest = new AttachInstancesRequest();
        attachInstancesRequest.withAutoScalingGroupName(groupName).setInstanceIds(instances);
        aasClient.attachInstances(attachInstancesRequest);
    }

    @Override
    public Collection<String> instancesInAutoScalingGroup(Collection<String> instances) throws Exception {
        DescribeAutoScalingInstancesRequest request = new DescribeAutoScalingInstancesRequest();
        request.setInstanceIds(instances);
        List<AutoScalingInstanceDetails> asgInstanceDetails = aasClient.describeAutoScalingInstances(request).getAutoScalingInstances();
        Collection<String>  asgInstances = new ArrayList<>();
        for (AutoScalingInstanceDetails instance : asgInstanceDetails) {
            asgInstances.add(instance.getInstanceId());
        }
        return asgInstances;
    }

    @Override
    public void detachInstancesFromAutoScalingGroup(Collection<String> instances, String groupName, boolean decreaseSize) throws Exception {
        DescribeAutoScalingGroupsRequest request = new DescribeAutoScalingGroupsRequest();
        request.setAutoScalingGroupNames(Arrays.asList(groupName));
        DescribeAutoScalingGroupsResult result = aasClient.describeAutoScalingGroups(request);
        List<AutoScalingGroup> groups = result.getAutoScalingGroups();
        if (groups.isEmpty()) {
            return;
        }

        AutoScalingGroup group = groups.get(0);
        int curCapacity = group.getDesiredCapacity();
        int minSize = group.getMinSize();
        if (decreaseSize && (curCapacity == minSize)) {
            UpdateAutoScalingGroupRequest updateRequest = new UpdateAutoScalingGroupRequest();
            updateRequest.setAutoScalingGroupName(groupName);
            updateRequest.setMinSize(minSize - 1);
            aasClient.updateAutoScalingGroup(updateRequest);
        }

        DetachInstancesRequest detachInstancesRequest = new DetachInstancesRequest();
        detachInstancesRequest.withAutoScalingGroupName(groupName).setInstanceIds(instances);
        detachInstancesRequest.setShouldDecrementDesiredCapacity(decreaseSize);
        aasClient.detachInstances(detachInstancesRequest);
    }

    @Override
    public void increaseASGDesiredCapacityBySize(String groupName, int instanceCnt) throws Exception {
        DescribeAutoScalingGroupsRequest request = new DescribeAutoScalingGroupsRequest();
        request.setAutoScalingGroupNames(Arrays.asList(groupName));
        DescribeAutoScalingGroupsResult result = aasClient.describeAutoScalingGroups(request);
        List<AutoScalingGroup> groups = result.getAutoScalingGroups();
        if (groups.isEmpty()) {
            return;
        }

        AutoScalingGroup group = groups.get(0);
        int curCapacity = group.getDesiredCapacity();

        UpdateAutoScalingGroupRequest updateRequest = new UpdateAutoScalingGroupRequest();
        updateRequest.setAutoScalingGroupName(groupName);
        updateRequest.setDesiredCapacity(curCapacity + instanceCnt);
        updateRequest.setMaxSize(Math.max(group.getMaxSize(), curCapacity + instanceCnt));
        aasClient.updateAutoScalingGroup(updateRequest);
    }

    @Override
    public void terminateInstanceInAutoScalingGroup(String instanceId, boolean decreaseSize) throws Exception {
        TerminateInstanceInAutoScalingGroupRequest request = new TerminateInstanceInAutoScalingGroupRequest();
        request.setShouldDecrementDesiredCapacity(decreaseSize);
        request.setInstanceId(instanceId);
        aasClient.terminateInstanceInAutoScalingGroup(request);
    }

    //------ Scaling Policy
    @Override
    public void addScalingPolicyToGroup(String groupName, ScalingPolicyBean policyBean) throws Exception {
        PutScalingPolicyRequest request = new PutScalingPolicyRequest();
        request.setAdjustmentType("ChangeInCapacity");
        request.setPolicyName(policyBean.getPolicyName());
        request.setAutoScalingGroupName(groupName);
        request.setScalingAdjustment(policyBean.getScaleSize());
        request.setCooldown(policyBean.getCoolDownTime() * 60);
        aasClient.putScalingPolicy(request);
    }

    @Override
    public Map<String, ScalingPolicyBean> getScalingPoliciesForGroup(String groupName) throws Exception {
        Map<String, ScalingPolicyBean> policyBeans = new HashMap<>();
        try {
            DescribePoliciesRequest request = new DescribePoliciesRequest();
            request.setAutoScalingGroupName(groupName);
            DescribePoliciesResult result = aasClient.describePolicies(request);
            List<ScalingPolicy> policySet = result.getScalingPolicies();
            for (ScalingPolicy policy : policySet) {
                ScalingPolicyBean bean = new ScalingPolicyBean();
                bean.setCoolDownTime(policy.getCooldown() / 60);
                bean.setPolicyName(policy.getPolicyName());
                bean.setScaleSize(policy.getScalingAdjustment());
                bean.setARN(policy.getPolicyARN());
                policyBeans.put(bean.getPolicyName(), bean);
            }
            return policyBeans;
        } catch (com.amazonaws.AmazonServiceException e) {
            return policyBeans;
        }
    }

    @Override
    public boolean hasAutoScalingGroup(String groupName) throws Exception {
        return !getAutoScalingGroupStatus(groupName).equals(ASGStatus.UNKNOWN);
    }

    @Override
    public ASGStatus getAutoScalingGroupStatus(String groupName) throws Exception {
        DescribeAutoScalingGroupsRequest request = new DescribeAutoScalingGroupsRequest();
        List<String> groupNames = new ArrayList<>();
        groupNames.add(groupName);
        request.setAutoScalingGroupNames(groupNames);
        DescribeAutoScalingGroupsResult result = aasClient.describeAutoScalingGroups(request);
        List<AutoScalingGroup> groups = result.getAutoScalingGroups();
        if (groups.isEmpty()) {
            return ASGStatus.UNKNOWN;
        }

        AutoScalingGroup group = groups.get(0);
        List<SuspendedProcess> suspendedProcesses = group.getSuspendedProcesses();
        HashSet<String> processName = new HashSet<>();
        for (SuspendedProcess process : suspendedProcesses) {
            processName.add(process.getProcessName());
        }
        if (processName.contains(PROCESS_ALARMNOTIFICATION) && processName.contains(PROCESS_SCHEDULEDACTIONS)) {
            return ASGStatus.DISABLED;
        } else {
            return ASGStatus.ENABLED;
        }
    }

    @Override
    public ScalingActivitiesBean getScalingActivity(String groupName, int pageSize, String token) throws Exception {
        DescribeScalingActivitiesRequest request = new DescribeScalingActivitiesRequest();
        request.setAutoScalingGroupName(groupName);
        request.setMaxRecords(pageSize);
        if (!token.isEmpty()) {
            request.setNextToken(token);
        }

        DescribeScalingActivitiesResult result = aasClient.describeScalingActivities(request);
        List<Activity> activities = result.getActivities();
        ScalingActivitiesBean scalingActivitiesInfo = new ScalingActivitiesBean();
        scalingActivitiesInfo.setActivities(new ArrayList<>());

        for (Activity activity : activities) {
            ScalingActivityBean scalingActivity = new ScalingActivityBean();
            scalingActivity.setDescription(activity.getDescription());
            scalingActivity.setCause(activity.getCause());
            if (activity.getStartTime() != null) {
                scalingActivity.setScalingTime(activity.getStartTime().getTime());
            } else {
                scalingActivity.setScalingTime(0);
            }
            scalingActivity.setStatus(activity.getStatusCode());
            scalingActivitiesInfo.addScalingActivity(scalingActivity);
        }

        scalingActivitiesInfo.setNextToken(result.getNextToken());
        return scalingActivitiesInfo;
    }

    @Override
    public boolean isScalingDownEventEnabled(String groupName) throws Exception {
        DescribeAutoScalingGroupsRequest request = new DescribeAutoScalingGroupsRequest();
        request.setAutoScalingGroupNames(Arrays.asList(groupName));
        DescribeAutoScalingGroupsResult result = aasClient.describeAutoScalingGroups(request);
        List<AutoScalingGroup> groups = result.getAutoScalingGroups();
        if (groups.isEmpty()) {
            return false;
        }

        AutoScalingGroup group = groups.get(0);
        List<SuspendedProcess> suspendedProcesses = group.getSuspendedProcesses();
        for (SuspendedProcess process : suspendedProcesses) {
            if (process.getProcessName().equals(PROCESS_TERMINATE)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void disableScalingDownEvent(String groupName) throws Exception {
        SuspendProcessesRequest request = new SuspendProcessesRequest();
        request.setScalingProcesses(Arrays.asList(PROCESS_TERMINATE));
        request.setAutoScalingGroupName(groupName);
        aasClient.suspendProcesses(request);
    }

    @Override
    public void enableScalingDownEvent(String groupName) throws Exception {
        ResumeProcessesRequest request = new ResumeProcessesRequest();
        request.setAutoScalingGroupName(groupName);
        request.setScalingProcesses(Arrays.asList(PROCESS_TERMINATE));
        aasClient.resumeProcesses(request);
    }

    // LifeCycle Hook
    @Override
    public void createLifecycleHook(String groupName, int timeout) throws Exception {
        PutLifecycleHookRequest request = new PutLifecycleHookRequest();
        request.setLifecycleHookName(String.format("LIFECYCLEHOOK-%s", groupName));
        request.setAutoScalingGroupName(groupName);
        request.setLifecycleTransition("autoscaling:EC2_INSTANCE_TERMINATING");
        request.setNotificationTargetARN(SNS_TOPIC_ARN);
        request.setRoleARN(ROLE_ARN);
        request.setHeartbeatTimeout(timeout);
        // If reach the timeout limit, ABANDON all the actions to that instances and proceed to terminate it
        request.setDefaultResult(AutoScalingConstants.LIFECYCLE_ACTION_ABANDON);
        aasClient.putLifecycleHook(request);
    }

    @Override
    public void deleteLifecycleHook(String groupName) throws Exception {
        List<String> lifecycleHooks = getLifecycleHookIds(groupName);
        for (String hookId : lifecycleHooks) {
            DeleteLifecycleHookRequest request = new DeleteLifecycleHookRequest();
            request.setLifecycleHookName(hookId);
            request.setAutoScalingGroupName(groupName);
            aasClient.deleteLifecycleHook(request);
        }
    }

    @Override
    public void completeLifecycleAction(String hookId, String tokenId, String groupName) throws Exception {
        List<String> lifecycleHooks = getLifecycleHookIds(groupName);
        if (!lifecycleHooks.contains(hookId)) {
            return;
        }

        CompleteLifecycleActionRequest completeLifecycleActionRequest = new CompleteLifecycleActionRequest();
        completeLifecycleActionRequest.setLifecycleHookName(hookId);
        completeLifecycleActionRequest.setLifecycleActionToken(tokenId);
        completeLifecycleActionRequest.setAutoScalingGroupName(groupName);
        // CONTINUE action will allow asg proceed to terminate instances earlier than timeout limit
        completeLifecycleActionRequest.setLifecycleActionResult(AutoScalingConstants.LIFECYCLE_ACTION_CONTINUE);
        aasClient.completeLifecycleAction(completeLifecycleActionRequest);
    }

    public List<String> getLifecycleHookIds(String groupName) throws Exception {
        DescribeLifecycleHooksRequest request = new DescribeLifecycleHooksRequest();
        request.setAutoScalingGroupName(groupName);
        DescribeLifecycleHooksResult result = aasClient.describeLifecycleHooks(request);
        List<String> lifecycleHookIds = new ArrayList<>();
        for (LifecycleHook hook : result.getLifecycleHooks()) {
            lifecycleHookIds.add(hook.getLifecycleHookName());
        }
        return lifecycleHookIds;
    }

    private AutoScalingGroupBean generateDefaultASGInfo() {
        AutoScalingGroupBean asgInfo = new AutoScalingGroupBean();
        asgInfo.setStatus(ASGStatus.UNKNOWN);
        asgInfo.setTerminationPolicy(AutoScalingTerminationPolicy.Default);
        asgInfo.setInstances(new ArrayList<>());
        asgInfo.setMaxSize(0);
        asgInfo.setMinSize(0);
        return asgInfo;
    }

    private String genLaunchConfigId(String groupName) {
        Date date = new Date();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HHmmss");
        return String.format("%s-%s", groupName, df.format(date));
    }
}
