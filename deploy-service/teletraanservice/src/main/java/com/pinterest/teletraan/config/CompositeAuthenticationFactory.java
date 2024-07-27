/**
 * Copyright (c) 2024 Pinterest, Inc.
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
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.universal.security.EnvoyAuthFilter;
import com.pinterest.teletraan.universal.security.EnvoyAuthenticator;
import com.pinterest.teletraan.universal.security.bean.EnvoyCredentials;
import com.pinterest.teletraan.universal.security.bean.TeletraanPrincipal;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.CachingAuthenticator;
import io.dropwizard.auth.JSONUnauthorizedHandler;
import io.dropwizard.auth.chained.ChainedAuthFilter;
import java.util.Arrays;
import javax.ws.rs.container.ContainerRequestFilter;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("composite")
public class CompositeAuthenticationFactory extends TokenAuthenticationFactory {
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public ContainerRequestFilter create(TeletraanServiceContext context) throws Exception {
        Authenticator<EnvoyCredentials, TeletraanPrincipal> authenticator =
                new EnvoyAuthenticator();

        if (StringUtils.isNotBlank(getTokenCacheSpec())) {
            MetricRegistry registry = SharedMetricRegistries.getDefault();
            Caffeine<Object, Object> cacheBuilder = Caffeine.from(getTokenCacheSpec());
            authenticator = new CachingAuthenticator<>(registry, authenticator, cacheBuilder);
        }

        AuthFilter<EnvoyCredentials, TeletraanPrincipal> envoyAuthFilter =
                new EnvoyAuthFilter.Builder<TeletraanPrincipal>()
                        .setAuthenticator(authenticator)
                        .setAuthorizer(context.getAuthorizationFactory().create(context))
                        .setUnauthorizedHandler(new JSONUnauthorizedHandler())
                        .buildAuthFilter();

        return new ChainedAuthFilter(
                Arrays.asList(
                        createScriptTokenAuthFilter(context),
                        envoyAuthFilter,
                        createOauthTokenAuthFilter(context),
                        createJwtTokenAuthFilter(context)));
    }
}
