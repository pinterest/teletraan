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
package com.pinterest.teletraan.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pinterest.teletraan.universal.security.AuthZResourceExtractor.ExtractionException;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EnvStagePathExtractorTest extends BasePathExtractorTest {
    private EnvStagePathExtractor sut;
    private static final String ENV_NAME_KEY = "envName";
    private static final String STAGE_NAME_KEY = "stageName";

    @BeforeEach
    void setUp() {
        super.setUp();
        sut = new EnvStagePathExtractor();
    }

    @Test
    void testExtractResource_noPathParams_exception() {
        assertThrows(ExtractionException.class, () -> sut.extractResource(context));
    }

    @Test
    void testExtractResource_0EnvName() {
        pathParameters.add("nonEnvName", "testEnv");

        assertThrows(ExtractionException.class, () -> sut.extractResource(context));
    }

    @Test
    void testExtractResource_onlyStageName() {
        pathParameters.add(STAGE_NAME_KEY, "testEnv");

        assertThrows(ExtractionException.class, () -> sut.extractResource(context));
    }

    @Test
    void testExtractResource_1EnvName() throws ExtractionException {
        pathParameters.add(ENV_NAME_KEY, "testEnv");

        AuthZResource resource = sut.extractResource(context);
        assertNotNull(resource);
        assertTrue(resource.getName().contains("testEnv"));
        assertEquals(AuthZResource.Type.ENV_STAGE, resource.getType());
    }

    @Test
    void testExtractResource_1EnvName1StageName() throws ExtractionException {
        pathParameters.add(ENV_NAME_KEY, "testEnv");
        pathParameters.add(STAGE_NAME_KEY, "testStage");

        AuthZResource resource = sut.extractResource(context);
        assertNotNull(resource);
        assertTrue(resource.getName().contains("testEnv"));
        assertTrue(resource.getName().contains("testStage"));
        assertEquals(AuthZResource.Type.ENV_STAGE, resource.getType());
    }

    @Test
    void testExtractResource_2EnvNames1StageName() throws ExtractionException {
        pathParameters.add(ENV_NAME_KEY, "testEnv");
        pathParameters.add(ENV_NAME_KEY, "testEnv2");
        pathParameters.add(STAGE_NAME_KEY, "testStage");

        AuthZResource resource = sut.extractResource(context);
        assertNotNull(resource);
        assertTrue(resource.getName().contains("testEnv"));
        assertTrue(resource.getName().contains("testStage"));
        assertEquals(AuthZResource.Type.ENV_STAGE, resource.getType());
    }
}
