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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import javax.ws.rs.BadRequestException;
import org.junit.jupiter.api.Test;

class SourceControlManagerProxyTest {
    @Test
    void resolvesConfiguredCasingAndDefaultWithoutConflatingAliases() throws Exception {
        SourceControlManager github = mock(GithubManager.class);
        Map<String, SourceControlManager> managers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        managers.put("GitHub", github);
        managers.put("Enterprise", github);
        SourceControlManagerProxy proxy = new SourceControlManagerProxy(managers, "gItHuB");

        assertEquals("GitHub", proxy.resolveScmTypeName("GITHUB"));
        assertEquals("GitHub", proxy.resolveScmTypeName(""));
        assertEquals("GitHub", proxy.resolveScmTypeName(null));
        assertEquals("Enterprise", proxy.resolveScmTypeName("ENTERPRISE"));
        assertTrue(proxy.isScmTypeCaseInsensitive("GitHub"));
        assertTrue(proxy.isScmTypeCaseInsensitive("Enterprise"));
        assertTrue(proxy.isGithub("Enterprise"));
    }

    @Test
    void preservesCaseDistinctAliasesInExactMap() throws Exception {
        Map<String, SourceControlManager> managers = new HashMap<>();
        managers.put("GitHub", mock(GithubManager.class));
        managers.put("github", mock(PhabricatorManager.class));
        SourceControlManagerProxy proxy = new SourceControlManagerProxy(managers, "github");

        assertEquals("GitHub", proxy.resolveScmTypeName("GitHub"));
        assertEquals("github", proxy.resolveScmTypeName("github"));
        assertEquals("github", proxy.resolveScmTypeName(""));
        assertFalse(proxy.isScmTypeCaseInsensitive("GitHub"));
        assertFalse(proxy.isScmTypeCaseInsensitive("github"));
        assertTrue(proxy.isGithub("GitHub"));
        assertFalse(proxy.isGithub("github"));
        assertThrows(BadRequestException.class, () -> proxy.resolveScmTypeName("GITHUB"));
    }

    @Test
    void doesNotCaseFoldNaturallySortedMap() {
        Map<String, SourceControlManager> managers = new TreeMap<>();
        managers.put("GitHub", mock(GithubManager.class));
        SourceControlManagerProxy proxy = new SourceControlManagerProxy(managers, "GitHub");

        assertEquals("GitHub", proxy.resolveScmTypeName(""));
        assertFalse(proxy.isScmTypeCaseInsensitive("GitHub"));
        assertThrows(BadRequestException.class, () -> proxy.resolveScmTypeName("github"));
    }

    @Test
    void rejectsUnsupportedAliasAndMissingDefault() {
        Map<String, SourceControlManager> managers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        managers.put("GitHub", mock(GithubManager.class));
        SourceControlManagerProxy proxy = new SourceControlManagerProxy(managers, "missing");

        assertThrows(BadRequestException.class, () -> proxy.resolveScmTypeName("unknown"));
        assertThrows(BadRequestException.class, () -> proxy.resolveScmTypeName(""));
    }
}
