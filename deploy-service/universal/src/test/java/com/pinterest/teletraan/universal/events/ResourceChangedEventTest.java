/**
 * Copyright (c) 2023-2024 Pinterest, Inc.
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
package com.pinterest.teletraan.universal.events;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ResourceChangedEventTest {
    private static final String OPERATOR = "operator";
    private static final String RESOURCE = "resource";

    @Test
    void testConstructor_oddTags() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ResourceChangedEvent(RESOURCE, OPERATOR, this, 0L, ""));
    }

    @Test
    void testConstructor_evenTags() {
        assertDoesNotThrow(
                () -> new ResourceChangedEvent(RESOURCE, OPERATOR, this, 0L, "t1", "v1"));
    }

    @Test
    void testConstructor_noTag() {
        assertDoesNotThrow(() -> new ResourceChangedEvent(RESOURCE, OPERATOR, this, 0L));
    }

    @Test
    void testConstructor_tagsAsMap() {
        Map<String, String> tags = ImmutableMap.of("t1", "v1");
        assertDoesNotThrow(() -> new ResourceChangedEvent(RESOURCE, OPERATOR, this, 0L, tags));
    }

    @Test
    void testConstructor_nullSource() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ResourceChangedEvent(RESOURCE, OPERATOR, null, 0L));
    }
}
