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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.PindeployBean;
import com.pinterest.deployservice.bean.TagBean;
import com.pinterest.deployservice.bean.TagTargetType;
import com.pinterest.deployservice.bean.TagValue;
import com.pinterest.deployservice.bean.TeletraanPrincipalRole;
import com.pinterest.deployservice.dao.BuildDAO;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.PindeployDAO;
import com.pinterest.deployservice.dao.TagDAO;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.security.TeletraanAuthZResourceExtractorFactory;
import com.pinterest.teletraan.universal.security.BaseAuthorizer;
import com.pinterest.teletraan.universal.security.Constants;
import com.pinterest.teletraan.universal.security.EnvoyAuthFilter;
import com.pinterest.teletraan.universal.security.EnvoyAuthenticator;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfoFeature;
import com.pinterest.teletraan.universal.security.TeletraanAuthorizer;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import com.pinterest.teletraan.universal.security.bean.ServicePrincipal;
import com.pinterest.teletraan.universal.security.bean.TeletraanPrincipal;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Exercises Envoy identities and endpoint permission alternatives; policy rules live in pinconf.
 */
@ExtendWith(DropwizardExtensionsSupport.class)
class PindeployServiceAuthorizationTest {
    private static final String SERVICE = "spiffe://svc.example.com/pindeploy/prod/main";
    private static final String ENV_NAME = "environment";
    private static final String STAGE_NAME = "prod";
    private static final String ENV_ID = "environment-id";
    private static final String PIPELINE = "pipeline";
    private static final AuthZResource ENV_STAGE = new AuthZResource(ENV_NAME, STAGE_NAME);
    private static final PindeployDAO PINDEPLOY_DAO = mock(PindeployDAO.class);
    private static final TagDAO TAG_DAO = mock(TagDAO.class);

    @SuppressWarnings("unchecked")
    private static final TeletraanAuthorizer<TeletraanPrincipal> POLICY =
            mock(TeletraanAuthorizer.class);

    private static final ResourceExtension EXT = createExtension();

