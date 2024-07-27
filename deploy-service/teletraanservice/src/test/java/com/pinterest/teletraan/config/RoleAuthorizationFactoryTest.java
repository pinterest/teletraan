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
package com.pinterest.teletraan.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.security.ScriptTokenRoleAuthorizer;
import com.pinterest.teletraan.security.UserRoleAuthorizer;
import com.pinterest.teletraan.universal.security.bean.ServicePrincipal;
import com.pinterest.teletraan.universal.security.bean.TeletraanPrincipal;
import com.pinterest.teletraan.universal.security.bean.UserPrincipal;

class RoleAuthorizationFactoryTest {
    private static RoleAuthorizationFactory sut = new RoleAuthorizationFactory();
    private static TeletraanServiceContext context = new TeletraanServiceContext();

    @Test
    void testCreate() {
        assertThrows(UnsupportedOperationException.class, () -> {
            sut.create(context);
        });
    }

    @Test
    void testCreate_servicePrincipal() throws Exception {
        assertEquals(ScriptTokenRoleAuthorizer.class, sut.create(context, ServicePrincipal.class).getClass());
    }

    @Test
    void testCreate_userPrincipal() throws Exception {
        assertEquals(UserRoleAuthorizer.class, sut.create(context, UserPrincipal.class).getClass());
    }

    @Test
    void testCreate_otherPrincipal() throws Exception {
        assertThrows(UnsupportedOperationException.class, () -> {
            sut.create(context, TeletraanPrincipal.class);
        });
    }
}