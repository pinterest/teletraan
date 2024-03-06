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

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * create table groups_and_roles (
 * group_name     VARCHAR(64)     NOT NULL,
 * resource_id   VARCHAR(64)     NOT NULL,
 * resource_type VARCHAR(16)     NOT NULL,
 * role          VARCHAR(22)     NOT NULL,
 * PRIMARY KEY (resource_id, resource_type, group_name)
 * );
 */
public class GroupRolesBean implements Updatable {

    @NotEmpty
    @JsonProperty("name")
    private String group_name;

    @JsonProperty("resource")
    private String resource_id;

    @JsonProperty("type")
    private Resource.Type resource_type;

    @NotNull
    @JsonProperty("role")
    private Role role;

    public String getGroup_name() {
        return group_name;
    }

    public void setGroup_name(String user_name) {
        this.group_name = user_name;
    }

    public String getResource_id() {
        return resource_id;
    }

    public void setResource_id(String resource_id) {
        this.resource_id = resource_id;
    }

    public Resource.Type getResource_type() {
        return resource_type;
    }

    public void setResource_type(Resource.Type resource_type) {
        this.resource_type = resource_type;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("group_name", group_name);
        clause.addColumn("resource_id", resource_id);
        clause.addColumn("resource_type", resource_type);
        clause.addColumn("role", role);
        return clause;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
