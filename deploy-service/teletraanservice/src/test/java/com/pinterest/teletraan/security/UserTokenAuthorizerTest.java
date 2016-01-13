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
import com.pinterest.deployservice.bean.Resource;
import com.pinterest.deployservice.bean.Role;
import com.pinterest.deployservice.bean.UserRolesBean;
import com.pinterest.deployservice.dao.UserRolesDAO;
import com.pinterest.teletraan.exception.TeletaanInternalException;
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
    private Resource env1;
    private Resource envX;

    @Before
    public void setUp() throws Exception {
        context = new ServiceContext();
        userRolesDAO = Mockito.mock(UserRolesDAO.class);
        context.setUserRolesDAO(userRolesDAO);
        authorizer = new RoleAuthorizer(context, null);

        sysAdmin = new UserRolesBean();
        sysAdmin.setRole(Role.ADMIN);

        sysOperator = new UserRolesBean();
        sysOperator.setRole(Role.OPERATOR);

        sysReader = new UserRolesBean();
        sysReader.setRole(Role.READER);

        envAdmin = new UserRolesBean();
        envAdmin.setRole(Role.ADMIN);

        envOperator = new UserRolesBean();
        envOperator.setRole(Role.OPERATOR);

        envReader = new UserRolesBean();
        envReader.setRole(Role.READER);

        env1 = new Resource(env1Name, Resource.Type.ENV);
        envX = new Resource(envXName, Resource.Type.ENV);
    }

    private void checkPositive(String userName, Resource resource, Role role) throws Exception {
        authorizer.checkUserPermission(userName, resource, null, role);
    }

    private void checkNegative(String userName, Resource resource, Role role) throws Exception {
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
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, Resource.ALL, Resource.Type.SYSTEM)).thenReturn(sysAdmin);
        checkPositive(envXName, Resource.SYSTEM_RESOURCE, Role.READER);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, Resource.ALL, Resource.Type.SYSTEM)).thenReturn(sysAdmin);
        checkPositive(envXName, Resource.SYSTEM_RESOURCE, Role.OPERATOR);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, Resource.ALL, Resource.Type.SYSTEM)).thenReturn(sysAdmin);
        checkPositive(envXName, Resource.SYSTEM_RESOURCE, Role.ADMIN);

        Mockito.when(userRolesDAO.getByNameAndResource(envXName, Resource.ALL, Resource.Type.SYSTEM)).thenReturn(sysOperator);
        checkPositive(envXName, Resource.SYSTEM_RESOURCE, Role.READER);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, Resource.ALL, Resource.Type.SYSTEM)).thenReturn(sysOperator);
        checkPositive(envXName, Resource.SYSTEM_RESOURCE, Role.OPERATOR);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, Resource.ALL, Resource.Type.SYSTEM)).thenReturn(sysOperator);
        checkNegative(envXName, Resource.SYSTEM_RESOURCE, Role.ADMIN);

        Mockito.when(userRolesDAO.getByNameAndResource(envXName, Resource.ALL, Resource.Type.SYSTEM)).thenReturn(sysReader);
        checkPositive(envXName, Resource.SYSTEM_RESOURCE, Role.READER);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, Resource.ALL, Resource.Type.SYSTEM)).thenReturn(sysReader);
        checkNegative(envXName, Resource.SYSTEM_RESOURCE, Role.OPERATOR);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, Resource.ALL, Resource.Type.SYSTEM)).thenReturn(sysReader);
        checkNegative(envXName, Resource.SYSTEM_RESOURCE, Role.ADMIN);
    }

    @Test
    public void testEnvSys() throws Exception {
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, Resource.ALL, Resource.Type.SYSTEM)).thenReturn(null);
        checkNegative(envXName, Resource.SYSTEM_RESOURCE, Role.OPERATOR);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, Resource.ALL, Resource.Type.SYSTEM)).thenReturn(null);
        checkNegative(envXName, Resource.SYSTEM_RESOURCE, Role.ADMIN);
    }

    @Test
    public void testEnvXEnv1() throws Exception {
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, env1Name, Resource.Type.ENV)).thenReturn(null);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, Resource.ALL, Resource.Type.SYSTEM)).thenReturn(null);
        checkNegative(envXName, env1, Role.OPERATOR);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, env1Name, Resource.Type.ENV)).thenReturn(null);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, Resource.ALL, Resource.Type.SYSTEM)).thenReturn(sysAdmin);
        checkPositive(envXName, env1, Role.ADMIN);
    }

    @Test
    public void testEnvXEnvX() throws Exception {
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, envXName, Resource.Type.ENV)).thenReturn(envAdmin);
        checkPositive(envXName, envX, Role.OPERATOR);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, envXName, Resource.Type.ENV)).thenReturn(envAdmin);
        checkPositive(envXName, envX, Role.ADMIN);

        Mockito.when(userRolesDAO.getByNameAndResource(envXName, envXName, Resource.Type.ENV)).thenReturn(envOperator);
        checkPositive(envXName, envX, Role.OPERATOR);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, envXName, Resource.Type.ENV)).thenReturn(envOperator);
        checkNegative(envXName, envX, Role.ADMIN);

        Mockito.when(userRolesDAO.getByNameAndResource(envXName, envXName, Resource.Type.ENV)).thenReturn(envReader);
        checkNegative(envXName, envX, Role.OPERATOR);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, envXName, Resource.Type.ENV)).thenReturn(envReader);
        checkNegative(envXName, envX, Role.ADMIN);
    }

    @Test
    public void testSysEnvX() throws Exception {
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, envXName, Resource.Type.ENV)).thenReturn(envAdmin);
        checkPositive(envXName, envX, Role.OPERATOR);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, envXName, Resource.Type.ENV)).thenReturn(envAdmin);
        checkPositive(envXName, envX, Role.ADMIN);

        Mockito.when(userRolesDAO.getByNameAndResource(envXName, envXName, Resource.Type.ENV)).thenReturn(envOperator);
        checkPositive(envXName, envX, Role.OPERATOR);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, envXName, Resource.Type.ENV)).thenReturn(envOperator);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, Resource.ALL, Resource.Type.SYSTEM)).thenReturn(sysAdmin);
        checkPositive(envXName, envX, Role.ADMIN);

        Mockito.when(userRolesDAO.getByNameAndResource(envXName, envXName, Resource.Type.ENV)).thenReturn(envReader);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, Resource.ALL, Resource.Type.SYSTEM)).thenReturn(null);
        checkNegative(envXName, envX, Role.OPERATOR);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, envXName, Resource.Type.ENV)).thenReturn(envReader);
        Mockito.when(userRolesDAO.getByNameAndResource(envXName, Resource.ALL, Resource.Type.SYSTEM)).thenReturn(sysOperator);
        checkNegative(envXName, envX, Role.ADMIN);
    }
}
