/**
 * Copyright (c) 2016-2025 Pinterest, Inc.
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

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/* Used for worker */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class InfraConfigBean extends BaseBean implements Serializable {
    private String operator;
    private String envName;
    private String stageName;
    private String clusterName;
    private String accountId;
    private String region;
    private String archName;
    private Integer maxCapacity;
    private Integer minCapacity;
    private CloudProvider provider;
    private String baseImage;
    private String baseImageName;
    private String hostType;
    private String securityGroup;
    private List<String> subnets;
    private Map<String, String> configs;
    private Boolean autoUpdateBaseImage;
    private Boolean statefulStatus;
    private Boolean autoRefresh;
    private Long replacementTimeout;
    private Boolean useEnaExpress;
    private Boolean useEbsCheck;
    private List<ScalingPolicyBean> scalingPolicies;
    private List<AutoScalingAlarmBean> autoScalingAlarm;
    private List<ScheduledActionBean> scheduledAction;

    public static InfraConfigBean fromInfraBean(
            String operator, String envName, String stageName, InfraBean infraBean) {
        return InfraConfigBean.builder()
                .operator(operator)
                .envName(envName)
                .stageName(stageName)
                .clusterName(infraBean.getClusterName())
                .accountId(infraBean.getAccountId())
                .region(infraBean.getRegion())
                .archName(infraBean.getArchName())
                .maxCapacity(infraBean.getMaxCapacity())
                .minCapacity(infraBean.getMinCapacity())
                .provider(infraBean.getProvider())
                .baseImage(infraBean.getBaseImage())
                .baseImageName(infraBean.getBaseImageName())
                .hostType(infraBean.getHostType())
                .securityGroup(infraBean.getSecurityGroup())
                .subnets(infraBean.getSubnets())
                .configs(infraBean.getConfigs())
                .autoUpdateBaseImage(infraBean.getAutoUpdateBaseImage())
                .statefulStatus(infraBean.getStatefulStatus())
                .autoRefresh(infraBean.getAutoRefresh())
                .replacementTimeout(infraBean.getReplacementTimeout())
                .useEnaExpress(infraBean.getUseEnaExpress())
                .useEbsCheck(infraBean.getUseEbsCheck())
                .scalingPolicies(infraBean.getScalingPolicies())
                .autoScalingAlarm(infraBean.getAutoScalingAlarm())
                .scheduledAction(infraBean.getScheduledAction())
                .build();
    }
}
