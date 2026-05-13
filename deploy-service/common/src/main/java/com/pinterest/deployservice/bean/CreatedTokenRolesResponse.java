/**
 * Copyright (c) 2016-2026 Pinterest, Inc.
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

/**
 * Response payload for the script-token creation endpoint.
 *
 * <p>This DTO is the <em>only</em> HTTP response shape that includes the raw bearer {@code token}
 * value. The on-disk {@link TokenRolesBean} marks {@code token} as write-only so that GET endpoints
 * never expose it (see BUG-285640). The newly minted token is disclosed exactly once, in the 201
 * Created response from {@code POST /v1/envs/{env}/token_roles}, to the WRITE-role caller that
 * created it. Callers must persist the token at that moment; it cannot be retrieved afterwards.
 */
public class CreatedTokenRolesResponse {

    @JsonProperty("name")
    private final String scriptName;

    @JsonProperty("resource")
    private final String resourceId;

    @JsonProperty("type")
    private final AuthZResource.Type resourceType;

    @JsonProperty("token")
    private final String token;

    @JsonProperty("role")
    private final TeletraanPrincipalRole role;

    @JsonProperty("expireDate")
    private final Long expireDate;

    public CreatedTokenRolesResponse(TokenRolesBean bean, String rawToken) {
        this.scriptName = bean.getScript_name();
        this.resourceId = bean.getResource_id();
        this.resourceType = bean.getResource_type();
        this.token = rawToken;
        this.role = bean.getRole();
        this.expireDate = bean.getExpire_date();
    }

    public String getScriptName() {
        return scriptName;
    }

    public String getResourceId() {
        return resourceId;
    }

    public AuthZResource.Type getResourceType() {
        return resourceType;
    }

    public String getToken() {
        return token;
    }

    public TeletraanPrincipalRole getRole() {
        return role;
    }

    public Long getExpireDate() {
        return expireDate;
    }
}
