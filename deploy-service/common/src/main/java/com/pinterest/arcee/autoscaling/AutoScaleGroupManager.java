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
import java.util.List;
import java.util.Map;


public interface AutoScaleGroupManager {
    //------ Launch Config
    String createLaunchConfig(GroupBean request) throws Exception;

    String createLaunchConfig(String groupName, String instanceId, String imageId) throws Exception;

    void deleteLaunchConfig(String configId) throws Exception;

    void updateSubnet(String groupName, String subnets) throws Exception;

    void createAutoScalingGroup(String configId, AutoScalingRequestBean request, String subnets) throws Exception;

    void updateAutoScalingGroup(AutoScalingRequestBean request, String subnets) throws Exception;

    void changeAutoScalingGroupLaunchConfig(String groupName, String configId) throws Exception;

    //------ Instance
    void addInstancesToAutoScalingGroup(Collection<String> instances, String groupName) throws Exception;

    void detachInstancesFromAutoScalingGroup(Collection<String> instances, String groupName, boolean decreaseSize) throws Exception;

    void increaseASGDesiredCapacityBySize(String groupName, int instanceCnt) throws Exception;

    void terminateInstanceInAutoScalingGroup(String instanceId, boolean decreaseSize) throws Exception;

    //------ Scaling policy
    GroupBean getLaunchConfigByName(String configId) throws Exception;

    AutoScalingGroupBean getAutoScalingGroupInfoByName(String asgName) throws Exception;

    boolean hasAutoScalingGroup(String groupName) throws Exception;

    void addScalingPolicyToGroup(String groupName, ScalingPolicyBean policyBean) throws Exception;

    Map<String, ScalingPolicyBean> getScalingPoliciesForGroup(String groupName) throws Exception;

    void disableAutoScalingGroup(String groupName) throws Exception;

    void enableAutoScalingGroup(String groupName) throws Exception;

    void deleteAutoScalingGroup(String groupName, boolean detachInstances) throws Exception;

    ASGStatus getAutoScalingGroupStatus(String groupName) throws Exception;

    ScalingActivitiesBean getScalingActivity(String groupName, int count, String token) throws Exception;

    //-------- Health Check
    boolean isScalingDownEventEnabled(String groupName) throws Exception;

    void disableScalingDownEvent(String groupName) throws Exception;

    void enableScalingDownEvent(String groupName) throws Exception;

    Collection<String> instancesInAutoScalingGroup(Collection<String> instances) throws Exception;

    //-------- LifeCycle Hook
    void createLifecycleHook(String groupName, int timeout) throws Exception;

    void deleteLifecycleHook(String groupName) throws Exception;

    void completeLifecycleAction(String hookId, String tokenId, String groupName) throws Exception;
}
