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

import com.pinterest.arcee.bean.AsgScheduleBean;
import com.pinterest.arcee.bean.AutoScalingGroupBean;
import com.pinterest.arcee.bean.ScalingPolicyBean;
import com.pinterest.arcee.bean.ScalingActivitiesBean;
import com.pinterest.clusterservice.bean.AwsVmBean;
import com.pinterest.deployservice.bean.ASGStatus;

import java.util.Collection;
import java.util.Map;


public interface AutoScalingManager {
    //------ Launch Config
    String createLaunchConfig(String groupName, AwsVmBean request) throws Exception;

    void deleteLaunchConfig(String configId) throws Exception;

    // Auto scaling group
    void createAutoScalingGroup(String groupName, AwsVmBean request) throws Exception;

    void deleteAutoScalingGroup(String groupName, boolean detachInstances) throws Exception;

    void updateAutoScalingGroup(String groupName, AwsVmBean request) throws Exception;

    void increaseGroupCapacity(String groupName, int size) throws Exception;

    void decreaseGroupCapacity(String groupName, int size) throws Exception;

    void disableAutoScalingGroup(String groupName) throws Exception;

    void enableAutoScalingGroup(String groupName) throws Exception;

    void disableAutoScalingActions(String groupName, Collection<String> actions) throws Exception;

    void enableAutoScalingActions(String groupName, Collection<String> actions) throws Exception;

    AutoScalingGroupBean getAutoScalingGroupInfoByName(String asgName) throws Exception;

    AwsVmBean getAutoScalingGroupInfo(String clusterName) throws Exception;

    AwsVmBean getLaunchConfigInfo(String launchConfigId) throws Exception;

    ASGStatus getAutoScalingGroupStatus(String groupName) throws Exception;

    Collection<String> getAutoScalingInstances(Collection<String> groupNames, Collection<String> hostIds) throws Exception;

    //------ Instance
    void addInstancesToAutoScalingGroup(Collection<String> instances, String groupName) throws Exception;

    void detachInstancesFromAutoScalingGroup(Collection<String> instances, String groupName, boolean decreaseSize) throws Exception;

    boolean isInstanceProtected(String instances) throws Exception;

    void protectInstanceInAutoScalingGroup(Collection<String> instances, String groupName) throws Exception;

    void unprotectInstanceInAutoScalingGroup(Collection<String> instances, String groupName) throws Exception;

    //-- auto scaling policies
    void addScalingPolicyToGroup(String groupName, ScalingPolicyBean policyBean) throws Exception;

    Map<String, ScalingPolicyBean> getScalingPoliciesForGroup(String groupName) throws Exception;

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

    //--------- Time based auto scaling
    void putScheduledAction(String clusterName, AsgScheduleBean asgScheduleBean) throws Exception;

    Collection<AsgScheduleBean> getScheduledActions(String clusterName) throws Exception;

    void deleteScheduledAction(String clusterName, String actionId) throws Exception;

    String transformUserDataConfigToString(String clusterName, Map<String, String> userDataConfigs) throws Exception;
}
