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
package com.pinterest.teletraan.universal.security.bean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import org.junit.jupiter.api.Test;

class AuthZResourceTest {
    private static final String ENV_NAME = "envName";
    private static final String STAGE_NAME = "stageName";

    @Test
    void getEnvName_envStageType_returnsEnvName() {
        AuthZResource resource =
                new AuthZResource("envName/stageName", AuthZResource.Type.ENV_STAGE);
        assertEquals(ENV_NAME, resource.getEnvName());
    }

    @Test
    void getEnvName_envType_returnsEnvName() {
        AuthZResource resource = new AuthZResource(ENV_NAME, AuthZResource.Type.ENV);
        assertEquals(ENV_NAME, resource.getEnvName());
    }

    @Test
    void getEnvName_otherType_returnsNull() {
        AuthZResource resource = new AuthZResource(ENV_NAME, AuthZResource.Type.GROUP);
        assertNull(resource.getEnvName());
    }

    @Test
    void constructor_withEnvNameAndStageName_setsNameAndType() {
        AuthZResource resource = new AuthZResource(ENV_NAME, STAGE_NAME);
        assertEquals("envName/stageName", resource.getName());
        assertEquals(AuthZResource.Type.ENV_STAGE, resource.getType());
        assertNull(resource.getAttributes());
    }

    @Test
    void constructor_withEnvNameAndStageNameAndAttributes_setsNameTypeAndAttributes() {
        HashMap<String, String> attributes = new HashMap<>();
        attributes.put("key1", "value1");
        AuthZResource resource = new AuthZResource(ENV_NAME, STAGE_NAME, attributes);
        assertEquals("envName/stageName", resource.getName());
        assertEquals(AuthZResource.Type.ENV_STAGE, resource.getType());
        assertEquals(attributes, resource.getAttributes());
    }

    @Test
    void constructor_missingName_exception() {
        assertThrows(IllegalArgumentException.class, () -> new AuthZResource(null, STAGE_NAME));
        assertThrows(
                IllegalArgumentException.class, () -> new AuthZResource(null, STAGE_NAME, null));
    }
}
