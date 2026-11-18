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
import com.pinterest.deployservice.bean.TokenRolesBean;
import com.pinterest.deployservice.dao.BuildDAO;
import com.pinterest.deployservice.dao.TokenRolesDAO;
import com.pinterest.deployservice.scm.GithubManager;
import com.pinterest.deployservice.scm.SourceControlManager;
import com.pinterest.deployservice.scm.SourceControlManagerProxy;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.config.RoleAuthorizationFactory;
import com.pinterest.teletraan.security.TeletraanScriptTokenProvider;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfoFeature;
import com.pinterest.teletraan.universal.security.ScriptTokenAuthenticator;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import com.pinterest.teletraan.universal.security.bean.ScriptTokenPrincipal;
import com.pinterest.teletraan.universal.security.bean.ValueBasedRole;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.Authorizer;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
    private static final BuildDAO BUILD_DAO = mock(BuildDAO.class);
    private static final TokenRolesDAO TOKEN_DAO = mock(TokenRolesDAO.class);
    private static final ResourceExtension EXT = createExtension();

    @SuppressWarnings("unchecked")
    private static ResourceExtension createExtension() {
        TeletraanServiceContext context = new TeletraanServiceContext();
        context.setBuildDAO(BUILD_DAO);
        context.setTokenRolesDAO(TOKEN_DAO);
        Map<String, SourceControlManager> managers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        managers.put("Github", MANAGER);
        managers.put("Github-Mirror", MANAGER);
        context.setSourceControlManagerProxy(new SourceControlManagerProxy(managers, "Github"));
        context.setAuthZResourceExtractorFactory(mock(AuthZResourceExtractor.Factory.class));
        Authorizer<ScriptTokenPrincipal<ValueBasedRole>> authorizer =
                (Authorizer<ScriptTokenPrincipal<ValueBasedRole>>)
                        (Authorizer<?>)
                                new RoleAuthorizationFactory()
                                        .create(context, ScriptTokenPrincipal.class);
        try {
            return ResourceExtension.builder()
                    .addProvider(
                            new AuthDynamicFeature(
                                    new OAuthCredentialAuthFilter.Builder<
                                                    ScriptTokenPrincipal<ValueBasedRole>>()
                                            .setAuthenticator(
                                                    new ScriptTokenAuthenticator<>(
                                                            new TeletraanScriptTokenProvider(
                                                                    context, false)))
                                            .setAuthorizer(authorizer)
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

    @BeforeEach
    void resetFixtures() throws Exception {
        reset(MANAGER, BUILD_DAO, TOKEN_DAO);
        when(MANAGER.getCommit(anyString(), anyString())).thenReturn(new CommitBean());
        when(MANAGER.getCommits(anyString(), any(), any(), anyInt(), anyString()))
                .thenReturn(Collections.emptyList());
        when(BUILD_DAO.hasBuildForRepository(anyString(), anyString(), eq(true), eq(true)))
                .thenAnswer(
                        invocation ->
                                "Github".equals(invocation.getArgument(0))
                                        && "acme/repo.name"
                                                .equalsIgnoreCase(invocation.getArgument(1)));
        Map<String, TokenRolesBean> tokens = new HashMap<>();
        tokens.put("read", token(TeletraanPrincipalRole.READ, AuthZResource.Type.SYSTEM, "*"));
        tokens.put("reader", token(TeletraanPrincipalRole.READER, AuthZResource.Type.SYSTEM, "*"));
        tokens.put(
                "publisher",
                token(TeletraanPrincipalRole.PUBLISHER, AuthZResource.Type.SYSTEM, "*"));
        tokens.put(
                "operator", token(TeletraanPrincipalRole.OPERATOR, AuthZResource.Type.SYSTEM, "*"));
        tokens.put(
                "env-admin",
                token(TeletraanPrincipalRole.ADMIN, AuthZResource.Type.ENV, "owned-env"));
        tokens.put(
                "system-admin",
                token(TeletraanPrincipalRole.ADMIN, AuthZResource.Type.SYSTEM, "*"));
        when(TOKEN_DAO.getByToken(anyString()))
                .thenAnswer(invocation -> tokens.get(invocation.getArgument(0)));
    }

    private static TokenRolesBean token(
            TeletraanPrincipalRole role, AuthZResource.Type type, String resourceId) {
        TokenRolesBean bean = new TokenRolesBean();
        bean.setScript_name("test");
        bean.setRole(role);
        bean.setResource_type(type);
        bean.setResource_id(resourceId);
        return bean;
    }

    private WebTarget commits(String repo) {
        return EXT.target("/v1/commits")
                .queryParam("repo", repo.replace("%", "%25"))
                .queryParam("startSha", "deadbeef");
    }

    private WebTarget commit(String scm, String repo, String ref) {
        return EXT.target("/v1/commits/" + scm + "/" + repo + "/" + ref);
    }

    private void assertStatus(WebTarget target, String token, int status) {
        try (Response response = target.request().header("Authorization", "token " + token).get()) {
            assertEquals(status, response.getStatus());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"read", "reader", "publisher", "operator", "env-admin", "system-admin"})
    void existingReadRolesCanReadRepositoriesWithBuilds(String token) throws Exception {
        assertStatus(commits("acme/repo.name"), token, 200);
        assertStatus(commit("Github", "acme/repo.name", "deadbeef"), token, 200);
        verify(MANAGER).getCommits("acme/repo.name", "deadbeef", null, 100, "");
        verify(MANAGER).getCommit("acme/repo.name", "deadbeef");
    }

    @ParameterizedTest
    @ValueSource(strings = {"read", "system-admin"})
    void repositoryWithoutBuildsIsDeniedEvenToSystemAdministrators(String token) {
        assertStatus(commits("victim-private-org/secret-repo"), token, 403);
        assertStatus(commit("Github", "victim-private-org/secret-repo", "deadbeef"), token, 403);
        verifyNoInteractions(MANAGER);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Github", "github", "gItHuB"})
    void scmAndGithubRepositoryCasingResolveToTheSameBuiltRepository(String scm) {
        assertStatus(commits("ACME/REPO.NAME").queryParam("scm", scm), "read", 200);
        assertStatus(commit(scm, "ACME/REPO.NAME", "deadbeef"), "read", 200);
    }

    @Test
    void emptyScmUsesTheConfiguredDefault() {
        assertStatus(commits("acme/repo.name").queryParam("scm", ""), "read", 200);
    }

    @Test
    void configuredAliasNeedsItsOwnStoredBuildEvenWithTheSameManager() throws Exception {
        assertStatus(commits("acme/repo.name").queryParam("scm", "gItHuB-mIrRoR"), "read", 403);
        assertStatus(commit("GITHUB-MIRROR", "acme/repo.name", "deadbeef"), "read", 403);
        verifyNoInteractions(MANAGER);
        when(BUILD_DAO.hasBuildForRepository("Github-Mirror", "acme/repo.name", true, true))
                .thenReturn(true);
        assertStatus(commits("acme/repo.name").queryParam("scm", "gItHuB-mIrRoR"), "read", 200);
        assertStatus(commit("GITHUB-MIRROR", "acme/repo.name", "deadbeef"), "read", 200);
    }

    @Test
    void builtRepositoryCanReadBranchRefsAndCommitsWithoutTheirOwnBuilds() throws Exception {
        assertStatus(
                EXT.target("/v1/commits")
                        .queryParam("repo", "acme/repo.name")
                        .queryParam("startSha", "feature/hotfix.ref")
                        .queryParam("endSha", "refs/tags/v1.2"),
                "read",
                200);
        assertStatus(commit("Github", "acme/repo.name", "main"), "read", 200);
        verify(MANAGER)
                .getCommits("acme/repo.name", "feature/hotfix.ref", "refs/tags/v1.2", 100, "");
        verify(MANAGER).getCommit("acme/repo.name", "main");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "victim/x/../../other",
                "x/../../orgs/VICTIMORG/repos",
                "acme/repo.name?per_page=100",
                "acme/repo.name#fragment",
                "acme%2Frepo.name",
                "acme/..",
                "acme/repo.name/extra",
                ""
            })
    void malformedRepositoryIsRejectedBeforeScmCall(String repo) {
        assertStatus(commits(repo), "read", 400);
        verifyNoInteractions(MANAGER);
    }

    @Test
    void missingAmbiguousOrUnknownRepositoryIdentityIsRejected() {
        assertStatus(EXT.target("/v1/commits"), "read", 400);
        assertStatus(commits("acme/repo.name").queryParam("repo", "acme/other"), "read", 400);
        assertStatus(
                commits("acme/repo.name").queryParam("scm", "Github", "Github-Mirror"),
                "read",
                400);
        assertStatus(commits("acme/repo.name").queryParam("scm", "unknown"), "read", 400);
        verifyNoInteractions(MANAGER);
    }

    @Test
    void anonymousAndUnknownTokensCannotReadCommits() {
        WebTarget[] targets = {
            commits("acme/repo.name"), commit("Github", "acme/repo.name", "deadbeef")
        };
        for (WebTarget target : targets) {
            try (Response response = target.request().get()) {
                assertEquals(401, response.getStatus());
            }
            assertStatus(target, "unknown", 401);
        }
        verifyNoInteractions(BUILD_DAO, MANAGER);
    }
}
