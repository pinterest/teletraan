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

import com.pinterest.deployservice.bean.ScalingPolicyBean;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents Rodimus AutoScaling Policy resource as used in Rodimus API
 *
 * <p>This is related to:
 * https://github.com/pinternal/rodimus/blob/main/common/src/main/java/com/pinterest/clusterservice/bean/ScalingPolicyBean.java
 */
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class RodimusAutoScalingPolicy {
    private String policyType;
    private String policyName;
    private Integer minAdjustmentMagnitude;
    private String metricAggregationType;
    private List<StepAdjustment> stepAdjustments;
    private TargetTrackingScalingConfiguration targetTrackingScalingConfiguration;
    private Integer instanceWarmup;
    private List<Alarm>
            alarms; // Same resource but different fields compared to RodimusAutoScalingAlarm
    private String arn;
    private String scalingType;
    private Integer scaleSize;
    private Integer coolDown;

    @AllArgsConstructor
    @Data
    @NoArgsConstructor
    public static class StepAdjustment {
        private Double metricIntervalLowerBound;
        private Double metricIntervalUpperBound;
        private Integer scalingAdjustment;
    }

    @AllArgsConstructor
    @Data
    @NoArgsConstructor
    public static class TargetTrackingScalingConfiguration {
        private PredefinedMetricSpecification predefinedMetricSpecification;
        private Double targetValue;
        private Boolean disableScaleIn;

        @AllArgsConstructor
        @Data
        @NoArgsConstructor
        public static class PredefinedMetricSpecification {
            private String predefinedMetricType;
            private String resourceLabel;
        }
    }

    @AllArgsConstructor
    @Data
    @NoArgsConstructor
    public static class Alarm {
        private String alarmName;
        private String alarmARN;
    }

    /**
     * Determines if two autoscaling policies match on Teletraan IaC template user-available
     * parameters
     *
     * <p>The current Teletraan deploy-board form only allows 1 scaleup policy and 1 scaledown
     * policy Teletraan IaC will enforce the same constraint.
     */
    public boolean matches(RodimusAutoScalingPolicy other) {
        return this.policyType.equalsIgnoreCase(other.policyType)
                && this.scalingType.equals(other.scalingType)
                && Objects.equals(this.scaleSize, other.scaleSize)
                && Objects.equals(this.coolDown, other.coolDown);
    }

    public static RodimusAutoScalingPolicy fromScalingPolicyBean(ScalingPolicyBean scalingPolicy) {
        return RodimusAutoScalingPolicy.builder()
                .policyType(scalingPolicy.getPolicyType().name())
                .policyName("")
                .minAdjustmentMagnitude(null)
                .metricAggregationType(null)
                .stepAdjustments(null)
                .targetTrackingScalingConfiguration(null)
                .instanceWarmup(null)
                .alarms(null)
                .arn("")
                .scalingType(scalingPolicy.getScalingType().name())
                .scaleSize(scalingPolicy.getScaleSize())
                .coolDown(scalingPolicy.getCoolDown())
                .build();
    }
}
