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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.GroupRolesBean;
import com.pinterest.deployservice.bean.TeletraanPrincipalRole;
import com.pinterest.deployservice.bean.UserRolesBean;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.GroupRolesDAO;
import com.pinterest.deployservice.dao.UserRolesDAO;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import com.pinterest.teletraan.universal.security.bean.UserPrincipal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class UserRoleAuthorizerTest {
    private static final Logger LOG = LoggerFactory.getLogger(UserRoleAuthorizerTest.class);
    private static final String envXName = "envX";
    private static final String env1Name = "env1";
    private static final String stageName = "stage";
    private static final AuthZResource env1AuthZResource = new AuthZResource(env1Name, stageName);
    private static final AuthZResource env1AdminAuthZResource =
            new AuthZResource(env1Name, AuthZResource.Type.ENV);
    private static final AuthZResource envXAuthZResource = new AuthZResource(envXName, stageName);
    private static final AuthZResource buildResource =
            new AuthZResource("build1", AuthZResource.Type.BUILD);
    private static final MultivaluedMap<TeletraanPrincipalRole, TeletraanPrincipalRole>
            legacyToNewRoles = new MultivaluedHashMap<>();
    private static final List<TeletraanPrincipalRole> newRoles =
            Arrays.asList(
                    TeletraanPrincipalRole.READ,
                    TeletraanPrincipalRole.WRITE,
                    TeletraanPrincipalRole.EXECUTE,
                    TeletraanPrincipalRole.DELETE);

    @BeforeAll
    static void setUpAll() {
        legacyToNewRoles.add(TeletraanPrincipalRole.READER, TeletraanPrincipalRole.READ);
        legacyToNewRoles.add(TeletraanPrincipalRole.OPERATOR, TeletraanPrincipalRole.READ);
        legacyToNewRoles.add(TeletraanPrincipalRole.OPERATOR, TeletraanPrincipalRole.WRITE);
        legacyToNewRoles.add(TeletraanPrincipalRole.OPERATOR, TeletraanPrincipalRole.EXECUTE);
        legacyToNewRoles.add(TeletraanPrincipalRole.OPERATOR, TeletraanPrincipalRole.DELETE);
        legacyToNewRoles.add(TeletraanPrincipalRole.ADMIN, TeletraanPrincipalRole.READ);
        legacyToNewRoles.add(TeletraanPrincipalRole.ADMIN, TeletraanPrincipalRole.WRITE);
        legacyToNewRoles.add(TeletraanPrincipalRole.ADMIN, TeletraanPrincipalRole.EXECUTE);
        legacyToNewRoles.add(TeletraanPrincipalRole.ADMIN, TeletraanPrincipalRole.DELETE);
    }

    private static AuthZResource[] resourceProvider() {
        return new AuthZResource[] {
            AuthZResource.SYSTEM_RESOURCE,
            env1AuthZResource,
            env1AdminAuthZResource,
            envXAuthZResource,
        };
    }

    private ServiceContext context;
    private EnvironDAO environDAO;

    private UserRolesDAO userRolesDAO;
    private GroupRolesDAO groupRolesDAO;
    private UserRoleAuthorizer authorizer;
    private UserPrincipal sysAdmin;
    private UserPrincipal sysOperator;
    private UserPrincipal sysReader;

    private UserPrincipal sysAdminByGroup;
    private UserPrincipal sysOperatorByGroup;
    private UserPrincipal sysReaderByGroup;
    private UserPrincipal envAdmin;
    private UserPrincipal envOperator;
    private UserPrincipal envReader;

    private UserPrincipal envAdminByGroup;
    private UserPrincipal envOperatorByGroup;
    private UserPrincipal envReaderByGroup;

    private String adminGroupName = "admin";

    private String operatorGroupName = "operator";

    private String readerGroupName = "reader";

    @BeforeEach
    void setUp() throws Exception {
        context = new ServiceContext();
        userRolesDAO = Mockito.mock(UserRolesDAO.class);
        groupRolesDAO = Mockito.mock(GroupRolesDAO.class);
        environDAO = Mockito.mock(EnvironDAO.class);
        context.setUserRolesDAO(userRolesDAO);
        context.setGroupRolesDAO(groupRolesDAO);
        context.setEnvironDAO(environDAO);
        authorizer = new UserRoleAuthorizer(context, null);

        setUpUserPrincipals();
        setUpGroupRolesBeans();

        when(environDAO.getByName(anyString()))
                .thenReturn(Collections.singletonList(new EnvironBean()));
    }

    @ParameterizedTest
    @MethodSource("resourceProvider")
    void testSystemUserOnResources(AuthZResource requestedResource) {
        checkAllRequiredRoles(sysAdmin, TeletraanPrincipalRole.ADMIN, requestedResource);
        checkAllRequiredRoles(sysOperator, TeletraanPrincipalRole.OPERATOR, requestedResource);
        checkAllRequiredRoles(sysReader, TeletraanPrincipalRole.READER, requestedResource);
    }

    @ParameterizedTest
    @MethodSource("resourceProvider")
    void testSystemUserOnResources_viaGroup(AuthZResource requestedResource) throws Exception {
        GroupRolesBean sysAdminBean =
                createGroupRolesBean(TeletraanPrincipalRole.ADMIN, adminGroupName);
        when(groupRolesDAO.getByResource(AuthZResource.ALL, AuthZResource.Type.SYSTEM))
                .thenReturn(Collections.singletonList(sysAdminBean));
        checkAllRequiredRoles(sysAdminByGroup, TeletraanPrincipalRole.ADMIN, requestedResource);

        GroupRolesBean sysOperatorBean =
                createGroupRolesBean(TeletraanPrincipalRole.OPERATOR, operatorGroupName);
        when(groupRolesDAO.getByResource(AuthZResource.ALL, AuthZResource.Type.SYSTEM))
                .thenReturn(Collections.singletonList(sysOperatorBean));
        checkAllRequiredRoles(
                sysOperatorByGroup, TeletraanPrincipalRole.OPERATOR, requestedResource);

        GroupRolesBean sysReaderBean =
                createGroupRolesBean(TeletraanPrincipalRole.READER, readerGroupName);
        when(groupRolesDAO.getByResource(AuthZResource.ALL, AuthZResource.Type.SYSTEM))
                .thenReturn(Collections.singletonList(sysReaderBean));
        checkAllRequiredRoles(sysReaderByGroup, TeletraanPrincipalRole.READER, requestedResource);
    }

    @Test
    void testEnvUserOnEnvResource() {
        checkAllRequiredRoles(envAdmin, TeletraanPrincipalRole.ADMIN, env1AuthZResource);
        checkAllRequiredRoles(envOperator, TeletraanPrincipalRole.OPERATOR, env1AuthZResource);
        checkAllRequiredRoles(envReader, TeletraanPrincipalRole.READER, env1AuthZResource);

        checkAllRequiredRoles(envAdmin, TeletraanPrincipalRole.ADMIN, env1AdminAuthZResource);
        checkAllNegative(envOperator, env1AdminAuthZResource);
        checkAllNegative(envReader, env1AdminAuthZResource);
    }

    @Test
    void testEnvUserOnEnvXResource_viaGroup() throws Exception {
        GroupRolesBean envAdminBean =
                createGroupRolesBean(TeletraanPrincipalRole.ADMIN, adminGroupName);
        when(groupRolesDAO.getByResource(env1Name, AuthZResource.Type.ENV))
                .thenReturn(Collections.singletonList(envAdminBean));
        checkAllNegative(envAdminByGroup, envXAuthZResource);

        GroupRolesBean envOperatorBean =
                createGroupRolesBean(TeletraanPrincipalRole.OPERATOR, operatorGroupName);
        when(groupRolesDAO.getByResource(env1Name, AuthZResource.Type.ENV))
                .thenReturn(Collections.singletonList(envOperatorBean));
        checkAllNegative(envOperatorByGroup, envXAuthZResource);

        GroupRolesBean envReaderBean =
                createGroupRolesBean(TeletraanPrincipalRole.READER, readerGroupName);
        when(groupRolesDAO.getByResource(env1Name, AuthZResource.Type.ENV))
                .thenReturn(Collections.singletonList(envReaderBean));
        checkAllNegative(envReaderByGroup, envXAuthZResource);
    }

    @ParameterizedTest
    @MethodSource("resourceProvider")
    void testRandomUserOnDisallowedResources(AuthZResource requestedResource) {
        UserPrincipal randomUser =
                new UserPrincipal("randomUser", Collections.singletonList("someGroup"));
        checkAllNegative(randomUser, requestedResource);
    }

    @Test
    void testRandomUserOnOtherResources() throws Exception {
        UserPrincipal randomUser =
                new UserPrincipal("randomUser", Collections.singletonList("someGroup"));

        // Special case for creating a new environment
        AuthZResource envXResource = new AuthZResource(envXName, AuthZResource.Type.ENV_STAGE);
        when(environDAO.getByName(envXName)).thenReturn(null);
        checkPositive(randomUser, envXResource, TeletraanPrincipalRole.WRITE);

        when(environDAO.getByName(envXName))
                .thenReturn(Collections.singletonList(new EnvironBean()));
        checkNegative(randomUser, envXResource, TeletraanPrincipalRole.WRITE);

        checkPositive(randomUser, buildResource, TeletraanPrincipalRole.PUBLISHER);
    }

    @Test
    void testBuildResource() {
        checkPositive(sysAdmin, buildResource, TeletraanPrincipalRole.PUBLISHER);
        checkPositive(sysOperator, buildResource, TeletraanPrincipalRole.PUBLISHER);
        checkPositive(sysReader, buildResource, TeletraanPrincipalRole.PUBLISHER);
        checkPositive(envAdmin, buildResource, TeletraanPrincipalRole.PUBLISHER);
        checkPositive(envOperator, buildResource, TeletraanPrincipalRole.PUBLISHER);
        checkPositive(envReader, buildResource, TeletraanPrincipalRole.PUBLISHER);
    }

    private void checkAllRequiredRoles(
            UserPrincipal principal,
            TeletraanPrincipalRole legacyRole,
            AuthZResource requestedResource) {
        for (TeletraanPrincipalRole requiredRole : newRoles) {
            if (legacyToNewRoles.get(legacyRole).contains(requiredRole)) {
                checkPositive(principal, requestedResource, requiredRole);
            } else {
                checkNegative(principal, requestedResource, requiredRole);
            }
        }
    }

    private void checkAllNegative(UserPrincipal principal, AuthZResource requestedResource) {
        for (TeletraanPrincipalRole requiredRole : newRoles) {
            checkNegative(principal, requestedResource, requiredRole);
        }
    }

    private void setUpGroupRolesBeans() throws Exception {
        sysAdminByGroup =
                new UserPrincipal("sysAdminByGroup", Collections.singletonList(adminGroupName));
        sysOperatorByGroup =
                new UserPrincipal(
                        "sysOperatorByGroup", Collections.singletonList(operatorGroupName));
        sysReaderByGroup =
                new UserPrincipal("sysReaderByGroup", Collections.singletonList(readerGroupName));

        envAdminByGroup =
                new UserPrincipal("env1AdminByGroup", Collections.singletonList(adminGroupName));
        envOperatorByGroup =
                new UserPrincipal(
                        "env1OperatorByGroup", Collections.singletonList(operatorGroupName));
        envReaderByGroup =
                new UserPrincipal("env1ReaderByGroup", Collections.singletonList(readerGroupName));
    }

    private void setUpUserPrincipals() throws Exception {
        UserRolesBean sysAdminBean = createUserRolesBean(TeletraanPrincipalRole.ADMIN, "sysAdmin");
        when(userRolesDAO.getByNameAndResource(
                        sysAdminBean.getUser_name(), AuthZResource.ALL, AuthZResource.Type.SYSTEM))
                .thenReturn(sysAdminBean);
        sysAdmin = new UserPrincipal(sysAdminBean.getUser_name(), null);

        UserRolesBean sysOperatorBean =
                createUserRolesBean(TeletraanPrincipalRole.OPERATOR, "sysOperator");
        when(userRolesDAO.getByNameAndResource(
                        sysOperatorBean.getUser_name(),
                        AuthZResource.ALL,
                        AuthZResource.Type.SYSTEM))
                .thenReturn(sysOperatorBean);
        sysOperator = new UserPrincipal(sysOperatorBean.getUser_name(), null);

        UserRolesBean sysReaderBean =
                createUserRolesBean(TeletraanPrincipalRole.READER, "sysReader");
        when(userRolesDAO.getByNameAndResource(
                        sysReaderBean.getUser_name(), AuthZResource.ALL, AuthZResource.Type.SYSTEM))
                .thenReturn(sysReaderBean);
        sysReader = new UserPrincipal(sysReaderBean.getUser_name(), null);

        UserRolesBean envAdminBean = createUserRolesBean(TeletraanPrincipalRole.ADMIN, "env1Admin");
        when(userRolesDAO.getByNameAndResource(
                        envAdminBean.getUser_name(), env1Name, AuthZResource.Type.ENV))
                .thenReturn(envAdminBean);
        envAdmin = new UserPrincipal(envAdminBean.getUser_name(), null);

        UserRolesBean envOperatorBean =
                createUserRolesBean(TeletraanPrincipalRole.OPERATOR, "env1Operator");
        when(userRolesDAO.getByNameAndResource(
                        envOperatorBean.getUser_name(), env1Name, AuthZResource.Type.ENV))
                .thenReturn(envOperatorBean);
        envOperator = new UserPrincipal(envOperatorBean.getUser_name(), null);

        UserRolesBean envReaderBean =
                createUserRolesBean(TeletraanPrincipalRole.READER, "env1Reader");
        when(userRolesDAO.getByNameAndResource(
                        envReaderBean.getUser_name(), env1Name, AuthZResource.Type.ENV))
                .thenReturn(envReaderBean);
        envReader = new UserPrincipal(envReaderBean.getUser_name(), null);
    }

    private UserRolesBean createUserRolesBean(TeletraanPrincipalRole role, String userName) {
        UserRolesBean userRolesBean = new UserRolesBean();
        userRolesBean.setRole(role);
        userRolesBean.setUser_name(userName);
        return userRolesBean;
    }

    private GroupRolesBean createGroupRolesBean(TeletraanPrincipalRole role, String groupName) {
        GroupRolesBean groupRolesBean = new GroupRolesBean();
        groupRolesBean.setRole(role);
        groupRolesBean.setGroup_name(groupName);
        return groupRolesBean;
    }

    private void checkPositive(
            UserPrincipal user,
            AuthZResource requestedResource,
            TeletraanPrincipalRole requiredRole) {
        LOG.info(
                "Checking positive {} for {} on {}",
                user.getName(),
                requiredRole,
                requestedResource);
        assertTrue(authorizer.authorize(user, requiredRole.name(), requestedResource, null));
    }

    private void checkNegative(
            UserPrincipal user,
            AuthZResource requestedResource,
            TeletraanPrincipalRole requiredRole) {
        LOG.info(
                "Checking negative {} for {} on {}",
                user.getName(),
                requiredRole,
                requestedResource);
        assertFalse(authorizer.authorize(user, requiredRole.name(), requestedResource, null));
    }
}
