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
package com.pinterest.teletraan.config;

import com.codahale.metrics.SharedMetricRegistries;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.security.TeletraanScriptTokenProvider;
import com.pinterest.teletraan.universal.security.OAuthAuthenticator;
import com.pinterest.teletraan.universal.security.ScriptTokenAuthenticator;
import com.pinterest.teletraan.universal.security.bean.ScriptTokenPrincipal;
import com.pinterest.teletraan.universal.security.bean.ServicePrincipal;
import com.pinterest.teletraan.universal.security.bean.UserPrincipal;
import com.pinterest.teletraan.universal.security.bean.ValueBasedRole;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.Authorizer;
import io.dropwizard.auth.CachingAuthenticator;
import io.dropwizard.auth.JSONUnauthorizedHandler;
import io.dropwizard.auth.chained.ChainedAuthFilter;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import java.util.Arrays;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.container.ContainerRequestFilter;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("token")
public class TokenAuthenticationFactory implements AuthenticationFactory {
    @JsonProperty @NotEmpty private String userDataUrl;

    @JsonProperty private String groupDataUrl;

    @JsonProperty private String userNameKey;

    @JsonProperty private Boolean extractUserNameFromEmail;

    @JsonProperty private String tokenCacheSpec;

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

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public ContainerRequestFilter create(TeletraanServiceContext context) throws Exception {
        return new ChainedAuthFilter(
                Arrays.asList(
                        createScriptTokenAuthFilter(context),
                        createOauthTokenAuthFilter(context),
                        createJwtTokenAuthFilter(context)));
    }

    @SuppressWarnings({"unchecked"})
    AuthFilter<String, ScriptTokenPrincipal<ValueBasedRole>> createScriptTokenAuthFilter(
            TeletraanServiceContext context) throws Exception {
        Authenticator<String, ScriptTokenPrincipal<ValueBasedRole>> scriptTokenAuthenticator =
                new ScriptTokenAuthenticator<>(new TeletraanScriptTokenProvider(context));
        if (StringUtils.isNotBlank(getTokenCacheSpec())) {
            scriptTokenAuthenticator =
                    new CachingAuthenticator<>(
                            SharedMetricRegistries.getDefault(),
                            scriptTokenAuthenticator,
                            Caffeine.from(getTokenCacheSpec()));
        }
        return new OAuthCredentialAuthFilter.Builder<ScriptTokenPrincipal<ValueBasedRole>>()
                .setAuthenticator(scriptTokenAuthenticator)
                .setAuthorizer(
                        (Authorizer<ScriptTokenPrincipal<ValueBasedRole>>)
                                context.getAuthorizationFactory()
                                        .create(context, ServicePrincipal.class))
                .setPrefix("token")
                .setUnauthorizedHandler(new JSONUnauthorizedHandler())
                .buildAuthFilter();
    }

    // TODO: CDP-7837 remove this after all the clients are updated to use the new token scheme
    @SuppressWarnings({"unchecked"})
    AuthFilter<String, UserPrincipal> createOauthTokenAuthFilter(TeletraanServiceContext context)
            throws Exception {
        Authenticator<String, UserPrincipal> oauthAuthenticator =
                new OAuthAuthenticator(getUserDataUrl(), getGroupDataUrl());
        if (StringUtils.isNotBlank(getTokenCacheSpec())) {
            oauthAuthenticator =
                    new CachingAuthenticator<>(
                            SharedMetricRegistries.getDefault(),
                            oauthAuthenticator,
                            Caffeine.from(getTokenCacheSpec()));
        }
        return new OAuthCredentialAuthFilter.Builder<UserPrincipal>()
                .setAuthenticator(oauthAuthenticator)
                .setAuthorizer(
                        (Authorizer<UserPrincipal>)
                                context.getAuthorizationFactory()
                                        .create(context, UserPrincipal.class))
                .setPrefix("token")
                .setUnauthorizedHandler(new JSONUnauthorizedHandler())
                .buildAuthFilter();
    }

    @SuppressWarnings({"unchecked"})
    AuthFilter<String, UserPrincipal> createJwtTokenAuthFilter(TeletraanServiceContext context)
            throws Exception {
        Authenticator<String, UserPrincipal> oauthJwtAuthenticator =
                new OAuthAuthenticator(getUserDataUrl(), getGroupDataUrl());
        if (StringUtils.isNotBlank(getTokenCacheSpec())) {
            oauthJwtAuthenticator =
                    new CachingAuthenticator<>(
                            SharedMetricRegistries.getDefault(),
                            oauthJwtAuthenticator,
                            Caffeine.from(getTokenCacheSpec()));
        }
        return new OAuthCredentialAuthFilter.Builder<UserPrincipal>()
                .setAuthenticator(oauthJwtAuthenticator)
                .setAuthorizer(
                        (Authorizer<UserPrincipal>)
                                context.getAuthorizationFactory()
                                        .create(context, UserPrincipal.class))
                .setPrefix("Bearer")
                .setUnauthorizedHandler(new JSONUnauthorizedHandler())
                .buildAuthFilter();
    }
}
