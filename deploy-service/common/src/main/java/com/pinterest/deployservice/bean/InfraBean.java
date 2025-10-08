/**
 * Copyright (c) 2016-2024 Pinterest, Inc.
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

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class InfraBean {
    private String clusterName; // pindeploy-staging
    private String accountId; // 9de91a7b-cb73-4c9f-865a-a984cb58abca
    private List<AutoScalingAlarm> alarms;
    private String archName; // x86_64
    private Boolean autoRefresh; // false
    private Boolean autoUpdateBaseImage; // false
    private String baseImageId; // nVHLGYclQpGpMA6QfaiKzQ
    private String baseImageName; // cmp_base-ebs-18.04
    private Boolean useIdForBaseImageLookUp; // false
    private String cellName; // aws-us-east-1
    private String hostType; // PZO0dqLSRWWts1eBASiBeg -> m4.large
    private String launchConfig; // pindeploy-staging-2023-02-14-013048
    private String launchTemplateName; // pindeploy-staging-2023-02-15-220830
    private Boolean useLaunchTemplate; // true
    private Integer maxCapacity; // 4
    private Integer minCapacity; // 2
    /*
    placement:
      - GB2Ycl5pQXaYh-jar5YPjQ
      - Dp22H9BcR7-wf0LmZcZcWw
      - qsXEfVfKR-mIFTFdOctCmA
     */
    private List<String> placement;
    private Integer replacementTimeout; // 45
    private List<AutoScalingPolicy> scalingPolicies;
    private List<AutoScalingScheduledActions> scheduledActions;
    private String securityZone; // dev-private-service
    private State state;
    private Boolean statefulStatus; // false
    /*
    access_role: eng-prod
    assign_public_ip: "false"
    cmp_group: CMP,pindeploy-staging
    iam_role: PinDeploy
    pinfo_environment: prod
    pinfo_role: cmp_base
    pinfo_team: cloudeng
    restricteddev: "True"
    */
    private Map<String, String> userData;
}

enum State {
    NORMAL
}

/*
scheduledActions:
  - schedule: 0 0 * * *
    capacity: 1
  - schedule: 0 6 * * *
    capacity: 10
 */
@Setter
@Getter
class AutoScalingScheduledActions {
    private String schedule;
    private Integer capacity;
}

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
class AutoScalingPolicy {
    private Integer cooldown; // 30
    private AutoScalingPolicyType policyType; // SCALEUP
    private Integer scaleSize; // 2
    private AutoScalingPolicyScalingType scalingType; // ChangeInCapacity
}

enum AutoScalingPolicyType {
    SCALEUP,
    SCALEDOWN
}

enum AutoScalingPolicyScalingType {
    ChangeInCapacity,
    PercentChangeInCapacity
}

/*
alarms:
  - comparisonOperator: GreaterThanOrEqualToThreshold
    evaluationPeriod: 5
    fromAwsMetric: true
    metric: CPUUtilization
    threshold: 50
    type: GROW
  - comparisonOperator: LessThanThreshold
    evaluationPeriod: 30
    fromAwsMetric: true
    metric: CPUUtilization
    threshold: 25
    type: SHRINK
 */
@Setter
@Getter
class AutoScalingAlarm {
    private AutoScalingAlarmComparisonOperator comparisonOperator;
    private Integer evaluationPeriod;
    private Boolean fromAwsMetric;
    private String metric;
    private Double threshold;
    private AutoScalingAlarmType type;
}

enum AutoScalingAlarmComparisonOperator {
    LessThanThreshold,
    LessThanOrEqualToThreshold,
    GreaterThanThreshold,
    GreaterThanOrEqualToThreshold
}

enum AutoScalingAlarmType {
    GROW,
    SHRINK
}
