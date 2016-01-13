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

import com.pinterest.arcee.bean.*;
import com.pinterest.deployservice.bean.ASGStatus;

import java.util.Collection;
import java.util.Map;


public interface AutoScaleGroupManager {
    //------ Launch Config
    public String createLaunchConfig(GroupBean request) throws Exception;

    public String createLaunchConfig(String groupName, String instanceId, String imageId) throws Exception;

    public void deleteLaunchConfig(String configId) throws Exception;

    public void updateSubnet(String groupName, String subnets) throws Exception;

    public void createAutoScalingGroup(String configId, AutoScalingRequestBean request, String subnets) throws Exception;

    public void updateAutoScalingGroup(AutoScalingRequestBean request, String subnets) throws Exception;

    public void changeAutoScalingGroupLaunchConfig(String groupName, String configId) throws Exception;

    //------ Instance
    public void addInstancesToAutoScalingGroup(Collection<String> instances, String groupName) throws Exception;

    public void detachInstancesFromAutoScalingGroup(Collection<String> instances, String groupName, boolean decreaseSize) throws Exception;

    public void increaseASGDesiredCapacityBySize(String groupName, int instanceCnt) throws Exception;

    public void terminateInstanceInAutoScalingGroup(String instanceId, boolean decreaseSize) throws Exception;

    //------ Scaling policy
    public GroupBean getLaunchConfigByName(String configId) throws Exception;

    public AutoScalingGroupBean getAutoScalingGroupInfoByName(String asgName) throws Exception;

    public boolean hasAutoScalingGroup(String groupName) throws Exception;

    public void addScalingPolicyToGroup(String groupName, ScalingPolicyBean policyBean) throws Exception;

    public Map<String, ScalingPolicyBean> getScalingPoliciesForGroup(String groupName) throws Exception;

    public void disableAutoScalingGroup(String groupName) throws Exception;

    public void enableAutoScalingGroup(String groupName) throws Exception;

    public void deleteAutoScalingGroup(String groupName, boolean detachInstances) throws Exception;

    public ASGStatus getAutoScalingGroupStatus(String groupName) throws Exception;

    public ScalingActivitiesBean getScalingActivity(String groupName, int count, String token) throws Exception;

    //-------- Health Check
    public boolean isScalingDownEventEnabled(String groupName) throws Exception;

    public void disableScalingDownEvent(String groupName) throws Exception;

    public void enableScalingDownEvent(String groupName) throws Exception;

    public Collection<String> instancesInAutoScalingGroup(Collection<String> instances) throws Exception;
}
