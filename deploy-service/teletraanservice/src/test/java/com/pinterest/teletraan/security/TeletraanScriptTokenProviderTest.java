package com.pinterest.teletraan.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.TeletraanPrincipalRoles;
import com.pinterest.deployservice.bean.TokenRolesBean;
import com.pinterest.deployservice.dao.TokenRolesDAO;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import com.pinterest.teletraan.universal.security.bean.ScriptTokenPrincipal;
import com.pinterest.teletraan.universal.security.bean.ValueBasedRole;

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
        tokenRolesBean.setRole(TeletraanPrincipalRoles.ADMIN);
        tokenRolesBean.setResource_type(AuthZResource.Type.SYSTEM);

        when(tokenRolesDAO.getByToken(GOOD_TOKEN)).thenReturn(tokenRolesBean);
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "badToken" })
    void testGetPrincipal_invalidToken(String token) {
        Optional<?> principal = sut.getPrincipal(token);
        assertFalse(principal.isPresent());
    }

    @Test
    void testGetPrincipal_validToken() {
        ScriptTokenPrincipal<ValueBasedRole> principal = sut.getPrincipal(GOOD_TOKEN).get();
        assertEquals(TeletraanPrincipalRoles.ADMIN.getRole(), principal.getRole());
        assertEquals(AuthZResource.Type.SYSTEM, principal.getResource().getType());
        assertEquals(tokenRolesBean.getResource_id(), principal.getResource().getName());
        assertEquals(tokenRolesBean.getScript_name(), principal.getName());
    }
}