    private static ResourceExtension createExtension() {
        try {
            TeletraanServiceContext context = new TeletraanServiceContext();
            EnvironDAO environDAO = mock(EnvironDAO.class);
            context.setEnvironDAO(environDAO);
            context.setBuildDAO(mock(BuildDAO.class));
            context.setPindeployDAO(PINDEPLOY_DAO);
            context.setTagDAO(TAG_DAO);
            TeletraanAuthZResourceExtractorFactory factory =
                    new TeletraanAuthZResourceExtractorFactory(context);
            BaseAuthorizer<TeletraanPrincipal> authorizer =
                    new BaseAuthorizer<TeletraanPrincipal>(factory) {
                        @Override
                        public boolean authorize(
                                TeletraanPrincipal principal,
                                String role,
                                AuthZResource resource,
                                ContainerRequestContext request) {
                            return POLICY.authorize(principal, role, resource, request);
                        }
                    };
            EnvironBean environment = new EnvironBean();
            environment.setEnv_id(ENV_ID);
            environment.setEnv_name(ENV_NAME);
            environment.setStage_name(STAGE_NAME);
            when(environDAO.getByStage(ENV_NAME, STAGE_NAME)).thenReturn(environment);
            when(environDAO.getById(ENV_ID)).thenReturn(environment);
            TagBean tag = environmentTag();
            tag.setId("tag-id");
            when(TAG_DAO.getById("tag-id")).thenReturn(tag);
            return ResourceExtension.builder()
                    .addProvider(
                            new AuthDynamicFeature(
                                    new EnvoyAuthFilter.Builder<TeletraanPrincipal>()
                                            .setAuthenticator(new EnvoyAuthenticator())
                                            .setAuthorizer(authorizer)
                                            .buildAuthFilter()))
                    .addProvider(RolesAllowedDynamicFeature.class)
                    .addProvider(ResourceAuthZInfoFeature.class)
                    .addResource(new Pindeploy(context))
                    .addResource(new Tags(context))
                    .build();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @BeforeEach
    void setUp() {
        reset(POLICY);
        clearInvocations(PINDEPLOY_DAO, TAG_DAO);
    }

    @ParameterizedTest
    @ValueSource(strings = {SERVICE, "spiffe://example.com/teletraan/pindeploy/prod"})
    void mappingPermissionEnablesAndDisablesWithoutGeneralMutationPermission(String identity)
            throws Exception {
        grant(identity, TeletraanPrincipalRole.Names.MANAGE_PINDEPLOY, ENV_STAGE);
        grant(
                identity,
                TeletraanPrincipalRole.Names.MANAGE_PINDEPLOY,
                AuthZResource.SYSTEM_RESOURCE);
        try (Response enabled = request(enableTarget(STAGE_NAME), identity).post(Entity.json(""));
                Response disabled = request(disableTarget(), identity).delete()) {
            assertEquals(204, enabled.getStatus());
            assertEquals(204, disabled.getStatus());
        }
        verify(PINDEPLOY_DAO).insertOrUpdate(any(PindeployBean.class));
        verify(PINDEPLOY_DAO).delete(PIPELINE);
        verify(POLICY)
                .authorize(
                        eq(new ServicePrincipal(identity)),
                        eq(TeletraanPrincipalRole.Names.MANAGE_PINDEPLOY),
                        eq(ENV_STAGE),
                        any());
    }

    @Test
    void stageMappingPermissionCannotDisablePipelineOrEnableAnotherStage() throws Exception {
        grant(SERVICE, TeletraanPrincipalRole.Names.MANAGE_PINDEPLOY, ENV_STAGE);
        try (Response disabled = request(disableTarget(), SERVICE).delete();
                Response otherStage =
                        request(enableTarget("staging"), SERVICE).post(Entity.json(""))) {
            assertEquals(403, disabled.getStatus());
            assertEquals(403, otherStage.getStatus());
        }
        verifyNoPipelineMutations();
    }

    @Test
    void mappingPermissionDoesNotAllowTagMutations() throws Exception {
        grant(SERVICE, TeletraanPrincipalRole.Names.MANAGE_PINDEPLOY, ENV_STAGE);
        grant(
                SERVICE,
                TeletraanPrincipalRole.Names.MANAGE_PINDEPLOY,
                AuthZResource.SYSTEM_RESOURCE);
        try (Response created =
                        request(EXT.target("/v1/tags"), SERVICE)
                                .post(Entity.json(environmentTag()));
                Response deleted = request(EXT.target("/v1/tags/tag-id"), SERVICE).delete()) {
            assertEquals(403, created.getStatus());
            assertEquals(403, deleted.getStatus());
        }
        verify(TAG_DAO, never()).insert(any(TagBean.class));
        verify(TAG_DAO, never()).delete(any());
    }

    @Test
    void readerCannotMutatePipelineMappings() throws Exception {
        grant(SERVICE, TeletraanPrincipalRole.Names.READ, ENV_STAGE);
        grant(SERVICE, TeletraanPrincipalRole.Names.READ, AuthZResource.SYSTEM_RESOURCE);
        try (Response enabled = request(enableTarget(STAGE_NAME), SERVICE).post(Entity.json(""));
                Response disabled = request(disableTarget(), SERVICE).delete()) {
            assertEquals(403, enabled.getStatus());
            assertEquals(403, disabled.getStatus());
        }
        verifyNoPipelineMutations();
    }

    @Test
    void anotherServiceCannotUsePindeployGrant() throws Exception {
        grant(SERVICE, TeletraanPrincipalRole.Names.MANAGE_PINDEPLOY, ENV_STAGE);
        grant(
                SERVICE,
                TeletraanPrincipalRole.Names.MANAGE_PINDEPLOY,
                AuthZResource.SYSTEM_RESOURCE);
        String other = "spiffe://svc.example.com/another-service/prod/main";
        try (Response enabled = request(enableTarget(STAGE_NAME), other).post(Entity.json(""));
                Response disabled = request(disableTarget(), other).delete()) {
            assertEquals(403, enabled.getStatus());
            assertEquals(403, disabled.getStatus());
        }
        verifyNoPipelineMutations();
    }

    @Test
    void existingWriteAndDeletePermissionsStillAuthorizeMappings() throws Exception {
        grant(SERVICE, TeletraanPrincipalRole.Names.WRITE, ENV_STAGE);
        grant(SERVICE, TeletraanPrincipalRole.Names.DELETE, AuthZResource.SYSTEM_RESOURCE);
        try (Response enabled = request(enableTarget(STAGE_NAME), SERVICE).post(Entity.json(""));
                Response disabled = request(disableTarget(), SERVICE).delete()) {
            assertEquals(204, enabled.getStatus());
            assertEquals(204, disabled.getStatus());
        }
        verify(PINDEPLOY_DAO).insertOrUpdate(any(PindeployBean.class));
        verify(PINDEPLOY_DAO).delete(PIPELINE);
    }

    private static TagBean environmentTag() {
        TagBean tag = new TagBean();
        tag.setTarget_type(TagTargetType.ENVIRONMENT);
        tag.setTarget_id(ENV_ID);
        tag.setValue(TagValue.DISABLE_ENV);
        return tag;
    }

    private void grant(String identity, String action, AuthZResource resource) {
        when(POLICY.authorize(eq(new ServicePrincipal(identity)), eq(action), eq(resource), any()))
                .thenReturn(true);
    }

    private WebTarget enableTarget(String stage) {
        return EXT.target("/v1/pindeploy/enable")
                .queryParam("envName", ENV_NAME)
                .queryParam("stageName", stage)
                .queryParam("pipeline", PIPELINE);
    }

    private WebTarget disableTarget() {
        return EXT.target("/v1/pindeploy/disable").queryParam("pipeline", PIPELINE);
    }

    private Invocation.Builder request(WebTarget target, String identity) {
        return target.request().header(Constants.CLIENT_CERT_HEADER, "URI=" + identity);
    }

    private void verifyNoPipelineMutations() throws Exception {
        verify(PINDEPLOY_DAO, never()).insertOrUpdate(any(PindeployBean.class));
        verify(PINDEPLOY_DAO, never()).delete(any());
    }
}
