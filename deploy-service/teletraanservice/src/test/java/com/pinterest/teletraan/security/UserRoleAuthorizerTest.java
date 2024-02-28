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
package com.pinterest.teletraan.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.TeletraanPrincipalRoles;
import com.pinterest.deployservice.bean.UserRolesBean;
import com.pinterest.deployservice.dao.UserRolesDAO;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import com.pinterest.teletraan.universal.security.bean.UserPrincipal;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class UserRoleAuthorizerTest {
    private ServiceContext context;
    private UserRolesDAO userRolesDAO;
    private UserRoleAuthorizer authorizer;

    private UserRolesBean sysAdmin;
    private UserRolesBean sysOperator;
    private UserRolesBean sysReader;

    private UserRolesBean envAdmin;
    private UserRolesBean envOperator;
    private UserRolesBean envReader;

    private String envXName = "envX";
    private String env1Name = "env1";
    private AuthZResource env1;
    private AuthZResource envX;

    @Before
    public void setUp() throws Exception {
        context = new ServiceContext();
        userRolesDAO = Mockito.mock(UserRolesDAO.class);
        context.setUserRolesDAO(userRolesDAO);
        authorizer = new UserRoleAuthorizer(context, null);

        sysAdmin = new UserRolesBean();
        sysAdmin.setRole(TeletraanPrincipalRoles.ADMIN);

        sysOperator = new UserRolesBean();
        sysOperator.setRole(TeletraanPrincipalRoles.OPERATOR);

        sysReader = new UserRolesBean();
        sysReader.setRole(TeletraanPrincipalRoles.READER);

        envAdmin = new UserRolesBean();
        envAdmin.setRole(TeletraanPrincipalRoles.ADMIN);

        envOperator = new UserRolesBean();
        envOperator.setRole(TeletraanPrincipalRoles.OPERATOR);

        envReader = new UserRolesBean();
        envReader.setRole(TeletraanPrincipalRoles.READER);

        env1 = new AuthZResource(env1Name, AuthZResource.Type.ENV);
        envX = new AuthZResource(envXName, AuthZResource.Type.ENV);
    }

    private void checkPositive(
            String userName, AuthZResource resource, TeletraanPrincipalRoles role)
            throws Exception {
        UserPrincipal userPrincipal = new UserPrincipal(userName, null);
        assertTrue(authorizer.authorize(userPrincipal, role.name(), resource, null));
    }

    private void checkNegative(
            String userName, AuthZResource resource, TeletraanPrincipalRoles role)
            throws Exception {
        UserPrincipal userPrincipal = new UserPrincipal(userName, null);
        assertFalse(authorizer.authorize(userPrincipal, role.name(), resource, null));
    }

    @Test
    public void testSysSys() throws Exception {
        when(userRolesDAO.getByNameAndResource(
                        envXName, AuthZResource.ALL, AuthZResource.Type.SYSTEM))
                .thenReturn(sysAdmin);
        checkPositive(envXName, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRoles.READER);
        checkPositive(envXName, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRoles.OPERATOR);
        checkPositive(envXName, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRoles.ADMIN);

        when(userRolesDAO.getByNameAndResource(
                        envXName, AuthZResource.ALL, AuthZResource.Type.SYSTEM))
                .thenReturn(sysOperator);
        checkPositive(envXName, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRoles.READER);
        checkPositive(envXName, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRoles.OPERATOR);
        checkNegative(envXName, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRoles.ADMIN);

        when(userRolesDAO.getByNameAndResource(
                        envXName, AuthZResource.ALL, AuthZResource.Type.SYSTEM))
                .thenReturn(sysReader);
        checkPositive(envXName, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRoles.READER);
        checkNegative(envXName, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRoles.OPERATOR);
        checkNegative(envXName, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRoles.ADMIN);
    }

    @Test
    public void testEnvSys() throws Exception {
        when(userRolesDAO.getByNameAndResource(
                        envXName, AuthZResource.ALL, AuthZResource.Type.SYSTEM))
                .thenReturn(null);
        checkNegative(envXName, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRoles.OPERATOR);
        checkNegative(envXName, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRoles.ADMIN);
    }

    @Test
    public void testEnvXEnv1() throws Exception {
        when(userRolesDAO.getByNameAndResource(envXName, env1Name, AuthZResource.Type.ENV))
                .thenReturn(null);
        when(userRolesDAO.getByNameAndResource(
                        envXName, AuthZResource.ALL, AuthZResource.Type.SYSTEM))
                .thenReturn(null);
        checkNegative(envXName, env1, TeletraanPrincipalRoles.OPERATOR);
        when(userRolesDAO.getByNameAndResource(
                        envXName, AuthZResource.ALL, AuthZResource.Type.SYSTEM))
                .thenReturn(sysAdmin);
        checkPositive(envXName, env1, TeletraanPrincipalRoles.ADMIN);
    }

    @Test
    public void testEnvXEnvX() throws Exception {
        when(userRolesDAO.getByNameAndResource(envXName, envXName, AuthZResource.Type.ENV))
                .thenReturn(envAdmin);
        checkPositive(envXName, envX, TeletraanPrincipalRoles.OPERATOR);
        checkPositive(envXName, envX, TeletraanPrincipalRoles.ADMIN);

        when(userRolesDAO.getByNameAndResource(envXName, envXName, AuthZResource.Type.ENV))
                .thenReturn(envOperator);
        checkPositive(envXName, envX, TeletraanPrincipalRoles.OPERATOR);
        checkNegative(envXName, envX, TeletraanPrincipalRoles.ADMIN);

        when(userRolesDAO.getByNameAndResource(envXName, envXName, AuthZResource.Type.ENV))
                .thenReturn(envReader);
        checkNegative(envXName, envX, TeletraanPrincipalRoles.OPERATOR);
        checkNegative(envXName, envX, TeletraanPrincipalRoles.ADMIN);
    }

    @Test
    public void testSysEnvX() throws Exception {
        when(userRolesDAO.getByNameAndResource(envXName, envXName, AuthZResource.Type.ENV))
                .thenReturn(envAdmin);
        checkPositive(envXName, envX, TeletraanPrincipalRoles.OPERATOR);
        checkPositive(envXName, envX, TeletraanPrincipalRoles.ADMIN);

        when(userRolesDAO.getByNameAndResource(envXName, envXName, AuthZResource.Type.ENV))
                .thenReturn(envOperator);
        when(userRolesDAO.getByNameAndResource(
                        envXName, AuthZResource.ALL, AuthZResource.Type.SYSTEM))
                .thenReturn(sysAdmin);
        checkPositive(envXName, envX, TeletraanPrincipalRoles.OPERATOR);
        checkPositive(envXName, envX, TeletraanPrincipalRoles.ADMIN);

        when(userRolesDAO.getByNameAndResource(envXName, envXName, AuthZResource.Type.ENV))
                .thenReturn(envReader);
        when(userRolesDAO.getByNameAndResource(
                        envXName, AuthZResource.ALL, AuthZResource.Type.SYSTEM))
                .thenReturn(null);
        checkNegative(envXName, envX, TeletraanPrincipalRoles.OPERATOR);
        when(userRolesDAO.getByNameAndResource(
                        envXName, AuthZResource.ALL, AuthZResource.Type.SYSTEM))
                .thenReturn(sysOperator);
        checkNegative(envXName, envX, TeletraanPrincipalRoles.ADMIN);
    }
}
