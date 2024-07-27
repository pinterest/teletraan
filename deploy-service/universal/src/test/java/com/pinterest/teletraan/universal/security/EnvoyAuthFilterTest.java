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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pinterest.teletraan.universal.security.bean.EnvoyCredentials;
import com.pinterest.teletraan.universal.security.bean.TeletraanPrincipal;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EnvoyAuthFilterTest {
    private static final String CERT_HEADER =
            "C=US\";URI=spiffe://pin220.com/k8s/jupyter/fgac-test/username/testuser;DNS=fa2a9e01-dc86-477a-9661-d6ca997556ec.k8s.pin220.com";
    private static final String SPIFFE_ID =
            "spiffe://pin220.com/k8s/jupyter/fgac-test/username/testuser";

    private ContainerRequestContext requestContext;
    private MultivaluedMap<String, String> headers;
    private Authenticator<EnvoyCredentials, TeletraanPrincipal> authenticator;

    @BeforeEach
    void prepareFilterTest() throws IOException, AuthenticationException {
        requestContext = mock(ContainerRequestContext.class);
        headers = new MultivaluedHashMap<>();
        authenticator = new EnvoyAuthenticator();
        when(requestContext.getHeaders()).thenReturn(headers);
    }

    @Test
    void getSpiffeId_null() {
        String spiffeId = EnvoyAuthFilter.getSpiffeId(null);
        assertNull(spiffeId);
    }

    @Test
    void getSpiffeId_valid() {
        String spiffeId = EnvoyAuthFilter.getSpiffeId(CERT_HEADER);
        assertEquals(SPIFFE_ID, spiffeId);
    }

    @Test
    void getSpiffeId_invalid() {
        String spiffeId = EnvoyAuthFilter.getSpiffeId("random stuff");
        assertNull(spiffeId);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "group1 group2   group3",
                "group1,group2,,group3",
                "group1, group2,,   group3",
                " group1,group2,group3 "
            })
    void getGroups_space(String groups) {
        List<String> groupsList = EnvoyAuthFilter.getGroups(groups);
        assertNotNull(groupsList);
        assertEquals(3, groupsList.size());
    }

    @Test
    void getGroups_null() {
        List<String> groupsList = EnvoyAuthFilter.getGroups(null);
        assertNull(groupsList);
    }

    @Test
    void testFilter_user() {
        headers.put(Constants.USER_HEADER, Collections.singletonList("user"));
        headers.put(Constants.GROUPS_HEADER, Collections.singletonList("group1 group2 group3"));

        doFilterTest();

        verify(requestContext).setSecurityContext(any(SecurityContext.class));
    }

    @Test
    void testFilter_group() {
        headers.put(Constants.CLIENT_CERT_HEADER, Collections.singletonList(CERT_HEADER));

        doFilterTest();

        verify(requestContext).setSecurityContext(any(SecurityContext.class));
    }

    @Test
    void testFilter_failure() {
        AuthFilter<EnvoyCredentials, TeletraanPrincipal> sut =
                new EnvoyAuthFilter.Builder<TeletraanPrincipal>()
                        .setAuthenticator(authenticator)
                        .buildAuthFilter();
        assertThrows(RuntimeException.class, () -> sut.filter(requestContext));
        verify(requestContext, never()).setSecurityContext(any(SecurityContext.class));
    }

    private void doFilterTest() {
        AuthFilter<EnvoyCredentials, TeletraanPrincipal> sut =
                new EnvoyAuthFilter.Builder<TeletraanPrincipal>()
                        .setAuthenticator(authenticator)
                        .buildAuthFilter();

        assertDoesNotThrow(() -> sut.filter(requestContext));
    }
}
