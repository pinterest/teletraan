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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import javax.ws.rs.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SourceControlManagerProxyTest {
    private SourceControlManagerProxy proxy;

    @BeforeEach
    void setUp() {
        Map<String, SourceControlManager> managers = new HashMap<>();
        managers.put("github-enterprise", mock(GithubManager.class));
        managers.put("phabricator", mock(PhabricatorManager.class));
        proxy = new SourceControlManagerProxy(managers, "github-enterprise");
    }

    @Test
    void testConfiguredGithubManagerUsesCanonicalRepositoryIdentity() {
        assertEquals(
                "pinterest/repo.v2",
                proxy.canonicalizeRepository("github-enterprise", "Pinterest/Repo.v2"));
        assertEquals("pinterest/repo.v2", proxy.canonicalizeRepository(null, "Pinterest/Repo.v2"));
        assertEquals("pinterest/repo.v2", proxy.canonicalizeRepository("", "Pinterest/Repo.v2"));
        assertThrows(
                BadRequestException.class,
                () -> proxy.canonicalizeRepository("github-enterprise", "victim/x/../../other"));
    }

    @Test
    void testOtherScmRepositoryIdentityIsPreserved() {
        assertEquals("CALLSIGN", proxy.canonicalizeRepository("phabricator", "CALLSIGN"));
    }

    @Test
    void testUnknownScmIsRejected() {
        assertThrows(
                BadRequestException.class,
                () -> proxy.canonicalizeRepository("unsupported", "pinterest/teletraan"));
    }

    @Test
    void testRepositoryResourceUsesConfiguredKeyFromCaseInsensitiveLookup() {
        Map<String, SourceControlManager> managers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        SourceControlManager manager = mock(GithubManager.class);
        managers.put("GitHubCorp", manager);
        managers.put("OtherGithub", manager);
        SourceControlManagerProxy caseInsensitiveProxy =
                new SourceControlManagerProxy(managers, "githubcorp");

        assertEquals(
                "GitHubCorp/pinterest/repo.v2",
                caseInsensitiveProxy.getRepositoryResourceName("GITHUBCORP", "Pinterest/Repo.v2"));
        assertEquals(
                "GitHubCorp/pinterest/repo.v2",
                caseInsensitiveProxy.getRepositoryResourceName(null, "Pinterest/Repo.v2"));
        assertEquals(
                "GitHubCorp/pinterest/repo.v2",
                caseInsensitiveProxy.getRepositoryResourceName("", "Pinterest/Repo.v2"));
        assertEquals(
                "OtherGithub/pinterest/repo.v2",
                caseInsensitiveProxy.getRepositoryResourceName("othergithub", "Pinterest/Repo.v2"));
    }

    @Test
    void testRepositoryResourcePreservesExactLookupAndOtherScmNames() {
        assertEquals(
                "phabricator/CALLSIGN", proxy.getRepositoryResourceName("phabricator", "CALLSIGN"));
        assertThrows(
                BadRequestException.class,
                () -> proxy.getRepositoryResourceName("GITHUB-ENTERPRISE", "pinterest/teletraan"));
        assertThrows(
                BadRequestException.class,
                () -> proxy.getRepositoryResourceName("unsupported", "pinterest/teletraan"));
        assertThrows(
                BadRequestException.class,
                () -> proxy.getRepositoryResourceName("github-enterprise", "victim/x/../../other"));
        assertThrows(
                BadRequestException.class,
                () -> proxy.getRepositoryResourceName("phabricator", ""));
    }

    @Test
    void testRepositoryResourceFitsTheRoleStorageBoundary() {
        String alias = String.join("", Collections.nCopies(34, "g"));
        String owner = String.join("", Collections.nCopies(39, "o"));
        String repository = String.join("", Collections.nCopies(100, "r"));
        Map<String, SourceControlManager> managers = new HashMap<>();
        SourceControlManager manager = mock(GithubManager.class);
        managers.put(alias, manager);
        managers.put(alias + "g", manager);
        SourceControlManagerProxy boundedProxy = new SourceControlManagerProxy(managers, alias);

        String resourceName =
                boundedProxy.getRepositoryResourceName(alias, owner + "/" + repository);
        assertEquals(175, resourceName.length());
        assertEquals(alias + "/" + owner + "/" + repository, resourceName);
        assertThrows(
                BadRequestException.class,
                () ->
                        boundedProxy.getRepositoryResourceName(
                                alias + "g", owner + "/" + repository));
    }

    @Test
    void testRepositoryResourceEscapesConfiguredScmKeysWithoutCollisions() {
        Map<String, SourceControlManager> managers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        SourceControlManager manager = mock(SourceControlManager.class);
        managers.put("foo", manager);
        managers.put("foo/bar", manager);
        managers.put("foo%2Fbar", manager);
        managers.put("GitHub.Corp:v2 env", manager);
        SourceControlManagerProxy escapedProxy = new SourceControlManagerProxy(managers, "foo/bar");

        assertEquals("foo%2Fbar/Repo", escapedProxy.getRepositoryResourceName("FOO/BAR", "Repo"));
        assertEquals("foo/bar/Repo", escapedProxy.getRepositoryResourceName("foo", "bar/Repo"));
        assertEquals(
                "foo%252Fbar/Repo", escapedProxy.getRepositoryResourceName("foo%2fbar", "Repo"));
        assertEquals("foo%2Fbar/Repo", escapedProxy.getRepositoryResourceName(null, "Repo"));
        assertEquals(
                "GitHub.Corp%3Av2%20env/Repo",
                escapedProxy.getRepositoryResourceName("github.corp:V2 ENV", "Repo"));
    }

    @Test
    void testRepositoryResourceStorageBoundaryIncludesEscapingExpansion() {
        Map<String, SourceControlManager> managers = new HashMap<>();
        managers.put("/", mock(SourceControlManager.class));
        SourceControlManagerProxy escapedProxy = new SourceControlManagerProxy(managers, "/");
        String repository = String.join("", Collections.nCopies(171, "r"));

        String resourceName = escapedProxy.getRepositoryResourceName("/", repository);
        assertEquals(175, resourceName.length());
        assertEquals("%2F/" + repository, resourceName);
        assertThrows(
                BadRequestException.class,
                () -> escapedProxy.getRepositoryResourceName("/", repository + "r"));
    }
}
