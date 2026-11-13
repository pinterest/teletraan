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
package com.pinterest.teletraan.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.pinterest.deployservice.bean.TeletraanPrincipalRole;
import com.pinterest.deployservice.bean.TokenRolesBean;
import com.pinterest.deployservice.dao.TokenRolesDAO;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import com.pinterest.teletraan.universal.security.bean.ScriptTokenPrincipal;
import com.pinterest.teletraan.universal.security.bean.ValueBasedRole;
import io.dropwizard.auth.Authenticator;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

class TokenAuthenticationFactoryTest {
    private static final String TOKEN = "test-token";
    private TeletraanServiceContext context;
    private TokenRolesDAO tokenRolesDAO;

    @BeforeEach
    void setUp() {
        context = new TeletraanServiceContext();
        tokenRolesDAO = mock(TokenRolesDAO.class);
        context.setTokenRolesDAO(tokenRolesDAO);
    }

    @ParameterizedTest
    @ValueSource(strings = {"maximumSize=100", "maximumSize=100,expireAfterAccess=1h"})
    void warmRepositoryCacheCannotExtendExpiry(String cacheSpec) throws Exception {
        TokenRolesBean token = token(AuthZResource.Type.SCM_REPOSITORY);
        when(tokenRolesDAO.getByToken(TOKEN)).thenReturn(token);
        Authenticator<String, ScriptTokenPrincipal<ValueBasedRole>> authenticator =
                authenticator(cacheSpec);
        assertTrue(authenticator.authenticate(TOKEN).isPresent());

        token.setExpire_date(Instant.now().minusSeconds(1).toEpochMilli());
        clearInvocations(tokenRolesDAO);
        assertFalse(authenticator.authenticate(TOKEN).isPresent());
        verify(tokenRolesDAO).getByToken(TOKEN);

        token.setExpire_date(null);
        clearInvocations(tokenRolesDAO);
        assertFalse(authenticator.authenticate(TOKEN).isPresent());
        verify(tokenRolesDAO).getByToken(TOKEN);
    }

    @ParameterizedTest
    @ValueSource(strings = {"maximumSize=100", "maximumSize=100,expireAfterAccess=1h"})
    void warmRepositoryCacheCannotSurviveRevocation(String cacheSpec) throws Exception {
        when(tokenRolesDAO.getByToken(TOKEN)).thenReturn(token(AuthZResource.Type.SCM_REPOSITORY));
        Authenticator<String, ScriptTokenPrincipal<ValueBasedRole>> authenticator =
                authenticator(cacheSpec);
        assertTrue(authenticator.authenticate(TOKEN).isPresent());

        when(tokenRolesDAO.getByToken(TOKEN)).thenReturn(null);
        clearInvocations(tokenRolesDAO);
        assertFalse(authenticator.authenticate(TOKEN).isPresent());
        verify(tokenRolesDAO).getByToken(TOKEN);
    }

    @ParameterizedTest
    @EnumSource(
            value = AuthZResource.Type.class,
            names = {"ENV", "SYSTEM"})
    void legacyScriptTokenCachingIsPreserved(AuthZResource.Type resourceType) throws Exception {
        when(tokenRolesDAO.getByToken(TOKEN)).thenReturn(token(resourceType));
        Authenticator<String, ScriptTokenPrincipal<ValueBasedRole>> authenticator =
                authenticator("maximumSize=100");
        ScriptTokenPrincipal<ValueBasedRole> principal = authenticator.authenticate(TOKEN).get();

        when(tokenRolesDAO.getByToken(TOKEN)).thenReturn(null);
        clearInvocations(tokenRolesDAO);
        assertSame(principal, authenticator.authenticate(TOKEN).get());
        verifyNoInteractions(tokenRolesDAO);
    }

    @Test
    void noCacheStillUsesTheCurrentTokenRecord() throws Exception {
        when(tokenRolesDAO.getByToken(TOKEN)).thenReturn(token(AuthZResource.Type.ENV));
        Authenticator<String, ScriptTokenPrincipal<ValueBasedRole>> authenticator =
                authenticator(null);
        assertTrue(authenticator.authenticate(TOKEN).isPresent());

        when(tokenRolesDAO.getByToken(TOKEN)).thenReturn(null);
        assertFalse(authenticator.authenticate(TOKEN).isPresent());
    }

    private Authenticator<String, ScriptTokenPrincipal<ValueBasedRole>> authenticator(
            String cacheSpec) {
        TokenAuthenticationFactory factory = new TokenAuthenticationFactory();
        factory.setTokenCacheSpec(cacheSpec);
        factory.setCheckTokenExpiry(false);
        // Isolate the cache metrics without changing other tests' global default registry.
        try (MockedStatic<SharedMetricRegistries> registries =
                mockStatic(SharedMetricRegistries.class)) {
            registries.when(SharedMetricRegistries::getDefault).thenReturn(new MetricRegistry());
            return factory.createScriptTokenAuthenticator(context);
        }
    }

    private TokenRolesBean token(AuthZResource.Type resourceType) {
        TokenRolesBean token = new TokenRolesBean();
        token.setScript_name("test-script");
        token.setResource_id(
                resourceType == AuthZResource.Type.SCM_REPOSITORY ? "Github/acme/repo" : "legacy");
        token.setResource_type(resourceType);
        token.setRole(TeletraanPrincipalRole.READ);
        token.setExpire_date(Instant.now().plusSeconds(3600).toEpochMilli());
        return token;
    }
}
