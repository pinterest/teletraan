/**
 * Copyright (c) 2025 Pinterest, Inc.
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
package com.pinterest.deployservice.bean;

import com.pinterest.deployservice.bean.rodimus.RodimusAutoScalingPolicy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ScalingPolicyBean {
    private Integer coolDown;
    private PolicyType policyType;
    private Integer scaleSize;
    private ScalingType scalingType;

    public enum PolicyType {
        SCALEUP,
        SCALEDOWN
    }

    public enum ScalingType {
        ChangeInCapacity,
        PercentChangeInCapacity
    }

    public static ScalingPolicyBean fromRodimusAutoScalingPolicy(RodimusAutoScalingPolicy rodimusAutoScalingPolicy) {
      return ScalingPolicyBean.builder()
              .coolDown(rodimusAutoScalingPolicy.getCoolDown())
              .policyType(PolicyType.valueOf(rodimusAutoScalingPolicy.getPolicyType()))
              .scalingType(ScalingType.valueOf(rodimusAutoScalingPolicy.getScalingType()))
              .scaleSize(rodimusAutoScalingPolicy.getScaleSize())
              .build();
    }
}
