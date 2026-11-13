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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.pinterest.deployservice.bean.CommitBean;
import com.pinterest.deployservice.bean.TeletraanPrincipalRole;
import com.pinterest.deployservice.scm.GithubManager;
import com.pinterest.deployservice.scm.SourceControlManager;
import com.pinterest.deployservice.scm.SourceControlManagerProxy;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.security.ScriptTokenRoleAuthorizer;
import com.pinterest.teletraan.security.TeletraanAuthZResourceExtractorFactory;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfoFeature;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import com.pinterest.teletraan.universal.security.bean.ScriptTokenPrincipal;
import com.pinterest.teletraan.universal.security.bean.ValueBasedRole;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(DropwizardExtensionsSupport.class)
class CommitsAuthorizationTest {
    private static final GithubManager MANAGER = mock(GithubManager.class);
    private static final ResourceExtension EXT = createExtension();

    private static ResourceExtension createExtension() {
        TeletraanServiceContext context = new TeletraanServiceContext();
        Map<String, SourceControlManager> managers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        managers.put("GitHub", MANAGER);
        managers.put("GitHub-Mirror", MANAGER);
        context.setSourceControlManagerProxy(new SourceControlManagerProxy(managers, "github"));
        TeletraanAuthZResourceExtractorFactory extractorFactory =
                new TeletraanAuthZResourceExtractorFactory(context);
        Map<String, ScriptTokenPrincipal<ValueBasedRole>> principals = new HashMap<>();
        principals.put(
                "reader", principal(TeletraanPrincipalRole.READ, AuthZResource.SYSTEM_RESOURCE));
        principals.put(
                "publisher",
                principal(TeletraanPrincipalRole.PUBLISHER, AuthZResource.SYSTEM_RESOURCE));
        principals.put(
                "admin", principal(TeletraanPrincipalRole.ADMIN, AuthZResource.SYSTEM_RESOURCE));
        principals.put(
                "repo-reader",
                principal(
                        TeletraanPrincipalRole.READ,
                        new AuthZResource(
                                "GitHub/acme/allowed", AuthZResource.Type.SCM_REPOSITORY)));
        try {
            return ResourceExtension.builder()
                    .addProvider(
                            new AuthDynamicFeature(
                                    new OAuthCredentialAuthFilter.Builder<
                                                    ScriptTokenPrincipal<ValueBasedRole>>()
                                            .setAuthenticator(
                                                    token ->
                                                            Optional.ofNullable(
                                                                    principals.get(token)))
                                            .setAuthorizer(
                                                    new ScriptTokenRoleAuthorizer(extractorFactory))
                                            .setPrefix("token")
                                            .buildAuthFilter()))
                    .addProvider(RolesAllowedDynamicFeature.class)
                    .addResource(ResourceAuthZInfoFeature.class)
                    .addResource(new Commits(context))
                    .build();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static ScriptTokenPrincipal<ValueBasedRole> principal(
            TeletraanPrincipalRole role, AuthZResource resource) {
        return new ScriptTokenPrincipal<>("test", role.getRole(), resource);
    }

    @BeforeEach
    void resetManager() throws Exception {
        reset(MANAGER);
        when(MANAGER.getCommit(anyString(), anyString())).thenReturn(new CommitBean());
        when(MANAGER.getCommits(anyString(), any(), any(), anyInt(), anyString()))
                .thenReturn(Collections.emptyList());
    }

    private WebTarget commits(String repo) {
        return EXT.target("/v1/commits")
                // Jersey preserves existing percent escapes; send the literal test value.
                .queryParam("repo", repo.replace("%", "%25"))
                .queryParam("startSha", "deadbeef");
    }

    private void assertStatus(WebTarget target, String token, int status) {
        try (Response response = target.request().header("Authorization", "token " + token).get()) {
            assertEquals(status, response.getStatus());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"reader", "publisher"})
    void baselineSystemRolesCannotReadAnyRepository(String token) {
        assertStatus(commits("acme/allowed"), token, 403);
        assertStatus(commits("victim-private-org/secret-repo"), token, 403);
        assertStatus(EXT.target("/v1/commits/github/acme/allowed/deadbeef"), token, 403);
        verifyNoInteractions(MANAGER);
    }

    @Test
    void repositoryGrantAllowsListAndSingleCommit() throws Exception {
        assertStatus(commits("acme/allowed"), "repo-reader", 200);
        assertStatus(EXT.target("/v1/commits/github/acme/allowed/deadbeef"), "repo-reader", 200);
        verify(MANAGER).getCommits("acme/allowed", "deadbeef", null, 100, "");
        verify(MANAGER).getCommit("acme/allowed", "deadbeef");
    }

    @Test
    void repositoryGrantCannotReadAnotherRepository() {
        assertStatus(commits("victim-private-org/secret-repo"), "repo-reader", 403);
        assertStatus(
                EXT.target("/v1/commits/github/victim-private-org/secret-repo/deadbeef"),
                "repo-reader",
                403);
        verifyNoInteractions(MANAGER);
    }

    @Test
    void repositoryIdentityUsesDefaultScmAndIgnoresGithubCase() {
        assertStatus(commits("ACME/ALLOWED"), "repo-reader", 200);
        assertStatus(commits("acme/allowed").queryParam("scm", ""), "repo-reader", 200);
    }

    @ParameterizedTest
    @ValueSource(strings = {"github", "GITHUB", "gItHuB"})
    void configuredScmPermissionAcceptsCallerCasingForBothEndpoints(String scm) throws Exception {
        assertStatus(commits("acme/allowed").queryParam("scm", scm), "repo-reader", 200);
        assertStatus(
                EXT.target("/v1/commits/" + scm + "/acme/allowed/deadbeef"), "repo-reader", 200);
        verify(MANAGER).getCommits("acme/allowed", "deadbeef", null, 100, "");
        verify(MANAGER).getCommit("acme/allowed", "deadbeef");
    }

    @Test
    void distinctConfiguredAliasRequiresItsOwnPermissionEvenWithTheSameManager() {
        assertStatus(
                commits("acme/allowed").queryParam("scm", "gItHuB-mIrRoR"), "repo-reader", 403);
        assertStatus(
                EXT.target("/v1/commits/GITHUB-MIRROR/acme/allowed/deadbeef"), "repo-reader", 403);
        verifyNoInteractions(MANAGER);
    }

    @Test
    void systemAdministratorCanReadRepositoryWithDots() {
        assertStatus(commits("acme/repo.name"), "admin", 200);
        assertStatus(EXT.target("/v1/commits/github/acme/repo.name/deadbeef"), "admin", 200);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "victim/x/../../other",
                "x/../../orgs/VICTIMORG/repos",
                "acme/allowed?per_page=100",
                "acme/allowed#fragment",
                "acme%2Fallowed",
                "acme/..",
                "acme/allowed/extra",
                ""
            })
    void malformedRepositoryIsRejectedBeforeScmCall(String repo) {
        assertStatus(commits(repo), "admin", 400);
        verifyNoInteractions(MANAGER);
    }

    @Test
    void missingOrAmbiguousRepositoryIsRejected() {
        assertStatus(EXT.target("/v1/commits"), "admin", 400);
        assertStatus(commits("acme/allowed").queryParam("repo", "acme/other"), "admin", 400);
        assertStatus(commits("acme/allowed").queryParam("scm", "github", "other"), "admin", 400);
        verifyNoInteractions(MANAGER);
    }

    @Test
    void unauthenticatedRequestCannotReadRepository() {
        assertStatus(commits("acme/allowed"), "unknown", 401);
        verifyNoInteractions(MANAGER);
    }
}
