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
import com.pinterest.deployservice.bean.UserRolesBean;
import com.pinterest.deployservice.dao.UserRolesDAO;
import com.pinterest.deployservice.exception.TeletaanInternalException;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import com.pinterest.teletraan.universal.security.bean.TeletraanPrincipalRoles;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertFalse;

public class UserTokenAuthorizerTest {
    private ServiceContext context;
    private UserRolesDAO userRolesDAO;
    private RoleAuthorizer authorizer;

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
        authorizer = new RoleAuthorizer(context, null);

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

    private void checkPositive(String userName, AuthZResource resource, TeletraanPrincipalRoles role) throws Exception {
        authorizer.checkUserPermission(userName, resource, null, role);
    }

    private void checkNegative(String userName, AuthZResource resource, TeletraanPrincipalRoles role) throws Exception {
        try {
            authorizer.checkUserPermission(userName, resource, null, role);
        } catch (TeletaanInternalException e) {
            // expected
            return;
        }
        assertFalse("Expecting exception", true);
    }

    @Test
    public void testSysSys() throws Exception {
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, AuthZResource.ALL, AuthZResource.Type.SYSTEM)).thenReturn(sysAdmin);
        checkPositive(envXName, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRoles.READER);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, AuthZResource.ALL, AuthZResource.Type.SYSTEM)).thenReturn(sysAdmin);
        checkPositive(envXName, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRoles.OPERATOR);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, AuthZResource.ALL, AuthZResource.Type.SYSTEM)).thenReturn(sysAdmin);
        checkPositive(envXName, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRoles.ADMIN);

        Mockito.when(userRolesDAO.getByNameAndResource(envXName, AuthZResource.ALL, AuthZResource.Type.SYSTEM)).thenReturn(sysOperator);
        checkPositive(envXName, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRoles.READER);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, AuthZResource.ALL, AuthZResource.Type.SYSTEM)).thenReturn(sysOperator);
        checkPositive(envXName, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRoles.OPERATOR);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, AuthZResource.ALL, AuthZResource.Type.SYSTEM)).thenReturn(sysOperator);
        checkNegative(envXName, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRoles.ADMIN);

        Mockito.when(userRolesDAO.getByNameAndResource(envXName, AuthZResource.ALL, AuthZResource.Type.SYSTEM)).thenReturn(sysReader);
        checkPositive(envXName, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRoles.READER);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, AuthZResource.ALL, AuthZResource.Type.SYSTEM)).thenReturn(sysReader);
        checkNegative(envXName, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRoles.OPERATOR);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, AuthZResource.ALL, AuthZResource.Type.SYSTEM)).thenReturn(sysReader);
        checkNegative(envXName, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRoles.ADMIN);
    }

    @Test
    public void testEnvSys() throws Exception {
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, AuthZResource.ALL, AuthZResource.Type.SYSTEM)).thenReturn(null);
        checkNegative(envXName, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRoles.OPERATOR);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, AuthZResource.ALL, AuthZResource.Type.SYSTEM)).thenReturn(null);
        checkNegative(envXName, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRoles.ADMIN);
    }

    @Test
    public void testEnvXEnv1() throws Exception {
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, env1Name, AuthZResource.Type.ENV)).thenReturn(null);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, AuthZResource.ALL, AuthZResource.Type.SYSTEM)).thenReturn(null);
        checkNegative(envXName, env1, TeletraanPrincipalRoles.OPERATOR);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, env1Name, AuthZResource.Type.ENV)).thenReturn(null);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, AuthZResource.ALL, AuthZResource.Type.SYSTEM)).thenReturn(sysAdmin);
        checkPositive(envXName, env1, TeletraanPrincipalRoles.ADMIN);
    }

    @Test
    public void testEnvXEnvX() throws Exception {
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, envXName, AuthZResource.Type.ENV)).thenReturn(envAdmin);
        checkPositive(envXName, envX, TeletraanPrincipalRoles.OPERATOR);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, envXName, AuthZResource.Type.ENV)).thenReturn(envAdmin);
        checkPositive(envXName, envX, TeletraanPrincipalRoles.ADMIN);

        Mockito.when(userRolesDAO.getByNameAndResource(envXName, envXName, AuthZResource.Type.ENV)).thenReturn(envOperator);
        checkPositive(envXName, envX, TeletraanPrincipalRoles.OPERATOR);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, envXName, AuthZResource.Type.ENV)).thenReturn(envOperator);
        checkNegative(envXName, envX, TeletraanPrincipalRoles.ADMIN);

        Mockito.when(userRolesDAO.getByNameAndResource(envXName, envXName, AuthZResource.Type.ENV)).thenReturn(envReader);
        checkNegative(envXName, envX, TeletraanPrincipalRoles.OPERATOR);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, envXName, AuthZResource.Type.ENV)).thenReturn(envReader);
        checkNegative(envXName, envX, TeletraanPrincipalRoles.ADMIN);
    }

    @Test
    public void testSysEnvX() throws Exception {
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, envXName, AuthZResource.Type.ENV)).thenReturn(envAdmin);
        checkPositive(envXName, envX, TeletraanPrincipalRoles.OPERATOR);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, envXName, AuthZResource.Type.ENV)).thenReturn(envAdmin);
        checkPositive(envXName, envX, TeletraanPrincipalRoles.ADMIN);

        Mockito.when(userRolesDAO.getByNameAndResource(envXName, envXName, AuthZResource.Type.ENV)).thenReturn(envOperator);
        checkPositive(envXName, envX, TeletraanPrincipalRoles.OPERATOR);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, envXName, AuthZResource.Type.ENV)).thenReturn(envOperator);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, AuthZResource.ALL, AuthZResource.Type.SYSTEM)).thenReturn(sysAdmin);
        checkPositive(envXName, envX, TeletraanPrincipalRoles.ADMIN);

        Mockito.when(userRolesDAO.getByNameAndResource(envXName, envXName, AuthZResource.Type.ENV)).thenReturn(envReader);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, AuthZResource.ALL, AuthZResource.Type.SYSTEM)).thenReturn(null);
        checkNegative(envXName, envX, TeletraanPrincipalRoles.OPERATOR);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, envXName, AuthZResource.Type.ENV)).thenReturn(envReader);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, AuthZResource.ALL, AuthZResource.Type.SYSTEM)).thenReturn(sysOperator);
        checkNegative(envXName, envX, TeletraanPrincipalRoles.ADMIN);
    }
}
