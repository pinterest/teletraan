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

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.*;
import com.pinterest.arcee.aws.AwsConfigManager;
import com.pinterest.arcee.bean.*;
import com.pinterest.arcee.common.AutoScalingConstants;
import com.pinterest.clusterservice.bean.AwsVmBean;
import com.pinterest.deployservice.bean.ASGStatus;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;

public class AwsAutoScalingManager implements AutoScalingManager {
    private static final Logger LOG = LoggerFactory.getLogger(AwsAutoScalingManager.class);
    private static final String PROCESS_ALARMNOTIFICATION = "AlarmNotification";
    private static final String PROCESS_SCHEDULEDACTIONS = "ScheduledActions";
    private static final String PROCESS_LAUNCH = "Launch";
    private static final String PROCESS_TERMINATE = "Terminate";
    private static final String PROCESS_HEALTHCHECK = "HealthCheck";
    private static final String PROCESS_REPLACEUNHEALTHY = "ReplaceUnhealthy";
    private static final String PROCESS_ADDTOLOADBALANCER = "AddToLoadBalancer";
    private static final String PROCESS_AZREBALANCE = "AZRebalance";

    private final AmazonAutoScalingClient aasClient;
    private final String ownerId;
    private final String snsArn;
    private final String vmKeyName;
    private final String defaultRole;
    private final String pinfoEnvironment;
    private final String roleTemplate;
    private final String userDataTemplate;
    private final String roleARN;

    private static final  String[] NOTIFICATION_TYPE = {
        "autoscaling:EC2_INSTANCE_LAUNCH",
        "autoscaling:EC2_INSTANCE_LAUNCH_ERROR",
        "autoscaling:EC2_INSTANCE_TERMINATE",
        "autoscaling:EC2_INSTANCE_TERMINATE_ERROR"};
    // http://docs.aws.amazon.com/AutoScaling/latest/APIReference/API_DetachInstances.html
    private static final int MAX_DETACH_INSTANCE_LENGTH = 16;

    public AwsAutoScalingManager(AwsConfigManager configManager) {
        if (StringUtils.isNotEmpty(configManager.getId()) && StringUtils.isNotEmpty(configManager.getKey())) {
            AWSCredentials
                awsCredentials = new BasicAWSCredentials(configManager.getId(), configManager.getKey());
            this.aasClient = new AmazonAutoScalingClient(awsCredentials);
        } else {
            LOG.debug("AWS credential is missing for creating AWS client. Assuming to use role for authentication.");
            this.aasClient = new AmazonAutoScalingClient();
        }

        this.ownerId = configManager.getOwnerId();
        this.snsArn = configManager.getSnsArn();
        this.vmKeyName = configManager.getVmKeyName();
        this.defaultRole = configManager.getDefaultRole();
        this.pinfoEnvironment = configManager.getPinfoEnvironment();
        this.roleTemplate = configManager.getRoleTemplate();
        this.userDataTemplate = configManager.getUserDataTemplate();
        this.roleARN = configManager.getRoleArn();
    }

    //------ Launch Config
    @Override
    public String createLaunchConfig(String groupName, AwsVmBean request) throws Exception {
        return createLaunchConfigInternal(groupName, request, null);
    }

    @Override
    public String createSpotLaunchConfig(String groupName, AwsVmBean request, String bidPrice) throws Exception {
        return createLaunchConfigInternal(groupName, request, bidPrice);
    }

