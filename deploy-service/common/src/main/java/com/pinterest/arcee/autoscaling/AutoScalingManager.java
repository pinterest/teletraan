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

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.pinterest.arcee.bean.AutoScalingGroupBean;
import com.pinterest.clusterservice.bean.AwsVmBean;

public interface AutoScalingManager {
    // Auto scaling group
    void updateAutoScalingGroup(String groupName, AwsVmBean request) throws Exception;

    AutoScalingGroupBean getAutoScalingGroupInfoByName(String asgName) throws Exception;

    AwsVmBean getAutoScalingGroupInfo(String clusterName) throws Exception;

    AwsVmBean getLaunchConfigInfo(String launchConfigId) throws Exception;

    AutoScalingGroup getAutoScalingGroup(String clusterName) throws Exception;
}
