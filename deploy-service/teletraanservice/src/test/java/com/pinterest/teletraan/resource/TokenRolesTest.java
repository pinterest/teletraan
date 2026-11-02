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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pinterest.deployservice.bean.TeletraanPrincipalRole;
import com.pinterest.deployservice.bean.TokenRolesBean;
import com.pinterest.deployservice.dao.TokenRolesDAO;
import com.pinterest.deployservice.db.DBTokenRolesDAOImpl;
import com.pinterest.deployservice.db.DBUtils;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.security.ScriptTokenRoleAuthorizer;
import com.pinterest.teletraan.security.TeletraanScriptTokenProvider;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import com.pinterest.teletraan.universal.security.bean.ScriptTokenPrincipal;
import com.pinterest.teletraan.universal.security.bean.ValueBasedRole;
import javax.ws.rs.BadRequestException;
import org.apache.commons.dbcp.BasicDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Regression coverage for BUG-316353 using the resource, DAO, and script-token authorization. */
class TokenRolesTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SCRIPT_NAME = "owned-script";
    private static final String OWNED_ENV = "owned-env";
    private static final String OTHER_ENV = "other-env";
    private static final String ORIGINAL_TOKEN = "original-bearer";
    private static final String ATTACK_TOKEN = "bearer-promoted";
    private static final long ORIGINAL_EXPIRY = 2_000_000_000_000L;
    private static BasicDataSource dataSource;

    private TokenRolesDAO tokenRolesDAO;
    private EnvTokenRoles resource;
    private SystemTokenRoles systemResource;
    private TeletraanScriptTokenProvider tokenProvider;

    @BeforeAll
    static void setUpDatabase() throws Exception {
        dataSource = DBUtils.createTestDataSource();
    }

    @BeforeEach
    void setUp() throws Exception {
        tokenRolesDAO = new DBTokenRolesDAOImpl(dataSource);
        TeletraanServiceContext context = new TeletraanServiceContext();
        context.setTokenRolesDAO(tokenRolesDAO);
        resource = new EnvTokenRoles(context);
        systemResource = new SystemTokenRoles(context);
        tokenProvider = new TeletraanScriptTokenProvider(context, true);
        insertToken(SCRIPT_NAME, OWNED_ENV, AuthZResource.Type.ENV, ORIGINAL_TOKEN);
        insertToken("another-script", OWNED_ENV, AuthZResource.Type.ENV, "another-script-bearer");
        insertToken(SCRIPT_NAME, OTHER_ENV, AuthZResource.Type.ENV, "other-env-bearer");
        insertToken(SCRIPT_NAME, AuthZResource.ALL, AuthZResource.Type.SYSTEM, "system-bearer");
    }

    @AfterEach
    void tearDown() throws Exception {
        DBUtils.truncateAllTables(dataSource);
    }

    @Test
    void updateCannotPromoteEnvironmentTokenToSystemAdmin() throws Exception {
        assertUpdateKeepsOriginalScope(AuthZResource.ALL, "SYSTEM");
    }

    @Test
    void updateCannotMoveTokenToAnotherEnvironment() throws Exception {
        assertUpdateKeepsOriginalScope(OTHER_ENV, "ENV");
    }

    @ParameterizedTest
    @EnumSource(
            value = TeletraanPrincipalRole.class,
            names = {"OPERATOR", "ADMIN"})
    void roleOnlyUpdatesPreserveTokenIdentityAndExpiry(TeletraanPrincipalRole role)
            throws Exception {
        TokenRolesBean body =
                MAPPER.readValue("{\"role\":\"" + role.name() + "\"}", TokenRolesBean.class);

        resource.update(OWNED_ENV, SCRIPT_NAME, body);

        assertOriginalToken(role);
        assertOtherTokensUnchanged();
    }

    @Test
    void systemUpdatesAlsoPreserveTokenIdentityAndExpiry() throws Exception {
        TokenRolesBean body =
                MAPPER.readValue(
                        "{\"name\":\"renamed-script\",\"resource\":\"other-env\",\"type\":\"ENV\","
                                + "\"token\":\"bearer-promoted\",\"role\":\"ADMIN\","
                                + "\"expireDate\":4102444800000}",
                        TokenRolesBean.class);

        systemResource.update(SCRIPT_NAME, body);

        TokenRolesBean stored = tokenRolesDAO.getByToken("system-bearer");
        assertNotNull(stored);
        assertEquals(SCRIPT_NAME, stored.getScript_name());
        assertEquals(AuthZResource.ALL, stored.getResource_id());
        assertEquals(AuthZResource.Type.SYSTEM, stored.getResource_type());
        assertEquals(ORIGINAL_EXPIRY, stored.getExpire_date());
        assertEquals(TeletraanPrincipalRole.ADMIN, stored.getRole());
        assertNull(tokenRolesDAO.getByToken(ATTACK_TOKEN));
        assertOriginalToken(TeletraanPrincipalRole.OPERATOR);
        assertEquals(
                TeletraanPrincipalRole.OPERATOR,
                tokenRolesDAO.getByToken("other-env-bearer").getRole());
    }

    @Test
    void updateRequiresBody() throws Exception {
        BadRequestException error =
                assertThrows(
                        BadRequestException.class,
                        () -> resource.update(OWNED_ENV, SCRIPT_NAME, null));

        assertEquals(400, error.getResponse().getStatus());
        assertOriginalToken(TeletraanPrincipalRole.OPERATOR);
    }

    @Test
    void updateRequiresRole() throws Exception {
        TokenRolesBean body = MAPPER.readValue("{}", TokenRolesBean.class);
        BadRequestException error =
                assertThrows(
                        BadRequestException.class,
                        () -> resource.update(OWNED_ENV, SCRIPT_NAME, body));

        assertEquals(400, error.getResponse().getStatus());
        assertOriginalToken(TeletraanPrincipalRole.OPERATOR);
    }

    private void assertUpdateKeepsOriginalScope(String targetResource, String targetType)
            throws Exception {
        String json =
                "{\"name\":\"global-script\",\"resource\":\""
                        + targetResource
                        + "\",\"type\":\""
                        + targetType
                        + "\",\"token\":\""
                        + ATTACK_TOKEN
                        + "\",\"role\":\"ADMIN\",\"expireDate\":4102444800000}";

        resource.update(OWNED_ENV, SCRIPT_NAME, MAPPER.readValue(json, TokenRolesBean.class));

        assertOriginalToken(TeletraanPrincipalRole.ADMIN);
        assertOtherTokensUnchanged();
        assertNull(tokenRolesDAO.getByToken(ATTACK_TOKEN));
        assertNull(
                tokenRolesDAO.getByNameAndResource(
                        "global-script", targetResource, AuthZResource.Type.valueOf(targetType)));
        assertFalse(tokenProvider.getPrincipal(ATTACK_TOKEN).isPresent());

        ScriptTokenPrincipal<ValueBasedRole> principal =
                tokenProvider.getPrincipal(ORIGINAL_TOKEN).get();
        ScriptTokenRoleAuthorizer authorizer = new ScriptTokenRoleAuthorizer(null);
        assertEquals(SCRIPT_NAME, principal.getName());
        assertEquals(new AuthZResource(OWNED_ENV, AuthZResource.Type.ENV), principal.getResource());
        assertTrue(
                authorizer.authorize(
                        principal,
                        TeletraanPrincipalRole.Names.ADMIN,
                        new AuthZResource(OWNED_ENV, AuthZResource.Type.ENV),
                        null));
        assertFalse(
                authorizer.authorize(
                        principal,
                        TeletraanPrincipalRole.Names.ADMIN,
                        AuthZResource.SYSTEM_RESOURCE,
                        null));
        assertFalse(
                authorizer.authorize(
                        principal,
                        TeletraanPrincipalRole.Names.ADMIN,
                        new AuthZResource(OTHER_ENV, AuthZResource.Type.ENV),
                        null));
    }

    private void assertOriginalToken(TeletraanPrincipalRole role) throws Exception {
        TokenRolesBean stored = tokenRolesDAO.getByToken(ORIGINAL_TOKEN);
        assertNotNull(stored);
        assertEquals(SCRIPT_NAME, stored.getScript_name());
        assertEquals(OWNED_ENV, stored.getResource_id());
        assertEquals(AuthZResource.Type.ENV, stored.getResource_type());
        assertEquals(ORIGINAL_TOKEN, stored.getToken());
        assertEquals(ORIGINAL_EXPIRY, stored.getExpire_date());
        assertEquals(role, stored.getRole());
    }

    private void assertOtherTokensUnchanged() throws Exception {
        TokenRolesBean sibling = tokenRolesDAO.getByToken("another-script-bearer");
        assertNotNull(sibling);
        assertEquals("another-script", sibling.getScript_name());
        assertEquals(OWNED_ENV, sibling.getResource_id());
        assertEquals(AuthZResource.Type.ENV, sibling.getResource_type());
        assertEquals(TeletraanPrincipalRole.OPERATOR, sibling.getRole());
        assertEquals(ORIGINAL_EXPIRY, sibling.getExpire_date());

        TokenRolesBean other = tokenRolesDAO.getByToken("other-env-bearer");
        assertNotNull(other);
        assertEquals(SCRIPT_NAME, other.getScript_name());
        assertEquals(OTHER_ENV, other.getResource_id());
        assertEquals(AuthZResource.Type.ENV, other.getResource_type());
        assertEquals(TeletraanPrincipalRole.OPERATOR, other.getRole());
        assertEquals(ORIGINAL_EXPIRY, other.getExpire_date());

        TokenRolesBean system = tokenRolesDAO.getByToken("system-bearer");
        assertNotNull(system);
        assertEquals(SCRIPT_NAME, system.getScript_name());
        assertEquals(AuthZResource.ALL, system.getResource_id());
        assertEquals(AuthZResource.Type.SYSTEM, system.getResource_type());
        assertEquals(TeletraanPrincipalRole.OPERATOR, system.getRole());
        assertEquals(ORIGINAL_EXPIRY, system.getExpire_date());
    }

    private void insertToken(
            String scriptName, String resourceId, AuthZResource.Type type, String token)
            throws Exception {
        TokenRolesBean bean = new TokenRolesBean();
        bean.setScript_name(scriptName);
        bean.setResource_id(resourceId);
        bean.setResource_type(type);
        bean.setToken(token);
        bean.setRole(TeletraanPrincipalRole.OPERATOR);
        bean.setExpire_date(ORIGINAL_EXPIRY);
        tokenRolesDAO.insert(bean);
    }
}
