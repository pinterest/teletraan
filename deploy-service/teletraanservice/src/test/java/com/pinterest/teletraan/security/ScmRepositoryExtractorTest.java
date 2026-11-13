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
package com.pinterest.teletraan.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.scm.GithubManager;
import com.pinterest.deployservice.scm.SourceControlManager;
import com.pinterest.deployservice.scm.SourceControlManagerProxy;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor.ExtractionException;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfo;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import java.util.Map;
import java.util.TreeMap;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class ScmRepositoryExtractorTest {
    private ServiceContext serviceContext;
    private ContainerRequestContext requestContext;
    private MultivaluedMap<String, String> pathParameters;
    private MultivaluedMap<String, String> queryParameters;

    @BeforeEach
    void setUp() {
        serviceContext = new ServiceContext();
        Map<String, SourceControlManager> managers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        GithubManager githubManager = mock(GithubManager.class);
        managers.put("GitHubCorp", githubManager);
        managers.put("OtherGithub", githubManager);
        managers.put("Phabricator", mock(SourceControlManager.class));
        serviceContext.setSourceControlManagerProxy(
                new SourceControlManagerProxy(managers, "GitHubCorp"));

        requestContext = mock(ContainerRequestContext.class);
        UriInfo uriInfo = mock(UriInfo.class);
        pathParameters = new MultivaluedHashMap<>();
        queryParameters = new MultivaluedHashMap<>();
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPathParameters()).thenReturn(pathParameters);
        when(uriInfo.getQueryParameters()).thenReturn(queryParameters);
    }

    @ParameterizedTest
    @EnumSource(
            value = ResourceAuthZInfo.Location.class,
            names = {"PATH", "QUERY"})
    void canonicalizesRepositoryAndPreservesConfiguredScmKey(ResourceAuthZInfo.Location location)
            throws Exception {
        parameters(location).putSingle("scm", "GitHubCorp");
        parameters(location).putSingle("repo", "Pinterest/Teletraan");
        AuthZResource result = extractor(location).extractResource(requestContext);
        assertEquals(
                new AuthZResource(
                        "GitHubCorp/pinterest/teletraan", AuthZResource.Type.SCM_REPOSITORY),
                result);
    }

    @Test
    void resolvesMissingOrEmptyScmToTheConfiguredDefault() throws Exception {
        queryParameters.putSingle("repo", "Pinterest/Teletraan");
        ScmRepositoryExtractor extractor = extractor(ResourceAuthZInfo.Location.QUERY);
        AuthZResource missing = extractor.extractResource(requestContext);
        queryParameters.putSingle("scm", "");
        assertEquals(missing, extractor.extractResource(requestContext));
        assertEquals("GitHubCorp/pinterest/teletraan", missing.getName());
    }

    @Test
    void differentConfiguredScmKeysHaveSeparatePermissions() throws Exception {
        queryParameters.putSingle("repo", "Pinterest/Teletraan");
        queryParameters.putSingle("scm", "OtherGithub");
        assertEquals(
                "OtherGithub/pinterest/teletraan",
                extractor(ResourceAuthZInfo.Location.QUERY)
                        .extractResource(requestContext)
                        .getName());
    }

    @ParameterizedTest
    @EnumSource(
            value = ResourceAuthZInfo.Location.class,
            names = {"PATH", "QUERY"})
    void callerCasingUsesTheSameConfiguredScmPermission(ResourceAuthZInfo.Location location)
            throws Exception {
        parameters(location).putSingle("scm", "gItHuBcOrP");
        parameters(location).putSingle("repo", "Pinterest/Teletraan");
        assertEquals(
                "GitHubCorp/pinterest/teletraan",
                extractor(location).extractResource(requestContext).getName());

        parameters(location).putSingle("scm", "othergithub");
        assertEquals(
                "OtherGithub/pinterest/teletraan",
                extractor(location).extractResource(requestContext).getName());
    }

    @Test
    void matchesThePathEndpointsLegacyEncodedSlashHandling() throws Exception {
        pathParameters.putSingle("scm", "GitHubCorp");
        pathParameters.putSingle("repo", "Pinterest%2FTeletraan");
        assertEquals(
                "GitHubCorp/pinterest/teletraan",
                extractor(ResourceAuthZInfo.Location.PATH)
                        .extractResource(requestContext)
                        .getName());

        queryParameters.putSingle("repo", "Pinterest%2FTeletraan");
        assertThrows(
                BadRequestException.class,
                () -> extractor(ResourceAuthZInfo.Location.QUERY).extractResource(requestContext));
    }

    @Test
    void preservesNonGithubRepositoryNames() throws Exception {
        queryParameters.putSingle("scm", "Phabricator");
        queryParameters.putSingle("repo", "MixedCaseCallsign");
        assertEquals(
                "Phabricator/MixedCaseCallsign",
                extractor(ResourceAuthZInfo.Location.QUERY)
                        .extractResource(requestContext)
                        .getName());
    }

    @Test
    void resolvesScmProxyAfterFactoryInitialization() throws Exception {
        SourceControlManagerProxy proxy = serviceContext.getSourceControlManagerProxy();
        serviceContext.setSourceControlManagerProxy(null);
        ScmRepositoryExtractor extractor = extractor(ResourceAuthZInfo.Location.QUERY);
        queryParameters.putSingle("repo", "Pinterest/Teletraan");
        assertThrows(ExtractionException.class, () -> extractor.extractResource(requestContext));
        serviceContext.setSourceControlManagerProxy(proxy);
        assertEquals(
                "GitHubCorp/pinterest/teletraan",
                extractor.extractResource(requestContext).getName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"repo", "scm"})
    void rejectsAmbiguousParameters(String parameter) {
        queryParameters.putSingle("repo", "Pinterest/Teletraan");
        queryParameters.putSingle("scm", "GitHubCorp");
        queryParameters.add(parameter, "other");
        assertThrows(
                BadRequestException.class,
                () -> extractor(ResourceAuthZInfo.Location.QUERY).extractResource(requestContext));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "",
                "owner/repo/../../private",
                "owner/repo?path=private",
                "owner/repo#fragment",
                "owner%2Frepo",
                "owner/.."
            })
    void rejectsMalformedGithubRepositories(String repository) {
        queryParameters.putSingle("repo", repository);
        assertThrows(
                BadRequestException.class,
                () -> extractor(ResourceAuthZInfo.Location.QUERY).extractResource(requestContext));
    }

    @Test
    void rejectsMissingRepositoryAndUnknownScm() {
        ScmRepositoryExtractor extractor = extractor(ResourceAuthZInfo.Location.QUERY);
        assertThrows(BadRequestException.class, () -> extractor.extractResource(requestContext));
        queryParameters.putSingle("repo", "owner/repo");
        queryParameters.putSingle("scm", "unsupported");
        assertThrows(BadRequestException.class, () -> extractor.extractResource(requestContext));
    }

    private ScmRepositoryExtractor extractor(ResourceAuthZInfo.Location location) {
        return new ScmRepositoryExtractor(serviceContext, location);
    }

    private MultivaluedMap<String, String> parameters(ResourceAuthZInfo.Location location) {
        return location == ResourceAuthZInfo.Location.PATH ? pathParameters : queryParameters;
    }
}
