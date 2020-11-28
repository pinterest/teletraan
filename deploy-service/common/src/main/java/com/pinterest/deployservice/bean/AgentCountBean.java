/**
 * Copyright 2020 Pinterest, Inc.
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
 * <p/>
 * CREATE TABLE agent_count (
 * env_id   VARCHAR(22)    NOT NULL,
 * deploy_id VARCHAR(22)    NOT NULL,
 * existingCount    INT            NOT NULL DEFAULT 0,
 * activeCount    INT            NOT NULL DEFAULT 0,
 * PRIMARY KEY (env_id)
 * );
 */
public class AgentCountBean implements Updatable {
    @JsonProperty("envId")
    private String env_id;

    @JsonProperty("deployId")
    private String deploy_id;

    @JsonProperty("existingCount")
    private Long existing_count;

    @JsonProperty("activeCount")
    private Long active_count;

    public String getEnv_id() {
        return env_id;
    }

    public void setEnv_id(String env_id) {
        this.env_id = env_id;
    }

    public String getDeploy_id() {
        return deploy_id;
    }

    public void setDeploy_id(String deploy_id) {
        this.deploy_id = deploy_id;
    }

    public Long getExisting_count() {
        return existing_count;
    }

    public void setExisting_count(Long existing_count) {
        this.existing_count = existing_count;
    }

    public void setActive_count(Long active_count) {
        this.active_count = active_count;
    }

    public Long getActive_count() {
        return active_count;
    }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("env_id", env_id);
        clause.addColumn("deploy_id", deploy_id);
        clause.addColumn("existing_count", existing_count);
        clause.addColumn("active_count", active_count);
        return clause;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
