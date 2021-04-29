/**
 * Copyright 2021 Pinterest, Inc.
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
 * CREATE TABLE host_and_agents (
 * host_id         VARCHAR(64)         NOT NULL,
 * host_name       VARCHAR(64),
 * ip              VARCHAR(64),
 * create_date     BIGINT              NOT NULL,
 * last_update     BIGINT              NOT NULL,
 * agent_version   VARCHAR(64),
 * auto_scaling_group VARCHAR(128)     DEFAULT NULL,
 * PRIMARY KEY    host_id
 * );
 */
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

    public String getHost_name() {
        return host_name;
    }

    public void setHost_name(String host_name) {
        this.host_name = host_name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getHost_id() {
        return host_id;
    }

    public void setHost_id(String host_id) {
        this.host_id = host_id;
    }

    public Long getCreate_date() {
        return create_date;
    }

    public void setCreate_date(Long create_date) {
        this.create_date = create_date;
    }

    public Long getLast_update() {
        return last_update;
    }

    public void setLast_update(Long last_update) {
        this.last_update = last_update;
    }

    public String getAgent_Version() {
        return agent_version;
    }

    public void setAgent_Version(String agent_version) {
        this.agent_version = agent_version;
    }

    public String getAuto_scaling_group() {
        return this.auto_scaling_group;
    }

    public void setAuto_scaling_group(String asg) {
        this.auto_scaling_group = asg;
    }

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
        return clause;
    }

    public final static String UPDATE_CLAUSE =
        "host_name=VALUES(host_name)," +
        "ip=VALUES(ip)," +
        "host_id=VALUES(host_id)," +
        "last_update=VALUES(last_update)," +
        "agent_version=VALUES(agent_version)," +
        "auto_scaling_group=VALUES(auto_scaling_group)";

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
