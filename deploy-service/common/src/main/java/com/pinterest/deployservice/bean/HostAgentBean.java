/**
 * Copyright 2016 Pinterest, Inc.
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
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

/**
 * Keep the bean and table in sync
 * <p>
 * CREATE TABLE hosts_and_agents (
 * host_id      VARCHAR(64),
 * agent_version   VARCHAR(64),
 * PRIMARY KEY    (host_id)
 * );
 */
public class HostAgentBean implements Updatable {
    @JsonProperty("hostId")
    private String host_id;

    @JsonProperty("agentVersion")
    private String agent_version;

    public String getHost_Id() {
        return host_id;
    }

    public void setHost_Id(String host_id) {
        this.host_id = host_id;
    }

    public String getAgent_Version() {
        return agent_version;
    }

    public void setAgent_Version(String agent_version) {
        this.agent_version = agent_version;
    }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("host_id", host_id);
        clause.addColumn("agent_version", agent_version);
        return clause;
    }

    public final static String UPDATE_CLAUSE =
        "host_id=VALUES(host_id)," +
        "agent_version=VALUES(agent_version)";

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
