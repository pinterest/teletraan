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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.pinterest.commons.pastis.PastisAuthorizer;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import com.pinterest.teletraan.universal.security.bean.ServicePrincipal;
import com.pinterest.teletraan.universal.security.bean.UserPrincipal;
import com.sun.net.httpserver.HttpServer;
import com.twitter.finagle.stats.NullStatsReceiver;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.ws.rs.container.ContainerRequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BasePastisAuthorizerTest {
    private static final String PRINCIPAL_NAME = "testUser";
    private static final String SPIFFE_ID = "testSpiffe";
    private static final String GROUP_NAME = "testGroup";
    private static final String ACTION_READ = "READ";
    private static final String ACTION_WRITE = "WRITE";
    private static final String ACCOUNT_ID = "account123";
    private static final int PASTIS_TIMEOUT_MS = 5000;
    private static final String REQUEST_TEMPLATE =
            "{\"input\":{\"principal\":{\"id\":\"%s\",\"type\":\"%s\",\"groups\":[\"%s\"]},\"action\":\"%s\",\"resource\":{\"name\":\"%s\",\"type\":\"%s\"}}}";
    private static final String REQUEST_WITH_ATTR_TEMPLATE =
            "{\"input\":{\"principal\":{\"id\":\"%s\",\"type\":\"%s\",\"groups\":[\"%s\"]},\"action\":\"%s\",\"resource\":{\"name\":\"%s\",\"type\":\"%s\",\"accountId\":\"%s\",\"attributes\":{\"attr_key1\":\"attr_value1\"}}}}";
    private static final String SERVICE_REQUEST_TEMPLATE =
            "{\"input\":{\"principal\":{\"id\":\"%s\",\"type\":\"%s\"},\"action\":\"%s\",\"resource\":{\"name\":\"%s\",\"type\":\"%s\"}}}";
    private static final AuthZResource resource = new AuthZResource("env", "stage");
    private static final Map<String, String> attributes =
            ImmutableMap.of("attr_key1", "attr_value1");
    private static final AuthZResource resourceWithOptionalFields =
            new AuthZResource(
                    "resourceName", AuthZResource.Type.BASE_IMAGE, ACCOUNT_ID, attributes);

    private static ContainerRequestContext context;
    private static PastisAuthorizer pastis;
    private static AuthZResourceExtractor.Factory factory;

    private final AtomicInteger pastisStatus = new AtomicInteger();
    private final AtomicInteger pastisRequests = new AtomicInteger();
    private HttpServer pastisServer;
    private BasePastisAuthorizer sut;

    @BeforeEach
    public void setUp() {
        context = mock(ContainerRequestContext.class);
        pastis = mock(PastisAuthorizer.class);
        when(pastis.authorize(anyString())).thenReturn(true);
        factory = mock(AuthZResourceExtractor.Factory.class);
        sut = new BasePastisAuthorizer(pastis, factory);
    }

    @AfterEach
    public void tearDown() {
        if (pastisServer != null) {
            pastisServer.stop(0);
        }
    }

    private BasePastisAuthorizer authorizerWithPastisServer() throws IOException {
        pastisServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        pastisServer.createContext(
                "/",
                exchange -> {
                    pastisRequests.incrementAndGet();
                    exchange.sendResponseHeaders(pastisStatus.get(), -1);
                    exchange.close();
                });
        pastisServer.start();
        PastisAuthorizer realPastis =
                new PastisAuthorizer(
                        "teletraan",
                        "localhost:" + pastisServer.getAddress().getPort(),
                        false,
                        PastisAuthorizer.DEFAULT_CACHE_SIZE,
                        PASTIS_TIMEOUT_MS,
                        new NullStatsReceiver());
        return new BasePastisAuthorizer(realPastis, factory);
    }

    @ParameterizedTest
    @ValueSource(strings = {ACTION_READ, ACTION_WRITE})
    void testAuthorize_userPrincipal(String action) {
        UserPrincipal principal = new UserPrincipal(PRINCIPAL_NAME, Arrays.asList(GROUP_NAME));
        sut.authorize(principal, action, resource, context);
        verify(pastis)
                .authorize(
                        String.format(
                                REQUEST_TEMPLATE,
                                PRINCIPAL_NAME,
                                principal.getType(),
                                GROUP_NAME,
                                action,
                                resource.getName(),
                                resource.getType()));
    }

    @Test
    void testAuthorize_userPrincipal_failure() {
        UserPrincipal principal = new UserPrincipal(PRINCIPAL_NAME, Arrays.asList(GROUP_NAME));
        when(pastis.authorize(anyString())).thenThrow(new RuntimeException());
        assertFalse(sut.authorize(principal, ACTION_READ, resource, context));
    }

    @ParameterizedTest
    @ValueSource(strings = {ACTION_READ, ACTION_WRITE})
    void testAuthorize_servicePrincipal(String action) {
        ServicePrincipal principal = new ServicePrincipal(SPIFFE_ID);
        sut.authorize(principal, action, resource, context);
        verify(pastis)
                .authorize(
                        String.format(
                                SERVICE_REQUEST_TEMPLATE,
                                SPIFFE_ID,
                                principal.getType(),
                                action,
                                resource.getName(),
                                resource.getType()));
    }

    @Test
    void testBuilder() {
        assertThrows(NullPointerException.class, () -> BasePastisAuthorizer.builder().build());
        assertThrows(
                IllegalArgumentException.class,
                () -> BasePastisAuthorizer.builder().pastisTimeout(1).build());
        assertDoesNotThrow(
                () -> BasePastisAuthorizer.builder().pastisTimeout(1).serviceName("").build());
        assertDoesNotThrow(
                () -> BasePastisAuthorizer.builder().pastisTimeout(1).pastis(pastis).build());
    }

    @ParameterizedTest
    @ValueSource(strings = {ACTION_READ, ACTION_WRITE})
    void testAuthorize_payloadContainsOptionalFields(String action) {
        UserPrincipal principal = new UserPrincipal(PRINCIPAL_NAME, Arrays.asList(GROUP_NAME));
        sut.authorize(principal, action, resourceWithOptionalFields, context);
        verify(pastis)
                .authorize(
                        String.format(
                                REQUEST_WITH_ATTR_TEMPLATE,
                                PRINCIPAL_NAME,
                                principal.getType(),
                                GROUP_NAME,
                                action,
                                resourceWithOptionalFields.getName(),
                                resourceWithOptionalFields.getType(),
                                resourceWithOptionalFields.getAccountId()));
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 403})
    void testAuthorize_definitiveResultIsCached(int status) throws IOException {
        BasePastisAuthorizer authorizer = authorizerWithPastisServer();
        UserPrincipal principal = new UserPrincipal(PRINCIPAL_NAME, Arrays.asList(GROUP_NAME));
        pastisStatus.set(status);

        boolean first = authorizer.authorize(principal, ACTION_READ, resource, context);
        boolean second = authorizer.authorize(principal, ACTION_READ, resource, context);

        assertEquals(status == 200, first);
        assertEquals(first, second);
        assertEquals(1, pastisRequests.get());
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 503})
    void testAuthorize_errorResultIsNotCached(int status) throws IOException {
        BasePastisAuthorizer authorizer = authorizerWithPastisServer();
        UserPrincipal principal = new UserPrincipal(PRINCIPAL_NAME, Arrays.asList(GROUP_NAME));
        pastisStatus.set(status);

        assertFalse(authorizer.authorize(principal, ACTION_READ, resource, context));
        assertFalse(authorizer.authorize(principal, ACTION_READ, resource, context));
        assertEquals(2, pastisRequests.get());

        pastisStatus.set(200);
        assertTrue(authorizer.authorize(principal, ACTION_READ, resource, context));
    }

    @Test
    void testAuthorize_errorEvictionKeepsDefinitiveResults() throws IOException {
        BasePastisAuthorizer authorizer = authorizerWithPastisServer();
        UserPrincipal allowed = new UserPrincipal("allowedUser", Arrays.asList(GROUP_NAME));
        UserPrincipal denied = new UserPrincipal("deniedUser", Arrays.asList(GROUP_NAME));
        UserPrincipal failed = new UserPrincipal("failedUser", Arrays.asList(GROUP_NAME));

        pastisStatus.set(200);
        assertTrue(authorizer.authorize(allowed, ACTION_READ, resource, context));
        pastisStatus.set(403);
        assertFalse(authorizer.authorize(denied, ACTION_READ, resource, context));
        pastisStatus.set(500);
        assertFalse(authorizer.authorize(failed, ACTION_READ, resource, context));

        assertTrue(authorizer.authorize(allowed, ACTION_READ, resource, context));
        assertFalse(authorizer.authorize(denied, ACTION_READ, resource, context));
        assertEquals(3, pastisRequests.get());
    }
}
