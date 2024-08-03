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
import javax.annotation.Nonnull;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class TokenRolesBean implements Updatable {

    @NotEmpty
    @JsonProperty("name")
    private String script_name;

    @JsonProperty("resource")
    private String resource_id;

    @JsonProperty("type")
    private AuthZResource.Type resource_type;

    @JsonProperty("token")
    private String token;

    @NotNull
    @JsonProperty("role")
    private TeletraanPrincipalRole role;

    @JsonProperty("expireDate")
    private Long expire_date;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getScript_name() {
        return script_name;
    }

    public void setScript_name(String scriptName) {
        this.script_name = scriptName;
    }

    public @Nonnull String getResource_id() {
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

    public Long getExpire_date() {
        return expire_date;
    }

    public void setExpire_date(Long expireDate) {
        this.expire_date = expireDate;
    }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("script_name", script_name);
        clause.addColumn("resource_id", resource_id);
        clause.addColumn("resource_type", resource_type);
        clause.addColumn("token", token);
        clause.addColumn("role", role);
        clause.addColumn("expire_date", expire_date);
        return clause;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
