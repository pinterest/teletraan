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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pinterest.deployservice.bean.AcceptanceStatus;
import com.pinterest.deployservice.bean.DeployBean;
import com.pinterest.deployservice.bean.DeployState;
import com.pinterest.deployservice.bean.DeployType;
import com.pinterest.deployservice.dao.DeployDAO;
import com.pinterest.teletraan.TeletraanServiceContext;
import java.security.Principal;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * BUG-318661: deploy PUT is authorized against the stored deploy's env. Hotfixes derive their repo
 * and CI job from a deploy's build, so PUT must not let callers rewrite a deploy's env or build.
 */
class DeploysTest {
    private static final String DEPLOY_ID = "attacker-deploy-001";

    private DeployDAO deployDAO;
    private Deploys deploys;
    private SecurityContext securityContext;

    @BeforeEach
    void setUp() {
        deployDAO = mock(DeployDAO.class);
        TeletraanServiceContext context = new TeletraanServiceContext();
        context.setDeployDAO(deployDAO);
        deploys = new Deploys(context);

        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("lab-attacker");
        securityContext = mock(SecurityContext.class);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
    }

    @Test
    void updateWritesOnlyAcceptanceStatusAndDescription() throws Exception {
        DeployBean body = new DeployBean();
        body.setAcc_status(AcceptanceStatus.ACCEPTED);
        body.setDescription("looks good");
        body.setDeploy_id("victim-deploy-001");
        body.setEnv_id("victim-env-prod");
        body.setBuild_id("victim-build-001");
        body.setDeploy_type(DeployType.HOTFIX);
        body.setState(DeployState.SUCCEEDING);
        body.setOperator("someone-else");
        body.setStart_date(1L);
        body.setSuc_date(2L);
        body.setSuc_total(10);
        body.setFail_total(0);
        body.setTotal(10);
        body.setFrom_deploy("victim-deploy-000");
        body.setAlias("victim-alias");

        deploys.update(securityContext, DEPLOY_ID, body);

        DeployBean written = captureUpdate();
        assertEquals(AcceptanceStatus.ACCEPTED, written.getAcc_status());
        assertEquals("looks good", written.getDescription());
        assertNotNull(written.getLast_update());
        assertNull(written.getDeploy_id());
        assertNull(written.getEnv_id());
        assertNull(written.getBuild_id());
        assertNull(written.getDeploy_type());
        assertNull(written.getState());
        assertNull(written.getOperator());
        assertNull(written.getStart_date());
        assertNull(written.getSuc_date());
        assertNull(written.getSuc_total());
        assertNull(written.getFail_total());
        assertNull(written.getTotal());
        assertNull(written.getFrom_deploy());
        assertNull(written.getAlias());
    }

    @Test
    void updateAcceptsDescriptionOnly() throws Exception {
        // deploy-board's "edit description" sends only {"description": ...}.
        DeployBean body = new DeployBean();
        body.setDescription("new description");

        deploys.update(securityContext, DEPLOY_ID, body);

        DeployBean written = captureUpdate();
        assertEquals("new description", written.getDescription());
        assertNull(written.getAcc_status());
    }

    @Test
    void updateRequiresASupportedField() throws Exception {
        DeployBean body = new DeployBean();
        body.setBuild_id("victim-build-001");
        body.setEnv_id("victim-env-prod");

        assertThrows(
                BadRequestException.class, () -> deploys.update(securityContext, DEPLOY_ID, body));
        assertThrows(
                BadRequestException.class, () -> deploys.update(securityContext, DEPLOY_ID, null));
        verify(deployDAO, never()).update(anyString(), any());
    }

    private DeployBean captureUpdate() throws Exception {
        ArgumentCaptor<DeployBean> updated = ArgumentCaptor.forClass(DeployBean.class);
        verify(deployDAO).update(eq(DEPLOY_ID), updated.capture());
        return updated.getValue();
    }
}
