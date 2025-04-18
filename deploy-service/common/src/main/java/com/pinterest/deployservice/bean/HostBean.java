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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class HostBean implements Updatable {
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

    @JsonIgnore
    public Boolean isPendingTerminate() {
        return this.state == HostState.PENDING_TERMINATE
                || this.state == HostState.PENDING_TERMINATE_NO_REPLACE;
    }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("host_name", host_name);
        clause.addColumn("group_name", group_name);
        clause.addColumn("ip", ip);
        clause.addColumn("host_id", host_id);
        clause.addColumn("create_date", create_date);
        clause.addColumn("last_update", last_update);
        clause.addColumn("state", state);
        clause.addColumn("can_retire", can_retire);
        clause.addColumn("account_id", account_id);
        return clause;
    }

    public static final String UPDATE_CLAUSE =
            "host_name=VALUES(host_name),"
                    + "group_name=VALUES(group_name),"
                    + "ip=VALUES(ip),"
                    + "host_id=VALUES(host_id),"
                    + "create_date=VALUES(create_date),"
                    + "last_update=VALUES(last_update),"
                    + "state=VALUES(state),"
                    + "can_retire=VALUES(can_retire)";

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
