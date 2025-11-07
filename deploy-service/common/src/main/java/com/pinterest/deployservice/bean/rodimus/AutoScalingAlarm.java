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
public class AutoScalingAlarm {
    private AutoScalingAlarmComparisonOperator comparisonOperator;
    private Integer evaluationPeriod;
    private Boolean fromAwsMetric;
    private String metric;
    private Double threshold;
    private AutoScalingAlarmType type;
}
