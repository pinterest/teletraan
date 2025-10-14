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

import com.pinterest.deployservice.bean.rodimus.*;
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
    private AccountProvider provider; // AWS
    private String accountId; // 998131032990
    private List<AutoScalingAlarm> alarms;
    private String archName; // x86_64
    private Boolean autoRefresh; // true
    private List<AutoRefreshConfig> autoRefreshConfigs;
    private Boolean autoUpdateBaseImage; // false
    private String baseImageId; // ami-0819e5c9beb57ec20
    private String baseImageName; // cmp_base-ebs-18.04
    private Boolean useIdForBaseImageLookUp; // false
    private String cellName; // aws-us-east-1
    private String hostType; // m4.large
    private Integer maxCapacity; // 4
    private Integer minCapacity; // 2
    /*
    placement:
      - subnet-3f51dd65
      - subnet-46cbaf31
      - subnet-eeab3fc2
     */
    private List<String> placement;
    private Integer replacementTimeout; // 45
    private List<AutoScalingPolicy> scalingPolicies;
    private List<AutoScalingScheduledActions> scheduledActions;
    private String securityZone; // sg-10468758
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