    private String createLaunchConfigInternal(String groupName, AwsVmBean request, String bidPrice) throws Exception {
        String launchConfigId = genLaunchConfigId(groupName);
        CreateLaunchConfigurationRequest configurationRequest = new CreateLaunchConfigurationRequest();
        configurationRequest.setImageId(request.getImage());
        configurationRequest.setKeyName(vmKeyName);
        configurationRequest.setLaunchConfigurationName(launchConfigId);
        configurationRequest.setAssociatePublicIpAddress(request.getAssignPublicIp());
        if (request.getSecurityZone() != null) {
            configurationRequest.setSecurityGroups(Arrays.asList(request.getSecurityZone()));
        }
        if (bidPrice != null) {
            configurationRequest.setSpotPrice(bidPrice);
        }

        if (request.getRole() == null) {
            configurationRequest.setIamInstanceProfile(String.format(roleTemplate, ownerId, defaultRole));
        } else {
            configurationRequest.setIamInstanceProfile(String.format(roleTemplate, ownerId, request.getRole()));
        }

        configurationRequest.setInstanceType(request.getHostType());
        InstanceMonitoring monitoring = new InstanceMonitoring();
        // DO NOT enable detailed instance monitoring
        monitoring.setEnabled(false);
        configurationRequest.setInstanceMonitoring(monitoring);
        if (StringUtils.isEmpty(request.getRawUserDataString())) {
            String userData = transformUserDataConfigToString(groupName, request.getUserDataConfigs());
            configurationRequest.setUserData(Base64.encodeBase64String(userData.getBytes()));
        } else {
            configurationRequest.setUserData(Base64.encodeBase64String(request.getRawUserDataString().getBytes()));
        }

        aasClient.createLaunchConfiguration(configurationRequest);
        return launchConfigId;
    }

    @Override
    public void deleteLaunchConfig(String launchConfig) throws Exception {
        try {
            if (launchConfig == null) {
                return;
            }

            DeleteLaunchConfigurationRequest request = new DeleteLaunchConfigurationRequest();
            request.setLaunchConfigurationName(launchConfig);
            aasClient.deleteLaunchConfiguration(request);
        } catch (AmazonServiceException e) {
            // if the launch config not found, or still in use. siliently ignore the error
            if (e.getErrorType() != AmazonServiceException.ErrorType.Client) {
                throw e;
            }
        }
    }


    @Override
    public void disableAutoScalingGroup(String groupName) throws Exception {
        disableAutoScalingActions(groupName, Arrays.asList(PROCESS_ALARMNOTIFICATION, PROCESS_SCHEDULEDACTIONS,
                                                         PROCESS_LAUNCH, PROCESS_TERMINATE, PROCESS_REPLACEUNHEALTHY));
    }

