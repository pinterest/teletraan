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

import java.util.*;

public class AwsAutoScalingManager implements AutoScalingManager {
    private static final Logger LOG = LoggerFactory.getLogger(AwsAutoScalingManager.class);

    private final AmazonAutoScalingClient aasClient;
    private final String userDataTemplate;

    public AwsAutoScalingManager(AwsConfigManager configManager) {
        if (StringUtils.isNotEmpty(configManager.getId()) && StringUtils.isNotEmpty(configManager.getKey())) {
            AWSCredentials
                awsCredentials = new BasicAWSCredentials(configManager.getId(), configManager.getKey());
            this.aasClient = new AmazonAutoScalingClient(awsCredentials);
        } else {
            LOG.debug("AWS credential is missing for creating AWS client. Assuming to use role for authentication.");
            this.aasClient = new AmazonAutoScalingClient();
        }

        this.userDataTemplate = configManager.getUserDataTemplate();
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

        if (request.getCurSize() != null) {
            updateAutoScalingGroupRequest.setDesiredCapacity(request.getCurSize());
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
        if (processName.contains(AutoScalingConstants.PROCESS_ALARMNOTIFICATION) &&
                processName.contains(AutoScalingConstants.PROCESS_SCHEDULEDACTIONS)) {
            asgInfo.setStatus(ASGStatus.DISABLED);
        } else {
            asgInfo.setStatus(ASGStatus.ENABLED);
        }

        asgInfo.setMinSize(asgroup.getMinSize());
        asgInfo.setMaxSize(asgroup.getMaxSize());
        asgInfo.setDesiredCapacity(asgroup.getDesiredCapacity());
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
    public AwsVmBean getAutoScalingGroupInfo(String clusterName) throws Exception {
        AutoScalingGroup autoScalingGroup = getAutoScalingGroup(clusterName);
        if (autoScalingGroup == null) {
            LOG.warn(String.format("Failed to get cluster %s: auto scaling group %s does not exist", clusterName, clusterName));
            return null;
        }

        AwsVmBean awsVmBean = getLaunchConfigInfo(autoScalingGroup.getLaunchConfigurationName());

        List<SuspendedProcess> suspendedProcesses = autoScalingGroup.getSuspendedProcesses();
        HashSet<String> processName = new HashSet<>();
        for (SuspendedProcess process : suspendedProcesses) {
            processName.add(process.getProcessName());
        }


        if (processName.contains(AutoScalingConstants.PROCESS_ALARMNOTIFICATION) &&
                processName.contains(AutoScalingConstants.PROCESS_SCHEDULEDACTIONS)) {
            awsVmBean.setAsgStatus(ASGStatus.DISABLED);
        } else {
            awsVmBean.setAsgStatus(ASGStatus.ENABLED);
        }

        awsVmBean.setClusterName(clusterName);
        awsVmBean.setUserDataConfigs(transformUserDataToConfigMap(awsVmBean.getRawUserDataString()));
        awsVmBean.setSubnet(autoScalingGroup.getVPCZoneIdentifier());
        awsVmBean.setMinSize(autoScalingGroup.getMinSize());
        awsVmBean.setMaxSize(autoScalingGroup.getMaxSize());
        awsVmBean.setCurSize(autoScalingGroup.getDesiredCapacity());
        awsVmBean.setTerminationPolicy(autoScalingGroup.getTerminationPolicies().get(0));
        return awsVmBean;
    }

    @Override
    public AwsVmBean getLaunchConfigInfo(String launchConfigId) throws Exception {
        DescribeLaunchConfigurationsRequest configRequest = new DescribeLaunchConfigurationsRequest();
        configRequest.setLaunchConfigurationNames(Collections.singletonList(launchConfigId));
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
        awsVmBean.setBidPrice(config.getSpotPrice());
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

    private AutoScalingGroupBean generateDefaultASGInfo() {
        AutoScalingGroupBean asgInfo = new AutoScalingGroupBean();
        asgInfo.setStatus(ASGStatus.UNKNOWN);
        asgInfo.setTerminationPolicy(AutoScalingTerminationPolicy.Default);
        asgInfo.setInstances(new ArrayList<>());
        asgInfo.setMaxSize(0);
        asgInfo.setMinSize(0);
        return asgInfo;
    }

    private Map<String, String> transformUserDataToConfigMap(String userData) throws Exception {
        String userDataString = userData.replace(userDataTemplate, "");
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

    @Override
    public AutoScalingGroup getAutoScalingGroup(String clusterName) throws Exception {
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
