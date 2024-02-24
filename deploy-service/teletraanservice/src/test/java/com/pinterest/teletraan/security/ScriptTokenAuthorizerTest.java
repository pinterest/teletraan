/**
 * Copyright 2016 Pinterest, Inc.
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

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.TokenRolesBean;
import com.pinterest.deployservice.exception.TeletaanInternalException;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import com.pinterest.teletraan.universal.security.bean.TeletraanPrincipalRoles;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class ScriptTokenAuthorizerTest {
    private ServiceContext context;
    private RoleAuthorizer authorizer;

    private TokenRolesBean sysAdmin;
    private TokenRolesBean sysOperator;
    private TokenRolesBean sysReader;

    private TokenRolesBean envAdmin;
    private TokenRolesBean envOperator;
    private TokenRolesBean envReader;

    private AuthZResource env1;
    private AuthZResource envX;

    @Before
    public void setUp() throws Exception {
        context = new ServiceContext();
        context.setUserRolesDAO(null);
        authorizer = new RoleAuthorizer(context, null);

        sysAdmin = new TokenRolesBean();
        sysAdmin.setResource_id(AuthZResource.ALL);
        sysAdmin.setResource_type(AuthZResource.Type.SYSTEM);
        sysAdmin.setRole(TeletraanPrincipalRoles.ADMIN);

        sysOperator = new TokenRolesBean();
        sysOperator.setResource_id(AuthZResource.ALL);
        sysOperator.setResource_type(AuthZResource.Type.SYSTEM);
        sysOperator.setRole(TeletraanPrincipalRoles.OPERATOR);

        sysReader = new TokenRolesBean();
        sysReader.setResource_id(AuthZResource.ALL);
        sysReader.setResource_type(AuthZResource.Type.SYSTEM);
        sysReader.setRole(TeletraanPrincipalRoles.READER);

        envAdmin = new TokenRolesBean();
        envAdmin.setResource_id("envX");
        envAdmin.setResource_type(AuthZResource.Type.ENV);
        envAdmin.setRole(TeletraanPrincipalRoles.ADMIN);

        envOperator = new TokenRolesBean();
        envOperator.setResource_id("envX");
        envOperator.setResource_type(AuthZResource.Type.ENV);
        envOperator.setRole(TeletraanPrincipalRoles.OPERATOR);

        envReader = new TokenRolesBean();
        envReader.setResource_id("envX");
        envReader.setResource_type(AuthZResource.Type.ENV);
        envReader.setRole(TeletraanPrincipalRoles.READER);

        env1 = new AuthZResource("env1", AuthZResource.Type.ENV);
        envX = new AuthZResource("envX", AuthZResource.Type.ENV);
    }

    private void checkPositive(TokenRolesBean bean, AuthZResource resource, TeletraanPrincipalRoles role) throws Exception {
        authorizer.checkAPITokenPermission(bean, resource, role);
    }

    private void checkNegative(TokenRolesBean bean, AuthZResource resource, TeletraanPrincipalRoles role) throws Exception {
        try {
            authorizer.checkAPITokenPermission(bean, resource, role);
        } catch (TeletaanInternalException e) {
            // expected
            return;
        }
        assertFalse("Expecting exception", true);
    }

    @Test
    public void testSysEnv() throws Exception {
        checkPositive(sysAdmin, env1, TeletraanPrincipalRoles.OPERATOR);
        checkPositive(sysAdmin, env1, TeletraanPrincipalRoles.ADMIN);

        checkPositive(sysOperator, env1, TeletraanPrincipalRoles.OPERATOR);
        checkNegative(sysOperator, env1, TeletraanPrincipalRoles.ADMIN);

        checkNegative(sysReader, env1, TeletraanPrincipalRoles.OPERATOR);
        checkNegative(sysReader, env1, TeletraanPrincipalRoles.ADMIN);
    }

    @Test
    public void testSysSys() throws Exception {
        checkPositive(sysAdmin, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRoles.OPERATOR);
        checkPositive(sysAdmin, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRoles.ADMIN);

        checkPositive(sysOperator, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRoles.OPERATOR);
        checkNegative(sysOperator, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRoles.ADMIN);

        checkNegative(sysReader, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRoles.OPERATOR);
        checkNegative(sysReader, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRoles.ADMIN);
    }

    @Test
    public void testEnvSys() throws Exception {
        checkNegative(envAdmin, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRoles.OPERATOR);
        checkNegative(envAdmin, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRoles.ADMIN);

        checkNegative(envOperator, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRoles.OPERATOR);
        checkNegative(envOperator, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRoles.ADMIN);

        checkNegative(envReader, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRoles.OPERATOR);
        checkNegative(envReader, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRoles.ADMIN);
    }

    @Test
    public void testEnvXEnv1() throws Exception {
        checkNegative(envAdmin, env1, TeletraanPrincipalRoles.OPERATOR);
        checkNegative(envAdmin, env1, TeletraanPrincipalRoles.ADMIN);

        checkNegative(envOperator, env1, TeletraanPrincipalRoles.OPERATOR);
        checkNegative(envOperator, env1, TeletraanPrincipalRoles.ADMIN);

        checkNegative(envReader, env1, TeletraanPrincipalRoles.OPERATOR);
        checkNegative(envReader, env1, TeletraanPrincipalRoles.ADMIN);
    }

    @Test
    public void testEnvXEnvX() throws Exception {
        checkPositive(envAdmin, envX, TeletraanPrincipalRoles.OPERATOR);
        checkPositive(envAdmin, envX, TeletraanPrincipalRoles.ADMIN);

        checkPositive(envOperator, envX, TeletraanPrincipalRoles.OPERATOR);
        checkNegative(envOperator, envX, TeletraanPrincipalRoles.ADMIN);

        checkNegative(envReader, envX, TeletraanPrincipalRoles.OPERATOR);
        checkNegative(envReader, envX, TeletraanPrincipalRoles.ADMIN);
    }
}
