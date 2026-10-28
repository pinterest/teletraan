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
package com.pinterest.teletraan.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.BuildBean;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.TagBean;
import com.pinterest.deployservice.bean.TagTargetType;
import com.pinterest.deployservice.dao.BuildDAO;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor.ExtractionException;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TagResourceResolverTest {
    private BuildDAO buildDAO;
    private EnvironDAO environDAO;
    private TagResourceResolver resolver;

    @BeforeEach
    void setUp() {
        ServiceContext context = new ServiceContext();
        buildDAO = mock(BuildDAO.class);
        environDAO = mock(EnvironDAO.class);
        context.setBuildDAO(buildDAO);
        context.setEnvironDAO(environDAO);
        resolver = new TagResourceResolver(context);
    }

    private TagBean tag(TagTargetType type, String id) {
        TagBean tag = new TagBean();
        tag.setTarget_type(type);
        tag.setTarget_id(id);
        return tag;
    }

    private BuildBean build() {
        BuildBean build = new BuildBean();
        build.setBuild_id("build-id");
        build.setBuild_name("build-name");
        build.setArtifact_url("s3://build-bucket/artifact");
        return build;
    }

    @Test
    void createBuildUsesDatabaseTargetAndIgnoresClientMetadata() throws Exception {
        TagBean tag = tag(TagTargetType.BUILD, "build-id");
        BuildBean spoofed = build();
        spoofed.setBuild_name("other-build");
        tag.serializeTagMetaInfo(spoofed);
        when(buildDAO.getById("build-id")).thenReturn(build());

        AuthZResource resource = resolver.resolve(tag, false);

        assertEquals(AuthZResource.Type.BUILD, resource.getType());
        assertEquals("build-name", resource.getName());
        assertEquals(
                "s3://build-bucket/artifact",
                resource.getAttributes()
                        .get(AuthZResource.AttributeKeys.BUILD_ARTIFACT_URL.name()));
    }

    @Test
    void deleteBuildUsesPersistedSnapshotWithNormalizedBuildName() throws Exception {
        TagBean tag = tag(TagTargetType.BUILD, "build-name");
        tag.serializeTagMetaInfo(build());

        AuthZResource resource = resolver.resolve(tag, true);

        assertEquals("build-name", resource.getName());
        assertEquals(AuthZResource.Type.BUILD, resource.getType());
        assertEquals(
                "s3://build-bucket/artifact",
                resource.getAttributes()
                        .get(AuthZResource.AttributeKeys.BUILD_ARTIFACT_URL.name()));
        verifyNoInteractions(buildDAO);
    }

    @Test
    void deleteBuildWithMissingMalformedOrInconsistentMetadataRequiresSystemPermission()
            throws Exception {
        TagBean tag = tag(TagTargetType.BUILD, "build-name");
        assertEquals(AuthZResource.SYSTEM_RESOURCE, resolver.resolve(tag, true));
        tag.setMeta_info("not json");
        assertEquals(AuthZResource.SYSTEM_RESOURCE, resolver.resolve(tag, true));
        tag.setMeta_info("null");
        assertEquals(AuthZResource.SYSTEM_RESOURCE, resolver.resolve(tag, true));
        tag.serializeTagMetaInfo(new BuildBean());
        assertEquals(AuthZResource.SYSTEM_RESOURCE, resolver.resolve(tag, true));
        tag.serializeTagMetaInfo(build());
        tag.setTarget_id("other-build");
        assertEquals(AuthZResource.SYSTEM_RESOURCE, resolver.resolve(tag, true));
    }

    @Test
    void environmentTargetUsesDatabaseStageAndMatchesStageScopedGrant() throws Exception {
        EnvironBean env = new EnvironBean();
        env.setEnv_name("test-env");
        env.setStage_name("prod");
        env.setIs_sox(true);
        when(environDAO.getById("env-id")).thenReturn(env);

        AuthZResource resource = resolver.resolve(tag(TagTargetType.ENVIRONMENT, "env-id"), false);

        assertEquals("test-env/prod", resource.getName());
        assertEquals(AuthZResource.Type.ENV_STAGE, resource.getType());
        assertEquals(new AuthZResource("test-env", "prod"), resource);
        assertEquals(resource, resolver.resolve(tag(TagTargetType.ENVIRONMENT, "env-id"), true));
    }

    @Test
    void teletraanTargetRequiresSystemAuthorization() throws Exception {
        TagBean tag = tag(TagTargetType.TELETRAAN, "TELETRAAN");
        assertEquals(AuthZResource.SYSTEM_RESOURCE, resolver.resolve(tag, false));
        assertEquals(AuthZResource.SYSTEM_RESOURCE, resolver.resolve(tag, true));
    }

    @Test
    void missingTargetFieldsAreRejected() {
        assertThrows(BadRequestException.class, () -> resolver.resolve(null, false));
        assertThrows(BadRequestException.class, () -> resolver.resolve(new TagBean(), false));
        assertThrows(
                BadRequestException.class,
                () -> resolver.resolve(tag(TagTargetType.ENVIRONMENT, " "), false));
    }

    @Test
    void missingTargetsDoNotFallBackToUnspecifiedOrSystem() {
        assertThrows(
                NotFoundException.class,
                () -> resolver.resolve(tag(TagTargetType.BUILD, "missing"), false));
        assertThrows(
                NotFoundException.class,
                () -> resolver.resolve(tag(TagTargetType.ENVIRONMENT, "missing"), false));
    }

    @Test
    void deletingOrphanedEnvironmentTagRequiresSystemPermission() throws Exception {
        TagBean tag = tag(TagTargetType.ENVIRONMENT, "deleted-environment");

        assertEquals(AuthZResource.SYSTEM_RESOURCE, resolver.resolve(tag, true));
        assertThrows(NotFoundException.class, () -> resolver.resolve(tag, false));
    }

    @Test
    void storedMissingTargetFieldsRequireSystemPermission() throws Exception {
        assertEquals(AuthZResource.SYSTEM_RESOURCE, resolver.resolve(new TagBean(), true));
        assertEquals(
                AuthZResource.SYSTEM_RESOURCE,
                resolver.resolve(tag(TagTargetType.ENVIRONMENT, " "), true));
        assertThrows(BadRequestException.class, () -> resolver.resolve(null, true));
    }

    @Test
    void databaseFailuresDoNotAuthorize() throws Exception {
        when(buildDAO.getById("build-id")).thenThrow(new Exception("database unavailable"));
        when(environDAO.getById("env-id")).thenThrow(new Exception("database unavailable"));
        assertThrows(
                ExtractionException.class,
                () -> resolver.resolve(tag(TagTargetType.BUILD, "build-id"), false));
        assertThrows(
                ExtractionException.class,
                () -> resolver.resolve(tag(TagTargetType.ENVIRONMENT, "env-id"), false));
        assertThrows(
                ExtractionException.class,
                () -> resolver.resolve(tag(TagTargetType.ENVIRONMENT, "env-id"), true));
    }
}
