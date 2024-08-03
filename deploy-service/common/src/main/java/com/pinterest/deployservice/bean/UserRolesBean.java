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
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class UserRolesBean implements Updatable {

    @NotEmpty
    @JsonProperty("name")
    private String user_name;

    @JsonProperty("resource")
    private String resource_id;

    @JsonProperty("type")
    private AuthZResource.Type resource_type;

    @NotNull
    @JsonProperty("role")
    private TeletraanPrincipalRole role;

    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String userName) {
        this.user_name = userName;
    }

    public String getResource_id() {
        return resource_id;
    }

    public void setResource_id(String resourceId) {
        this.resource_id = resourceId;
    }

    public AuthZResource.Type getResource_type() {
        return resource_type;
    }

    public void setResource_type(AuthZResource.Type resourceType) {
        this.resource_type = resourceType;
    }

    public TeletraanPrincipalRole getRole() {
        return role;
    }

    public void setRole(TeletraanPrincipalRole role) {
        this.role = role;
    }

    public static final String UPDATE_CLAUSE =
            "user_name=VALUES(user_name),"
                    + "resource_id=VALUES(resource_id),"
                    + "resource_type=VALUES(resource_type),"
                    + "role=VALUES(role)";

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("user_name", user_name);
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
