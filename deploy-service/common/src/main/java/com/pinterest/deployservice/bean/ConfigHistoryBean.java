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

public class ConfigHistoryBean extends BaseBean implements Updatable {
    @JsonProperty("id")
    private String config_id;

    @JsonProperty("changeId")
    private String change_id;

    @JsonProperty("createTime")
    private Long creation_time;

    @JsonProperty("operator")
    private String operator;

    @JsonProperty("type")
    private String type;

    @JsonProperty("configChange")
    private String config_change;

    public void setConfig_id(String config_id) {
        this.config_id = config_id;
    }

    public String getConfig_id() {
        return config_id;
    }

    public void setChange_id(String change_id) {
        this.change_id = change_id;
    }

    public String getChange_id() {
        return change_id;
    }

    public void setCreation_time(Long creation_time) {
        this.creation_time = creation_time;
    }

    public Long getCreation_time() {
        return creation_time;
    }

    public void setOperator(String operator) {
        this.operator = getStringWithSizeLimit(operator, 64);
    }

    public String getOperator() {
        return operator;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setConfig_change(String config_change) {
        this.config_change = config_change;
    }

    public String getConfig_change() {
        return config_change;
    }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("config_id", config_id);
        clause.addColumn("change_id", change_id);
        clause.addColumn("creation_time", creation_time);
        clause.addColumn("operator", operator);
        clause.addColumn("type", type);
        clause.addColumn("config_change", config_change);
        return clause;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
