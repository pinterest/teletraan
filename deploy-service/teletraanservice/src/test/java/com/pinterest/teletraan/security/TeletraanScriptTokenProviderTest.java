/**
 * Copyright (c) 2024 Pinterest, Inc.
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.TeletraanPrincipalRole;
import com.pinterest.deployservice.bean.TokenRolesBean;
import com.pinterest.deployservice.dao.TokenRolesDAO;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import com.pinterest.teletraan.universal.security.bean.ScriptTokenPrincipal;
import com.pinterest.teletraan.universal.security.bean.ValueBasedRole;
import java.sql.SQLException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TeletraanScriptTokenProviderTest {
    private static final String GOOD_TOKEN = "goodToken";
    private ServiceContext context;
    private TokenRolesDAO tokenRolesDAO;
    private TeletraanScriptTokenProvider sut;
    private TokenRolesBean tokenRolesBean;

    @BeforeEach
    void setUp() throws Exception {
        context = new ServiceContext();
        tokenRolesDAO = mock(TokenRolesDAO.class);
        context.setTokenRolesDAO(tokenRolesDAO);
        sut = new TeletraanScriptTokenProvider(context);
        tokenRolesBean = new TokenRolesBean();
        tokenRolesBean.setScript_name("scriptName");
        tokenRolesBean.setResource_id("resourceId");
        tokenRolesBean.setRole(TeletraanPrincipalRole.ADMIN);
        tokenRolesBean.setResource_type(AuthZResource.Type.SYSTEM);

        when(tokenRolesDAO.getByToken(GOOD_TOKEN)).thenReturn(tokenRolesBean);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "badToken"})
    void testGetPrincipal_invalidToken(String token) {
        Optional<?> principal = sut.getPrincipal(token);
        assertFalse(principal.isPresent());
    }

    @Test
    void testGetPrincipal_validToken() {
        ScriptTokenPrincipal<ValueBasedRole> principal = sut.getPrincipal(GOOD_TOKEN).get();
        assertEquals(TeletraanPrincipalRole.ADMIN.getRole(), principal.getRole());
        assertEquals(AuthZResource.Type.SYSTEM, principal.getResource().getType());
        assertEquals(tokenRolesBean.getResource_id(), principal.getResource().getName());
        assertEquals(tokenRolesBean.getScript_name(), principal.getName());
    }

    @Test
    void testGetPrincipal_tokenRolesDAOException() throws Exception {
        String token = "exceptionToken";
        when(tokenRolesDAO.getByToken(token)).thenThrow(SQLException.class);
        Optional<?> principal = sut.getPrincipal(token);
        assertFalse(principal.isPresent());
    }
}
