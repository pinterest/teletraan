/**
 * Copyright (c) 2016-2024 Pinterest, Inc.
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.security.ScriptTokenRoleAuthorizer;
import com.pinterest.teletraan.security.TeletraanAuthZResourceExtractorFactory;
import com.pinterest.teletraan.universal.security.BasePastisAuthorizer;
import com.pinterest.teletraan.universal.security.DenyAllAuthorizer;
import com.pinterest.teletraan.universal.security.TeletraanAuthorizer;
import com.pinterest.teletraan.universal.security.bean.ScriptTokenPrincipal;
import com.pinterest.teletraan.universal.security.bean.ServicePrincipal;
import com.pinterest.teletraan.universal.security.bean.TeletraanPrincipal;
import com.pinterest.teletraan.universal.security.bean.UserPrincipal;
import io.dropwizard.auth.Authorizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompositeAuthorizationFactoryTest {
    private TeletraanServiceContext context;
    private CompositeAuthorizationFactory sut;

    @BeforeEach
    void setUp() {
        context = new TeletraanServiceContext();
        context.setAuthZResourceExtractorFactory(
                new TeletraanAuthZResourceExtractorFactory(context));
        sut = new CompositeAuthorizationFactory();
    }

    @Test
    void testCreate() {
        Authorizer<?> authorizer = sut.create(context);
        assertNotNull(authorizer);
        assertTrue(authorizer instanceof BasePastisAuthorizer);

        Authorizer<?> authorizer2 = sut.create(context);
        assertSame(authorizer, authorizer2);
    }

    @Test
    void testCreateWithNullPrincipalClass() {
        Authorizer<?> authorizer = sut.create(context, null);
        assertNotNull(authorizer);
        assertTrue(authorizer instanceof BasePastisAuthorizer);
    }

    @Test
    void testCreateWithScriptTokenPrincipalClass() {
        Authorizer<?> authorizer = sut.create(context, ScriptTokenPrincipal.class);
        assertTrue(authorizer instanceof ScriptTokenRoleAuthorizer);
    }

    @Test
    void testCreateWithUserPrincipalClass() {
        Authorizer<?> authorizer = sut.create(context, UserPrincipal.class);
        assertTrue(authorizer instanceof BasePastisAuthorizer);
    }

    @Test
    void testCreateSecondaryAuthorizerWithNullPrincipalClass() {
        TeletraanAuthorizer<TeletraanPrincipal> authorizer =
                sut.createSecondaryAuthorizer(context, null);
        assertNotNull(authorizer);
        assertTrue(authorizer instanceof BasePastisAuthorizer);
    }

    @Test
    void testCreateSecondaryAuthorizerWithScriptTokenPrincipalClass() {
        TeletraanPrincipal scriptTokenPrincipal = new ScriptTokenPrincipal<>(null, null, null);
        TeletraanAuthorizer<TeletraanPrincipal> authorizer =
                sut.createSecondaryAuthorizer(context, scriptTokenPrincipal.getClass());
        assertTrue(authorizer instanceof DenyAllAuthorizer);
    }

    @Test
    void testCreateSecondaryAuthorizerWithServicePrincipalClass() {
        TeletraanPrincipal servicePrincipal = new ServicePrincipal("");
        TeletraanAuthorizer<TeletraanPrincipal> authorizer =
                sut.createSecondaryAuthorizer(context, servicePrincipal.getClass());
        assertTrue(authorizer instanceof BasePastisAuthorizer);
    }
}
