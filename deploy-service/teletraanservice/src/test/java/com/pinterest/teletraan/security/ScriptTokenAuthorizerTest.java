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
import com.pinterest.deployservice.bean.TokenRolesBean;
import com.pinterest.deployservice.exception.TeletaanInternalException;
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

    private Resource env1;
    private Resource envX;

    @Before
    public void setUp() throws Exception {
        context = new ServiceContext();
        context.setUserRolesDAO(null);
        authorizer = new RoleAuthorizer(context, null);

        sysAdmin = new TokenRolesBean();
        sysAdmin.setResource_id(Resource.ALL);
        sysAdmin.setResource_type(Resource.Type.SYSTEM);
        sysAdmin.setRole(Role.ADMIN);

        sysOperator = new TokenRolesBean();
        sysOperator.setResource_id(Resource.ALL);
        sysOperator.setResource_type(Resource.Type.SYSTEM);
        sysOperator.setRole(Role.OPERATOR);

        sysReader = new TokenRolesBean();
        sysReader.setResource_id(Resource.ALL);
        sysReader.setResource_type(Resource.Type.SYSTEM);
        sysReader.setRole(Role.READER);

        envAdmin = new TokenRolesBean();
        envAdmin.setResource_id("envX");
        envAdmin.setResource_type(Resource.Type.ENV);
        envAdmin.setRole(Role.ADMIN);

        envOperator = new TokenRolesBean();
        envOperator.setResource_id("envX");
        envOperator.setResource_type(Resource.Type.ENV);
        envOperator.setRole(Role.OPERATOR);

        envReader = new TokenRolesBean();
        envReader.setResource_id("envX");
        envReader.setResource_type(Resource.Type.ENV);
        envReader.setRole(Role.READER);

        env1 = new Resource("env1", Resource.Type.ENV);
        envX = new Resource("envX", Resource.Type.ENV);
    }

    private void checkPositive(TokenRolesBean bean, Resource resource, Role role) throws Exception {
        authorizer.checkAPITokenPermission(bean, resource, role);
    }

    private void checkNegative(TokenRolesBean bean, Resource resource, Role role) throws Exception {
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
        checkPositive(sysAdmin, env1, Role.OPERATOR);
        checkPositive(sysAdmin, env1, Role.ADMIN);

        checkPositive(sysOperator, env1, Role.OPERATOR);
        checkNegative(sysOperator, env1, Role.ADMIN);

        checkNegative(sysReader, env1, Role.OPERATOR);
        checkNegative(sysReader, env1, Role.ADMIN);
    }

    @Test
    public void testSysSys() throws Exception {
        checkPositive(sysAdmin, Resource.SYSTEM_RESOURCE, Role.OPERATOR);
        checkPositive(sysAdmin, Resource.SYSTEM_RESOURCE, Role.ADMIN);

        checkPositive(sysOperator, Resource.SYSTEM_RESOURCE, Role.OPERATOR);
        checkNegative(sysOperator, Resource.SYSTEM_RESOURCE, Role.ADMIN);

        checkNegative(sysReader, Resource.SYSTEM_RESOURCE, Role.OPERATOR);
        checkNegative(sysReader, Resource.SYSTEM_RESOURCE, Role.ADMIN);
    }

    @Test
    public void testEnvSys() throws Exception {
        checkNegative(envAdmin, Resource.SYSTEM_RESOURCE, Role.OPERATOR);
        checkNegative(envAdmin, Resource.SYSTEM_RESOURCE, Role.ADMIN);

        checkNegative(envOperator, Resource.SYSTEM_RESOURCE, Role.OPERATOR);
        checkNegative(envOperator, Resource.SYSTEM_RESOURCE, Role.ADMIN);

        checkNegative(envReader, Resource.SYSTEM_RESOURCE, Role.OPERATOR);
        checkNegative(envReader, Resource.SYSTEM_RESOURCE, Role.ADMIN);
    }

    @Test
    public void testEnvXEnv1() throws Exception {
        checkNegative(envAdmin, env1, Role.OPERATOR);
        checkNegative(envAdmin, env1, Role.ADMIN);

        checkNegative(envOperator, env1, Role.OPERATOR);
        checkNegative(envOperator, env1, Role.ADMIN);

        checkNegative(envReader, env1, Role.OPERATOR);
        checkNegative(envReader, env1, Role.ADMIN);
    }

    @Test
    public void testEnvXEnvX() throws Exception {
        checkPositive(envAdmin, envX, Role.OPERATOR);
        checkPositive(envAdmin, envX, Role.ADMIN);

        checkPositive(envOperator, envX, Role.OPERATOR);
        checkNegative(envOperator, envX, Role.ADMIN);

        checkNegative(envReader, envX, Role.OPERATOR);
        checkNegative(envReader, envX, Role.ADMIN);
    }
}
