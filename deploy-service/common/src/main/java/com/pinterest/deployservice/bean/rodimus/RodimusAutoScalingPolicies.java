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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class RodimusAutoScalingPolicies {
    private List<RodimusAutoScalingPolicy> scalingPolicies = new ArrayList<>();
    private List<RodimusAutoScalingPolicy> scaleupPolicies = new ArrayList<>();
    private List<RodimusAutoScalingPolicy> scaledownPolicies = new ArrayList<>();

    public List<RodimusAutoScalingPolicy> allSimplePolicies() {
        return Stream.of(scaleupPolicies, scaledownPolicies)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }
}
/*
-> helloworlddummyservice-server-production_stage
RodimusAutoScalingPolicies(
  scalingPolicies = [],
  scaleupPolicies = [
    RodimusAutoScalingPolicy(
      policyType = "SCALEUP",
      policyName = "helloworlddummyservice-server-production_stage_scaleup_rule",
      minAdjustmentMagnitude = null,
      metricAggregationType = null,
      stepAdjustments = [],
      targetTrackingScalingConfiguration = null,
      instanceWarmup = null,
      alarms = [
        RodimusAutoScalingPolicy.Alarm(
          alarmName = "helloworlddummyservice-server-production_stage-alarm-cml46z6uTHCWcmlmKN1R9w",
          alarmARN = "arn:aws:cloudwatch:us-east-1:998131032990:alarm:helloworlddummyservice-server-production_stage-alarm-cml46z6uTHCWcmlmKN1R9w"
        )
      ],
      arn = "arn:aws:autoscaling:us-east-1:998131032990:scalingPolicy:06c9670c-e59e-4a31-8a60-6de92b461389:autoScalingGroupName/helloworlddummyservice-server-production_stage:policyName/helloworlddummyservice-server-production_stage_scaleup_rule",
      scalingType = "ChangeInCapacity",
      scaleSize = 1,
      coolDown = 30
    )
  ],
  scaledownPolicies = [
    RodimusAutoScalingPolicy(
      policyType = "SCALEDOWN",
      policyName = "helloworlddummyservice-server-production_stage_scaledown_rule",
      minAdjustmentMagnitude = null,
      metricAggregationType = null,
      stepAdjustments = [],
      targetTrackingScalingConfiguration = null,
      instanceWarmup = null,
      alarms = [],
      arn = "arn:aws:autoscaling:us-east-1:998131032990:scalingPolicy:75d35953-a7f9-4f8b-9e44-4302a67790c7:autoScalingGroupName/helloworlddummyservice-server-production_stage:policyName/helloworlddummyservice-server-production_stage_scaledown_rule",
      scalingType = "ChangeInCapacity",
      scaleSize = 0,
      coolDown = 0
    )
  ]
)
 */
