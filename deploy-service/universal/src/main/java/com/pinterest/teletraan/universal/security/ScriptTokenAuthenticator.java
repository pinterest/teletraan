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

import com.pinterest.teletraan.universal.security.bean.Role;
import com.pinterest.teletraan.universal.security.bean.ScriptTokenPrincipal;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * ScriptTokenAuthenticator is an authenticator that authenticates a principal using a script token.
 */
@Slf4j
public class ScriptTokenAuthenticator<R extends Role<R>>
        implements Authenticator<String, ScriptTokenPrincipal<R>> {

    private ScriptTokenProvider<R> tokenProvider;

    public ScriptTokenAuthenticator(ScriptTokenProvider<R> tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public Optional<ScriptTokenPrincipal<R>> authenticate(String credentials)
            throws AuthenticationException {
        log.debug("Authenticating...");
        try {
            return tokenProvider.getPrincipal(credentials);
        } catch (Exception e) {
            throw new AuthenticationException(e);
        }
    }
}
