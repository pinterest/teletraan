/**
 * Copyright (c) 2024 Pinterest, Inc.
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.common.EntitlementEnforcementProvider;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.fixture.EnvironBeanFixture;
import com.pinterest.teletraan.TeletraanServiceContext;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import java.io.File;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.Principal;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(DropwizardExtensionsSupport.class)
class EnvStagesTest {
    private static final String ENV1 = "env1";
    private static final String STAGE1 = "stage1";
    private static final String TARGET = "/v1/envs/";
    private static final ResourceExtension EXT;
    private static EnvironDAO environDAO = mock(EnvironDAO.class);

    static {
        SecurityContext mockSecurityContext = mock(SecurityContext.class);
        Principal mockPrincipal = mock(Principal.class);
        when(mockPrincipal.getName()).thenReturn("mockPrincipal");
        when(mockSecurityContext.getUserPrincipal()).thenReturn(mockPrincipal);

        TeletraanServiceContext context = new TeletraanServiceContext();
        context.setEnvironDAO(environDAO);
        EXT =
                ResourceExtension.builder()
                        .addResource(new EnvStages(context))
                        .addProvider(new SecurityContextProvider(mockSecurityContext))
                        .build();
    }

    public static class SecurityContextProvider
            implements javax.ws.rs.ext.Provider, javax.ws.rs.core.Feature {
        private final SecurityContext securityContext;

        public SecurityContextProvider(SecurityContext securityContext) {
            this.securityContext = securityContext;
        }

        @Override
        public boolean configure(javax.ws.rs.core.FeatureContext context) {
            context.register(
                    (javax.ws.rs.container.ContainerRequestFilter)
                            requestContext -> {
                                requestContext.setSecurityContext(securityContext);
                            });
            return true;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return null;
        }
    }

    @TempDir File tempDir;

    private EnvStages resourceWithGate(String flagJson, String onboardedJson) throws Exception {
        File flags = new File(tempDir, "flags");
        File onboarded = new File(tempDir, "onboarded");
        Files.write(flags.toPath(), flagJson.getBytes(StandardCharsets.UTF_8));
        Files.write(onboarded.toPath(), onboardedJson.getBytes(StandardCharsets.UTF_8));

        TeletraanServiceContext context = new TeletraanServiceContext();
        context.setEnvironDAO(environDAO);
        EnvStages resource = new EnvStages(context);
        resource.setEntitlementEnforcementProvider(
                new EntitlementEnforcementProvider(
                        flags.getAbsolutePath(), onboarded.getAbsolutePath()));
        return resource;
    }

    @Test
    void testGet_overridesUseEntitlementsFromGate() throws Exception {
        EnvironBean bean = EnvironBeanFixture.createRandomEnvironBean();
        bean.setEnv_name(ENV1);
        bean.setStage_name(STAGE1);
        // The static DB column says false, but the live gate should flip it on.
        bean.setUse_entitlements(false);
        when(environDAO.getByStage(ENV1, STAGE1)).thenReturn(bean);

        EnvStages resource =
                resourceWithGate(
                        "{\"entitlement_enforcement\": 100}", "[\"" + ENV1 + "-" + STAGE1 + "\"]");

        assertTrue(resource.get(ENV1, STAGE1).getUse_entitlements());
    }

    @Test
    void testGet_killSwitchOffReenablesEditing() throws Exception {
        EnvironBean bean = EnvironBeanFixture.createRandomEnvironBean();
        bean.setEnv_name(ENV1);
        bean.setStage_name(STAGE1);
        // Column stale-true, but kill switch is off -> UI must re-enable editing.
        bean.setUse_entitlements(true);
        when(environDAO.getByStage(ENV1, STAGE1)).thenReturn(bean);

        EnvStages resource =
                resourceWithGate(
                        "{\"entitlement_enforcement\": 0}", "[\"" + ENV1 + "-" + STAGE1 + "\"]");

        assertFalse(resource.get(ENV1, STAGE1).getUse_entitlements());
    }

    @Test
    void testUpdate_cannotChangeIsSox() throws Exception {
        EnvironBean originalBean = EnvironBeanFixture.createRandomEnvironBean();
        EnvironBean updatedBean = EnvironBeanFixture.createRandomEnvironBean();

        Boolean[] originalSox = {null, true, false};
        Boolean[] newSox = {true, false, true};
        for (int i = 0; i < originalSox.length; i++) {
            originalBean.setIs_sox(originalSox[i]);
            updatedBean.setIs_sox(newSox[i]);
            when(environDAO.getByStage(ENV1, STAGE1)).thenReturn(originalBean);

            Response response =
                    EXT.target(TARGET + ENV1 + "/" + STAGE1)
                            .request()
                            .put(Entity.json(updatedBean));

            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }
    }
}