    @Override
    public void enableAutoScalingGroup(String groupName) throws Exception {
        enableAutoScalingActions(groupName, Arrays.asList(PROCESS_LAUNCH, PROCESS_TERMINATE, PROCESS_HEALTHCHECK,
                    PROCESS_REPLACEUNHEALTHY,
                    PROCESS_ALARMNOTIFICATION, PROCESS_SCHEDULEDACTIONS, PROCESS_ADDTOLOADBALANCER));
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
            List<String> ids = new ArrayList<>();
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
    public void createAutoScalingGroup(String groupName, AwsVmBean request) throws Exception {
        CreateAutoScalingGroupRequest autoScalingGroupRequest = new CreateAutoScalingGroupRequest();
        autoScalingGroupRequest.setAutoScalingGroupName(groupName);
        autoScalingGroupRequest.setVPCZoneIdentifier(request.getSubnet());
        autoScalingGroupRequest.withMinSize(request.getMinSize()).withMaxSize(request.getMaxSize());
        autoScalingGroupRequest.setLaunchConfigurationName(request.getLaunchConfigId());
        autoScalingGroupRequest.setTerminationPolicies(
            Arrays.asList(request.getTerminationPolicy()));
        aasClient.createAutoScalingGroup(autoScalingGroupRequest);

        LOG.info(String.format("Creating auto scaling group: %s, with min size: %d, max size: %d",
                               groupName, request.getMinSize(), request.getMaxSize()));
        disableAutoScalingActions(groupName, Collections.singletonList(PROCESS_AZREBALANCE));

        // setup notificaiton
        if (!StringUtils.isEmpty(snsArn)) {
            PutNotificationConfigurationRequest notifRequest = new PutNotificationConfigurationRequest();
            notifRequest.setAutoScalingGroupName(groupName);
            notifRequest.setTopicARN(snsArn);
            notifRequest.setNotificationTypes(Arrays.asList(NOTIFICATION_TYPE));
            aasClient.putNotificationConfiguration(notifRequest);
        }
    }

    @Override
    public void updateAutoScalingGroup(String groupName, AwsVmBean request) throws Exception {
        UpdateAutoScalingGroupRequest updateAutoScalingGroupRequest = new UpdateAutoScalingGroupRequest();
        updateAutoScalingGroupRequest.setAutoScalingGroupName(groupName);
        if (!StringUtils.isEmpty(request.getLaunchConfigId())) {
            updateAutoScalingGroupRequest.setLaunchConfigurationName(request.getLaunchConfigId());
        }

        if (!StringUtils.isEmpty(request.getSubnet())) {
            updateAutoScalingGroupRequest.setVPCZoneIdentifier(request.getSubnet());
        }

        if (!StringUtils.isEmpty(request.getTerminationPolicy())) {
            updateAutoScalingGroupRequest.setTerminationPolicies(
                Collections.singletonList(request.getTerminationPolicy()));
        }

        if (request.getMinSize() != null) {
            updateAutoScalingGroupRequest.setMinSize(request.getMinSize());
        }
        if (request.getMaxSize() != null) {
            updateAutoScalingGroupRequest.setMaxSize(request.getMaxSize());
        }
        aasClient.updateAutoScalingGroup(updateAutoScalingGroupRequest);
    }

    @Override
    public AutoScalingGroupBean getAutoScalingGroupInfoByName(String groupName) throws Exception {
        AutoScalingGroupBean asgInfo = generateDefaultASGInfo();
        AutoScalingGroup asgroup = getAutoScalingGroup(groupName);
        if (asgroup == null) {
            return asgInfo;
        }
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

    @Override
    public void decreaseGroupCapacity(String groupName, int size) throws Exception {
        changeGroupCapacity(groupName, size, false);
    }

    @Override
    public void increaseGroupCapacity(String groupName, int size) throws Exception {
        changeGroupCapacity(groupName, size, true);
    }

    private void changeGroupCapacity(String groupName, int size, boolean increment) throws Exception {
        AutoScalingGroup group = getAutoScalingGroup(groupName);
        if (group == null) {
            return;
        }

        int currCapacity = group.getDesiredCapacity();
        int currMinSize = group.getMinSize();
        int currMaxSize = group.getMaxSize();
        if (increment) {
            currCapacity += size;
            if (currMaxSize == currMinSize) {
                currMinSize = currCapacity;
                currMaxSize = currCapacity;
            } else {
                currMaxSize = Math.max(currMaxSize, currCapacity);
            }
        } else {
            currCapacity = Math.max(currCapacity - size, 0);
            if (currMaxSize == currMinSize) {
                currMinSize = currCapacity;
                currMaxSize = currCapacity;
            } else {
                currMinSize = Math.min(currMinSize, currCapacity);
            }
        }

        UpdateAutoScalingGroupRequest updateRequest = new UpdateAutoScalingGroupRequest();
        updateRequest.setAutoScalingGroupName(groupName);
        updateRequest.setDesiredCapacity(currCapacity);
        updateRequest.setMaxSize(currMaxSize);
        updateRequest.setMinSize(currMinSize);
        aasClient.updateAutoScalingGroup(updateRequest);
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
        request.setAutoScalingGroupNames(Collections.singletonList(groupName));
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
    public boolean isInstanceProtected(String instances, String groupName) throws Exception {
        DescribeAutoScalingInstancesResult result = aasClient.describeAutoScalingInstances();
        if (result.getAutoScalingInstances().isEmpty()) {
            return false;
        }
        AutoScalingInstanceDetails details = result.getAutoScalingInstances().get(0);
        return details.getProtectedFromScaleIn();
    }

    @Override
    public void protectInstanceInAutoScalingGroup(Collection<String> instances, String groupName) throws Exception {
        setInstanceProtection(instances, groupName, true);
    }

    @Override
    public void unprotectInstanceInAutoScalingGroup(Collection<String> instances, String groupName) throws Exception {
        setInstanceProtection(instances, groupName, false);
    }


    private void setInstanceProtection(Collection<String> instances, String groupName, boolean protect) throws Exception {
        SetInstanceProtectionRequest setInstanceProtectionRequest = new SetInstanceProtectionRequest();
        setInstanceProtectionRequest.setAutoScalingGroupName(groupName);
        setInstanceProtectionRequest.setInstanceIds(instances);
        setInstanceProtectionRequest.setProtectedFromScaleIn(protect);
        aasClient.setInstanceProtection(setInstanceProtectionRequest);
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
        request.setAdjustmentType(policyBean.getScalingType());
        request.setPolicyName(getScalingPolicyName(groupName, policyBean.getPolicyType()));
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
                bean.setScalingType(policy.getAdjustmentType());
                bean.setScaleSize(policy.getScalingAdjustment());
                if (policy.getScalingAdjustment() > 0) {
                    bean.setPolicyType(PolicyType.SCALEUP.toString());
                } else {
                    bean.setPolicyType(PolicyType.SCALEDOWN.toString());
                }
                bean.setARN(policy.getPolicyARN());
                policyBeans.put(bean.getPolicyType(), bean);
            }
            return policyBeans;
        } catch (com.amazonaws.AmazonServiceException e) {
            return policyBeans;
        }
    }

    @Override
    public ASGStatus getAutoScalingGroupStatus(String groupName) throws Exception {
        AutoScalingGroup group = getAutoScalingGroup(groupName);
        if (group == null) {
            return ASGStatus.UNKNOWN;
        }

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
    public Collection<String> getAutoScalingInstances(String groupName, Collection<String> hostIds) throws Exception {
        Collection<String> asgHostIds = new ArrayList<>();
        DescribeAutoScalingInstancesRequest asgInstancesRequest = new DescribeAutoScalingInstancesRequest();
        asgInstancesRequest.setInstanceIds(hostIds);
        DescribeAutoScalingInstancesResult asgInstancesResult = aasClient.describeAutoScalingInstances(asgInstancesRequest);
        List<AutoScalingInstanceDetails> instanceDetails = asgInstancesResult.getAutoScalingInstances();
        for (AutoScalingInstanceDetails instanceDetail : instanceDetails) {
            if (instanceDetail.getAutoScalingGroupName().equals(groupName)) {
                asgHostIds.add(instanceDetail.getInstanceId());
            }
        }
        return asgHostIds;
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
        return isScalingProcessEnabled(groupName, PROCESS_TERMINATE);
    }

    @Override
    public void disableScalingDownEvent(String groupName) throws Exception {
        disableAutoScalingActions(groupName, Arrays.asList(PROCESS_TERMINATE));
    }

    @Override
    public void enableScalingDownEvent(String groupName) throws Exception {
        enableAutoScalingActions(groupName, Arrays.asList(PROCESS_TERMINATE));
    }

    @Override
    public AwsVmBean getAutoScalingGroupInfo(String clusterName) throws Exception {
        AutoScalingGroup autoScalingGroup = getAutoScalingGroup(clusterName);
        if (autoScalingGroup == null) {
            LOG.warn(String.format("Failed to get cluster %s: auto scaling group %s does not exist", clusterName, clusterName));
            return null;
        }

        AwsVmBean launchConfigInfo = getLaunchConfigInfo(autoScalingGroup.getLaunchConfigurationName());
        AwsVmBean awsVmBean = new AwsVmBean();
        awsVmBean.setClusterName(clusterName);
        awsVmBean.setImage(launchConfigInfo.getImage());
        awsVmBean.setHostType(launchConfigInfo.getHostType());
        awsVmBean.setSecurityZone(launchConfigInfo.getSecurityZone());
        awsVmBean.setAssignPublicIp(launchConfigInfo.getAssignPublicIp());
        awsVmBean.setLaunchConfigId(launchConfigInfo.getLaunchConfigId());
        awsVmBean.setRole(launchConfigInfo.getRole());
        awsVmBean.setUserDataConfigs(transformUserDataToConfigMap(clusterName, launchConfigInfo.getRawUserDataString()));
        awsVmBean.setSubnet(autoScalingGroup.getVPCZoneIdentifier());
        awsVmBean.setMinSize(autoScalingGroup.getMinSize());
        awsVmBean.setMaxSize(autoScalingGroup.getMaxSize());
        return awsVmBean;
    }

    @Override
    public AwsVmBean getLaunchConfigInfo(String launchConfigId) throws Exception {
        DescribeLaunchConfigurationsRequest configRequest = new DescribeLaunchConfigurationsRequest();
        configRequest.setLaunchConfigurationNames(Arrays.asList(launchConfigId));
        DescribeLaunchConfigurationsResult configResult = aasClient.describeLaunchConfigurations(configRequest);
        List<LaunchConfiguration> configs = configResult.getLaunchConfigurations();
        if (configs.isEmpty()) {
            LOG.error(String.format("Failed to get cluster: Launch config %s does not exist", launchConfigId));
            return null;
        }

        LaunchConfiguration config = configs.get(0);
        AwsVmBean awsVmBean = new AwsVmBean();
        awsVmBean.setImage(config.getImageId());
        awsVmBean.setHostType(config.getInstanceType());
        awsVmBean.setSecurityZone(config.getSecurityGroups().get(0));
        awsVmBean.setAssignPublicIp(config.getAssociatePublicIpAddress());
        awsVmBean.setLaunchConfigId(config.getLaunchConfigurationName());
        String roleName = config.getIamInstanceProfile();
        if (roleName.contains("/")) {
            awsVmBean.setRole(roleName.split("/")[1]);
        } else {
            awsVmBean.setRole(roleName);
        }
        String userData = new String(Base64.decodeBase64(config.getUserData()));
        awsVmBean.setRawUserDataString(userData);
        return awsVmBean;
    }

    private boolean isScalingProcessEnabled(String groupName, String processName) throws Exception {
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
            if (process.getProcessName().equals(processName)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void disableAutoScalingActions(String groupName, Collection<String> processes) throws Exception {
        SuspendProcessesRequest request = new SuspendProcessesRequest();
        request.setScalingProcesses(processes);
        request.setAutoScalingGroupName(groupName);
        aasClient.suspendProcesses(request);
    }

    @Override
    public void enableAutoScalingActions(String groupName, Collection<String> processes) throws Exception {
        ResumeProcessesRequest request = new ResumeProcessesRequest();
        request.setAutoScalingGroupName(groupName);
        request.setScalingProcesses(processes);
        aasClient.resumeProcesses(request);
    }

    // LifeCycle Hook
    @Override
    public void createLifecycleHook(String groupName, int timeout) throws Exception {
        PutLifecycleHookRequest request = new PutLifecycleHookRequest();
        request.setLifecycleHookName(String.format("LIFECYCLEHOOK-%s", groupName));
        request.setAutoScalingGroupName(groupName);
        request.setLifecycleTransition("autoscaling:EC2_INSTANCE_TERMINATING");
        request.setNotificationTargetARN(snsArn);
        request.setRoleARN(roleARN);
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

    private List<String> getLifecycleHookIds(String groupName) throws Exception {
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

    private String getScalingPolicyName(String groupName, String scaleType) {
        return String.format("%s_%s_rule", groupName, scaleType.toLowerCase());
    }

    private String transformUserDataConfigToString(String clusterName, Map<String, String> userDataConfigs) throws Exception {
        String prefix = String.format(userDataTemplate, clusterName, pinfoEnvironment);
        StringBuilder resultBuilder = new StringBuilder();
        resultBuilder.append(prefix);
        if (userDataConfigs == null) {
            return resultBuilder.toString();
        }

        for (Map.Entry<String, String> entry : userDataConfigs.entrySet()) {
            resultBuilder.append(String.format("\n%s: %s", entry.getKey(), entry.getValue()));
        }
        return resultBuilder.toString();
    }

    private Map<String, String> transformUserDataToConfigMap(String clusterName, String userData) throws Exception {
        String userDataString = userData.replace(String.format(userDataTemplate, clusterName, pinfoEnvironment), "");
        Map<String, String> resultMap = new HashMap<>();
        if (userDataString.length() == 0) {
            return resultMap;
        }

        Scanner scanner = new Scanner(userDataString);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            List<String> config = Arrays.asList(line.split(": "));
            if (config.size() == 2) {
                resultMap.put(config.get(0), config.get(1));
            }
        }
        scanner.close();
        return resultMap;
    }

    private AutoScalingGroup getAutoScalingGroup(String clusterName) throws Exception {
        DescribeAutoScalingGroupsRequest asgRequest = new DescribeAutoScalingGroupsRequest();
        asgRequest.setAutoScalingGroupNames(Collections.singletonList(clusterName));
        DescribeAutoScalingGroupsResult asgResult = aasClient.describeAutoScalingGroups(asgRequest);
        List<AutoScalingGroup> groups = asgResult.getAutoScalingGroups();
        if (groups.isEmpty()) {
            LOG.warn(String.format("Auto scaling group %s does not exist", clusterName));
            return null;
        }
        return groups.get(0);
    }
}
