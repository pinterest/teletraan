package com.pinterest.deployservice.handler;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.allowlists.BuildAllowlistImpl;
import com.pinterest.deployservice.bean.BuildBean;
import com.pinterest.deployservice.bean.EnvType;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.common.DeployInternalException;
import com.pinterest.deployservice.dao.BuildDAO;
import com.pinterest.deployservice.exception.TeletaanInternalException;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static com.pinterest.deployservice.handler.DeployHandler.ERROR_BUILD_NAME_NOT_MATCH_STAGE_CONFIG;
import static com.pinterest.deployservice.handler.DeployHandler.ERROR_EMPTY_BUILD_ID;
import static com.pinterest.deployservice.handler.DeployHandler.ERROR_NON_PRIVATE_UNTRUSTED_LOCATION;
import static com.pinterest.deployservice.handler.DeployHandler.ERROR_STAGE_NOT_ALLOW_PRIVATE_BUILD;
import static com.pinterest.deployservice.handler.DeployHandler.ERROR_STAGE_REQUIRES_SOX_BUILD_COMPLIANT_SOURCE;
import static com.pinterest.deployservice.handler.DeployHandler.ERROR_STAGE_REQUIRES_SOX_BUILD_COMPLIANT_STAGE;
import static com.pinterest.deployservice.handler.DeployHandler.PRIVATE_BUILD_SCM_BRANCH;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeployHandlerTest {
    private DeployHandler deployHandler;
    private BuildDAO buildDAO;
    private final String BUILD_ID = "buildId01";
    private final String NON_PRIVATE_BUILD_SCM_BRANCH ="non_private";
    private final String FAIL_MESSAGE = "Should have thrown TeletaanInternalException: %s";

    private ServiceContext createMockServiceContext() throws Exception {
        buildDAO = mock(BuildDAO.class);

        ServiceContext serviceContext = new ServiceContext();
        serviceContext.setBuildDAO(buildDAO);
        serviceContext.setBuildAllowlist( new BuildAllowlistImpl(new ArrayList<>(),new ArrayList<>(),new ArrayList<>()));
        return serviceContext;
    }

    @Before
    public void setUp() throws Exception {
        deployHandler = new DeployHandler(createMockServiceContext());
    }

    @Test
    public void validateBuild() throws Exception {
        when(buildDAO.getById(BUILD_ID)).thenReturn(genBuildBean(PRIVATE_BUILD_SCM_BRANCH));

        EnvironBean envBean = genDefaultEnvBean();
        envBean.setAllow_private_build(true);
        try {
            deployHandler.validateBuild(envBean, BUILD_ID);
        } catch (Exception ex){
            fail("Should not throw exception");
        }
    }

    @Test
    public void validateBuildNonPrivate() throws Exception {
        when(buildDAO.getById(BUILD_ID)).thenReturn(genBuildBean(NON_PRIVATE_BUILD_SCM_BRANCH));

        EnvironBean envBean = genDefaultEnvBean();
        envBean.setAllow_private_build(true);
        try {
            deployHandler.validateBuild(envBean, BUILD_ID);
        } catch (Exception ex){
            fail("Should not throw exception");
        }
    }

    @Test
    public void validateBuildFailBuildIdEmpty() throws Exception {
        EnvironBean envBean = genDefaultEnvBean();
        try {
            deployHandler.validateBuild(envBean, "");
            fail(String.format(FAIL_MESSAGE,
                    ERROR_EMPTY_BUILD_ID
            ));
        } catch (TeletaanInternalException ex){
            assertTrue(ex.getResponse().getEntity().toString().contains(
                    ERROR_EMPTY_BUILD_ID
            ));
        }
    }

    @Test
    public void validateBuildFailBuildNotMatchStageConfig() throws Exception {
        EnvironBean envBean = genDefaultEnvBean();
        envBean.setBuild_name("other");
        when(buildDAO.getById(BUILD_ID)).thenReturn(genBuildBean(NON_PRIVATE_BUILD_SCM_BRANCH));

        try {
            deployHandler.validateBuild(envBean, BUILD_ID);
            fail(String.format(FAIL_MESSAGE,
                    String.format(ERROR_BUILD_NAME_NOT_MATCH_STAGE_CONFIG,
                            genBuildBean(NON_PRIVATE_BUILD_SCM_BRANCH).getBuild_name(),
                            envBean.getBuild_name())));
        } catch (DeployInternalException ex){
            assertTrue(ex.getMessage().contains(
                    String.format(ERROR_BUILD_NAME_NOT_MATCH_STAGE_CONFIG,
                            genBuildBean(NON_PRIVATE_BUILD_SCM_BRANCH).getBuild_name(),
                            envBean.getBuild_name())));
        }
    }

    @Test
    public void validateBuildFailNonPrivateFromUntrustedLocation() throws Exception {
        BuildBean buildBean = genBuildBean(NON_PRIVATE_BUILD_SCM_BRANCH);
        when(buildDAO.getById(BUILD_ID)).thenReturn(buildBean);
        EnvironBean envBean = genDefaultEnvBean();
        envBean.setEnsure_trusted_build(true);

        try {
            deployHandler.validateBuild(envBean, BUILD_ID);
            fail(String.format(FAIL_MESSAGE,
                    String.format(ERROR_NON_PRIVATE_UNTRUSTED_LOCATION,
                            buildBean.getArtifact_url())));
        } catch (TeletaanInternalException ex){
            assertTrue(ex.getResponse().getEntity().toString().contains(
                    String.format(ERROR_NON_PRIVATE_UNTRUSTED_LOCATION,
                            buildBean.getArtifact_url())
            ));
        }
    }

    @Test
    public void validateBuildFailErrorStageNotAllowedPrivateBuild() throws Exception {
        BuildBean buildBean = genBuildBean(PRIVATE_BUILD_SCM_BRANCH);
        when(buildDAO.getById(BUILD_ID)).thenReturn(buildBean);

        try {
            deployHandler.validateBuild(genDefaultEnvBean(), BUILD_ID);
            fail(String.format(FAIL_MESSAGE,
                    ERROR_STAGE_NOT_ALLOW_PRIVATE_BUILD));
        } catch (TeletaanInternalException ex){
            assertTrue(ex.getResponse().getEntity().toString().contains(
                    ERROR_STAGE_NOT_ALLOW_PRIVATE_BUILD));
        }
    }

    @Test
    public void validateBuildFailErrorStageRequiresSoxBuild() throws Exception {
        BuildBean buildBean = genBuildBean(PRIVATE_BUILD_SCM_BRANCH);
        when(buildDAO.getById(BUILD_ID)).thenReturn(buildBean);

        EnvironBean envBean = genDefaultEnvBean();
        envBean.setIs_sox(true);
        envBean.setAllow_private_build(true);

        try {
            deployHandler.validateBuild(envBean, BUILD_ID);
            fail(String.format(FAIL_MESSAGE,
                    ERROR_STAGE_REQUIRES_SOX_BUILD_COMPLIANT_STAGE));
        } catch (TeletaanInternalException ex){
            assertTrue(ex.getResponse().getEntity().toString().contains(
                    ERROR_STAGE_REQUIRES_SOX_BUILD_COMPLIANT_STAGE)
            );
        }
    }
    @Test
    public void validateBuildFailErrorStageRequiresSoxBuildCompliantSource() throws Exception {
        BuildBean buildBean = genBuildBean(NON_PRIVATE_BUILD_SCM_BRANCH);
        when(buildDAO.getById(BUILD_ID)).thenReturn(buildBean);

        EnvironBean envBean = genDefaultEnvBean();
        envBean.setIs_sox(true);
        envBean.setAllow_private_build(true);

        try {
            deployHandler.validateBuild(envBean, BUILD_ID);
            fail(String.format(FAIL_MESSAGE,
                    ERROR_STAGE_REQUIRES_SOX_BUILD_COMPLIANT_SOURCE));
        } catch (TeletaanInternalException ex){
            assertTrue(ex.getResponse().getEntity().toString().contains(
                    ERROR_STAGE_REQUIRES_SOX_BUILD_COMPLIANT_SOURCE)
            );
        }
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
