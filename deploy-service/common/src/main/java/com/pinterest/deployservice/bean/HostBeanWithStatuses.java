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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class HostBeanWithStatuses {
    @JsonProperty("hostName")
    private String host_name;

    @JsonProperty("groupName")
    private String group_name;

    @JsonProperty("ip")
    private String ip;

    @JsonProperty("hostId")
    private String host_id;

    @JsonProperty("accountId")
    private String account_id;

    @JsonProperty("createDate")
    private Long create_date;

    @JsonProperty("lastUpdateDate")
    private Long last_update;

    @JsonProperty("state")
    private HostState state;

    @JsonProperty("canRetire")
    private Integer can_retire;
    // canRetire used to be a Boolean field
    // canRetire now represent the state `HostCanRetireType'
    // which can be:
    // NEW(0) = the default value upon a host is launched, means cannot retire
    // TO_BE_REPLACED(1) = marked by cluster replace event to be replaced
    // HEALTH_CHECK(2) = host only launch for health check purpose and not to be replaced/retired

    // Normandie and Knox Statuses are NOT in the hosts table, but instead on the hosts_and_agents table
    @JsonProperty("normandieStatus")
    private NormandieStatus normandie_status;

    @JsonProperty("knoxStatus")
    private KnoxStatus knox_status;

}
