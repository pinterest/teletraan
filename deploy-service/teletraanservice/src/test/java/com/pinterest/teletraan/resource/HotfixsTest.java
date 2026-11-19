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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pinterest.deployservice.bean.BuildBean;
import com.pinterest.deployservice.bean.DeployBean;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.HotfixBean;
import com.pinterest.deployservice.bean.HotfixState;
import com.pinterest.deployservice.dao.BuildDAO;
import com.pinterest.deployservice.dao.DeployDAO;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.HotfixDAO;
import com.pinterest.teletraan.TeletraanServiceContext;
import java.security.Principal;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * BUG-318661: hotfix authorization uses envName (create) or the stored hotfix's env (update), so
 * the base deploy, repo and CI job a hotfix acts on must not come from another environment.
 */
class HotfixsTest {
    private static final String ATTACKER_ENV = "attacker-env";

    private HotfixDAO hotfixDAO;
    private Hotfixs hotfixs;
    private SecurityContext securityContext;
    private UriInfo uriInfo;

    @BeforeEach
    void setUp() throws Exception {
        DeployDAO deployDAO = mock(DeployDAO.class);
        BuildDAO buildDAO = mock(BuildDAO.class);
        EnvironDAO environDAO = mock(EnvironDAO.class);
        hotfixDAO = mock(HotfixDAO.class);

        TeletraanServiceContext context = new TeletraanServiceContext();
        context.setDeployDAO(deployDAO);
        context.setBuildDAO(buildDAO);
        context.setEnvironDAO(environDAO);
        context.setHotfixDAO(hotfixDAO);
        hotfixs = new Hotfixs(context);

        when(deployDAO.getById("victim-deploy-001"))
                .thenReturn(deploy("victim-env-prod", "victim-build-001"));
        when(deployDAO.getById("attacker-deploy-001"))
                .thenReturn(deploy("attacker-env-prod", "attacker-build-001"));
        when(environDAO.getById("victim-env-prod")).thenReturn(env("victim-env"));
        when(environDAO.getById("attacker-env-prod")).thenReturn(env(ATTACKER_ENV));
        when(buildDAO.getById("victim-build-001"))
                .thenReturn(build("pinternal/victim-critical-service"));
        when(buildDAO.getById("attacker-build-001")).thenReturn(build("pinternal/attacker-svc"));

        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("lab-attacker");
        securityContext = mock(SecurityContext.class);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        uriInfo = mock(UriInfo.class);
        when(uriInfo.getAbsolutePathBuilder())
                .thenAnswer(i -> UriBuilder.fromUri("http://localhost/v1/hotfixs"));
    }

    @Test
    void createRejectsBaseDeployFromAnotherEnv() throws Exception {
        assertThrows(
                ForbiddenException.class,
                () ->
                        hotfixs.create(
                                securityContext,
                                uriInfo,
                                request(ATTACKER_ENV, "victim-deploy-001")));
        verify(hotfixDAO, never()).insert(any());
    }

    @Test
    void createRejectsUnknownBaseDeploy() throws Exception {
        assertThrows(
                NotFoundException.class,
                () -> hotfixs.create(securityContext, uriInfo, request(ATTACKER_ENV, "missing")));
        verify(hotfixDAO, never()).insert(any());
    }

    @Test
    void createDerivesRepoAndJobFromSameEnvDeploy() throws Exception {
        assertEquals(
                201,
                hotfixs.create(
                                securityContext,
                                uriInfo,
                                request(ATTACKER_ENV, "attacker-deploy-001"))
                        .getStatus());

        ArgumentCaptor<HotfixBean> inserted = ArgumentCaptor.forClass(HotfixBean.class);
        verify(hotfixDAO).insert(inserted.capture());
        assertEquals(ATTACKER_ENV, inserted.getValue().getEnv_name());
        assertEquals("pinternal/attacker-svc", inserted.getValue().getRepo());
        assertEquals("attacker-svc-hotfix-job", inserted.getValue().getJob_name());
        assertEquals("lab-attacker", inserted.getValue().getOperator());
    }

    @Test
    void updateWritesOnlyJobNum() throws Exception {
        HotfixBean body = request("victim-env", "victim-deploy-001");
        body.setJob_num("42");
        body.setJob_name("victim-critical-service-hotfix-job");
        body.setRepo("pinternal/victim-critical-service");
        body.setState(HotfixState.INITIAL);
        body.setOperator("someone-else");

        hotfixs.update("du9snDEBQ-KEYRI4X4pVjA", body);

        ArgumentCaptor<HotfixBean> updated = ArgumentCaptor.forClass(HotfixBean.class);
        verify(hotfixDAO).update(eq("du9snDEBQ-KEYRI4X4pVjA"), updated.capture());
        HotfixBean written = updated.getValue();
        assertEquals("42", written.getJob_num());
        assertNull(written.getEnv_name());
        assertNull(written.getBase_deploy());
        assertNull(written.getRepo());
        assertNull(written.getJob_name());
        assertNull(written.getCommits());
        assertNull(written.getState());
        assertNull(written.getOperator());
    }

    @Test
    void updateRequiresJobNum() throws Exception {
        HotfixBean body = new HotfixBean();
        body.setCommits("feedface");
        body.setBase_deploy("victim-deploy-001");

        assertThrows(BadRequestException.class, () -> hotfixs.update("id", body));
        assertThrows(BadRequestException.class, () -> hotfixs.update("id", null));
        verify(hotfixDAO, never()).update(anyString(), any());
    }

    private static HotfixBean request(String envName, String baseDeployId) {
        HotfixBean bean = new HotfixBean();
        bean.setEnv_name(envName);
        bean.setBase_deploy(baseDeployId);
        bean.setCommits("cafebabe");
        return bean;
    }

    private static DeployBean deploy(String envId, String buildId) {
        DeployBean bean = new DeployBean();
        bean.setEnv_id(envId);
        bean.setBuild_id(buildId);
        return bean;
    }

    private static EnvironBean env(String envName) {
        EnvironBean bean = new EnvironBean();
        bean.setEnv_name(envName);
        return bean;
    }

    private static BuildBean build(String repo) {
        BuildBean bean = new BuildBean();
        bean.setScm_repo(repo);
        return bean;
    }
}
