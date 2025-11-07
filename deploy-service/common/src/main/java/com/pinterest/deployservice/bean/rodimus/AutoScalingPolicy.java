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
package com.pinterest.deployservice.bean.rodimus;

import lombok.Getter;
import lombok.Setter;

/*
scalingPolicies:
  - cooldown: 30
    policyType: SCALEUP
    scaleSize: 2
    scalingType: ChangeInCapacity
  - cooldown: 30
    policyType: SCALEDOWN
    scaleSize: 2
    scalingType: ChangeInCapacity
 */
@Setter
@Getter
public class AutoScalingPolicy {
    private Integer cooldown; // 30
    private AutoScalingPolicyType policyType; // SCALEUP
    private Integer scaleSize; // 2
    private AutoScalingPolicyScalingType scalingType; // ChangeInCapacity
}
