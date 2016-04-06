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

import com.pinterest.arcee.autoscaling.AutoScalingManager;
import com.pinterest.arcee.bean.AutoScalingUpdateBean;
import com.pinterest.clusterservice.bean.AwsVmBean;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.pinterest.deployservice.ServiceContext;

import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;


public class AwsVmManager implements ClusterManager<AwsVmBean> {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(AwsVmManager.class);
    private static final String PROCESS_LAUNCH = "Launch";
    private AmazonEC2Client ec2Client;

    private AutoScalingManager autoScalingManager;

    public AwsVmManager(ServiceContext serviceContext) {
        autoScalingManager = serviceContext.getAutoScalingManager();
        ec2Client = serviceContext.getEc2Client();
    }

    @Override
    public void createCluster(String clusterName, AwsVmBean bean) throws Exception {
        AutoScalingUpdateBean requestBean = new AutoScalingUpdateBean();
        requestBean.setMinSize(bean.getMinSize());
        requestBean.setMaxSize(bean.getMaxSize());
        requestBean.setTerminationPolicy("Default");
        requestBean.setSubnets(bean.getSubnet());
        requestBean.setLaunchConfig(autoScalingManager.createLaunchConfig(clusterName, bean));
        autoScalingManager.createAutoScalingGroup(clusterName, requestBean);
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
            autoScalingManager.deleteLaunchConfig(oldBean.getLaunchConfigId());
        }
    }

    @Override
    public AwsVmBean getCluster(String clusterName) throws Exception {
        return autoScalingManager.getAutoScalingGroupInfo(clusterName);
    }

    @Override
    public void deleteCluster(String clusterName) throws Exception {
        AwsVmBean awsVmBean = autoScalingManager.getAutoScalingGroupInfo(clusterName);
        autoScalingManager.deleteAutoScalingGroup(clusterName, true);
        autoScalingManager.deleteLaunchConfig(awsVmBean.getLaunchConfigId());
    }

    @Override
    public void launchHosts(String clusterName, int num) throws Exception {
        autoScalingManager.increaseGroupCapacity(clusterName, num);
    }

    @Override
    public void terminateHosts(String clusterName, Collection<String> hostIds, boolean replaceHost) throws Exception {
        if (replaceHost) {
            termianteEC2Hosts(hostIds);
        } else {
            autoScalingManager.disableAutoScalingActions(clusterName, Collections
                .singletonList(PROCESS_LAUNCH));
            termianteEC2Hosts(hostIds);
            autoScalingManager.decreaseGroupCapacity(clusterName, hostIds.size());
            autoScalingManager
                .enableAutoScalingActions(clusterName, Collections.singletonList(PROCESS_LAUNCH));
        }
    }

    @Override
    public Collection<String> getHosts(String clusterName, Collection<String> hostIds) throws Exception {
        if (hostIds == null || hostIds.isEmpty()) {
           return autoScalingManager.getAutoScalingGroupInfoByName(clusterName).getInstances();
        } else {
            return autoScalingManager.getAutoScalingInstances(clusterName, hostIds);
        }
    }

    private String updateLaunchConfig(String clusterName, AwsVmBean oldBean, AwsVmBean newBean) throws Exception {
        try {
            if (newBean.getImage() == null) {
                newBean.setImage(oldBean.getImage());
            }

            if (newBean.getHostType() == null) {
                newBean.setHostType(oldBean.getHostType());
            }

            if (newBean.getSecurityZone() == null) {
                newBean.setSecurityZone(oldBean.getSecurityZone());
            }

            if (newBean.getAssignPublicIp() == null) {
                newBean.setAssignPublicIp(oldBean.getAssignPublicIp());
            }

            if (newBean.getUserDataConfigs() == null) {
                newBean.setUserDataConfigs(oldBean.getUserDataConfigs());
            }

            if (newBean.getRole() == null) {
                newBean.setRole(oldBean.getRole());
            }

            return autoScalingManager.createLaunchConfig(clusterName, newBean);
        } catch (AmazonClientException e) {
            LOG.error(String.format("Failed to update launch config for %s: %s", clusterName, e.getMessage()));
            throw new Exception(String.format("Failed to udpate launch config for %s: %s", clusterName, e.getMessage()));
        }
    }

    private void updateAutoScalingGroup(String clusterName, AwsVmBean newBean) throws Exception {
        try {
            AutoScalingUpdateBean updateAsgRequest = new AutoScalingUpdateBean();
            updateAsgRequest.setSubnets(newBean.getSubnet());
            updateAsgRequest.setMinSize(newBean.getMinSize());
            updateAsgRequest.setMaxSize(newBean.getMaxSize());
            updateAsgRequest.setLaunchConfig(newBean.getLaunchConfigId());
            autoScalingManager.updateAutoScalingGroup(clusterName, updateAsgRequest);
        } catch (AmazonClientException e) {
            LOG.error(String.format("Failed to update auto scaling group %s: %s", clusterName, e.getMessage()));
            throw new Exception(String.format("Failed to update auto scaling group %s: %s", clusterName, e.getMessage()));
        }
    }

    private void termianteEC2Hosts(Collection<String> hostIds) throws Exception {
        TerminateInstancesRequest terminateRequest = new TerminateInstancesRequest();
        terminateRequest.setInstanceIds(hostIds);
        ec2Client.terminateInstances(terminateRequest);
    }
}
