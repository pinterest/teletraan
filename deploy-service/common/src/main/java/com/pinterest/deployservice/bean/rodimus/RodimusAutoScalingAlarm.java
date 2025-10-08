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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents Rodimus AutoScaling Alarm resource as used in Rodimus API
 *
 * <p>This is related to:
 * https://github.com/pinternal/rodimus/blob/main/common/src/main/java/com/pinterest/clusterservice/bean/AsgAlarmBean.java
 */
@AllArgsConstructor
@Data
@NoArgsConstructor
public class RodimusAutoScalingAlarm {
    // Note: This is always a list of 1 policy. The arn of the policy is the only value consumed,
    //    however, Rodimus still constructs the whole policybean for some reason.
    private List<RodimusAutoScalingPolicy> scalingPolicies = new ArrayList<>();
    // Note: alarmsActions list of 1 arn is not consumed during simple policy POST based on rodimus
    // api code,
    //  deploy-board code, and manual testing.
    private List<String> alarmActions = new ArrayList<>();

    private String alarmId;
    private String metricSource;
    private String comparator;
    private String actionType;
    private String groupName;
    private Double threshold;
    private Integer evaluationTime;
    private Boolean fromAwsMetric;

    /** Determines if two alarms match on Teletraan IaC template user-available parameters */
    public boolean matches(RodimusAutoScalingAlarm other) {
        return Objects.equals(this.metricSource, other.metricSource)
                && Objects.equals(this.comparator, other.comparator)
                && Objects.equals(this.actionType, other.actionType)
                && Objects.equals(this.threshold, other.threshold)
                && Objects.equals(this.evaluationTime, other.evaluationTime)
                && arnMatches(other);
    }

    private boolean arnMatches(RodimusAutoScalingAlarm other) {
        // Match empty policy lists if needed
        if (this.scalingPolicies.isEmpty() && other.scalingPolicies.isEmpty()) {
            return true;
        }
        if (this.scalingPolicies.isEmpty() || other.scalingPolicies.isEmpty()) {
            return false;
        }

        // compare arn when both lists have a policy
        // per rodimus requirements, each list may only have 1 policy
        return Objects.equals(
                this.scalingPolicies.get(0).getArn(), other.scalingPolicies.get(0).getArn());
    }
}
