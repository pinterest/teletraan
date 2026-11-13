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

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.TeletraanPrincipalRole;
import com.pinterest.deployservice.bean.TokenRolesBean;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import com.pinterest.teletraan.universal.security.bean.ScriptTokenPrincipal;
import com.pinterest.teletraan.universal.security.bean.ValueBasedRole;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class ScriptTokenRoleAuthorizerTest {
    private ServiceContext context;
    private ScriptTokenRoleAuthorizer authorizer;

    private TokenRolesBean sysAdmin;
    private TokenRolesBean sysOperator;
    private TokenRolesBean sysReader;

    private TokenRolesBean envAdmin;
    private TokenRolesBean envOperator;
    private TokenRolesBean envReader;

    private TokenRolesBean pinger;
    private TokenRolesBean publisher;

    private AuthZResource env1;
    private AuthZResource envX;
    private AuthZResource env1Stage;
    private AuthZResource envXStage;

    @BeforeEach
    public void setUp() throws Exception {
        context = new ServiceContext();
        context.setUserRolesDAO(null);
        authorizer = new ScriptTokenRoleAuthorizer(null);

        sysAdmin = new TokenRolesBean();
        sysAdmin.setResource_id(AuthZResource.ALL);
        sysAdmin.setResource_type(AuthZResource.Type.SYSTEM);
        sysAdmin.setRole(TeletraanPrincipalRole.ADMIN);

        sysOperator = new TokenRolesBean();
        sysOperator.setResource_id(AuthZResource.ALL);
        sysOperator.setResource_type(AuthZResource.Type.SYSTEM);
        sysOperator.setRole(TeletraanPrincipalRole.OPERATOR);

        sysReader = new TokenRolesBean();
        sysReader.setResource_id(AuthZResource.ALL);
        sysReader.setResource_type(AuthZResource.Type.SYSTEM);
        sysReader.setRole(TeletraanPrincipalRole.READER);

        envAdmin = new TokenRolesBean();
        envAdmin.setResource_id("envX");
        envAdmin.setResource_type(AuthZResource.Type.ENV);
        envAdmin.setRole(TeletraanPrincipalRole.ADMIN);

        envOperator = new TokenRolesBean();
        envOperator.setResource_id("envX");
        envOperator.setResource_type(AuthZResource.Type.ENV);
        envOperator.setRole(TeletraanPrincipalRole.OPERATOR);

        envReader = new TokenRolesBean();
        envReader.setResource_id("envX");
        envReader.setResource_type(AuthZResource.Type.ENV);
        envReader.setRole(TeletraanPrincipalRole.READER);

        pinger = new TokenRolesBean();
        pinger.setResource_id(AuthZResource.ALL);
        pinger.setResource_type(AuthZResource.Type.SYSTEM);
        pinger.setRole(TeletraanPrincipalRole.PINGER);

        publisher = new TokenRolesBean();
        publisher.setResource_id(AuthZResource.ALL);
        publisher.setResource_type(AuthZResource.Type.SYSTEM);
        publisher.setRole(TeletraanPrincipalRole.PUBLISHER);

        env1 = new AuthZResource("env1", AuthZResource.Type.ENV);
        envX = new AuthZResource("envX", AuthZResource.Type.ENV);
        env1Stage = new AuthZResource("env1", "stage1");
        envXStage = new AuthZResource("envX", "stageX");
    }

    private void checkPositive(
            TokenRolesBean bean, AuthZResource resource, TeletraanPrincipalRole role)
            throws Exception {
        ScriptTokenPrincipal<ValueBasedRole> principal =
                new ScriptTokenPrincipal<>(
                        "testPrincipal",
                        bean.getRole().getRole(),
                        new AuthZResource(bean.getResource_id(), bean.getResource_type()));
        assertTrue(authorizer.authorize(principal, role.name(), resource, null));
    }

    private void checkNegative(
            TokenRolesBean bean, AuthZResource resource, TeletraanPrincipalRole requiredRole)
            throws Exception {
        ScriptTokenPrincipal<ValueBasedRole> principal =
                new ScriptTokenPrincipal<>(
                        "testPrincipal",
                        bean.getRole().getRole(),
                        new AuthZResource(bean.getResource_id(), bean.getResource_type()));
        assertFalse(authorizer.authorize(principal, requiredRole.name(), resource, null));
    }

    @Test
    public void testSysEnv() throws Exception {
        checkPositive(sysAdmin, env1, TeletraanPrincipalRole.OPERATOR);
        checkPositive(sysAdmin, env1, TeletraanPrincipalRole.ADMIN);

        checkPositive(sysOperator, env1, TeletraanPrincipalRole.OPERATOR);
        checkNegative(sysOperator, env1, TeletraanPrincipalRole.ADMIN);

        checkNegative(sysReader, env1, TeletraanPrincipalRole.OPERATOR);
        checkNegative(sysReader, env1, TeletraanPrincipalRole.ADMIN);
    }

    @Test
    void testSysSys() throws Exception {
        checkPositive(sysAdmin, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRole.OPERATOR);
        checkPositive(sysAdmin, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRole.ADMIN);

        checkPositive(sysOperator, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRole.OPERATOR);
        checkNegative(sysOperator, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRole.ADMIN);

        checkNegative(sysReader, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRole.OPERATOR);
        checkNegative(sysReader, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRole.ADMIN);
    }

    @Test
    void testSysEnvStage() throws Exception {
        checkPositive(sysAdmin, env1Stage, TeletraanPrincipalRole.OPERATOR);
        checkPositive(sysAdmin, env1Stage, TeletraanPrincipalRole.ADMIN);

        checkPositive(sysOperator, env1Stage, TeletraanPrincipalRole.OPERATOR);
        checkNegative(sysOperator, env1Stage, TeletraanPrincipalRole.ADMIN);

        checkNegative(sysReader, env1Stage, TeletraanPrincipalRole.OPERATOR);
        checkNegative(sysReader, env1Stage, TeletraanPrincipalRole.ADMIN);
    }

    @Test
    public void testEnvSys() throws Exception {
        checkNegative(envAdmin, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRole.OPERATOR);
        checkNegative(envAdmin, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRole.ADMIN);

        checkNegative(envOperator, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRole.OPERATOR);
        checkNegative(envOperator, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRole.ADMIN);

        checkNegative(envReader, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRole.OPERATOR);
        checkNegative(envReader, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRole.ADMIN);
    }

    @Test
    public void testEnvXEnv1() throws Exception {
        checkNegative(envAdmin, env1, TeletraanPrincipalRole.OPERATOR);
        checkNegative(envAdmin, env1, TeletraanPrincipalRole.ADMIN);

        checkNegative(envOperator, env1, TeletraanPrincipalRole.OPERATOR);
        checkNegative(envOperator, env1, TeletraanPrincipalRole.ADMIN);

        checkNegative(envReader, env1, TeletraanPrincipalRole.OPERATOR);
        checkNegative(envReader, env1, TeletraanPrincipalRole.ADMIN);
    }

    @Test
    public void testEnvXEnvX() throws Exception {
        checkPositive(envAdmin, envX, TeletraanPrincipalRole.OPERATOR);
        checkPositive(envAdmin, envX, TeletraanPrincipalRole.ADMIN);

        checkNegative(envOperator, envX, TeletraanPrincipalRole.OPERATOR);
        checkNegative(envOperator, envX, TeletraanPrincipalRole.ADMIN);

        checkNegative(envReader, envX, TeletraanPrincipalRole.OPERATOR);
        checkNegative(envReader, envX, TeletraanPrincipalRole.ADMIN);
    }

    @Test
    public void testEnvXEnv1Stage() throws Exception {
        checkNegative(envAdmin, env1Stage, TeletraanPrincipalRole.OPERATOR);
        checkNegative(envAdmin, env1Stage, TeletraanPrincipalRole.ADMIN);

        checkNegative(envOperator, env1Stage, TeletraanPrincipalRole.OPERATOR);
        checkNegative(envOperator, env1Stage, TeletraanPrincipalRole.ADMIN);

        checkNegative(envReader, env1Stage, TeletraanPrincipalRole.OPERATOR);
        checkNegative(envReader, env1Stage, TeletraanPrincipalRole.ADMIN);
    }

    @Test
    public void testEnvXEnvXStage() throws Exception {
        checkPositive(envAdmin, envXStage, TeletraanPrincipalRole.OPERATOR);
        checkPositive(envAdmin, envXStage, TeletraanPrincipalRole.ADMIN);

        checkPositive(envOperator, envXStage, TeletraanPrincipalRole.OPERATOR);
        checkNegative(envOperator, envXStage, TeletraanPrincipalRole.ADMIN);

        checkNegative(envReader, envXStage, TeletraanPrincipalRole.OPERATOR);
        checkNegative(envReader, envXStage, TeletraanPrincipalRole.ADMIN);
    }

    @Test
    void testPingerCanPing() throws Exception {
        checkPositive(pinger, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRole.PINGER);
    }

    @Test
    void testRepositoryRequiresExactScopeOrSystemAdmin() throws Exception {
        AuthZResource repository =
                new AuthZResource("GitHub/pinterest/teletraan", AuthZResource.Type.SCM_REPOSITORY);
        TokenRolesBean repositoryReader = new TokenRolesBean();
        repositoryReader.setResource_id(repository.getName());
        repositoryReader.setResource_type(AuthZResource.Type.SCM_REPOSITORY);
        repositoryReader.setRole(TeletraanPrincipalRole.READER);

        checkPositive(repositoryReader, repository, TeletraanPrincipalRole.READ);
        checkNegative(
                repositoryReader,
                new AuthZResource("GitHub/private/secret", AuthZResource.Type.SCM_REPOSITORY),
                TeletraanPrincipalRole.READ);
        checkNegative(repositoryReader, repository, TeletraanPrincipalRole.WRITE);
        checkPositive(sysAdmin, repository, TeletraanPrincipalRole.READ);
        checkNegative(sysReader, repository, TeletraanPrincipalRole.READ);
        checkNegative(sysOperator, repository, TeletraanPrincipalRole.READ);
        checkNegative(envAdmin, repository, TeletraanPrincipalRole.READ);
        checkNegative(envReader, repository, TeletraanPrincipalRole.READ);
        checkNegative(publisher, repository, TeletraanPrincipalRole.READ);
        checkNegative(pinger, repository, TeletraanPrincipalRole.READ);
    }

    @Test
    public void testPublish() throws Exception {
        AuthZResource build = new AuthZResource("build", AuthZResource.Type.BUILD);
        checkPositive(publisher, AuthZResource.SYSTEM_RESOURCE, TeletraanPrincipalRole.PUBLISHER);
        checkPositive(publisher, build, TeletraanPrincipalRole.PUBLISHER);

        checkNegative(pinger, build, TeletraanPrincipalRole.PUBLISHER);

        checkPositive(envAdmin, build, TeletraanPrincipalRole.PUBLISHER);
        checkPositive(envOperator, build, TeletraanPrincipalRole.PUBLISHER);
        checkNegative(envReader, build, TeletraanPrincipalRole.PUBLISHER);

        checkPositive(sysAdmin, build, TeletraanPrincipalRole.PUBLISHER);
        checkPositive(sysOperator, build, TeletraanPrincipalRole.PUBLISHER);
        checkNegative(sysReader, build, TeletraanPrincipalRole.PUBLISHER);
    }

    @ParameterizedTest
    @EnumSource(
            value = TeletraanPrincipalRole.class,
            names = {"WRITE", "DELETE"})
    void buildMutationsRequireBuildOrSystemScope(TeletraanPrincipalRole action) throws Exception {
        AuthZResource build =
                new AuthZResource(
                        "envX",
                        AuthZResource.Type.BUILD,
                        Collections.singletonMap(
                                AuthZResource.AttributeKeys.BUILD_ARTIFACT_URL.name(),
                                "s3://bucket/build.tar.gz"));
        TokenRolesBean buildOperator = new TokenRolesBean();
        buildOperator.setResource_id("envX");
        buildOperator.setResource_type(AuthZResource.Type.BUILD);
        buildOperator.setRole(TeletraanPrincipalRole.OPERATOR);

        checkPositive(buildOperator, build, action);
        checkPositive(sysAdmin, build, action);
        checkPositive(sysOperator, build, action);
        checkNegative(sysReader, build, action);
        checkNegative(publisher, build, action);
        checkNegative(envAdmin, build, action);
        checkNegative(envOperator, build, action);

        buildOperator.setResource_id("other-build");
        checkNegative(buildOperator, build, action);
        buildOperator.setResource_id("envX");
        buildOperator.setRole(TeletraanPrincipalRole.PUBLISHER);
        checkNegative(buildOperator, build, action);
    }

    @ParameterizedTest
    @EnumSource(TeletraanPrincipalRole.class)
    void legacyTokenRolesDoNotGrantPastisOnlyPindeployAction(TeletraanPrincipalRole tokenRole) {
        ScriptTokenPrincipal<ValueBasedRole> principal =
                new ScriptTokenPrincipal<>(
                        "testPrincipal", tokenRole.getRole(), AuthZResource.SYSTEM_RESOURCE);

        assertFalse(
                authorizer.authorize(
                        principal,
                        TeletraanPrincipalRole.Names.MANAGE_PINDEPLOY,
                        AuthZResource.SYSTEM_RESOURCE,
                        null));
    }

    @Test
    public void testUnspecified() throws Exception {
        checkPositive(sysAdmin, AuthZResource.UNSPECIFIED_RESOURCE, TeletraanPrincipalRole.READ);
        checkPositive(sysOperator, AuthZResource.UNSPECIFIED_RESOURCE, TeletraanPrincipalRole.READ);
        checkPositive(sysReader, AuthZResource.UNSPECIFIED_RESOURCE, TeletraanPrincipalRole.READ);

        checkPositive(envAdmin, AuthZResource.UNSPECIFIED_RESOURCE, TeletraanPrincipalRole.READ);
        checkPositive(envOperator, AuthZResource.UNSPECIFIED_RESOURCE, TeletraanPrincipalRole.READ);
        checkPositive(envReader, AuthZResource.UNSPECIFIED_RESOURCE, TeletraanPrincipalRole.READ);

        checkPositive(pinger, AuthZResource.UNSPECIFIED_RESOURCE, TeletraanPrincipalRole.READ);
        checkPositive(publisher, AuthZResource.UNSPECIFIED_RESOURCE, TeletraanPrincipalRole.READ);
    }
}
