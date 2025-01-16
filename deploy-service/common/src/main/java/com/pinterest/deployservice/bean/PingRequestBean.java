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
import java.util.Set;
import javax.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class PingRequestBean {
    @NotEmpty private String hostId;

    private String hostName;

    private String hostIp;

    private String autoscalingGroup;

    private String availabilityZone;

    private String ec2Tags;

    private String agentVersion;

    private EnvType stageType;

    private Set<String> groups;

    private String accountId;

    private NormandieStatus normandieStatus;

    private KnoxStatus knoxStatus;

    private String processSingleEnvId;

    private List<PingReportBean> reports;

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
