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
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.GroupRolesBean;
import com.pinterest.deployservice.bean.TeletraanPrincipalRoles;
import com.pinterest.deployservice.bean.UserRolesBean;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.GroupRolesDAO;
import com.pinterest.deployservice.dao.UserRolesDAO;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import com.pinterest.teletraan.universal.security.bean.UserPrincipal;

class UserRoleAuthorizerTest {
    private static final String envXName = "envX";
    private static final String env1Name = "env1";
    private static final AuthZResource env1AuthZResource = new AuthZResource(env1Name, AuthZResource.Type.ENV_STAGE);
    private static final AuthZResource envXAuthZResource = new AuthZResource(envXName, AuthZResource.Type.ENV_STAGE);
    private static final AuthZResource ratingResource = new AuthZResource("rating", AuthZResource.Type.RATINGS);
    private static final MultivaluedMap<TeletraanPrincipalRoles, TeletraanPrincipalRoles> legacyToNewRoles = new MultivaluedHashMap<>();
    static AuthZResource[] resourceProvider() {
        return new AuthZResource[] {
                AuthZResource.SYSTEM_RESOURCE,
                env1AuthZResource,
                envXAuthZResource
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

    @BeforeAll
    static void setUpAll() {
        legacyToNewRoles.add(TeletraanPrincipalRoles.READER, TeletraanPrincipalRoles.READ);
        legacyToNewRoles.add(TeletraanPrincipalRoles.OPERATOR, TeletraanPrincipalRoles.READ);
        legacyToNewRoles.add(TeletraanPrincipalRoles.OPERATOR, TeletraanPrincipalRoles.WRITE);
        legacyToNewRoles.add(TeletraanPrincipalRoles.OPERATOR, TeletraanPrincipalRoles.EXECUTE);
        legacyToNewRoles.add(TeletraanPrincipalRoles.ADMIN, TeletraanPrincipalRoles.READ);
        legacyToNewRoles.add(TeletraanPrincipalRoles.ADMIN, TeletraanPrincipalRoles.WRITE);
        legacyToNewRoles.add(TeletraanPrincipalRoles.ADMIN, TeletraanPrincipalRoles.EXECUTE);
    }

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
    }

    @ParameterizedTest
    @MethodSource("resourceProvider")
    void testSystemUserOnResources(AuthZResource requestedResource) {
        checkPositive(sysAdmin, requestedResource, TeletraanPrincipalRoles.READ);
        checkPositive(sysAdmin, requestedResource, TeletraanPrincipalRoles.EXECUTE);
        checkPositive(sysAdmin, requestedResource, TeletraanPrincipalRoles.WRITE);

        checkPositive(sysOperator, requestedResource, TeletraanPrincipalRoles.READ);
        checkPositive(sysOperator, requestedResource, TeletraanPrincipalRoles.EXECUTE);
        checkPositive(sysOperator, requestedResource, TeletraanPrincipalRoles.WRITE);

        checkPositive(sysReader, requestedResource, TeletraanPrincipalRoles.READ);
        checkNegative(sysReader, requestedResource, TeletraanPrincipalRoles.EXECUTE);
        checkNegative(sysReader, requestedResource, TeletraanPrincipalRoles.WRITE);
    }

    @ParameterizedTest
    @MethodSource("resourceProvider")
    void testSystemUserOnResources_viaGroup(AuthZResource requestedResource) throws Exception {
        GroupRolesBean sysAdminBean = createGroupRolesBean(TeletraanPrincipalRoles.ADMIN, adminGroupName);
        when(groupRolesDAO.getByResource(
                AuthZResource.ALL, AuthZResource.Type.SYSTEM)).thenReturn(Collections.singletonList(sysAdminBean));
        checkPositive(sysAdminByGroup, requestedResource, TeletraanPrincipalRoles.READ);
        checkPositive(sysAdminByGroup, requestedResource, TeletraanPrincipalRoles.EXECUTE);
        checkPositive(sysAdminByGroup, requestedResource, TeletraanPrincipalRoles.WRITE);

        GroupRolesBean sysOperatorBean = createGroupRolesBean(TeletraanPrincipalRoles.OPERATOR, operatorGroupName);
        when(groupRolesDAO.getByResource(
                AuthZResource.ALL, AuthZResource.Type.SYSTEM)).thenReturn(Collections.singletonList(sysOperatorBean));
        checkPositive(sysOperatorByGroup, requestedResource, TeletraanPrincipalRoles.READ);
        checkPositive(sysOperatorByGroup, requestedResource, TeletraanPrincipalRoles.EXECUTE);
        checkPositive(sysOperatorByGroup, requestedResource, TeletraanPrincipalRoles.WRITE);

        GroupRolesBean sysReaderBean = createGroupRolesBean(TeletraanPrincipalRoles.READER, readerGroupName);
        when(groupRolesDAO.getByResource(
                AuthZResource.ALL, AuthZResource.Type.SYSTEM)).thenReturn(Collections.singletonList(sysReaderBean));
        checkPositive(sysReaderByGroup, requestedResource, TeletraanPrincipalRoles.READ);
        checkNegative(sysReaderByGroup, requestedResource, TeletraanPrincipalRoles.EXECUTE);
        checkNegative(sysReaderByGroup, requestedResource, TeletraanPrincipalRoles.WRITE);
    }

    @Test
    void testEnvUserOnEnvResource() {
        checkPositive(envAdmin, env1AuthZResource, TeletraanPrincipalRoles.READ);
        checkPositive(envAdmin, env1AuthZResource, TeletraanPrincipalRoles.EXECUTE);
        checkPositive(envAdmin, env1AuthZResource, TeletraanPrincipalRoles.WRITE);

        checkPositive(envOperator, env1AuthZResource, TeletraanPrincipalRoles.READ);
        checkPositive(envOperator, env1AuthZResource, TeletraanPrincipalRoles.EXECUTE);
        checkPositive(envOperator, env1AuthZResource, TeletraanPrincipalRoles.WRITE);

        checkPositive(envReader, env1AuthZResource, TeletraanPrincipalRoles.READ);
        checkNegative(envReader, env1AuthZResource, TeletraanPrincipalRoles.EXECUTE);
        checkNegative(envReader, env1AuthZResource, TeletraanPrincipalRoles.WRITE);
    }

    @Test
    void testEnvUserOnEnvXResource_viaGroup() throws Exception {
        GroupRolesBean envAdminBean = createGroupRolesBean(TeletraanPrincipalRoles.ADMIN, adminGroupName);
        when(groupRolesDAO.getByResource(
                env1Name, AuthZResource.Type.ENV)).thenReturn(Collections.singletonList(envAdminBean));
        checkNegative(envAdminByGroup, envXAuthZResource, TeletraanPrincipalRoles.WRITE);
        checkNegative(envAdminByGroup, envXAuthZResource, TeletraanPrincipalRoles.EXECUTE);
        checkNegative(envAdminByGroup, envXAuthZResource, TeletraanPrincipalRoles.READ);

        GroupRolesBean envOperatorBean = createGroupRolesBean(TeletraanPrincipalRoles.OPERATOR, operatorGroupName);
        when(groupRolesDAO.getByResource(
                env1Name, AuthZResource.Type.ENV)).thenReturn(Collections.singletonList(envOperatorBean));
        checkNegative(envOperatorByGroup, envXAuthZResource, TeletraanPrincipalRoles.WRITE);
        checkNegative(envOperatorByGroup, envXAuthZResource, TeletraanPrincipalRoles.EXECUTE);
        checkNegative(envOperatorByGroup, envXAuthZResource, TeletraanPrincipalRoles.READ);

        GroupRolesBean envReaderBean = createGroupRolesBean(TeletraanPrincipalRoles.READER, readerGroupName);
        when(groupRolesDAO.getByResource(
                env1Name, AuthZResource.Type.ENV)).thenReturn(Collections.singletonList(envReaderBean));
        checkNegative(envReaderByGroup, envXAuthZResource, TeletraanPrincipalRoles.WRITE);
        checkNegative(envReaderByGroup, envXAuthZResource, TeletraanPrincipalRoles.EXECUTE);
        checkNegative(envReaderByGroup, envXAuthZResource, TeletraanPrincipalRoles.READ);
    }

    @ParameterizedTest
    @MethodSource("resourceProvider")
    void testRandomUserOnDisallowedResources(AuthZResource requestedResource) {
        UserPrincipal randomUser = new UserPrincipal("randomUser", Collections.singletonList("someGroup"));
        checkNegative(randomUser, requestedResource, TeletraanPrincipalRoles.READ);
        checkNegative(randomUser, requestedResource, TeletraanPrincipalRoles.EXECUTE);
        checkNegative(randomUser, requestedResource, TeletraanPrincipalRoles.WRITE);
    }

    @Test
    void testRandomUserOnOtherResources() throws Exception {
        UserPrincipal randomUser = new UserPrincipal("randomUser", Collections.singletonList("someGroup"));
        checkPositive(randomUser, ratingResource, TeletraanPrincipalRoles.WRITE);

        when(environDAO.getByName(envXName)).thenReturn(null);
        AuthZResource envXResource = new AuthZResource(envXName, AuthZResource.Type.ENV);
        checkPositive(randomUser, envXResource, TeletraanPrincipalRoles.WRITE);

        when(environDAO.getByName(envXName)).thenReturn(Collections.singletonList(new EnvironBean()));
        checkNegative(randomUser,envXResource, TeletraanPrincipalRoles.WRITE);
    }

    @Test
    void testRatingsResource() {
        checkPositive(sysAdmin, ratingResource, TeletraanPrincipalRoles.WRITE);
        checkPositive(sysOperator, ratingResource, TeletraanPrincipalRoles.WRITE);
        checkPositive(sysReader, ratingResource, TeletraanPrincipalRoles.WRITE);
        checkPositive(envAdmin, ratingResource, TeletraanPrincipalRoles.WRITE);
        checkPositive(envOperator, ratingResource, TeletraanPrincipalRoles.WRITE);
        checkPositive(envReader, ratingResource, TeletraanPrincipalRoles.WRITE);
    }

    private void setUpGroupRolesBeans() throws Exception {
        sysAdminByGroup = new UserPrincipal("sysAdminByGroup", Collections.singletonList(adminGroupName));
        sysOperatorByGroup = new UserPrincipal("sysOperatorByGroup", Collections.singletonList(operatorGroupName));
        sysReaderByGroup = new UserPrincipal("sysReaderByGroup", Collections.singletonList(readerGroupName));

        envAdminByGroup = new UserPrincipal("env1AdminByGroup", Collections.singletonList(adminGroupName));
        envOperatorByGroup = new UserPrincipal("env1OperatorByGroup",
                Collections.singletonList(operatorGroupName));
        envReaderByGroup = new UserPrincipal("env1ReaderByGroup", Collections.singletonList(readerGroupName));
    }

    private void setUpUserPrincipals() throws Exception {
        UserRolesBean sysAdminBean = createUserRolesBean(TeletraanPrincipalRoles.ADMIN, "sysAdmin");
        when(userRolesDAO.getByNameAndResource(
                sysAdminBean.getUser_name(), AuthZResource.ALL, AuthZResource.Type.SYSTEM))
                .thenReturn(sysAdminBean);
        sysAdmin = new UserPrincipal("sysAdmin", null);

        UserRolesBean sysOperatorBean = createUserRolesBean(TeletraanPrincipalRoles.OPERATOR, "sysOperator");
        when(userRolesDAO.getByNameAndResource(
                sysOperatorBean.getUser_name(), AuthZResource.ALL, AuthZResource.Type.SYSTEM))
                .thenReturn(sysOperatorBean);
        sysOperator = new UserPrincipal("sysOperator", null);

        UserRolesBean sysReaderBean = createUserRolesBean(TeletraanPrincipalRoles.READER, "sysReader");
        when(userRolesDAO.getByNameAndResource(
                sysReaderBean.getUser_name(), AuthZResource.ALL, AuthZResource.Type.SYSTEM))
                .thenReturn(sysReaderBean);
        sysReader = new UserPrincipal("sysReader", null);

        UserRolesBean envAdminBean = createUserRolesBean(TeletraanPrincipalRoles.ADMIN, "env1Admin");
        when(userRolesDAO.getByNameAndResource(
                envAdminBean.getUser_name(), env1Name, AuthZResource.Type.ENV))
                .thenReturn(envAdminBean);
        envAdmin = new UserPrincipal("env1Admin", null);

        UserRolesBean envOperatorBean = createUserRolesBean(TeletraanPrincipalRoles.OPERATOR, "env1Operator");
        when(userRolesDAO.getByNameAndResource(
                envOperatorBean.getUser_name(), env1Name, AuthZResource.Type.ENV))
                .thenReturn(envOperatorBean);
        envOperator = new UserPrincipal("env1Operator", null);

        UserRolesBean envReaderBean = createUserRolesBean(TeletraanPrincipalRoles.READER, "env1Reader");
        when(userRolesDAO.getByNameAndResource(
                envReaderBean.getUser_name(), env1Name, AuthZResource.Type.ENV))
                .thenReturn(envReaderBean);
        envReader = new UserPrincipal("env1Reader", null);
    }

    private UserRolesBean createUserRolesBean(TeletraanPrincipalRoles role, String userName) {
        UserRolesBean userRolesBean = new UserRolesBean();
        userRolesBean.setRole(role);
        userRolesBean.setUser_name(userName);
        return userRolesBean;
    }

    private GroupRolesBean createGroupRolesBean(TeletraanPrincipalRoles role, String groupName) {
        GroupRolesBean groupRolesBean = new GroupRolesBean();
        groupRolesBean.setRole(role);
        groupRolesBean.setGroup_name(groupName);
        return groupRolesBean;
    }

    private void checkPositive(
            UserPrincipal user, AuthZResource requestedResource, TeletraanPrincipalRoles requiredRole) {
        assertTrue(authorizer.authorize(user, requiredRole.name(), requestedResource, null));
    }

    private void checkNegative(
            UserPrincipal user, AuthZResource requestedResource, TeletraanPrincipalRoles requiredRole) {
        assertFalse(authorizer.authorize(user, requiredRole.name(), requestedResource, null));
    }
}
