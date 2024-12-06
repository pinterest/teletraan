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
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import lombok.Data;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class HostAgentBean implements Updatable {
    @JsonProperty("hostName")
    private String host_name;

    @JsonProperty("ip")
    private String ip;

    @JsonProperty("hostId")
    private String host_id;

    @JsonProperty("createDate")
    private Long create_date;

    @JsonProperty("lastUpdateDate")
    private Long last_update;

    @JsonProperty("agentVersion")
    private String agent_version;

    @JsonProperty("autoScalingGroup")
    private String auto_scaling_group;

    @JsonProperty("normandieStatus")
    private NormandieStatus normandie_status;

    @JsonProperty("knoxStatus")
    private KnoxStatus knox_status;

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("host_name", host_name);
        clause.addColumn("ip", ip);
        clause.addColumn("host_id", host_id);
        clause.addColumn("create_date", create_date);
        clause.addColumn("last_update", last_update);
        clause.addColumn("agent_version", agent_version);
        clause.addColumn("auto_scaling_group", auto_scaling_group);
        clause.addColumn("normandie_status", normandie_status);
        clause.addColumn("knox_status", knox_status);
        return clause;
    }

    public static final String UPDATE_CLAUSE =
            "host_name=VALUES(host_name),"
                    + "ip=VALUES(ip),"
                    + "host_id=VALUES(host_id),"
                    + "last_update=VALUES(last_update),"
                    + "agent_version=VALUES(agent_version),"
                    + "auto_scaling_group=VALUES(auto_scaling_group),"
                    + "normandie_status=VALUES(normandie_status),"
                    + "knox_status=VALUES(knox_status)";

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
