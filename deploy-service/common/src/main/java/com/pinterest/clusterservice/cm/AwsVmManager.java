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
package com.pinterest.clusterservice.cm;

import com.pinterest.arcee.aws.AwsConfigManager;
import com.pinterest.clusterservice.bean.AwsVmBean;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.autoscaling.model.*;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class AwsVmManager implements ClusterManager<AwsVmBean> {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(AwsVmManager.class);
    private static final String PROCESS_AZREBALANCE = "AZRebalance";
    private static final String AWS_VM_KEYNAME = "ops";
    private static final String AWS_DEFAULT_ROLE = "base";
    private static final String AWS_ROLE_TEMPLATE = "arn:aws:iam::%s:instance-profile/%s";
    private static final String AWS_USERDATA_TEMPLATE = "#cloud-config\nrole: %s\nCMP: \n  -group: %s";
    private static final String[] NOTIFICATION_TYPE = {
            "autoscaling:EC2_INSTANCE_LAUNCH",
            "autoscaling:EC2_INSTANCE_LAUNCH_ERROR",
            "autoscaling:EC2_INSTANCE_TERMINATE",
            "autoscaling:EC2_INSTANCE_TERMINATE_ERROR"};
    private AmazonAutoScalingClient aasClient;
    private AmazonEC2Client ec2Client;
    private String ownerId;
    private String snsArn;

    public AwsVmManager(AwsConfigManager configManager) {
        if (StringUtils.isNotEmpty(configManager.getId()) && StringUtils.isNotEmpty(configManager.getKey())) {
            AWSCredentials awsCredentials = new BasicAWSCredentials(configManager.getId(), configManager.getKey());
            this.aasClient = new AmazonAutoScalingClient(awsCredentials);
            this.ec2Client = new AmazonEC2Client(awsCredentials);
        } else {
            LOG.debug("AWS credential is missing for creating AWS client. Assuming to use role for authentication.");
            this.aasClient = new AmazonAutoScalingClient();
            this.ec2Client = new AmazonEC2Client();
        }

        this.ownerId = configManager.getOwnerId();
        this.snsArn = configManager.getSnsArn();
    }

    @Override
    public void createCluster(String clusterName, AwsVmBean bean) throws Exception {
        bean.setLaunchConfigId(createLaunchConfig(clusterName, bean));
        createAutoScalingGroup(clusterName, bean);

        SuspendProcessesRequest suspendProcessesRequest = new SuspendProcessesRequest();
        suspendProcessesRequest.setAutoScalingGroupName(clusterName);
        suspendProcessesRequest.setScalingProcesses(Arrays.asList(PROCESS_AZREBALANCE));

        if (!StringUtils.isEmpty(snsArn)) {
            PutNotificationConfigurationRequest notifRequest = new PutNotificationConfigurationRequest();
            notifRequest.setAutoScalingGroupName(clusterName);
            notifRequest.setTopicARN(snsArn);
            notifRequest.setNotificationTypes(Arrays.asList(NOTIFICATION_TYPE));
            aasClient.putNotificationConfiguration(notifRequest);
        }
    }

    @Override
    public void updateCluster(String clusterName, AwsVmBean newBean) throws Exception {
        Object asgObject = getCluster(clusterName);
        if (asgObject == null) {
            LOG.error(String.format("Autoscaling group %s does not exist. Cannot update cluster", clusterName));
            throw new Exception(String.format("Autoscaling group %s does not exist. Cannot update cluster", clusterName));
        }

        AwsVmBean oldBean = (AwsVmBean) asgObject;
        newBean.setLaunchConfigId(updateLaunchConfig(clusterName, oldBean, newBean));
        updateAutoScalingGroup(clusterName, newBean);
        if (newBean.getLaunchConfigId() != null) {
            deleteLaunchConfig(oldBean.getLaunchConfigId());
        }
    }

    @Override
    public AwsVmBean getCluster(String clusterName) throws Exception {
        AutoScalingGroup group = getAutoScalingGroup(clusterName);
        if (group == null) {
            LOG.warn(String.format("Failed to get cluster %s: auto scaling group %s does not exist", clusterName, clusterName));
            return null;
        }

        LaunchConfiguration config = getLaunchConfig(group.getLaunchConfigurationName());
        if (config == null) {
            LOG.warn(String.format("Failed to get cluster: Launch config %s does not exist", group.getLaunchConfigurationName()));
            return null;
        }

        AwsVmBean awsVmBean = new AwsVmBean();
        awsVmBean.setClusterName(clusterName);
        awsVmBean.setImage(config.getImageId());
        awsVmBean.setHostType(config.getInstanceType());
        awsVmBean.setSecurityZone(config.getSecurityGroups().get(0));
        awsVmBean.setAssignPublicIp(config.getAssociatePublicIpAddress());
        awsVmBean.setLaunchConfigId(config.getLaunchConfigurationName());
        String roleName = config.getIamInstanceProfile();
        awsVmBean.setRole(roleName.split("/")[1]);
        String userData = new String(Base64.decodeBase64(config.getUserData()));
        awsVmBean.setUserDataConfigs(transformUserDataToConfigMap(clusterName, userData));
        awsVmBean.setSubnet(group.getVPCZoneIdentifier());
        awsVmBean.setMinSize(group.getMinSize());
        awsVmBean.setMaxSize(group.getMaxSize());
        return awsVmBean;
    }

    @Override
    public void deleteCluster(String clusterName) throws Exception {
        AwsVmBean awsVmBean = getCluster(clusterName);
        deleteAutoScalingGroup(clusterName);
        deleteLaunchConfig(awsVmBean.getLaunchConfigId());
    }

    @Override
    public void launchHosts(String clusterName, int num) throws Exception {
        AutoScalingGroup group = getAutoScalingGroup(clusterName);
        if (group == null) {
            LOG.error(String.format("Failed to launch hosts: auto scaling group %s does not exist", clusterName));
            throw new Exception(String.format("Failed to launch hosts: auto scaling group %s does not exist", clusterName));
        }

        int currMinSize = group.getMinSize();
        int currMaxSize = group.getMaxSize();
        UpdateAutoScalingGroupRequest updateRequest = new UpdateAutoScalingGroupRequest();
        updateRequest.setAutoScalingGroupName(clusterName);
        updateRequest.setMaxSize(currMaxSize + num);
        if (currMaxSize == currMinSize) {
            updateRequest.setMinSize(currMinSize + num);
        } else {
            updateRequest.setDesiredCapacity(group.getDesiredCapacity() + num);
        }
        aasClient.updateAutoScalingGroup(updateRequest);
    }

    @Override
    public void terminateHosts(String clusterName, Collection<String> hostIds, boolean replaceHost) throws Exception {
        if (replaceHost) {
            TerminateInstancesRequest terminateRequest = new TerminateInstancesRequest();
            terminateRequest.setInstanceIds(hostIds);
            ec2Client.terminateInstances(terminateRequest);
        } else {
            // Do not replace host and decrease the cluster capacity
            AutoScalingGroup group = getAutoScalingGroup(clusterName);
            if (group == null) {
                LOG.error(String.format("Failed to terminate hosts: auto scaling group %s does not exist", clusterName));
                throw new Exception(String.format("Failed to terminate hosts: auto scaling group %s does not exist", clusterName));
            }

            int currMinSize = group.getMinSize();
            int currMaxSize = group.getMaxSize();
            UpdateAutoScalingGroupRequest updateRequest = new UpdateAutoScalingGroupRequest();
            updateRequest.setAutoScalingGroupName(clusterName);
            updateRequest.setMinSize(Math.max(currMinSize - hostIds.size(), 0));
            aasClient.updateAutoScalingGroup(updateRequest);

            for (String hostId : hostIds) {
                TerminateInstanceInAutoScalingGroupRequest terminateRequest = new TerminateInstanceInAutoScalingGroupRequest();
                terminateRequest.setShouldDecrementDesiredCapacity(true);
                terminateRequest.setInstanceId(hostId);
                aasClient.terminateInstanceInAutoScalingGroup(terminateRequest);
            }

            if (currMaxSize == currMinSize) {
                UpdateAutoScalingGroupRequest updateMaxSizeRequest = new UpdateAutoScalingGroupRequest();
                updateMaxSizeRequest.setAutoScalingGroupName(clusterName);
                updateMaxSizeRequest.setMaxSize(Math.max(currMaxSize - hostIds.size(), 0));
                aasClient.updateAutoScalingGroup(updateMaxSizeRequest);
            }
        }
    }

    @Override
    public Collection<String> getHosts(String clusterName, Collection<String> hostIds) throws Exception {
        Collection<String> asgHostIds = new ArrayList<>();
        if (hostIds == null || hostIds.isEmpty()) {
            AutoScalingGroup group = getAutoScalingGroup(clusterName);
            if (group == null) {
                LOG.error(String.format("Failed to get hosts: auto scaling group %s does not exist", clusterName));
                throw new Exception(String.format("Failed to get hosts: auto scaling group %s does not exist", clusterName));
            }

            List<Instance> asgInstances = group.getInstances();
            for (Instance asgInstance : asgInstances) {
                asgHostIds.add(asgInstance.getInstanceId());
            }
        } else {
            DescribeAutoScalingInstancesRequest asgInstancesRequest = new DescribeAutoScalingInstancesRequest();
            asgInstancesRequest.setInstanceIds(hostIds);
            DescribeAutoScalingInstancesResult asgInstancesResult = aasClient.describeAutoScalingInstances(asgInstancesRequest);
            List<AutoScalingInstanceDetails> instanceDetails = asgInstancesResult.getAutoScalingInstances();
            for (AutoScalingInstanceDetails instanceDetail : instanceDetails) {
                if (instanceDetail.getAutoScalingGroupName().equals(clusterName)) {
                    asgHostIds.add(instanceDetail.getInstanceId());
                }
            }
        }
        return asgHostIds;
    }

    // Launch Config utils
    private String genLaunchConfigId(String clusterName) {
        Date date = new Date();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HHmmss");
        return String.format("%s-%s", clusterName, df.format(date));
    }

    private String createLaunchConfig(String clusterName, AwsVmBean awsVmBean) throws Exception {
        try {
            CreateLaunchConfigurationRequest configRequest = new CreateLaunchConfigurationRequest();
            String launchConfigId = genLaunchConfigId(clusterName);
            configRequest.setLaunchConfigurationName(launchConfigId);
            configRequest.setKeyName(AWS_VM_KEYNAME);
            configRequest.setImageId(awsVmBean.getImage());
            configRequest.setInstanceType(awsVmBean.getHostType());
            configRequest.setSecurityGroups(Arrays.asList(awsVmBean.getSecurityZone()));
            configRequest.setAssociatePublicIpAddress(awsVmBean.getAssignPublicIp());
            String userData = transformUserDataConfigToString(clusterName, awsVmBean.getUserDataConfigs());
            configRequest.setUserData(Base64.encodeBase64String(userData.getBytes()));
            if (awsVmBean.getRole() == null) {
                configRequest.setIamInstanceProfile(String.format(AWS_ROLE_TEMPLATE, ownerId, AWS_DEFAULT_ROLE));
            } else {
                configRequest.setIamInstanceProfile(String.format(AWS_ROLE_TEMPLATE, ownerId, awsVmBean.getRole()));
            }
            configRequest.setInstanceMonitoring(new InstanceMonitoring().withEnabled(false));
            aasClient.createLaunchConfiguration(configRequest);
            return launchConfigId;
        } catch (AmazonClientException e) {
            LOG.error(String.format("Failed to create launch config for %s: %s", clusterName, e.getMessage()));
            throw new Exception(String.format("Failed to create launch config for %s: %s", clusterName, e.getMessage()));
        }
    }

    private String updateLaunchConfig(String clusterName, AwsVmBean oldBean, AwsVmBean newBean) throws Exception {
        try {
            CreateLaunchConfigurationRequest configRequest = new CreateLaunchConfigurationRequest();
            String launchConfigId = genLaunchConfigId(clusterName);
            configRequest.setLaunchConfigurationName(launchConfigId);
            configRequest.setKeyName(AWS_VM_KEYNAME);
            configRequest.setImageId(newBean.getImage() != null ? newBean.getImage() : oldBean.getImage());
            configRequest.setInstanceType(newBean.getHostType() != null ? newBean.getHostType() : oldBean.getHostType());
            configRequest.setSecurityGroups(Arrays.asList(newBean.getSecurityZone() != null ? newBean.getSecurityZone() : oldBean.getSecurityZone()));
            configRequest.setAssociatePublicIpAddress(newBean.getAssignPublicIp() != null ? newBean.getAssignPublicIp() : oldBean.getAssignPublicIp());
            String userData;
            if (newBean.getUserDataConfigs() != null) {
                userData = transformUserDataConfigToString(clusterName, newBean.getUserDataConfigs());
            } else {
                userData = transformUserDataConfigToString(clusterName, oldBean.getUserDataConfigs());
            }
            configRequest.setUserData(Base64.encodeBase64String(userData.getBytes()));

            if (newBean.getRole() != null) {
                configRequest.setIamInstanceProfile(String.format(AWS_ROLE_TEMPLATE, ownerId, newBean.getRole()));
            } else {
                configRequest.setIamInstanceProfile(String.format(AWS_ROLE_TEMPLATE, ownerId, oldBean.getRole()));
            }

            configRequest.setInstanceMonitoring(new InstanceMonitoring().withEnabled(false));
            aasClient.createLaunchConfiguration(configRequest);
            return launchConfigId;
        } catch (AmazonClientException e) {
            LOG.error(String.format("Failed to update launch config for %s: %s", clusterName, e.getMessage()));
            throw new Exception(String.format("Failed to udpate launch config for %s: %s", clusterName, e.getMessage()));
        }
    }

    private LaunchConfiguration getLaunchConfig(String launchConfigId) throws Exception {
        DescribeLaunchConfigurationsRequest configRequest = new DescribeLaunchConfigurationsRequest();
        configRequest.setLaunchConfigurationNames(Arrays.asList(launchConfigId));
        DescribeLaunchConfigurationsResult configResult = aasClient.describeLaunchConfigurations(configRequest);
        List<LaunchConfiguration> configs = configResult.getLaunchConfigurations();
        if (configs.isEmpty()) {
            LOG.warn(String.format("Launch config %s does not exist", launchConfigId));
            return null;
        }
        return configs.get(0);
    }

    private void deleteLaunchConfig(String launchConfigId) throws Exception {
        try {
            DeleteLaunchConfigurationRequest deleteRequest = new DeleteLaunchConfigurationRequest();
            deleteRequest.setLaunchConfigurationName(launchConfigId);
            aasClient.deleteLaunchConfiguration(deleteRequest);
        } catch (AmazonClientException e) {
            LOG.error(String.format("Failed to delete launch config id %s: %s", launchConfigId, e.getMessage()));
            throw new Exception(String.format("Failed to delete launch config id %s: %s", launchConfigId, e.getMessage()));
        }
    }

    // Auto Scaling Group util
    private void createAutoScalingGroup(String clusterName, AwsVmBean bean) throws Exception {
        try {
            CreateAutoScalingGroupRequest asgRequest = new CreateAutoScalingGroupRequest();
            asgRequest.setAutoScalingGroupName(clusterName);
            asgRequest.setLaunchConfigurationName(bean.getLaunchConfigId());
            asgRequest.setVPCZoneIdentifier(bean.getSubnet());
            asgRequest.setMinSize(bean.getMinSize());
            asgRequest.setMaxSize(bean.getMaxSize());
            aasClient.createAutoScalingGroup(asgRequest);
        } catch (AmazonClientException e) {
            LOG.error(String.format("Failed to create auto scaling group %s: %s", clusterName, e.getMessage()));
            throw new Exception(String.format("Failed to create auto scaling group %s: %s", clusterName, e.getMessage()));
        }
    }

    private void updateAutoScalingGroup(String clusterName, AwsVmBean newBean) throws Exception {
        try {
            UpdateAutoScalingGroupRequest updateAsgRequest = new UpdateAutoScalingGroupRequest();
            updateAsgRequest.setAutoScalingGroupName(clusterName);
            if (newBean.getSubnet() != null) {
                updateAsgRequest.setVPCZoneIdentifier(newBean.getSubnet());
            }

            if (newBean.getMinSize() != null) {
                updateAsgRequest.setMinSize(newBean.getMinSize());
            }

            if (newBean.getMaxSize() != null) {
                updateAsgRequest.setMaxSize(newBean.getMaxSize());
            }

            if (newBean.getLaunchConfigId() != null) {
                updateAsgRequest.setLaunchConfigurationName(newBean.getLaunchConfigId());
            }
            aasClient.updateAutoScalingGroup(updateAsgRequest);
        } catch (AmazonClientException e) {
            LOG.error(String.format("Failed to update auto scaling group %s: %s", clusterName, e.getMessage()));
            throw new Exception(String.format("Failed to update auto scaling group %s: %s", clusterName, e.getMessage()));
        }
    }

    private AutoScalingGroup getAutoScalingGroup(String clusterName) throws Exception {
        DescribeAutoScalingGroupsRequest asgRequest = new DescribeAutoScalingGroupsRequest();
        asgRequest.setAutoScalingGroupNames(Arrays.asList(clusterName));
        DescribeAutoScalingGroupsResult asgResult = aasClient.describeAutoScalingGroups(asgRequest);
        List<AutoScalingGroup> groups = asgResult.getAutoScalingGroups();
        if (groups.isEmpty()) {
            LOG.warn(String.format("Auto scaling group %s does not exist", clusterName));
            return null;
        }
        return groups.get(0);
    }

    private void deleteAutoScalingGroup(String clusterName) throws Exception {
        try {
            DeleteAutoScalingGroupRequest deleteRequest = new DeleteAutoScalingGroupRequest();
            deleteRequest.setAutoScalingGroupName(clusterName);
            deleteRequest.setForceDelete(true);
            aasClient.deleteAutoScalingGroup(deleteRequest);
        } catch (AmazonClientException e) {
            LOG.error(String.format("Failed to delete auto scaling group %s: %s", clusterName, e.getMessage()));
            throw new Exception(String.format("Failed to delete auto scaling group %s: %s", clusterName, e.getMessage()));
        }
    }

    private Map<String, String> transformUserDataToConfigMap(String clusterName, String userData) throws Exception {
        String userDataString = userData.replace(String.format(AWS_USERDATA_TEMPLATE, clusterName, clusterName), "");
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

    private String transformUserDataConfigToString(String clusterName, Map<String, String> userDataConfigs) throws Exception {
        String prefix = String.format(AWS_USERDATA_TEMPLATE, clusterName, clusterName);
        StringBuilder resultBuilder = new StringBuilder();
        resultBuilder.append(prefix);
        if (userDataConfigs == null) {
            return resultBuilder.toString();
        }

        for (Map.Entry<String, String> entry : userDataConfigs.entrySet()) {
            resultBuilder.append(String.format("\n   %s: %s", entry.getKey(), entry.getValue()));
        }
        return resultBuilder.toString();
    }
}
