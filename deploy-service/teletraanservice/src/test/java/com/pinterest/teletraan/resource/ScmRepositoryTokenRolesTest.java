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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.pinterest.deployservice.bean.CommitBean;
import com.pinterest.deployservice.bean.TeletraanPrincipalRole;
import com.pinterest.deployservice.bean.TokenRolesBean;
import com.pinterest.deployservice.dao.TokenRolesDAO;
import com.pinterest.deployservice.scm.GithubManager;
import com.pinterest.deployservice.scm.SourceControlManager;
import com.pinterest.deployservice.scm.SourceControlManagerProxy;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.security.ScriptTokenRoleAuthorizer;
import com.pinterest.teletraan.security.TeletraanAuthZResourceExtractorFactory;
import com.pinterest.teletraan.security.TeletraanScriptTokenProvider;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfoFeature;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import com.pinterest.teletraan.universal.security.bean.ScriptTokenPrincipal;
import com.pinterest.teletraan.universal.security.bean.ValueBasedRole;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(DropwizardExtensionsSupport.class)
class ScmRepositoryTokenRolesTest {
    private static final String TOKENS_PATH = "/v1/scm_repositories/token_roles";
    private static final AuthZResource REPOSITORY =
            new AuthZResource("Github/acme/allowed", AuthZResource.Type.SCM_REPOSITORY);
    private static final GithubManager MANAGER = mock(GithubManager.class);
    private static final TokenRolesDAO TOKEN_DAO = mock(TokenRolesDAO.class);
    private static final Map<String, TokenRolesBean> STORED_TOKENS = new ConcurrentHashMap<>();
    private static final ResourceExtension EXT = createExtension();

