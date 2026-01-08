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

import com.pinterest.deployservice.bean.rodimus.RodimusAutoScalingAlarm;
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
public class AutoScalingAlarmBean {
    private ComparisonOperator comparisonOperator;
    private Integer evaluationPeriod;
    private Boolean fromAwsMetric;
    private String metric;
    private Double threshold;
    private Type type;

    public enum ComparisonOperator {
        LessThanThreshold,
        LessThanOrEqualToThreshold,
        GreaterThanThreshold,
        GreaterThanOrEqualToThreshold
    }

    public enum Type {
        GROW,
        SHRINK
    }

    public static AutoScalingAlarmBean fromRodimusAutoScalingAlarm(
            RodimusAutoScalingAlarm rodimusAutoScalingAlarm) {
        return AutoScalingAlarmBean.builder()
                .comparisonOperator(
                        ComparisonOperator.valueOf(rodimusAutoScalingAlarm.getComparator()))
                .evaluationPeriod(rodimusAutoScalingAlarm.getEvaluationTime())
                .fromAwsMetric(rodimusAutoScalingAlarm.getFromAwsMetric())
                .metric(rodimusAutoScalingAlarm.getMetricSource())
                .threshold(rodimusAutoScalingAlarm.getThreshold())
                .type(Type.valueOf(rodimusAutoScalingAlarm.getActionType()))
                .build();
    }
}
