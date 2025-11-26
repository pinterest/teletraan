/**
 * Copyright (c) 2025 Pinterest, Inc.
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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.pinterest.teletraan.universal.security.AuthZResourceExtractor;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DeploySchedulePathExtractorTest extends BasePathExtractorTest {
    private DeploySchedulePathExtractor sut;
    private static final String ENV_NAME_KEY = "envName";
    private static final String STAGE_NAME_KEY = "stageName";

    @BeforeEach
    @Override
    void setUp() {
        super.setUp();
        sut = new DeploySchedulePathExtractor();
    }

    @Test
    void testExtractResource_noPathParams_exception() {
        assertThrows(
                AuthZResourceExtractor.ExtractionException.class,
                () -> sut.extractResource(context));
    }

    @Test
    void testExtractResource_0EnvName() {
        pathParameters.add("nonEnvName", "testEnv");

        assertThrows(
                AuthZResourceExtractor.ExtractionException.class,
                () -> sut.extractResource(context));
    }

    @Test
    void testExtractResource_onlyStageName() {
        pathParameters.add(STAGE_NAME_KEY, "testEnv");

        assertThrows(
                AuthZResourceExtractor.ExtractionException.class,
                () -> sut.extractResource(context));
    }

    @Test
    void testExtractResource_1EnvName() throws AuthZResourceExtractor.ExtractionException {
        pathParameters.add(ENV_NAME_KEY, "testEnv");

        AuthZResource resource = sut.extractResource(context);
        assertNotNull(resource);
        assertTrue(resource.getName().contains("testEnv"));
        assertEquals(AuthZResource.Type.DEPLOY_SCHEDULE, resource.getType());
    }

    @Test
    void testExtractResource_1EnvName1StageName()
            throws AuthZResourceExtractor.ExtractionException {
        pathParameters.add(ENV_NAME_KEY, "testEnv");
        pathParameters.add(STAGE_NAME_KEY, "testStage");

        AuthZResource resource = sut.extractResource(context);
        assertNotNull(resource);
        assertEquals(AuthZResource.Type.DEPLOY_SCHEDULE, resource.getType());
        assertEquals("testEnv/testStage", resource.getName());
    }

    @Test
    void testExtractResource_2EnvNames1StageName()
            throws AuthZResourceExtractor.ExtractionException {
        pathParameters.add(ENV_NAME_KEY, "testEnv");
        pathParameters.add(ENV_NAME_KEY, "testEnv2");
        pathParameters.add(STAGE_NAME_KEY, "testStage");

        AuthZResource resource = sut.extractResource(context);
        assertNotNull(resource);
        assertEquals("testEnv/testStage", resource.getName());
        assertEquals(AuthZResource.Type.DEPLOY_SCHEDULE, resource.getType());
    }
}
