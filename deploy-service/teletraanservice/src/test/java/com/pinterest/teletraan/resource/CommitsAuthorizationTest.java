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
import com.pinterest.deployservice.dao.TokenRolesDAO;
import com.pinterest.deployservice.scm.GithubManager;
import com.pinterest.deployservice.scm.SourceControlManagerProxy;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.config.RoleAuthorizationFactory;
import com.pinterest.teletraan.security.TeletraanScriptTokenProvider;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor;
import com.pinterest.teletraan.universal.security.BaseAuthorizer;
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
import java.util.concurrent.atomic.AtomicReference;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.ContainerRequestContext;
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
    private static final TokenRolesDAO TOKEN_DAO = mock(TokenRolesDAO.class);
    private static final AtomicReference<String> POLICY_ACTION = new AtomicReference<>();
    private static final AtomicReference<AuthZResource> POLICY_RESOURCE = new AtomicReference<>();
    private static final ResourceExtension EXT = createExtension(false);
    private static final ResourceExtension POLICY_EXT = createExtension(true);

    @SuppressWarnings("unchecked")
    private static ResourceExtension createExtension(boolean recordPolicy) {
        TeletraanServiceContext context = new TeletraanServiceContext();
        context.setTokenRolesDAO(TOKEN_DAO);
        context.setSourceControlManagerProxy(
                new SourceControlManagerProxy(
                        Collections.singletonMap("Github", MANAGER), "Github"));
        context.setAuthZResourceExtractorFactory(mock(AuthZResourceExtractor.Factory.class));
        Authorizer<ScriptTokenPrincipal<ValueBasedRole>> authorizer;
        if (recordPolicy) {
            authorizer =
                    new BaseAuthorizer<ScriptTokenPrincipal<ValueBasedRole>>(
                            context.getAuthZResourceExtractorFactory()) {
                        @Override
                        public boolean authorize(
                                ScriptTokenPrincipal<ValueBasedRole> principal,
                                String action,
                                AuthZResource resource,
                                ContainerRequestContext request) {
                            POLICY_ACTION.set(action);
                            POLICY_RESOURCE.set(resource);
                            return true;
                        }
                    };
        } else {
            authorizer =
                    (Authorizer<ScriptTokenPrincipal<ValueBasedRole>>)
                            (Authorizer<?>)
                                    new RoleAuthorizationFactory()
                                            .create(context, ScriptTokenPrincipal.class);
        }
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
        reset(MANAGER, TOKEN_DAO);
        POLICY_ACTION.set(null);
        POLICY_RESOURCE.set(null);
        when(MANAGER.getCommit(anyString(), anyString())).thenReturn(new CommitBean());
        when(MANAGER.getCommits(anyString(), any(), any(), anyInt(), anyString()))
                .thenReturn(Collections.emptyList());
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

    private WebTarget target(ResourceExtension extension, boolean single) {
        return single
                ? extension.target("/v1/commits/Github/acme/repo/deadbeef")
                : extension
                        .target("/v1/commits")
                        .queryParam("repo", "acme/repo")
                        .queryParam("startSha", "deadbeef");
    }

    private void assertStatus(WebTarget target, String token, int status) {
        try (Response response = target.request().header("Authorization", "token " + token).get()) {
            assertEquals(status, response.getStatus());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"read", "reader", "publisher", "operator", "env-admin"})
    void nonSystemAdministratorsCannotReadCommits(String token) {
        assertStatus(target(EXT, false), token, 403);
        assertStatus(target(EXT, true), token, 403);
        verifyNoInteractions(MANAGER);
    }

    @Test
    void anonymousAndUnknownTokensCannotReadCommits() {
        for (boolean single : new boolean[] {false, true}) {
            try (Response response = target(EXT, single).request().get()) {
                assertEquals(401, response.getStatus());
            }
            assertStatus(target(EXT, single), "unknown", 401);
        }
        verifyNoInteractions(MANAGER);
    }

    @Test
    void systemAdministratorsCanReadBothCommitRoutes() throws Exception {
        assertStatus(target(EXT, false), "system-admin", 200);
        assertStatus(target(EXT, true), "system-admin", 200);
        verify(MANAGER).getCommits("acme/repo", "deadbeef", null, 100, "");
        verify(MANAGER).getCommit("acme/repo", "deadbeef");
    }

    @Test
    void bothRoutesRequestTheExistingSystemAdministrationPolicy() {
        for (boolean single : new boolean[] {false, true}) {
            POLICY_ACTION.set(null);
            POLICY_RESOURCE.set(null);
            assertStatus(target(POLICY_EXT, single), "system-admin", 200);
            assertEquals(TeletraanPrincipalRole.Names.MANAGE_SCRIPT_TOKEN, POLICY_ACTION.get());
            assertEquals(AuthZResource.SYSTEM_RESOURCE, POLICY_RESOURCE.get());
        }
    }
}
