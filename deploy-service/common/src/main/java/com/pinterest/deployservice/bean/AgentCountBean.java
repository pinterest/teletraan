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
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class AgentCountBean implements Updatable {
    @JsonProperty("envId")
    private String env_id;

    @JsonProperty("deployId")
    private String deploy_id;

    @JsonProperty("existingCount")
    private Long existing_count;

    @JsonProperty("activeCount")
    private Long active_count;

    @JsonProperty("lastRefresh")
    private Long last_refresh;

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

    public Long getLast_refresh() {
        return last_refresh;
    }

    public void setLast_refresh(Long last_refresh) {
        this.last_refresh = last_refresh;
    }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("env_id", env_id);
        clause.addColumn("deploy_id", deploy_id);
        clause.addColumn("existing_count", existing_count);
        clause.addColumn("active_count", active_count);
        clause.addColumn("last_refresh", last_refresh);
        return clause;
    }

    public static final String UPDATE_CLAUSE =
            "env_id=VALUES(env_id),"
                    + "deploy_id=VALUES(deploy_id),"
                    + "existing_count=VALUES(existing_count),"
                    + "active_count=VALUES(active_count),"
                    + "last_refresh=CASE WHEN last_refresh IS NOT NULL THEN VALUES(last_refresh) ELSE last_refresh END";

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