    private static ResourceExtension createExtension() {
        TeletraanServiceContext context = new TeletraanServiceContext();
        context.setTokenRolesDAO(TOKEN_DAO);
        Map<String, SourceControlManager> managers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        managers.put("Github", MANAGER);
        managers.put("foo/bar", MANAGER);
        managers.put("foo%2Fbar", MANAGER);
        context.setSourceControlManagerProxy(new SourceControlManagerProxy(managers, "Github"));
        TeletraanAuthZResourceExtractorFactory extractorFactory =
                new TeletraanAuthZResourceExtractorFactory(context);
        TeletraanScriptTokenProvider tokenProvider =
                new TeletraanScriptTokenProvider(context, false);
        Map<String, ScriptTokenPrincipal<ValueBasedRole>> administrators = new HashMap<>();
        administrators.put("repo-manager", principal(TeletraanPrincipalRole.ADMIN, REPOSITORY));
        administrators.put("repo-reader", principal(TeletraanPrincipalRole.READ, REPOSITORY));
        administrators.put(
                "system-admin",
                principal(TeletraanPrincipalRole.ADMIN, AuthZResource.SYSTEM_RESOURCE));
        administrators.put(
                "system-reader",
                principal(TeletraanPrincipalRole.READ, AuthZResource.SYSTEM_RESOURCE));
        administrators.put(
                "env-admin",
                principal(
                        TeletraanPrincipalRole.ADMIN,
                        new AuthZResource("owned-env", AuthZResource.Type.ENV)));
        administrators.put(
                "env-reader",
                principal(
                        TeletraanPrincipalRole.READ,
                        new AuthZResource("owned-env", AuthZResource.Type.ENV)));
        try {
            return ResourceExtension.builder()
                    .addProvider(
                            new AuthDynamicFeature(
                                    new OAuthCredentialAuthFilter.Builder<
                                                    ScriptTokenPrincipal<ValueBasedRole>>()
                                            .setAuthenticator(
                                                    token -> {
                                                        ScriptTokenPrincipal<ValueBasedRole>
                                                                administrator =
                                                                        administrators.get(token);
                                                        return administrator == null
                                                                ? tokenProvider.getPrincipal(token)
                                                                : Optional.of(administrator);
                                                    })
                                            .setAuthorizer(
                                                    new ScriptTokenRoleAuthorizer(extractorFactory))
                                            .setPrefix("token")
                                            .buildAuthFilter()))
                    .addProvider(RolesAllowedDynamicFeature.class)
                    .addResource(ResourceAuthZInfoFeature.class)
                    .addResource(new ScmRepositoryTokenRoles(context))
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
    void setUp() throws Exception {
        STORED_TOKENS.clear();
        reset(TOKEN_DAO, MANAGER);
        when(MANAGER.getCommit(anyString(), anyString())).thenReturn(new CommitBean());
        when(MANAGER.getCommits(anyString(), any(), any(), anyInt(), anyString()))
                .thenReturn(Collections.emptyList());
        doAnswer(
                        call -> {
                            TokenRolesBean token = call.getArgument(0);
                            STORED_TOKENS.put(token.getToken(), token);
                            return null;
                        })
                .when(TOKEN_DAO)
                .insert(any(TokenRolesBean.class));
        when(TOKEN_DAO.getByToken(anyString()))
                .thenAnswer(call -> STORED_TOKENS.get(call.getArgument(0)));
        when(TOKEN_DAO.getByResource(anyString(), any()))
                .thenAnswer(call -> matchingTokens(null, call.getArgument(0), call.getArgument(1)));
        when(TOKEN_DAO.getByNameAndResource(anyString(), anyString(), any()))
                .thenAnswer(
                        call ->
                                matchingTokens(
                                                call.getArgument(0),
                                                call.getArgument(1),
                                                call.getArgument(2))
                                        .stream()
                                        .findFirst()
                                        .orElse(null));
        doAnswer(
                        call -> {
                            List<TokenRolesBean> matches =
                                    matchingTokens(
                                            call.getArgument(0),
                                            call.getArgument(1),
                                            call.getArgument(2));
                            matches.forEach(token -> STORED_TOKENS.remove(token.getToken()));
                            return null;
                        })
                .when(TOKEN_DAO)
                .delete(anyString(), anyString(), any());
    }

    private static List<TokenRolesBean> matchingTokens(
            String name, String resource, AuthZResource.Type type) {
        return STORED_TOKENS.values().stream()
                .filter(token -> name == null || name.equals(token.getScript_name()))
                .filter(
                        token ->
                                resource.equals(token.getResource_id())
                                        && type == token.getResource_type())
                .collect(Collectors.toList());
    }

    @ParameterizedTest
    @ValueSource(strings = {"repo-manager", "system-admin"})
    void administratorCreatesReadOnlyTokenUsableOnlyForItsRepository(String administrator)
            throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "commit-reader");
        body.put("role", "ADMIN");
        body.put("resource", "*");
        body.put("type", "SYSTEM");
        body.put("token", "attacker-chosen-secret");
        body.put("expireDate", Long.MAX_VALUE);
        long before = Instant.now().toEpochMilli();
        JsonNode created;
        URI location;
        try (Response response =
                request(tokens(null, "ACME/ALLOWED"), administrator).post(Entity.json(body))) {
            assertEquals(201, response.getStatus());
            created = response.readEntity(JsonNode.class);
            location = response.getLocation();
        }
        String secret = created.get("token").asText();
        assertFalse(secret.isEmpty());
        assertNotEquals("attacker-chosen-secret", secret);
        assertEquals("READ", created.get("role").asText());
        assertEquals("Github/acme/allowed", created.get("resource").asText());
        assertEquals("SCM_REPOSITORY", created.get("type").asText());
        assertTrue(
                created.get("expireDate").asLong()
                        >= before + ChronoUnit.DAYS.getDuration().toMillis() * 180);
        assertTrue(
                created.get("expireDate").asLong()
                        <= Instant.now().plus(180, ChronoUnit.DAYS).toEpochMilli());
        assertNotNull(location);
        assertTrue(location.getRawQuery().contains("repo="));

        assertStatus(commits("acme/allowed"), secret, 200);
        assertStatus(EXT.target("/v1/commits/Github/acme/allowed/deadbeef"), secret, 200);
        assertStatus(commits("private/secret"), secret, 403);
        verify(MANAGER, never()).getCommit(eq("private/secret"), anyString());
        assertStatus(tokens(null, "acme/allowed"), secret, 403);
        try (Response response =
                request(tokens(null, "acme/allowed"), secret)
                        .post(Entity.json(Collections.singletonMap("name", "child-token")))) {
            assertEquals(403, response.getStatus());
        }
        try (Response response =
                request(tokens("commit-reader", "acme/allowed"), secret).delete()) {
            assertEquals(403, response.getStatus());
        }
        assertEquals(1, STORED_TOKENS.size());
    }

    @Test
    void metadataRedactsSecretAndRevocationUsesTheExactRepository() throws Exception {
        String allowed = createToken("same-name", "acme/allowed", "repo-manager");
        String other = createToken("same-name", "acme/other", "system-admin");
        try (Response response = request(tokens(null, "acme/allowed"), "repo-manager").get()) {
            assertEquals(200, response.getStatus());
            JsonNode metadata = response.readEntity(JsonNode.class);
            assertEquals(1, metadata.size());
            assertEquals("same-name", metadata.get(0).get("name").asText());
            assertFalse(metadata.get(0).has("token"));
        }
        try (Response response =
                request(tokens("same-name", "acme/allowed"), "repo-manager").get()) {
            assertEquals(200, response.getStatus());
            assertFalse(response.readEntity(JsonNode.class).has("token"));
        }
        try (Response response =
                request(tokens("same-name", "acme/other"), "repo-manager").delete()) {
            assertEquals(403, response.getStatus());
        }
        try (Response response =
                request(tokens("same-name", "acme/allowed"), "repo-manager").delete()) {
            assertEquals(204, response.getStatus());
        }
        assertStatus(commits("acme/allowed"), allowed, 401);
        assertStatus(commits("acme/other"), other, 200);
        verifyNoRoleUpdates();
    }

