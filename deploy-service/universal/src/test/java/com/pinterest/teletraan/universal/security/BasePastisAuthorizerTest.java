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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pinterest.commons.pastis.PastisAuthorizer;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import com.pinterest.teletraan.universal.security.bean.ServicePrincipal;
import com.pinterest.teletraan.universal.security.bean.UserPrincipal;
import java.io.IOException;
import java.util.Arrays;
import javax.ws.rs.container.ContainerRequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BasePastisAuthorizerTest {
    private static final String principalName = "testUser";
    private static final String spiffeId = "testSpiffe";
    private static final String group = "testGroup";
    private static final String actionRead = "READ";
    private static final String actionWrite = "WRITE";
    private static final String requestTemplate =
            "{\"input\":{\"principal\":{\"id\":\"%s\",\"type\":\"%s\",\"groups\":[\"%s\"]},\"action\":\"%s\",\"resource\":{\"name\":\"%s\",\"type\":\"%s\"}}}";
    private static final String serviceRequestTemplate =
            "{\"input\":{\"principal\":{\"id\":\"%s\",\"type\":\"%s\"},\"action\":\"%s\",\"resource\":{\"name\":\"%s\",\"type\":\"%s\"}}}";
    private static final AuthZResource resource = new AuthZResource("env", "stage");

    private static ContainerRequestContext context;
    private static PastisAuthorizer pastis;
    private static AuthZResourceExtractor.Factory factory;

    @BeforeEach
    public void setUp() {
        context = mock(ContainerRequestContext.class);
        pastis = mock(PastisAuthorizer.class);
        factory = mock(AuthZResourceExtractor.Factory.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {actionRead, actionWrite})
    void testAuthorize_userPrincipal(String action) throws IOException {
        BasePastisAuthorizer<UserPrincipal> sut = new BasePastisAuthorizer<>(pastis, factory);
        UserPrincipal principal = new UserPrincipal(principalName, Arrays.asList(group));
        sut.authorize(principal, action, resource, context);
        verify(pastis)
                .authorize(
                        String.format(
                                requestTemplate,
                                principalName,
                                principal.getType(),
                                group,
                                action,
                                resource.getName(),
                                resource.getType()));
    }

    @Test
    void testAuthorize_userPrincipal_failure() {
        BasePastisAuthorizer<UserPrincipal> sut = new BasePastisAuthorizer<>(pastis, factory);
        UserPrincipal principal = new UserPrincipal(principalName, Arrays.asList(group));
        when(pastis.authorize(anyString())).thenThrow(new RuntimeException());
        assertFalse(sut.authorize(principal, actionRead, resource, context));
    }

    @ParameterizedTest
    @ValueSource(strings = {actionRead, actionWrite})
    void testAuthorize_servicePrincipal(String action) throws IOException {
        BasePastisAuthorizer<ServicePrincipal> sut = new BasePastisAuthorizer<>(pastis, factory);
        ServicePrincipal principal = new ServicePrincipal(spiffeId);
        sut.authorize(principal, action, resource, context);
        verify(pastis)
                .authorize(
                        String.format(
                                serviceRequestTemplate,
                                spiffeId,
                                principal.getType(),
                                action,
                                resource.getName(),
                                resource.getType()));
    }

    @Test
    void testBuilder() {
        assertThrows(IllegalArgumentException.class, () -> BasePastisAuthorizer.builder().build());
        assertDoesNotThrow(() -> BasePastisAuthorizer.builder().serviceName("").build());
        assertDoesNotThrow(() -> BasePastisAuthorizer.builder().pastis(pastis).build());
    }
}
