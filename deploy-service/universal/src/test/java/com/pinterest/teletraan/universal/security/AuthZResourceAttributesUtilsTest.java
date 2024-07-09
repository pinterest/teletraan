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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.container.ContainerRequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthZResourceAttributesUtilsTest {
    private static final String EXISTING_VALUE = "existingValue";
    private static final String EXISTING_KEY = "existingKey";
    private static final String NEW_VALUE = "value";
    private ContainerRequestContext requestContext;
    private Map<String, String> attributesMap;

    @BeforeEach
    void setUp() {
        attributesMap = new HashMap<>();
        attributesMap.put(EXISTING_KEY, EXISTING_VALUE);
        requestContext = mock(ContainerRequestContext.class);
    }

    @Test
    void insertAuthZResourceAttributes_nullRequestContext_returnsEmptyMap() {
        Map<String, String> result =
                AuthZResourceAttributesUtils.insertAuthZResourceAttributes(
                        null, AuthZResource.AttributeKeys.ENV_STAGE_IS_SOX, NEW_VALUE);
        assertTrue(result.isEmpty());
    }

    @Test
    void insertAuthZResourceAttributes_existingAttributesMap_insertsKeyValue() {
        when(requestContext.getProperty(Constants.AUTHZ_ATTR_REQ_CXT_KEY))
                .thenReturn(attributesMap);

        Map<String, String> result =
                AuthZResourceAttributesUtils.insertAuthZResourceAttributes(
                        requestContext, AuthZResource.AttributeKeys.ENV_STAGE_IS_SOX, NEW_VALUE);

        assertEquals(NEW_VALUE, result.get(AuthZResource.AttributeKeys.ENV_STAGE_IS_SOX.name()));
        assertEquals(EXISTING_VALUE, result.get(EXISTING_KEY));
    }

    @Test
    void insertAuthZResourceAttributes_nonMapAttributes_createsNewMapAndInsertsKeyValue() {
        when(requestContext.getProperty(Constants.AUTHZ_ATTR_REQ_CXT_KEY)).thenReturn(new Object());

        Map<String, String> result =
                AuthZResourceAttributesUtils.insertAuthZResourceAttributes(
                        requestContext, AuthZResource.AttributeKeys.ENV_STAGE_IS_SOX, NEW_VALUE);

        assertEquals(NEW_VALUE, result.get(AuthZResource.AttributeKeys.ENV_STAGE_IS_SOX.name()));
    }

    @Test
    void getAuthZResourceAttributes_existingAttributesMap_returnsAttributesMap() {
        attributesMap.put(AuthZResource.AttributeKeys.ENV_STAGE_IS_SOX.name(), NEW_VALUE);
        when(requestContext.getProperty(Constants.AUTHZ_ATTR_REQ_CXT_KEY))
                .thenReturn(attributesMap);

        Map<String, String> result =
                AuthZResourceAttributesUtils.getAuthZResourceAttributes(requestContext);

        assertEquals(attributesMap, result);
    }

    @Test
    void getAuthZResourceAttributes_nonMapAttributes_returnsEmptyMap() {
        when(requestContext.getProperty(Constants.AUTHZ_ATTR_REQ_CXT_KEY)).thenReturn(new Object());

        Map<String, String> result =
                AuthZResourceAttributesUtils.getAuthZResourceAttributes(requestContext);

        assertTrue(result.isEmpty());
    }
}
