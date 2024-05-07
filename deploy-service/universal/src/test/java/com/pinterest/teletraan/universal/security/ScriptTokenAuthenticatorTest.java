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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pinterest.teletraan.universal.security.bean.ScriptTokenPrincipal;
import com.pinterest.teletraan.universal.security.bean.ValueBasedRole;
import io.dropwizard.auth.AuthenticationException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScriptTokenAuthenticatorTest {
    private static final String PRINCIPAL_NAME = "testPrincipal";
    private static final String CREDENTIALS = "credentials";
    private ScriptTokenProvider<ValueBasedRole> scriptTokenProvider;
    private ScriptTokenPrincipal<ValueBasedRole> scriptTokenPrincipal;
    private ScriptTokenAuthenticator<ValueBasedRole> sut;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() throws Exception {
        scriptTokenProvider = mock(ScriptTokenProvider.class);
        scriptTokenPrincipal = mock(ScriptTokenPrincipal.class);
        when(scriptTokenProvider.getPrincipal(anyString())).thenReturn(Optional.empty());
        when(scriptTokenProvider.getPrincipal(CREDENTIALS))
                .thenReturn(Optional.of(scriptTokenPrincipal));
        when(scriptTokenPrincipal.getName()).thenReturn(PRINCIPAL_NAME);

        Metrics.globalRegistry.add(new SimpleMeterRegistry());
        sut = new ScriptTokenAuthenticator<>(scriptTokenProvider);
    }

    @AfterEach
    void tearDown() {
        Metrics.globalRegistry.clear();
    }

    @Test
    void testAuthenticate() throws Exception {
        Optional<ScriptTokenPrincipal<ValueBasedRole>> principal = sut.authenticate(CREDENTIALS);
        assertTrue(principal.isPresent());
        assertEquals(PRINCIPAL_NAME, principal.get().getName());
        Counter counter = assertCounterValue(true, 1.0);
        assertEquals(PRINCIPAL_NAME, counter.getId().getTag("principal"));
    }

    @Test
    void testAuthenticate_nonExistCredential() throws Exception {
        Optional<ScriptTokenPrincipal<ValueBasedRole>> principal =
                sut.authenticate("bad-credentials");
        assertFalse(principal.isPresent());
        assertCounterValue(false, 1.0);
    }

    @Test
    void testAuthenticateWithException() throws Exception {
        when(scriptTokenProvider.getPrincipal(anyString())).thenThrow(new RuntimeException());
        assertThrows(AuthenticationException.class, () -> sut.authenticate(CREDENTIALS));
        assertCounterValue(false, 1.0);
    }

    private Counter assertCounterValue(Boolean success, double expected) {
        Counter counter =
                Metrics.globalRegistry
                        .find("authn.ScriptTokenAuthenticator")
                        .tag(AuthMetricsFactory.SUCCESS, success.toString())
                        .tag(
                                AuthMetricsFactory.TYPE,
                                AuthMetricsFactory.PrincipalType.SERVICE.toString())
                        .counter();
        assertEquals(expected, counter.count());
        return counter;
    }
}
