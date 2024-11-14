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

import com.pinterest.teletraan.universal.security.AuthMetricsFactory.PrincipalType;
import com.pinterest.teletraan.universal.security.bean.Role;
import com.pinterest.teletraan.universal.security.bean.ScriptTokenPrincipal;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import java.util.Objects;
import java.util.Optional;

/**
 * ScriptTokenAuthenticator is an authenticator that authenticates a principal using a script token.
 */
public class ScriptTokenAuthenticator<R extends Role<R>>
        implements Authenticator<String, ScriptTokenPrincipal<R>> {

    private ScriptTokenProvider<R> tokenProvider;
    private final Counter.Builder scriptTokenSuccessCounterBuilder;
    private final Counter scriptTokenFailureCounter;

    public ScriptTokenAuthenticator(ScriptTokenProvider<R> tokenProvider) {
        this.tokenProvider = tokenProvider;
        scriptTokenSuccessCounterBuilder =
                AuthMetricsFactory.createAuthNCounterBuilder(
                        ScriptTokenAuthenticator.class, true, PrincipalType.SERVICE);
        scriptTokenFailureCounter =
                AuthMetricsFactory.createAuthNCounter(
                        ScriptTokenAuthenticator.class, false, PrincipalType.SERVICE);
    }

    @Override
    public Optional<ScriptTokenPrincipal<R>> authenticate(String credentials)
            throws AuthenticationException {
        try {
            Optional<ScriptTokenPrincipal<R>> principal = tokenProvider.getPrincipal(credentials);
            if (principal.isPresent()) {
                scriptTokenSuccessCounterBuilder
                        .tags(
                                "principal",
                                principal.get().getName(),
                                "env",
                                Objects.toString(principal.get().getResource().getEnvName(), "NA"))
                        .register(Metrics.globalRegistry)
                        .increment();
            } else {
                scriptTokenFailureCounter.increment();
            }
            return principal;
        } catch (Exception e) {
            scriptTokenFailureCounter.increment();
            throw new AuthenticationException(e);
        }
    }
}
