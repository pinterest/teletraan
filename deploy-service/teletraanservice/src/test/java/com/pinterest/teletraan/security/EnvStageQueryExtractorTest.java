/**
 * Copyright (c) 2026 Pinterest, Inc.
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
package com.pinterest.teletraan.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class EnvStageQueryExtractorTest {
    private final EnvStageQueryExtractor extractor = new EnvStageQueryExtractor();
    private ContainerRequestContext context;
    private MultivaluedMap<String, String> parameters;

    @BeforeEach
    void setUp() {
        context = mock(ContainerRequestContext.class);
        UriInfo uriInfo = mock(UriInfo.class);
        parameters = new MultivaluedHashMap<>();
        when(context.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getQueryParameters()).thenReturn(parameters);
    }

    @Test
    void extractsEnvironmentAndStageFromQueryParameters() {
        parameters.putSingle("envName", "testEnv");
        parameters.putSingle("stageName", "prod");

        AuthZResource resource = extractor.extractResource(context);

        assertEquals(AuthZResource.Type.ENV_STAGE, resource.getType());
        assertEquals("testEnv/prod", resource.getName());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void rejectsMissingOrBlankEnvironment(String envName) {
        parameters.putSingle("envName", envName);
        parameters.putSingle("stageName", "prod");

        assertThrows(BadRequestException.class, () -> extractor.extractResource(context));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void rejectsMissingOrBlankStage(String stageName) {
        parameters.putSingle("envName", "testEnv");
        parameters.putSingle("stageName", stageName);

        assertThrows(BadRequestException.class, () -> extractor.extractResource(context));
    }

    @Test
    void usesFirstQueryValuesMatchingResourceParameterInjection() {
        parameters.add("envName", "testEnv");
        parameters.add("envName", "otherEnv");
        parameters.add("stageName", "prod");
        parameters.add("stageName", "staging");

        assertEquals("testEnv/prod", extractor.extractResource(context).getName());
    }
}
