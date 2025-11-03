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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ClusterInfoPublicIdsBean {
    private String region;
    private String archName;
    private Integer capacity;
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
    private String accountId;
    private Boolean isManagedResource;

    @Range(
            min = 5,
            max = 24 * 60,
            message = "Timeout must be between 5 minutes and 1440 minutes (24 hours)")
    @JsonProperty("replacementTimeout")
    private Long replacementTimeout;
}
