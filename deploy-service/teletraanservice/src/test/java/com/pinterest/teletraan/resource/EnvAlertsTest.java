package com.pinterest.teletraan.resource;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pinterest.deployservice.alerts.AlertContext;
import com.pinterest.deployservice.alerts.AlertContextBuilder;
import com.pinterest.deployservice.alerts.AutoRollbackAction;
import com.pinterest.deployservice.alerts.MarkBadBuildAction;
import com.pinterest.deployservice.alerts.PinterestExternalAlertFactory;
import com.pinterest.deployservice.bean.BuildBean;
import com.pinterest.deployservice.bean.DeployBean;
import com.pinterest.deployservice.bean.DeployState;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.EnvironState;
import com.pinterest.deployservice.bean.TagBean;
import com.pinterest.deployservice.buildtags.BuildTagsManager;
import com.pinterest.deployservice.common.Constants;
import com.pinterest.deployservice.dao.BuildDAO;
import com.pinterest.deployservice.dao.DeployDAO;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.TagDAO;
import com.pinterest.deployservice.handler.DeployHandlerInterface;
import com.pinterest.deployservice.handler.TagHandler;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.universal.security.bean.UserPrincipal;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

public class EnvAlertsTest {

  TeletraanServiceContext context;
  DeployBean recent = new DeployBean();
  DeployBean lastKnownGoodDeploy = new DeployBean();
  EnvironBean environBean = new EnvironBean();
  BuildBean buildBean = new BuildBean();
  BuildBean lastKnownBuild = new BuildBean();
  BuildDAO buildDAO;
  EnvironDAO environDAO;
  DeployDAO deployDAO;
  TagDAO tagDAO;
  BuildTagsManager buildTagsManager;
  SecurityContext sc;
  AlertContextBuilder alertContextBuilder;
  AlertContext alertContext;
  DeployHandlerInterface deployHandler;

  @Before
  public void setUp() throws Exception {
    context = new TeletraanServiceContext();
    buildDAO = mock(BuildDAO.class);
    environDAO = mock(EnvironDAO.class);
    deployDAO = mock(DeployDAO.class);
    tagDAO = mock(TagDAO.class);
    buildTagsManager = mock(BuildTagsManager.class);
    alertContextBuilder = mock(AlertContextBuilder.class);
    deployHandler = mock(DeployHandlerInterface.class);
    context.setBuildTagsManager(buildTagsManager);
    context.setTagDAO(tagDAO);
    context.setBuildDAO(buildDAO);
    context.setEnvironDAO(environDAO);
    context.setDeployDAO(deployDAO);
    context.setExternalAlertsFactory(new PinterestExternalAlertFactory());
    buildBean.setBuild_id("0000001");
    buildBean.setBuild_name("BuildOne");

    lastKnownBuild.setBuild_id("0000002");
    lastKnownBuild.setBuild_name("BuildTwo");

    recent.setBuild_id(buildBean.getBuild_id());

    recent.setState(DeployState.SUCCEEDING);
    lastKnownGoodDeploy.setBuild_id(lastKnownBuild.getBuild_id());
    lastKnownGoodDeploy.setDeploy_id("lastGoodDeploy");
    environBean.setEnv_id("test1");
    environBean.setEnv_name("testenv");
    environBean.setStage_name("teststage");
    environBean.setDeploy_id("recentdeploy");
    environBean.setState(EnvironState.NORMAL);

    when(environDAO.getByStage(environBean.getEnv_name(), environBean.getStage_name())).thenReturn(environBean);
    when(deployDAO.getById("recentdeploy")).thenReturn(recent);
    when(deployDAO.getById("lastGoodDeploy")).thenReturn(recent);
    sc = mock(SecurityContext.class);
    when(sc.getUserPrincipal()).thenReturn(new UserPrincipal(Constants.AUTO_PROMOTER_NAME, new ArrayList<String>()));
    when(buildDAO.getById(recent.getBuild_id())).thenReturn(buildBean);
    alertContext = new AlertContext();
    alertContext.setTagHandler(mock(TagHandler.class));
    alertContext.setDeployHandler(deployHandler);
    alertContext.setDeployDAO(deployDAO);
    when(alertContextBuilder.build(any())).thenReturn(alertContext);

  }


  @Test
  public void alertsTriggered() throws Exception {
    EnvAlerts envAlerts=new EnvAlerts(context);
    envAlerts.setAlertContextBuilder(alertContextBuilder);
    recent.setStart_date(DateTime.now().minusMinutes(5).getMillis());

    //Test case 1, not in range, no actions
    Response
        resp = envAlerts.alertsTriggered("testenv","teststage",10, "markbadbuild rollback",sc, createAlertBody(DateTime.now().minusSeconds(1), true));
    Assert.assertEquals(200,resp.getStatus());
    HashMap entity = (HashMap)resp.getEntity();
    Assert.assertEquals(0,entity.size());

    //Test case 2, in range, only mark build no deploy
    resp = envAlerts.alertsTriggered("testenv","teststage",600, "markbadbuild rollback",sc, createAlertBody(DateTime.now().minusSeconds(1), true));
    Assert.assertEquals(200,resp.getStatus());
    entity = (HashMap)resp.getEntity();
    Assert.assertEquals(2,entity.size());
    Assert.assertEquals("No rollback candidate available", entity.get(AutoRollbackAction.class.getName()));
    Assert.assertEquals(buildBean.getBuild_id(), ((TagBean)entity.get(MarkBadBuildAction.class.getName())).getTarget_id());

    //Test case 3, in range, rollback and deploy
    when(deployHandler.getDeployCandidates(eq(environBean.getEnv_id()),any(),eq(100), eq(true))).thenReturn(Arrays.asList(
        lastKnownGoodDeploy));
    resp = envAlerts.alertsTriggered("testenv","teststage",600, "markbadbuild rollback",sc, createAlertBody(DateTime.now().minusSeconds(1), true));
    Assert.assertEquals(200,resp.getStatus());
    entity = (HashMap)resp.getEntity();
    Assert.assertEquals(2,entity.size());
    Assert.assertEquals(lastKnownGoodDeploy, entity.get(AutoRollbackAction.class.getName()));
    Assert.assertEquals(buildBean.getBuild_id(), ((TagBean)entity.get(MarkBadBuildAction.class.getName())).getTarget_id());


  }

  private String createAlertBody(DateTime triggeredTime, boolean triggered){
    return String.format("alert_name=alert&triggered=%s&triggered_date=%f",triggered, triggeredTime.getMillis()/1000.0);
  }

}
