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
import io.dropwizard.auth.AuthFilter;
import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Priority(Priorities.AUTHENTICATION)
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class EnvoyAuthFilter<P extends Principal> extends AuthFilter<EnvoyCredentials, P> {
    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        EnvoyCredentials credentials = getCredentials(requestContext);

        if (credentials != null) {
            String scheme =
                    StringUtils.isNotBlank(credentials.getSpiffeId())
                            ? SecurityContext.CLIENT_CERT_AUTH
                            : SecurityContext.BASIC_AUTH;
            if (authenticate(requestContext, credentials, scheme)) {
                return;
            }
        }
        throw unauthorizedHandler.buildException(prefix, realm);
    }

    /**
     * Get the Envoy credentials from the request headers.
     *
     * @param requestContext the context of the request
     * @return an instance of {@link EnvoyCredentials} if the request is authenticated, otherwise
     *     {@code null}
     */
    @Nullable
    private EnvoyCredentials getCredentials(ContainerRequestContext requestContext) {
        String user = requestContext.getHeaders().getFirst(Constants.USER_HEADER);
        String spiffeId =
                getSpiffeId(requestContext.getHeaders().getFirst(Constants.CLIENT_CERT_HEADER));
        List<String> groups =
                getGroups(requestContext.getHeaders().getFirst(Constants.GROUPS_HEADER));

        if (StringUtils.isBlank(spiffeId) && StringUtils.isBlank(user)) {
            return null;
        }

        return new EnvoyCredentials(user, spiffeId, groups);
    }

    /**
     * Builder for {@link EnvoyAuthFilter}.
     *
     * <p>An {@link Authenticator} must be provided during the building process.
     *
     * @param <P> the type of the principal
     */
    public static class Builder<P extends Principal>
            extends AuthFilterBuilder<EnvoyCredentials, P, EnvoyAuthFilter<P>> {

        @Override
        protected EnvoyAuthFilter<P> newInstance() {
            return new EnvoyAuthFilter<>();
        }
    }

    /**
     * Parses the raw value of a spiffe request header
     *
     * @return spiffe id
     */
    @Nullable
    protected static String getSpiffeId(String value) {
        if (value == null) {
            return null;
        }
        String[] headerValues = value.split(",");
        String lastHeaderValue = headerValues[headerValues.length - 1];
        String[] pairs = lastHeaderValue.split(";");
        for (String pair : pairs) {
            String[] pairKeyAndValue = pair.split("=", 2);
            if (pairKeyAndValue[0].equals("URI")) {
                return pairKeyAndValue[1];
            }
        }
        return null;
    }

    @Nullable
    protected static List<String> getGroups(String header) {
        if (header == null) {
            return null;
        }
        return Arrays.asList(header.trim().split("[\\s,]+"));
    }
}
