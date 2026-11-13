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
package com.pinterest.deployservice.scm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PhabricatorManagerTest {
    @Test
    void singleCommitRequestPreservesParametersWithoutAPathFilter() {
        JsonObject parameters =
                JsonParser.parseString(
                                PhabricatorManager.serializeHistoryParameters(
                                        "ALLOWED", "abc123", 1, null))
                        .getAsJsonObject();

        assertEquals("ALLOWED", parameters.get("repository").getAsString());
        assertEquals("abc123", parameters.get("commit").getAsString());
        assertEquals(1, parameters.get("limit").getAsInt());
        assertFalse(parameters.has("path"));
        assertEquals(3, parameters.size());
    }

    @Test
    void historyRequestPreservesAnEmptyPathFilter() {
        JsonObject parameters =
                JsonParser.parseString(
                                PhabricatorManager.serializeHistoryParameters(
                                        "ALLOWED", "main", 30, ""))
                        .getAsJsonObject();

        assertEquals("ALLOWED", parameters.get("repository").getAsString());
        assertEquals("main", parameters.get("commit").getAsString());
        assertEquals(30, parameters.get("limit").getAsInt());
        assertEquals("", parameters.get("path").getAsString());
        assertEquals(4, parameters.size());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "\",\"repository\":\"secret",
                "\",\"limit\":1000,\"repository\":\"secret",
                "quoted\"value\\with\ncontrol\tcharacters",
                "\"},\"unexpected\":{\"repository\":\"secret\"}"
            })
    void commitAndPathCannotOverrideTheAllowedRepository(String hostileValue) {
        JsonObject parameters =
                JsonParser.parseString(
                                PhabricatorManager.serializeHistoryParameters(
                                        "ALLOWED", hostileValue, 30, hostileValue))
                        .getAsJsonObject();

        assertEquals("ALLOWED", parameters.get("repository").getAsString());
        assertEquals(hostileValue, parameters.get("commit").getAsString());
        assertEquals(hostileValue, parameters.get("path").getAsString());
        assertEquals(30, parameters.get("limit").getAsInt());
        assertEquals(4, parameters.size());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "ALLOWED\",\"repository\":\"secret",
                "ALLOWED\",\"commit\":\"other",
                "ALLOWED\",\"limit\":1000,\"unused\":\"value",
                "quoted\"repository\\with\ncontrol\tcharacters"
            })
    void repositoryIsSerializedAsTheExactCheckedValue(String repository) {
        JsonObject parameters =
                JsonParser.parseString(
                                PhabricatorManager.serializeHistoryParameters(
                                        repository, "abc123", 1, null))
                        .getAsJsonObject();

        assertEquals(repository, parameters.get("repository").getAsString());
        assertEquals("abc123", parameters.get("commit").getAsString());
        assertEquals(1, parameters.get("limit").getAsInt());
        assertFalse(parameters.has("path"));
        assertEquals(3, parameters.size());
    }
}
