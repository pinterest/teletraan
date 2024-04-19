/*
 * Copyright 2024 Pinterest, Inc.
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

package com.pinterest.deployservice.handler;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.allowlists.BuildAllowlistImpl;
import com.pinterest.deployservice.bean.BuildBean;
import com.pinterest.deployservice.bean.EnvType;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.common.DeployInternalException;
import com.pinterest.deployservice.dao.BuildDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.WebApplicationException;
import java.util.ArrayList;

import static com.pinterest.deployservice.handler.DeployHandler.ERROR_BUILD_NAME_NOT_MATCH_STAGE_CONFIG;
import static com.pinterest.deployservice.handler.DeployHandler.ERROR_EMPTY_BUILD_ID;
import static com.pinterest.deployservice.handler.DeployHandler.ERROR_NON_PRIVATE_UNTRUSTED_LOCATION;
import static com.pinterest.deployservice.handler.DeployHandler.ERROR_STAGE_NOT_ALLOW_PRIVATE_BUILD;
import static com.pinterest.deployservice.handler.DeployHandler.ERROR_STAGE_REQUIRES_SOX_BUILD_COMPLIANT_SOURCE;
import static com.pinterest.deployservice.handler.DeployHandler.ERROR_STAGE_REQUIRES_SOX_BUILD_COMPLIANT_STAGE;
import static com.pinterest.deployservice.handler.DeployHandler.PRIVATE_BUILD_SCM_BRANCH;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeployHandlerTest {
    private static DeployHandler deployHandler;
    private BuildDAO buildDAO;
    private final String BUILD_ID = "buildId01";
    private final String NON_PRIVATE_BUILD_SCM_BRANCH = "non_private";
    private final String FAIL_MESSAGE = "Should have thrown WebApplicationException: %s";

    private ServiceContext createMockServiceContext() throws Exception {
        buildDAO = mock(BuildDAO.class);

        ServiceContext serviceContext = new ServiceContext();
        serviceContext.setBuildDAO(buildDAO);
        serviceContext.setBuildAllowlist(new BuildAllowlistImpl(new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
        return serviceContext;
    }

    @BeforeEach
    public void setUp() throws Exception {
        deployHandler = new DeployHandler(createMockServiceContext());
    }

    @Test
    public void validateBuild() throws Exception {
        when(buildDAO.getById(BUILD_ID)).thenReturn(genBuildBean(PRIVATE_BUILD_SCM_BRANCH));

        EnvironBean envBean = genDefaultEnvBean();
        envBean.setAllow_private_build(true);
        assertDoesNotThrow(() -> deployHandler.validateBuild(envBean, BUILD_ID));
    }

    @Test
    public void validateBuildNonPrivate() throws Exception {
        when(buildDAO.getById(BUILD_ID)).thenReturn(genBuildBean(NON_PRIVATE_BUILD_SCM_BRANCH));

        EnvironBean envBean = genDefaultEnvBean();
        envBean.setAllow_private_build(true);
        assertDoesNotThrow(() -> deployHandler.validateBuild(envBean, BUILD_ID));
    }

    @Test
    public void validateBuildFailBuildIdEmpty() throws Exception {
        EnvironBean envBean = genDefaultEnvBean();

        Throwable throwable = assertThrows(WebApplicationException.class,
                () -> deployHandler.validateBuild(envBean, ""));
        assertTrue(throwable.getMessage().contains(ERROR_EMPTY_BUILD_ID));
    }

    @Test
    public void validateBuildFailBuildNotMatchStageConfig() throws Exception {
        EnvironBean envBean = genDefaultEnvBean();
        envBean.setBuild_name("other");
        when(buildDAO.getById(BUILD_ID)).thenReturn(genBuildBean(NON_PRIVATE_BUILD_SCM_BRANCH));

        Throwable throwable = assertThrows(DeployInternalException.class,
                () -> deployHandler.validateBuild(envBean, BUILD_ID));
        assertTrue(throwable.getMessage().contains(
                String.format(ERROR_BUILD_NAME_NOT_MATCH_STAGE_CONFIG,
                        genBuildBean(NON_PRIVATE_BUILD_SCM_BRANCH).getBuild_name(),
                        envBean.getBuild_name())));
    }

    @Test
    public void validateBuildFailNonPrivateFromUntrustedLocation() throws Exception {
        BuildBean buildBean = genBuildBean(NON_PRIVATE_BUILD_SCM_BRANCH);
        when(buildDAO.getById(BUILD_ID)).thenReturn(buildBean);
        EnvironBean envBean = genDefaultEnvBean();
        envBean.setEnsure_trusted_build(true);
        Throwable throwable = assertThrows(WebApplicationException.class,
                () -> deployHandler.validateBuild(envBean, BUILD_ID));
        assertTrue(throwable.getMessage().contains(
                String.format(ERROR_NON_PRIVATE_UNTRUSTED_LOCATION,
                        buildBean.getArtifact_url()))
        );
    }

    @Test
    public void validateBuildFailErrorStageNotAllowedPrivateBuild() throws Exception {
        BuildBean buildBean = genBuildBean(PRIVATE_BUILD_SCM_BRANCH);
        when(buildDAO.getById(BUILD_ID)).thenReturn(buildBean);

        Throwable throwable = assertThrows(WebApplicationException.class,
                () -> deployHandler.validateBuild(genDefaultEnvBean(), BUILD_ID));
        assertTrue(throwable.getMessage().contains(ERROR_STAGE_NOT_ALLOW_PRIVATE_BUILD));
    }

    @Test
    public void validateBuildFailErrorStageRequiresSoxBuild() throws Exception {
        BuildBean buildBean = genBuildBean(PRIVATE_BUILD_SCM_BRANCH);
        when(buildDAO.getById(BUILD_ID)).thenReturn(buildBean);

        EnvironBean envBean = genDefaultEnvBean();
        envBean.setIs_sox(true);
        envBean.setAllow_private_build(true);

        Throwable throwable = assertThrows(WebApplicationException.class,
                () -> deployHandler.validateBuild(envBean, BUILD_ID));
        assertTrue(throwable.getMessage().contains(ERROR_STAGE_REQUIRES_SOX_BUILD_COMPLIANT_STAGE));
    }

    @Test
    public void validateBuildFailErrorStageRequiresSoxBuildCompliantSource() throws Exception {
        BuildBean buildBean = genBuildBean(NON_PRIVATE_BUILD_SCM_BRANCH);
        when(buildDAO.getById(BUILD_ID)).thenReturn(buildBean);

        EnvironBean envBean = genDefaultEnvBean();
        envBean.setIs_sox(true);
        envBean.setAllow_private_build(true);

        Throwable throwable = assertThrows(WebApplicationException.class,
                () -> deployHandler.validateBuild(envBean, BUILD_ID));
        assertTrue(throwable.getMessage().contains(ERROR_STAGE_REQUIRES_SOX_BUILD_COMPLIANT_SOURCE));
    }

    private BuildBean genBuildBean(String scmBranch) {
        BuildBean bean = new BuildBean();
        bean.setBuild_id(BUILD_ID);
        bean.setBuild_name("buildName");
        bean.setArtifact_url("www.pinterest.com");
        bean.setScm_branch(scmBranch);

        return bean;
    }

    private EnvironBean genDefaultEnvBean() {
        EnvironBean envBean = new EnvironBean();
        envBean.setBuild_name("buildName");
        envBean.setStage_type(EnvType.DEV);
        envBean.setEnv_name(EnvType.DEV.toString());
        envBean.setEnsure_trusted_build(false);
        envBean.setIs_sox(false);
        envBean.setAllow_private_build(false);

        return envBean;
    }
}
