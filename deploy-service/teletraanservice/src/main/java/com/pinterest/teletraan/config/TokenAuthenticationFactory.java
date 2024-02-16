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

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.cache.CacheBuilder;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.universal.security.EnvoyAuthFilter;
import com.pinterest.teletraan.universal.security.EnvoyPrincipal;
import com.pinterest.teletraan.universal.security.OAuthAuthenticator;
import com.pinterest.teletraan.universal.security.ScriptTokenAuthenticator;
import com.pinterest.teletraan.universal.security.ScriptTokenRoleAuthorizer;
import com.pinterest.teletraan.universal.security.ServicePrincipal;
import com.pinterest.teletraan.universal.security.UserPrincipal;
import com.pinterest.teletraan.universal.security.providers.MySqlScriptTokenProvider;

import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.CachingAuthenticator;
import io.dropwizard.auth.chained.ChainedAuthFilter;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.NotEmpty;
import javax.ws.rs.container.ContainerRequestFilter;

@JsonTypeName("token")
public class TokenAuthenticationFactory implements AuthenticationFactory {
    @JsonProperty
    @NotEmpty
    private String userDataUrl;

    @JsonProperty
    private String groupDataUrl;

    @JsonProperty
    private String userNameKey;

    @JsonProperty
    private Boolean extractUserNameFromEmail;

    @JsonProperty
    private String tokenCacheSpec;

    public String getUserDataUrl() {
        return userDataUrl;
    }

    public void setUserDataUrl(String userDataUrl) {
        this.userDataUrl = userDataUrl;
    }

    public String getUserNameKey() {
        return userNameKey;
    }

    public void setUserNameKey(String userNameKey) {
        this.userNameKey = userNameKey;
    }

    public Boolean getExtractUserNameFromEmail() {
        return extractUserNameFromEmail;
    }

    public void setExtractUserNameFromEmail(Boolean extractUserNameFromEmail) {
        this.extractUserNameFromEmail = extractUserNameFromEmail;
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

    @Override
    public ContainerRequestFilter create(TeletraanServiceContext context) throws Exception {
    LOG.info("Creating Token Authentication Factory");
    MetricRegistry registry = SharedMetricRegistries.getDefault();
    CacheBuilder<Object, Object> cacheBuilder =
        CacheBuilder.newBuilder().maximumSize(1000).expireAfterWrite(5, TimeUnit.MINUTES);

    CachingAuthenticator<String, ServicePrincipal> cachingScriptTokenAuthenticator =
        new CachingAuthenticator<>(
            registry,
            new ScriptTokenAuthenticator(new MySqlScriptTokenProvider(context.getDataSource())),
            cacheBuilder);
    AuthFilter<String, ServicePrincipal> scriptTokenAuthFilter =
        new OAuthCredentialAuthFilter.Builder<ServicePrincipal>()
            .setContextAuthorizer(new ScriptTokenRoleAuthorizer())
            .setAuthenticator(cachingScriptTokenAuthenticator)
            .setPrefix("token")
            .buildAuthFilter();

    CachingAuthenticator<String, UserPrincipal> cachingOAuthJwtAuthenticator =
        new CachingAuthenticator<>(
            registry, new OAuthAuthenticator(getUserDataUrl(), getGroupDataUrl()), cacheBuilder);
    AuthFilter<String, UserPrincipal> jwtTokenAuthFilter =
        new OAuthCredentialAuthFilter.Builder<UserPrincipal>()
            .setAuthorizer(context.getPastisAuthorizer())
            .setAuthenticator(cachingOAuthJwtAuthenticator)
            .setPrefix("token")
            .buildAuthFilter();

    // AuthFilter<String, EnvoyPrincipal> envoyAuthFilter =
    //     new EnvoyAuthFilter.Builder()
    //         .setContextAuthorizer(context.getPastisAuthorizer())
    //         .buildAuthFilter();

    List<AuthFilter<String, ? extends Principal>> filters =
        Arrays.asList(scriptTokenAuthFilter, envoyAuthFilter, jwtTokenAuthFilter);

    return new ChainedAuthFilter(filters);    }
}
