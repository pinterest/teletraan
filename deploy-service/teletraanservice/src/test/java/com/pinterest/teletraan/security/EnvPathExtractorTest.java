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

import com.pinterest.teletraan.universal.security.AuthZResourceExtractor.ExtractionException;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EnvPathExtractorTest extends BasePathExtractorTest {
    private EnvPathExtractor sut;

    @BeforeEach
    void setUp() {
        super.setUp();
        sut = new EnvPathExtractor();
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
    void testExtractResource_1EnvName() throws ExtractionException {
        pathParameters.add("envName", "testEnv");

        AuthZResource resource = sut.extractResource(context);
        assertNotNull(resource);
        assertEquals("testEnv", resource.getName());
        assertEquals(AuthZResource.Type.ENV, resource.getType());
    }

    @Test
    void testExtractResource_2EnvNames() throws ExtractionException {
        pathParameters.add("envName", "testEnv");
        pathParameters.add("envName", "testEnv2");

        AuthZResource resource = sut.extractResource(context);
        assertNotNull(resource);
        assertEquals("testEnv", resource.getName());
        assertEquals(AuthZResource.Type.ENV, resource.getType());
    }
}
