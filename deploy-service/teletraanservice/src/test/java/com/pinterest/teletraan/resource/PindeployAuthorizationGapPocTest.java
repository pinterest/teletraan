/**
 * Copyright (c) 2016-2026 Pinterest, Inc.
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.PindeployBean;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.PindeployDAO;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.config.AuthorizationFactory;
import com.pinterest.teletraan.universal.security.TeletraanAuthorizer;
import com.pinterest.teletraan.universal.security.bean.TeletraanPrincipal;
import com.pinterest.teletraan.universal.security.bean.UserPrincipal;
import java.util.Collections;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression test for the authorization gap in {@code Pindeploy.enablePindeployPipeline}/{@code
 * disablePindeployPipeline}: neither method previously carried any per-instance authorization
 * check (only the class-level {@code @RolesAllowed(READ)}, the lowest role, granted to every
 * authenticated Teletraan user). A caller with zero grants on an environment could flip its
 * pindeploy pipeline wiring. The fix adds an explicit on-the-fly WRITE check
 * (requireWriteOnEnvStage) mirroring the existing EnvironmentHandler.validateSystemPriorityPermission
 * pattern, since the framework's @ResourceAuthZInfo annotation does not support query-param-based
 * resource ids (see TeletraanAuthZResourceExtractorFactory).
 *
 * <p>These tests confirm: (1) a caller who is NOT authorized on the target env/stage is now
 * rejected with ForbiddenException before any DAO write happens, and (2) a caller who IS
 * authorized still succeeds, so the fix does not break the legitimate path.
 */
public class PindeployAuthorizationGapPocTest {

    private PindeployDAO pindeployDAO;
    private EnvironDAO environDAO;
    private AuthorizationFactory authorizationFactory;
    private TeletraanAuthorizer<TeletraanPrincipal> authorizer;
    private TeletraanServiceContext context;
    private SecurityContext sc;

    @SuppressWarnings("unchecked")
    @BeforeEach
    public void setUp() throws Exception {
        pindeployDAO = mock(PindeployDAO.class);
        environDAO = mock(EnvironDAO.class);
        authorizationFactory = mock(AuthorizationFactory.class);
        authorizer = mock(TeletraanAuthorizer.class);

        context = new TeletraanServiceContext();
        context.setPindeployDAO(pindeployDAO);
        context.setEnvironDAO(environDAO);
        context.setAuthorizationFactory(authorizationFactory);

        when(authorizationFactory.createSecondaryAuthorizer(any(), any())).thenReturn(authorizer);

        sc = mock(SecurityContext.class);
        when(sc.getUserPrincipal())
                .thenReturn(
                        new UserPrincipal("attacker-with-only-baseline-read", Collections.emptyList()));
    }

    @Test
    public void enable_rejectsCallerWithNoWriteGrantOnTargetEnv() throws Exception {
        String victimEnv = "payments-critical-service";
        String victimStage = "prod";
        String victimEnvId = "env-payments-prod-001";

        EnvironBean victimEnvBean = new EnvironBean();
        victimEnvBean.setEnv_id(victimEnvId);
        victimEnvBean.setEnv_name(victimEnv);
        victimEnvBean.setStage_name(victimStage);
        when(environDAO.getByStage(victimEnv, victimStage)).thenReturn(victimEnvBean);

        // Not authorized on the victim's env/stage.
        when(authorizer.authorize(any(), anyString(), any(), any())).thenReturn(false);

        Pindeploy pindeploy = new Pindeploy(context);

        assertThrows(
                ForbiddenException.class,
                () ->
                        pindeploy.enablePindeployPipeline(
                                sc, victimEnv, victimStage, "attacker-controlled-pipeline"));

        verify(pindeployDAO, never()).insertOrUpdate(any());
    }

    @Test
    public void enable_allowsCallerWithWriteGrantOnTargetEnv() throws Exception {
        String ownEnv = "my-own-service";
        String ownStage = "prod";
        String ownEnvId = "env-my-own-prod-001";

        EnvironBean ownEnvBean = new EnvironBean();
        ownEnvBean.setEnv_id(ownEnvId);
        ownEnvBean.setEnv_name(ownEnv);
        ownEnvBean.setStage_name(ownStage);
        when(environDAO.getByStage(ownEnv, ownStage)).thenReturn(ownEnvBean);

        // Authorized (WRITE) on this specific env/stage.
        when(authorizer.authorize(any(), eq("WRITE"), any(), any())).thenReturn(true);

        Pindeploy pindeploy = new Pindeploy(context);
        pindeploy.enablePindeployPipeline(sc, ownEnv, ownStage, "my-own-pipeline");

        verify(pindeployDAO, times(1)).insertOrUpdate(any());
    }

    @Test
    public void disable_rejectsCallerWithNoWriteGrantOnOwningEnv() throws Exception {
        String pipeline = "some-other-teams-pipeline";
        String victimEnvId = "env-payments-prod-001";

        PindeployBean existing = new PindeployBean();
        existing.setEnv_id(victimEnvId);
        existing.setPipeline(pipeline);
        when(pindeployDAO.getByPipeline(pipeline)).thenReturn(existing);

        EnvironBean victimEnvBean = new EnvironBean();
        victimEnvBean.setEnv_id(victimEnvId);
        victimEnvBean.setEnv_name("payments-critical-service");
        victimEnvBean.setStage_name("prod");
        when(environDAO.getById(victimEnvId)).thenReturn(victimEnvBean);

        when(authorizer.authorize(any(), anyString(), any(), any())).thenReturn(false);

        Pindeploy pindeploy = new Pindeploy(context);

        assertThrows(
                ForbiddenException.class, () -> pindeploy.disablePindeployPipeline(sc, pipeline));

        verify(pindeployDAO, never()).delete(anyString());
    }
}
