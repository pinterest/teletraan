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
package com.pinterest.teletraan.universal.security;

import com.google.common.collect.ImmutableList;
import com.pinterest.teletraan.universal.security.AuthMetricsFactory.PrincipalType;
import com.pinterest.teletraan.universal.security.bean.EnvoyCredentials;
import com.pinterest.teletraan.universal.security.bean.ServicePrincipal;
import com.pinterest.teletraan.universal.security.bean.TeletraanPrincipal;
import com.pinterest.teletraan.universal.security.bean.UserPrincipal;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.micrometer.core.instrument.Counter;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/** An authenticator for Envoy credentials. */
@NoArgsConstructor
@AllArgsConstructor
public class EnvoyAuthenticator implements Authenticator<EnvoyCredentials, TeletraanPrincipal> {
    private final Counter envoyAuthUserSuccessCounter =
            AuthMetricsFactory.createAuthNCounter(
                    EnvoyAuthenticator.class, true, PrincipalType.USER);
    private final Counter envoyAuthServiceSuccessCounter =
            AuthMetricsFactory.createAuthNCounter(
                    EnvoyAuthenticator.class, true, PrincipalType.SERVICE);
    private final Counter envoyAuthFailureCounter =
            AuthMetricsFactory.createAuthNCounter(
                    EnvoyAuthenticator.class, false, PrincipalType.NA);
    /**
     * List of principal replacers to be applied to the authenticated principal.
     *
     * <p>The default value is an empty list. Note that the order of the replacers matters.
     */
    private ImmutableList<PrincipalReplacer> principalReplacers = ImmutableList.of();

    @Override
    public Optional<TeletraanPrincipal> authenticate(EnvoyCredentials credentials)
            throws AuthenticationException {
        TeletraanPrincipal principal = null;
        if (StringUtils.isNotBlank(credentials.getUser())) {
            envoyAuthUserSuccessCounter.increment();
            principal = new UserPrincipal(credentials.getUser(), credentials.getGroups());
        } else if (StringUtils.isNotBlank(credentials.getSpiffeId())) {
            envoyAuthServiceSuccessCounter.increment();
            principal = new ServicePrincipal(credentials.getSpiffeId());
        } else {
            envoyAuthFailureCounter.increment();
        }

        for (PrincipalReplacer replacer : principalReplacers) {
            principal = replacer.replace(principal, credentials);
        }

        return Optional.ofNullable(principal);
    }
}
