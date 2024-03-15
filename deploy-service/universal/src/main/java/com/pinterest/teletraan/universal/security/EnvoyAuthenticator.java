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

import com.pinterest.teletraan.universal.security.bean.EnvoyCredentials;
import com.pinterest.teletraan.universal.security.bean.ServicePrincipal;
import com.pinterest.teletraan.universal.security.bean.TeletraanPrincipal;
import com.pinterest.teletraan.universal.security.bean.UserPrincipal;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

/** An authenticator for Envoy credentials. */
public class EnvoyAuthenticator implements Authenticator<EnvoyCredentials, TeletraanPrincipal> {

    @Override
    public Optional<TeletraanPrincipal> authenticate(EnvoyCredentials credentials)
            throws AuthenticationException {
        if (StringUtils.isNotBlank(credentials.getUser())) {
            return Optional.of(new UserPrincipal(credentials.getUser(), credentials.getGroups()));
        }
        if (StringUtils.isNotBlank(credentials.getSpiffeId())) {
            return Optional.of(new ServicePrincipal(credentials.getSpiffeId()));
        }
        return Optional.empty();
    }
}
