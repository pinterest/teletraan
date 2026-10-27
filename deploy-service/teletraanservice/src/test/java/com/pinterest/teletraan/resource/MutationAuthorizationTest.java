/**
 * Copyright (c) 2026 Pinterest, Inc.
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
package com.pinterest.teletraan.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pinterest.deployservice.bean.BuildBean;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.PindeployBean;
import com.pinterest.deployservice.bean.TagBean;
import com.pinterest.deployservice.bean.TagTargetType;
import com.pinterest.deployservice.bean.TagValue;
import com.pinterest.deployservice.bean.TeletraanPrincipalRole;
import com.pinterest.deployservice.bean.TokenRolesBean;
import com.pinterest.deployservice.dao.BuildDAO;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.PindeployDAO;
import com.pinterest.deployservice.dao.TagDAO;
import com.pinterest.deployservice.dao.TokenRolesDAO;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.config.RoleAuthorizationFactory;
import com.pinterest.teletraan.config.TokenAuthenticationFactory;
import com.pinterest.teletraan.security.TeletraanAuthZResourceExtractorFactory;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfoFeature;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(DropwizardExtensionsSupport.class)
class MutationAuthorizationTest {
    private static final String READ_TOKEN = "read-token";
    private static final String PUBLISHER_TOKEN = "publisher-token";
    private static final String ENV_TOKEN = "env-token";
    private static final String ENV_STAGE_TOKEN = "env-stage-token";
    private static final String SYSTEM_TOKEN = "system-token";
    private static final String ENV_ID = "environment-id";
    private static final String OTHER_ENV_ID = "other-environment-id";
    private static final String OTHER_STAGE_ID = "other-stage-id";
    private static final String BUILD_ID = "build-id";
    private static final String BUILD_NAME = "build-name";
    private static final String ENV_NAME = "environment";
    private static final String STAGE_NAME = "prod";
    private static final String PIPELINE = "pipeline";
    private static final TagDAO TAG_DAO = mock(TagDAO.class);
    private static final PindeployDAO PINDEPLOY_DAO = mock(PindeployDAO.class);
    private static final Map<String, TagBean> STORED_TAGS = new HashMap<>();
    private static final BuildBean BUILD = new BuildBean();
    private static final ResourceExtension EXT = createExtension();

    private static ResourceExtension createExtension() {
        try {
            TeletraanServiceContext context = new TeletraanServiceContext();
            TokenRolesDAO tokenRolesDAO = mock(TokenRolesDAO.class);
            EnvironDAO environDAO = mock(EnvironDAO.class);
            BuildDAO buildDAO = mock(BuildDAO.class);
            context.setTagDAO(TAG_DAO);
            context.setPindeployDAO(PINDEPLOY_DAO);
            context.setTokenRolesDAO(tokenRolesDAO);
            context.setEnvironDAO(environDAO);
            context.setBuildDAO(buildDAO);
            context.setAuthorizationFactory(new RoleAuthorizationFactory());
            context.setAuthZResourceExtractorFactory(
                    new TeletraanAuthZResourceExtractorFactory(context));
            when(tokenRolesDAO.getByToken(READ_TOKEN))
                    .thenReturn(
                            token(
                                    TeletraanPrincipalRole.READ,
                                    AuthZResource.ALL,
                                    AuthZResource.Type.SYSTEM,
                                    READ_TOKEN));
            when(tokenRolesDAO.getByToken(PUBLISHER_TOKEN))
                    .thenReturn(
                            token(
                                    TeletraanPrincipalRole.PUBLISHER,
                                    AuthZResource.ALL,
                                    AuthZResource.Type.SYSTEM,
                                    PUBLISHER_TOKEN));
            when(tokenRolesDAO.getByToken(ENV_TOKEN))
                    .thenReturn(
                            token(
                                    TeletraanPrincipalRole.OPERATOR,
                                    ENV_NAME,
                                    AuthZResource.Type.ENV,
                                    ENV_TOKEN));
            when(tokenRolesDAO.getByToken(ENV_STAGE_TOKEN))
                    .thenReturn(
                            token(
                                    TeletraanPrincipalRole.OPERATOR,
                                    ENV_NAME + "/" + STAGE_NAME,
                                    AuthZResource.Type.ENV_STAGE,
                                    ENV_STAGE_TOKEN));
            when(tokenRolesDAO.getByToken(SYSTEM_TOKEN))
                    .thenReturn(
                            token(
                                    TeletraanPrincipalRole.OPERATOR,
                                    AuthZResource.ALL,
                                    AuthZResource.Type.SYSTEM,
                                    SYSTEM_TOKEN));
            EnvironBean environment = environment(ENV_ID, ENV_NAME);
            when(environDAO.getById(ENV_ID)).thenReturn(environment);
            when(environDAO.getByStage(ENV_NAME, STAGE_NAME)).thenReturn(environment);
            EnvironBean otherEnvironment = environment(OTHER_ENV_ID, "other-environment");
            when(environDAO.getById(OTHER_ENV_ID)).thenReturn(otherEnvironment);
            when(environDAO.getByStage("other-environment", STAGE_NAME))
                    .thenReturn(otherEnvironment);
            EnvironBean otherStage = environment(OTHER_STAGE_ID, ENV_NAME);
            otherStage.setStage_name("staging");
            when(environDAO.getById(OTHER_STAGE_ID)).thenReturn(otherStage);
            when(environDAO.getByStage(ENV_NAME, "staging")).thenReturn(otherStage);
            BUILD.setBuild_id(BUILD_ID);
            BUILD.setBuild_name(BUILD_NAME);
            BUILD.setArtifact_url("s3://bucket/build.tar.gz");
            when(buildDAO.getById(BUILD_ID)).thenReturn(BUILD);
            when(TAG_DAO.getById(anyString()))
                    .thenAnswer(invocation -> STORED_TAGS.get(invocation.getArgument(0)));
            doAnswer(
                            invocation -> {
                                TagBean tag = invocation.getArgument(0);
                                STORED_TAGS.put(tag.getId(), tag);
                                return null;
                            })
                    .when(TAG_DAO)
                    .insert(any(TagBean.class));
            TokenAuthenticationFactory authentication = new TokenAuthenticationFactory();
            authentication.setUserDataUrl("http://localhost:1/user");
            return ResourceExtension.builder()
                    .addProvider(new AuthDynamicFeature(authentication.create(context)))
                    .addProvider(RolesAllowedDynamicFeature.class)
                    .addProvider(ResourceAuthZInfoFeature.class)
                    .addResource(new Tags(context))
                    .addResource(new Pindeploy(context))
                    .build();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static EnvironBean environment(String id, String name) {
        EnvironBean environment = new EnvironBean();
        environment.setEnv_id(id);
        environment.setEnv_name(name);
        environment.setStage_name(STAGE_NAME);
        return environment;
    }

    private static TokenRolesBean token(
            TeletraanPrincipalRole role, String resource, AuthZResource.Type type, String name) {
        TokenRolesBean token = new TokenRolesBean();
        token.setRole(role);
        token.setResource_id(resource);
        token.setResource_type(type);
        token.setScript_name(name);
        return token;
    }

    @BeforeEach
    void setUp() throws Exception {
        STORED_TAGS.clear();
        for (TagTargetType type : TagTargetType.values()) {
            TagBean tag = tag(type, target(type));
            tag.setId(type.name());
            if (type == TagTargetType.BUILD) {
                tag.setTarget_id(BUILD_NAME);
                tag.serializeTagMetaInfo(BUILD);
            }
            STORED_TAGS.put(tag.getId(), tag);
        }
        TagBean otherTag = tag(TagTargetType.ENVIRONMENT, OTHER_ENV_ID);
        otherTag.setId("other-env-tag");
        STORED_TAGS.put(otherTag.getId(), otherTag);
        TagBean otherStageTag = tag(TagTargetType.ENVIRONMENT, OTHER_STAGE_ID);
        otherStageTag.setId("other-stage-tag");
        STORED_TAGS.put(otherStageTag.getId(), otherStageTag);
        TagBean orphanTag = tag(TagTargetType.ENVIRONMENT, "deleted-env-id");
        orphanTag.setId("orphan-env-tag");
        STORED_TAGS.put(orphanTag.getId(), orphanTag);
        TagBean malformedTag = tag(TagTargetType.BUILD, BUILD_NAME);
        malformedTag.setId("malformed-build-tag");
        malformedTag.setMeta_info("invalid historical metadata");
        STORED_TAGS.put(malformedTag.getId(), malformedTag);
        clearInvocations(TAG_DAO, PINDEPLOY_DAO);
    }

    @ParameterizedTest
    @EnumSource(TagTargetType.class)
    void readerCannotCreateTags(TagTargetType type) throws Exception {
        try (Response response =
                request(EXT.target("/v1/tags"), READ_TOKEN)
                        .post(Entity.json(tag(type, target(type))))) {
            assertEquals(403, response.getStatus());
        }
        verifyNoMutations();
    }

    @ParameterizedTest
    @EnumSource(TagTargetType.class)
    void readerCannotDeleteTags(TagTargetType type) throws Exception {
        try (Response response =
                request(EXT.target("/v1/tags/" + type.name()), READ_TOKEN).delete()) {
            assertEquals(403, response.getStatus());
        }
        verifyNoMutations();
    }

    @ParameterizedTest
    @EnumSource(TagTargetType.class)
    void publisherCannotCreateDeployGatingTags(TagTargetType type) throws Exception {
        try (Response response =
                request(EXT.target("/v1/tags"), PUBLISHER_TOKEN)
                        .post(Entity.json(tag(type, target(type))))) {
            assertEquals(403, response.getStatus());
        }
        verifyNoMutations();
    }

    @ParameterizedTest
    @EnumSource(TagTargetType.class)
    void publisherCannotDeleteDeployGatingTags(TagTargetType type) throws Exception {
        try (Response response =
                request(EXT.target("/v1/tags/" + type.name()), PUBLISHER_TOKEN).delete()) {
            assertEquals(403, response.getStatus());
        }
        verifyNoMutations();
    }

    @ParameterizedTest
    @ValueSource(strings = {"orphan-env-tag", "malformed-build-tag"})
    void systemOperatorCanCleanUpTagsWithoutResolvableTargets(String tagId) throws Exception {
        try (Response response = request(EXT.target("/v1/tags/" + tagId), SYSTEM_TOKEN).delete()) {
            assertEquals(204, response.getStatus());
        }
        verify(TAG_DAO).delete(tagId);
    }

    @ParameterizedTest
    @CsvSource({
        "orphan-env-tag,read-token",
        "orphan-env-tag,env-token",
        "orphan-env-tag,env-stage-token",
        "malformed-build-tag,read-token",
        "malformed-build-tag,env-token",
        "malformed-build-tag,env-stage-token"
    })
    void unresolvedTargetsDoNotBypassSystemAuthorization(String tagId, String token)
            throws Exception {
        try (Response response = request(EXT.target("/v1/tags/" + tagId), token).delete()) {
            assertEquals(403, response.getStatus());
        }
        verifyNoMutations();
    }

    @Test
    void readerCannotEnablePindeploy() throws Exception {
        try (Response response =
                request(enableTarget(ENV_NAME), READ_TOKEN).post(Entity.json(""))) {
            assertEquals(403, response.getStatus());
        }
        verifyNoMutations();
    }

    @Test
    void readerCannotDisablePindeploy() throws Exception {
        try (Response response = request(disableTarget(), READ_TOKEN).delete()) {
            assertEquals(403, response.getStatus());
        }
        verifyNoMutations();
    }

    @Test
    void missingCredentialsCannotMutate() throws Exception {
        try (Response response =
                EXT.target("/v1/tags")
                        .request()
                        .post(Entity.json(tag(TagTargetType.ENVIRONMENT, ENV_ID)))) {
            assertEquals(401, response.getStatus());
        }
        try (Response response = EXT.target("/v1/tags/ENVIRONMENT").request().delete()) {
            assertEquals(401, response.getStatus());
        }
        try (Response response = enableTarget(ENV_NAME).request().post(Entity.json(""))) {
            assertEquals(401, response.getStatus());
        }
        try (Response response = disableTarget().request().delete()) {
            assertEquals(401, response.getStatus());
        }
        verifyNoMutations();
    }

    @Test
    void environmentOperatorCanCreateTagAndBodySurvivesAuthorization() throws Exception {
        TagBean input = tag(TagTargetType.ENVIRONMENT, ENV_ID);
        input.setComments("preserve this request body");
        try (Response response =
                request(EXT.target("/v1/tags"), ENV_TOKEN).post(Entity.json(input))) {
            assertEquals(201, response.getStatus());
            TagBean created = response.readEntity(TagBean.class);
            assertEquals(ENV_ID, created.getTarget_id());
            assertEquals(input.getComments(), created.getComments());
            assertEquals(ENV_TOKEN, created.getOperator());
        }
        verify(TAG_DAO).insert(any(TagBean.class));
    }

    @Test
    void environmentOperatorCanDeleteOwnTag() throws Exception {
        try (Response response = request(EXT.target("/v1/tags/ENVIRONMENT"), ENV_TOKEN).delete()) {
            assertEquals(204, response.getStatus());
        }
        verify(TAG_DAO).delete("ENVIRONMENT");
    }

    @Test
    void environmentOperatorCannotCreateAnotherEnvironmentTag() throws Exception {
        try (Response response =
                request(EXT.target("/v1/tags"), ENV_TOKEN)
                        .post(Entity.json(tag(TagTargetType.ENVIRONMENT, OTHER_ENV_ID)))) {
            assertEquals(403, response.getStatus());
        }
        verifyNoMutations();
    }

    @Test
    void environmentOperatorCannotDeleteAnotherEnvironmentTag() throws Exception {
        try (Response response =
                request(EXT.target("/v1/tags/other-env-tag"), ENV_TOKEN).delete()) {
            assertEquals(403, response.getStatus());
        }
        verifyNoMutations();
    }

    @Test
    void environmentOperatorCanEnableOwnPindeploy() throws Exception {
        try (Response response = request(enableTarget(ENV_NAME), ENV_TOKEN).post(Entity.json(""))) {
            assertEquals(204, response.getStatus());
        }
        verify(PINDEPLOY_DAO).insertOrUpdate(any(PindeployBean.class));
    }

    @Test
    void environmentOperatorCannotEnableAnotherEnvironmentPindeploy() throws Exception {
        try (Response response =
                request(enableTarget("other-environment"), ENV_TOKEN).post(Entity.json(""))) {
            assertEquals(403, response.getStatus());
        }
        verifyNoMutations();
    }

    @Test
    void environmentOperatorCannotDisablePipelineAcrossEnvironments() throws Exception {
        try (Response response = request(disableTarget(), ENV_TOKEN).delete()) {
            assertEquals(403, response.getStatus());
        }
        verifyNoMutations();
    }

    @Test
    void systemOperatorCanDisablePindeploy() throws Exception {
        try (Response response = request(disableTarget(), SYSTEM_TOKEN).delete()) {
            assertEquals(204, response.getStatus());
        }
        verify(PINDEPLOY_DAO).delete(PIPELINE);
    }

    @ParameterizedTest
    @EnumSource(TagTargetType.class)
    void systemOperatorCanCreateTags(TagTargetType type) throws Exception {
        try (Response response =
                request(EXT.target("/v1/tags"), SYSTEM_TOKEN)
                        .post(Entity.json(tag(type, target(type))))) {
            assertEquals(201, response.getStatus());
        }
        verify(TAG_DAO).insert(any(TagBean.class));
    }

    @Test
    void dashboardBuildTagPayloadRemainsAccepted() throws Exception {
        String body =
                "{\"targetId\":\""
                        + BUILD_ID
                        + "\",\"targetType\":\"Build\",\"value\":\"BAD_BUILD\","
                        + "\"comments\":\"deploy rollback, mark build as bad\"}";
        try (Response response =
                request(EXT.target("/v1/tags"), SYSTEM_TOKEN).post(Entity.json(body))) {
            assertEquals(201, response.getStatus());
            assertEquals(TagTargetType.BUILD, response.readEntity(TagBean.class).getTarget_type());
        }
        verify(TAG_DAO).insert(any(TagBean.class));
    }

    @Test
    void tagPayloadWithUnknownFieldsRemainsAccepted() throws Exception {
        String body =
                "{\"targetId\":\""
                        + ENV_ID
                        + "\",\"targetType\":\"ENVIRONMENT\",\"value\":\"DISABLE_ENV\","
                        + "\"extraClientField\":\"ignored\"}";
        try (Response response =
                request(EXT.target("/v1/tags"), ENV_TOKEN).post(Entity.json(body))) {
            assertEquals(201, response.getStatus());
            assertEquals(ENV_ID, response.readEntity(TagBean.class).getTarget_id());
        }
        verify(TAG_DAO).insert(any(TagBean.class));
    }

    @ParameterizedTest
    @EnumSource(TagTargetType.class)
    void systemOperatorCanDeleteTags(TagTargetType type) throws Exception {
        try (Response response =
                request(EXT.target("/v1/tags/" + type.name()), SYSTEM_TOKEN).delete()) {
            assertEquals(204, response.getStatus());
        }
        verify(TAG_DAO).delete(type.name());
    }

    @Test
    void environmentOperatorCannotCreateSystemTags() throws Exception {
        try (Response response =
                request(EXT.target("/v1/tags"), ENV_TOKEN)
                        .post(Entity.json(tag(TagTargetType.TELETRAAN, "TELETRAAN")))) {
            assertEquals(403, response.getStatus());
        }
        verifyNoMutations();
    }

    @Test
    void environmentOperatorCannotDeleteSystemTags() throws Exception {
        try (Response response = request(EXT.target("/v1/tags/TELETRAAN"), ENV_TOKEN).delete()) {
            assertEquals(403, response.getStatus());
        }
        verifyNoMutations();
    }

    @Test
    void stageOperatorCanCreateOwnStageTag() throws Exception {
        try (Response response =
                request(EXT.target("/v1/tags"), ENV_STAGE_TOKEN)
                        .post(Entity.json(tag(TagTargetType.ENVIRONMENT, ENV_ID)))) {
            assertEquals(201, response.getStatus());
        }
        verify(TAG_DAO).insert(any(TagBean.class));
    }

    @Test
    void stageOperatorCanDeleteOwnStageTag() throws Exception {
        try (Response response =
                request(EXT.target("/v1/tags/ENVIRONMENT"), ENV_STAGE_TOKEN).delete()) {
            assertEquals(204, response.getStatus());
        }
        verify(TAG_DAO).delete("ENVIRONMENT");
    }

    @Test
    void stageOperatorCanEnableOwnStagePindeploy() throws Exception {
        try (Response response =
                request(enableTarget(ENV_NAME), ENV_STAGE_TOKEN).post(Entity.json(""))) {
            assertEquals(204, response.getStatus());
        }
        verify(PINDEPLOY_DAO).insertOrUpdate(any(PindeployBean.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {OTHER_ENV_ID, OTHER_STAGE_ID})
    void stageOperatorCannotCreateAnotherStageTag(String targetId) throws Exception {
        try (Response response =
                request(EXT.target("/v1/tags"), ENV_STAGE_TOKEN)
                        .post(Entity.json(tag(TagTargetType.ENVIRONMENT, targetId)))) {
            assertEquals(403, response.getStatus());
        }
        verifyNoMutations();
    }

    @ParameterizedTest
    @ValueSource(strings = {"other-env-tag", "other-stage-tag"})
    void stageOperatorCannotDeleteAnotherStageTag(String tagId) throws Exception {
        try (Response response =
                request(EXT.target("/v1/tags/" + tagId), ENV_STAGE_TOKEN).delete()) {
            assertEquals(403, response.getStatus());
        }
        verifyNoMutations();
    }

    @ParameterizedTest
    @CsvSource({"other-environment,prod", "environment,staging"})
    void stageOperatorCannotEnableAnotherStagePindeploy(String environment, String stage)
            throws Exception {
        try (Response response =
                request(enableTarget(environment, stage), ENV_STAGE_TOKEN).post(Entity.json(""))) {
            assertEquals(403, response.getStatus());
        }
        verifyNoMutations();
    }

    private static WebTarget enableTarget(String environment) {
        return enableTarget(environment, STAGE_NAME);
    }

    private static WebTarget enableTarget(String environment, String stage) {
        return EXT.target("/v1/pindeploy/enable")
                .queryParam("envName", environment)
                .queryParam("stageName", stage)
                .queryParam("pipeline", PIPELINE);
    }

    private static WebTarget disableTarget() {
        return EXT.target("/v1/pindeploy/disable").queryParam("pipeline", PIPELINE);
    }

    private static Invocation.Builder request(WebTarget target, String token) {
        return target.request().header("Authorization", "token " + token);
    }

    private static TagBean tag(TagTargetType type, String target) {
        TagBean tag = new TagBean();
        tag.setTarget_type(type);
        tag.setTarget_id(target);
        tag.setValue(type == TagTargetType.BUILD ? TagValue.BAD_BUILD : TagValue.DISABLE_ENV);
        return tag;
    }

    private static String target(TagTargetType type) {
        switch (type) {
            case BUILD:
                return BUILD_ID;
            case ENVIRONMENT:
                return ENV_ID;
            default:
                return "TELETRAAN";
        }
    }

    private static void verifyNoMutations() throws Exception {
        verify(TAG_DAO, never()).insert(any(TagBean.class));
        verify(TAG_DAO, never()).delete(anyString());
        verify(PINDEPLOY_DAO, never()).insertOrUpdate(any(PindeployBean.class));
        verify(PINDEPLOY_DAO, never()).delete(anyString());
    }
}
