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
package com.pinterest.teletraan.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.security.TokenAuthFilter;
import org.hibernate.validator.constraints.NotEmpty;

import javax.ws.rs.container.ContainerRequestFilter;

@JsonTypeName("token")
public class TokenAuthenticationFactory implements AuthenticationFactory {
    @JsonProperty
    @NotEmpty
    private String userDataUrl;

    @JsonProperty
    private String oauthProvider;

    @JsonProperty
    private String groupDataUrl;

    @JsonProperty
    private String tokenCacheSpec;

    public String getUserDataUrl() {
        return userDataUrl;
    }

    public void setUserDataUrl(String userDataUrl) {
        this.userDataUrl = userDataUrl;
    }

    public String getTokenCacheSpec() {
        return tokenCacheSpec;
    }

    public void setTokenCacheSpec(String tokenCacheSpec) {
        this.tokenCacheSpec = tokenCacheSpec;
    }

    public String getGroupDataUrl() {
        return groupDataUrl;
    }

    public void setGroupDataUrl(String groupDataUrl) {
        this.groupDataUrl = groupDataUrl;
    }

    public String getOauthProvider() {
        return this.oauthProvider;
    }

    @Override
    public ContainerRequestFilter create(TeletraanServiceContext context) throws Exception {
        return new TokenAuthFilter(userDataUrl, groupDataUrl, tokenCacheSpec, oauthProvider, context);
    }
}