    @Test
    void repositoryTokenExpiryIsEnforcedWhenLegacyExpiryCheckingIsDisabled() {
        String secret = createToken("expires", "acme/allowed", "repo-manager");
        assertStatus(commits("acme/allowed"), secret, 200);
        STORED_TOKENS.get(secret).setExpire_date(Instant.now().minusSeconds(1).toEpochMilli());
        assertStatus(commits("acme/allowed"), secret, 401);
        STORED_TOKENS.get(secret).setExpire_date(null);
        assertStatus(commits("acme/allowed"), secret, 401);
    }

    @ParameterizedTest
    @CsvSource({"foo/bar,foo%2Fbar/acme/allowed", "foo%2Fbar,foo%252Fbar/acme/allowed"})
    void locationPreservesTheExactConfiguredScmAlias(String scm, String expectedScope)
            throws Exception {
        URI location;
        WebTarget createTarget =
                EXT.target(TOKENS_PATH)
                        .queryParam("repo", "acme/allowed")
                        .queryParam("scm", scm.replace("%", "%25"));
        try (Response response =
                request(createTarget, "system-admin")
                        .post(Entity.json(Collections.singletonMap("name", "alias-reader")))) {
            assertEquals(201, response.getStatus());
            JsonNode created = response.readEntity(JsonNode.class);
            assertEquals(expectedScope, created.get("resource").asText());
            String secret = created.get("token").asText();
            assertEquals(expectedScope, STORED_TOKENS.get(secret).getResource_id());
            location = response.getLocation();
        }

        assertNotNull(location);
        try (Response response = request(EXT.client().target(location), "system-admin").get()) {
            assertEquals(200, response.getStatus());
            JsonNode metadata = response.readEntity(JsonNode.class);
            assertEquals("alias-reader", metadata.get("name").asText());
            assertEquals(expectedScope, metadata.get("resource").asText());
            assertFalse(metadata.has("token"));
        }
        verify(TOKEN_DAO)
                .getByNameAndResource(
                        "alias-reader", expectedScope, AuthZResource.Type.SCM_REPOSITORY);
    }

    @ParameterizedTest
    @ValueSource(strings = {"repo-reader", "system-reader", "env-admin", "env-reader"})
    void nonManagersCannotReadOrChangeTokenMetadata(String credential) throws Exception {
        try (Response response =
                request(tokens(null, "acme/allowed"), credential)
                        .post(Entity.json(Collections.singletonMap("name", "unauthorized")))) {
            assertEquals(403, response.getStatus());
        }
        assertStatus(tokens(null, "acme/allowed"), credential, 403);
        assertStatus(tokens("unauthorized", "acme/allowed"), credential, 403);
        try (Response response =
                request(tokens("unauthorized", "acme/allowed"), credential).delete()) {
            assertEquals(403, response.getStatus());
        }
        verify(TOKEN_DAO, never()).insert(any());
        verify(TOKEN_DAO, never()).getByResource(anyString(), any());
        verify(TOKEN_DAO, never()).getByNameAndResource(anyString(), anyString(), any());
        verify(TOKEN_DAO, never()).delete(anyString(), anyString(), any());
    }

    @Test
    void namesAreValidatedAndNoUpdateEndpointCanPromoteAReadToken() throws Exception {
        for (String name :
                new String[] {
                    "", "../global", "with spaces", String.join("", Collections.nCopies(65, "x"))
                }) {
            try (Response response =
                    request(tokens(null, "acme/allowed"), "repo-manager")
                            .post(Entity.json(Collections.singletonMap("name", name)))) {
                assertEquals(400, response.getStatus());
            }
        }
        String secret = createToken("read-only", "acme/allowed", "repo-manager");
        try (Response response =
                request(tokens("read-only", "acme/allowed"), "repo-manager")
                        .put(Entity.json(Collections.singletonMap("role", "ADMIN")))) {
            assertEquals(405, response.getStatus());
        }
        assertEquals(TeletraanPrincipalRole.READ, STORED_TOKENS.get(secret).getRole());
        verifyNoRoleUpdates();
    }

    private void verifyNoRoleUpdates() throws Exception {
        verify(TOKEN_DAO, never()).updateRole(anyString(), anyString(), any(), any());
    }

    private String createToken(String name, String repository, String administrator) {
        try (Response response =
                request(tokens(null, repository), administrator)
                        .post(Entity.json(Collections.singletonMap("name", name)))) {
            assertEquals(201, response.getStatus());
            return response.readEntity(JsonNode.class).get("token").asText();
        }
    }

    private WebTarget tokens(String name, String repository) {
        return EXT.target(TOKENS_PATH + (name == null ? "" : "/" + name))
                .queryParam("scm", "gItHuB")
                .queryParam("repo", repository);
    }

    private WebTarget commits(String repository) {
        return EXT.target("/v1/commits")
                .queryParam("repo", repository)
                .queryParam("startSha", "main");
    }

    private Invocation.Builder request(WebTarget target, String credential) {
        return target.request().header("Authorization", "token " + credential);
    }

    private void assertStatus(WebTarget target, String credential, int expected) {
        try (Response response = request(target, credential).get()) {
            assertEquals(expected, response.getStatus());
        }
    }
}
