/**
 * Copyright (c) 2022 Pinterest, Inc.
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.pinterest.deployservice.bean.CommitBean;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.BadRequestException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class GithubManagerTest {
    private MockWebServer server;
    private GithubManager manager;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        manager = createManager("test-token", "/api/v3");
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    private GithubManager createManager(String token, String apiPath) {
        return new GithubManager(
                token,
                "unused-app-id",
                "",
                "unused-organization",
                "Github",
                server.url(apiPath).toString(),
                "https://github.com");
    }

    @Test
    void testGetCommit() throws Exception {
        server.enqueue(
                new MockResponse()
                        .setBody(
                                "{\"sha\":\"abc123\",\"author\":{\"login\":\"author\"},"
                                        + "\"commit\":{\"committer\":{\"date\":\"2024-06-01T12:00:00Z\"},"
                                        + "\"message\":\"Title\\nBody\"}}"));

        CommitBean commit = manager.getCommit("Pinterest/Teletraan.v2", "abc123");

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("/api/v3/repos/pinterest/teletraan.v2/commits/abc123", request.getPath());
        assertEquals("Token test-token", request.getHeader("Authorization"));
        assertEquals("abc123", commit.getSha());
        assertEquals("author", commit.getAuthor());
        assertEquals("Title", commit.getTitle());
        assertEquals("Body", commit.getMessage());
        assertEquals("https://github.com/pinterest/teletraan.v2/commit/abc123", commit.getInfo());
    }

    @ParameterizedTest
    @CsvSource(
            value = {
                "pinterest/teletraan,pinterest/teletraan",
                "Pinterest-Org/Repo_1.2-3,pinterest-org/repo_1.2-3",
                "pinterest/.github,pinterest/.github",
                "pinterest/..config,pinterest/..config"
            })
    void testValidRepositoryAndEncodedQueryParameters(String repo, String canonicalRepo)
            throws Exception {
        server.enqueue(new MockResponse().setBody("[]"));
        String startSha = "feature/test?sha=other#fragment";
        String path = "src/test file?path=other#fragment";

        assertEquals(0, manager.getCommits(repo, startSha, true, path).size());

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals(
                "/api/v3/repos/" + canonicalRepo + "/commits",
                request.getRequestUrl().encodedPath());
        assertEquals(canonicalRepo, GithubManager.canonicalizeRepository(repo));
        assertEquals(startSha, request.getRequestUrl().queryParameter("sha"));
        assertEquals(path, request.getRequestUrl().queryParameter("path"));
        assertEquals(2, request.getRequestUrl().querySize());
    }

    @Test
    void testEnterpriseApiPrefixWithTrailingSlash() throws Exception {
        server.enqueue(new MockResponse().setBody("[]"));

        createManager("test-token", "/api/v3/").getCommits("pinterest/teletraan", "main", true, "");

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals(
                "/api/v3/repos/pinterest/teletraan/commits", request.getRequestUrl().encodedPath());
    }

    @ParameterizedTest
    @CsvSource({
        "release/2026,release%2F2026",
        "release+prod,release+prod",
        "release@prod,release@prod",
        "release#prod,release%23prod",
        "release%prod,release%25prod",
        "%2e%2e,%252e%252e",
        "release/%2e%2e/other,release%2F%252e%252e%2Fother",
        "%252e%252e%252fother,%25252e%25252e%25252fother",
        "release?prod,release%3Fprod",
        "release\u2003prod,release%E2%80%83prod",
        "refs/heads/release/2026,refs%2Fheads%2Frelease%2F2026",
        "rélèase,r%C3%A9l%C3%A8ase"
    })
    void testRangeAcceptsAndEncodesEndReference(String endSha, String encodedEndSha)
            throws Exception {
        server.enqueue(new MockResponse().setBody(commitJson("abc123", "2024-06-01T12:00:00Z")));
        server.enqueue(
                new MockResponse()
                        .setBody("[" + commitJson("def456", "2024-06-02T12:00:00Z") + "]"));

        List<CommitBean> commits =
                manager.getCommits("pinterest/teletraan", "feature/new+work", endSha, 1);

        assertEquals(1, commits.size());
        assertEquals("def456", commits.get(0).getSha());
        RecordedRequest referenceRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(referenceRequest);
        assertEquals(
                "/api/v3/repos/pinterest/teletraan/commits/" + encodedEndSha,
                referenceRequest.getPath());
        assertEquals(7, referenceRequest.getRequestUrl().pathSegments().size());
        assertEquals(endSha, referenceRequest.getRequestUrl().pathSegments().get(6));
        assertEquals("Token test-token", referenceRequest.getHeader("Authorization"));
        RecordedRequest historyRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(historyRequest);
        assertEquals(
                "/api/v3/repos/pinterest/teletraan/commits",
                historyRequest.getRequestUrl().encodedPath());
        assertEquals("feature/new+work", historyRequest.getRequestUrl().queryParameter("sha"));
        assertEquals(2, server.getRequestCount());
    }

    @ParameterizedTest
    @CsvSource({
        "repo-1,release/2026,https://github.com/repo-1/commit/release%2F2026",
        "repo-1,release+prod,https://github.com/repo-1/commit/release+prod",
        "repo-1,release@prod,https://github.com/repo-1/commit/release@prod",
        "Pinterest/Repo,abc123,https://github.com/Pinterest/Repo/commit/abc123"
    })
    void testBuildMetadataLinksDoNotRequireOutboundRepositoryValidation(
            String repo, String reference, String expectedLink) {
        GithubManager unauthenticatedManager = createManager("", "/api/v3");

        assertEquals(expectedLink, unauthenticatedManager.generateCommitLink(repo, reference));
        assertEquals(0, server.getRequestCount());
    }

    @Test
    void testBuildMetadataLinksEncodeRoutingCharactersWithoutEscapingThePrefix() {
        GithubManager unauthenticatedManager = createManager("", "/api/v3");

        assertEquals(
                "https://github.com/owner/%252E%252E/repo%3Fquery%23fragment/commit/ref%2Fpart%3Fquery%23fragment",
                unauthenticatedManager.generateCommitLink(
                        "owner/../repo?query#fragment", "ref/part?query#fragment"));
        assertEquals(0, server.getRequestCount());
    }

    private static String commitJson(String sha, String date) {
        return String.format(
                "{\"sha\":\"%s\",\"author\":{\"login\":\"author\"},"
                        + "\"commit\":{\"committer\":{\"date\":\"%s\"},\"message\":\"Title\"}}",
                sha, date);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
            strings = {
                "pinterest",
                "pinterest/",
                "/teletraan",
                "/pinterest/teletraan",
                "pinterest/teletraan/",
                "pinterest/teletraan/extra",
                "pinterest//teletraan",
                "./teletraan",
                "../teletraan",
                "pinterest/.",
                "pinterest/..",
                "pinterest/../../../app/installations",
                "victim/x/../../other",
                "x/../../orgs/VICTIMORG/repos",
                "pinterest/%2e%2e",
                "pinterest%2fother/teletraan",
                "pinterest/teletraan%2fextra",
                "pinterest/teletraan?per_page=100#",
                "pinterest/teletraan#fragment",
                "pinterest\\other/teletraan",
                "pinterest/teletraan\\extra",
                "pinterest/tele traan",
                " pinterest/teletraan",
                "pinterest/teletraan\n",
                "https://github.com/pinterest/teletraan"
            })
    void testInvalidRepositoryRejectedBeforeCredentialsOrHttp(String repo) {
        // With no token or private key, reaching credential retrieval would fail differently.
        GithubManager unauthenticatedManager = createManager("", "/api/v3");

        assertThrows(BadRequestException.class, () -> GithubManager.canonicalizeRepository(repo));
        assertThrows(
                BadRequestException.class, () -> unauthenticatedManager.getCommit(repo, "abc123"));
        assertThrows(
                BadRequestException.class,
                () -> unauthenticatedManager.getCommits(repo, "main", true, ""));
        assertEquals(0, server.getRequestCount());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
            strings = {
                ".",
                "..",
                "../other",
                "release/../other",
                "release/./other",
                "/release/2026",
                "release/2026/",
                "release//2026",
                "abc123\\extra",
                "abc123 def",
                "abc123\n"
            })
    void testInvalidCommitReferenceRejectedBeforeCredentialsOrHttp(String sha) {
        GithubManager unauthenticatedManager = createManager("", "/api/v3");

        assertThrows(
                BadRequestException.class,
                () -> unauthenticatedManager.getCommit("pinterest/teletraan", sha));
        assertEquals(0, server.getRequestCount());
    }

    @ParameterizedTest
    @ValueSource(strings = {"release/../../other", "release/../other", "release/./other"})
    void testTraversalEndReferenceRejectedBeforeCredentialsOrHttp(String endSha) {
        GithubManager unauthenticatedManager = createManager("", "/api/v3");

        assertThrows(
                BadRequestException.class,
                () -> unauthenticatedManager.getCommits("pinterest/teletraan", "main", endSha, 1));
        assertEquals(0, server.getRequestCount());
    }
}
