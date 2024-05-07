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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pinterest.teletraan.universal.security.AuthMetricsFactory.PrincipalType;
import com.pinterest.teletraan.universal.security.bean.EnvoyCredentials;
import com.pinterest.teletraan.universal.security.bean.ServicePrincipal;
import com.pinterest.teletraan.universal.security.bean.TeletraanPrincipal;
import com.pinterest.teletraan.universal.security.bean.UserPrincipal;
import io.dropwizard.auth.AuthenticationException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EnvoyAuthenticatorTest {
    private static final String SPIFFE_ID = "testSpiffeId";
    private static final String USER_NAME = "testUser";

    private EnvoyAuthenticator authenticator;
    private String COUNTER_NAME = "authn.EnvoyAuthenticator";

    @BeforeEach
    public void setUp() {
        Metrics.globalRegistry.add(new SimpleMeterRegistry());
        authenticator = new EnvoyAuthenticator();
    }

    @AfterEach
    public void tearDown() {
        Metrics.globalRegistry.clear();;
    }

    @Test
    void testAuthenticate_withUserCredentials() throws AuthenticationException {
        EnvoyCredentials credentials = new EnvoyCredentials(USER_NAME, null, null);
        Optional<TeletraanPrincipal> result = authenticator.authenticate(credentials);

        assertTrue(result.isPresent());
        assertTrue(result.get() instanceof UserPrincipal);

        UserPrincipal userPrincipal = (UserPrincipal) result.get();
        assertEquals(USER_NAME, userPrincipal.getName());
        assertEquals(null, userPrincipal.getGroups());

        assertCounterValue(true, 1.0, PrincipalType.USER);
    }

    @Test
    void testAuthenticate_withServiceCredentials() throws AuthenticationException {
        EnvoyCredentials credentials = new EnvoyCredentials(null, SPIFFE_ID, null);
        Optional<TeletraanPrincipal> result = authenticator.authenticate(credentials);

        assertTrue(result.isPresent());
        assertTrue(result.get() instanceof ServicePrincipal);

        ServicePrincipal servicePrincipal = (ServicePrincipal) result.get();
        assertEquals(SPIFFE_ID, servicePrincipal.getName());

        assertCounterValue(true, 1.0, PrincipalType.SERVICE);
    }

    @Test
    void testAuthenticate_withEmptyCredentials() throws AuthenticationException {
        EnvoyCredentials credentials = new EnvoyCredentials(null, null, null);
        Optional<TeletraanPrincipal> result = authenticator.authenticate(credentials);
        assertFalse(result.isPresent());

        assertCounterValue(false, 1.0, PrincipalType.NA);
    }

    private void assertCounterValue(Boolean success, double expected, PrincipalType type) {
        Counter counter =
                Metrics.globalRegistry
                        .find(COUNTER_NAME)
                        .tag(AuthMetricsFactory.SUCCESS, success.toString())
                        .tag(AuthMetricsFactory.TYPE, type.toString())
                        .counter();
        assertEquals(expected, counter.count());
    }
}
