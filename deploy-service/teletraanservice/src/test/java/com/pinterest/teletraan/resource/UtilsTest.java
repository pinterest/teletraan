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
package com.pinterest.teletraan.resource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;

/** Tests for {@link Utils#rejectDisallowedUnicode} (T006). */
class UtilsTest {

    @Test
    void rejectDisallowedUnicode_allowsCleanValues() {
        Map<String, String> configs = new HashMap<>();
        configs.put("LD_LIBRARY_PATH", "/usr/lib/foo");
        configs.put("TEAM", "homefeed-serving");
        assertDoesNotThrow(() -> Utils.rejectDisallowedUnicode(configs, "alice", "agent_configs"));
    }

    @Test
    void rejectDisallowedUnicode_allowsNullInput() {
        assertDoesNotThrow(() -> Utils.rejectDisallowedUnicode(null, "alice", "agent_configs"));
    }

    @Test
    void rejectDisallowedUnicode_allowsNullValues() {
        Map<String, String> configs = new HashMap<>();
        configs.put("FOO", null);
        assertDoesNotThrow(() -> Utils.rejectDisallowedUnicode(configs, "alice", "agent_configs"));
    }

    @Test
    void rejectDisallowedUnicode_rejectsLeftToRightMark() {
        // U+200E LEFT-TO-RIGHT MARK — the exact codepoint from teletraan-5054
        Map<String, String> configs = new HashMap<>();
        configs.put("TEAM", "homefeed\u200E-serving");
        WebApplicationException ex =
                assertThrows(
                        WebApplicationException.class,
                        () -> Utils.rejectDisallowedUnicode(configs, "alice", "agent_configs"));
        assertEquals(422, ex.getResponse().getStatus());
        assertTrue(ex.getMessage().contains("TEAM"), ex.getMessage());
        assertTrue(ex.getMessage().contains("U+200E"), ex.getMessage());
    }

    @Test
    void rejectDisallowedUnicode_rejectsZeroWidthSpace() {
        Map<String, String> configs = new HashMap<>();
        configs.put("KEY", "val\u200Bue");
        WebApplicationException ex =
                assertThrows(
                        WebApplicationException.class,
                        () -> Utils.rejectDisallowedUnicode(configs, "alice", "script_configs"));
        assertEquals(422, ex.getResponse().getStatus());
        assertTrue(ex.getMessage().contains("U+200B"), ex.getMessage());
    }

    @Test
    void rejectDisallowedUnicode_rejectsByteOrderMark() {
        Map<String, String> configs = new HashMap<>();
        configs.put("KEY", "\uFEFFvalue");
        assertThrows(
                WebApplicationException.class,
                () -> Utils.rejectDisallowedUnicode(configs, "alice", "script_configs"));
    }
}
