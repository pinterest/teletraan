package com.pinterest.teletraan.worker;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.DeployBean;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.PromoteBean;
import com.pinterest.deployservice.dao.BuildDAO;
import com.pinterest.deployservice.dao.DeployDAO;
import com.pinterest.deployservice.dao.EnvironDAO;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AutoPromoteDeployTest {
  ServiceContext context;
  DeployBean first = new DeployBean();
  DeployBean second = new DeployBean();
  EnvironBean environBean = new EnvironBean();
  EnvironBean predEnvironBean = new EnvironBean();
  BuildDAO buildDAO;
  EnvironDAO environDAO;
  DeployDAO deployDAO;
  final static String CronTenAMPerDay = "0 0 10 * * ?";

  List<DeployBean> allDeployBeans = new ArrayList<>();

  public List<DeployBean> getAcceptedDeploysDelayed(String envId, Interval interval){
    List<DeployBean> ret = new ArrayList<>();
    for (DeployBean bean:allDeployBeans){
      if (bean.getEnv_id().equals(envId) &&
          interval.contains(bean.getStart_date())&&
          //contains is inclusive for beginning
          interval.getStartMillis()!= bean.getStart_date()){
        ret.add(bean);
      }
    }
    return ret;
  }

  @Before
  public void setUp() throws Exception {
    context = new ServiceContext();
    buildDAO = mock(BuildDAO.class);
    environDAO = mock(EnvironDAO.class);
    deployDAO = mock(DeployDAO.class);
    context.setBuildDAO(buildDAO);
    context.setEnvironDAO(environDAO);
    context.setDeployDAO(deployDAO);
    first.setBuild_id("0000001");
    second.setBuild_id("0000002");
    environBean.setEnv_id("test1");
    environBean.setEnv_name("testenv");
    predEnvironBean.setEnv_id("predtest1");
    predEnvironBean.setEnv_name("testenv");
  }

  @After
  public void clean() throws Exception{
    allDeployBeans.clear();
  }

  /* Autopromote enabled. But no current deploy and no prev deploys environ*/
  @Test
  public void testNoPredEnvironmentPromote() throws Exception {
    PromoteBean promoteBean = new PromoteBean();
    promoteBean.setPred_stage("pred");
    when(environDAO.getByStage(environBean.getEnv_name(), "pred")).thenReturn(null);

    AutoPromoter promoter = new AutoPromoter(context);
    //No build. No previous deploy
    PromoteResult result = promoter.computePromoteDeployResult(environBean, null, 10, promoteBean);
    Assert.assertEquals(PromoteResult.ResultCode.NoPredEnvironment, result.getResult());

    //Ensure same for scheduled build
    promoteBean.setSchedule(CronTenAMPerDay);
    result = promoter.computePromoteDeployResult(environBean, null, 10, promoteBean);
    Assert.assertEquals(PromoteResult.ResultCode.NoPredEnvironment, result.getResult());

  }

  /* Autopromote enabled. Has pred env but it has no deploys*/
  @Test
  public void testNoPredEnvironDeployPromote() throws Exception {
    PromoteBean promoteBean = new PromoteBean();
    promoteBean.setPred_stage("pred");
    predEnvironBean.setDeploy_id(null);
    when(environDAO.getByStage(environBean.getEnv_name(), "pred")).thenReturn(predEnvironBean);

    AutoPromoter promoter = new AutoPromoter(context);
    //No build. No previous deploy
    PromoteResult result = promoter.computePromoteDeployResult(environBean, null, 10, promoteBean);
    Assert.assertEquals(PromoteResult.ResultCode.NoPredEnvironmentDeploy, result.getResult());

    //Ensure same for scheduled build
    promoteBean.setSchedule(CronTenAMPerDay);
    result = promoter.computePromoteDeployResult(environBean, null, 10, promoteBean);
    Assert.assertEquals(PromoteResult.ResultCode.NoPredEnvironmentDeploy, result.getResult());

  }

  /*
  Autopromote enabled with no delay. Have pred environ and already promote one deploy.
  No current deploy available
  */
  @Test
  public void testPredDeployAlreadyPromote() throws Exception{
    DateTime now = DateTime.now();
    PromoteBean promoteBean = new PromoteBean();
    promoteBean.setPred_stage("pred");
    promoteBean.setDelay(0); //minutes
    predEnvironBean.setDeploy_id("deploy1");
    DeployBean prevDeploy = new DeployBean();
    prevDeploy.setDeploy_id("deploy1");
    prevDeploy.setEnv_id(predEnvironBean.getEnv_id());
    prevDeploy.setStart_date(now.minusHours(26).getMillis());
    DeployBean currentDeploy = new DeployBean();
    currentDeploy.setDeploy_id("deploy2");
    currentDeploy.setFrom_deploy("deploy1");
    currentDeploy.setStart_date(now.minusHours(25).getMillis());
    allDeployBeans.add(prevDeploy);
    when(environDAO.getByStage(environBean.getEnv_name(), "pred")).thenReturn(predEnvironBean);
    when(deployDAO.getById("deploy1")).thenReturn(prevDeploy);
    when(deployDAO.getAcceptedDeploysDelayed(anyString(), anyObject())).thenAnswer(
        new Answer<List<DeployBean>>(){
          @Override
          public List<DeployBean> answer(InvocationOnMock invocationOnMock) throws Throwable {
            return getAcceptedDeploysDelayed((String)invocationOnMock.getArguments()[0],
                (Interval)invocationOnMock.getArguments()[1]);
          }
        }
    );


    AutoPromoter promoter = new AutoPromoter(context);
    //No build. No previous deploy
    PromoteResult result = promoter.computePromoteDeployResult(environBean, currentDeploy, 10, promoteBean);
    Assert.assertEquals(PromoteResult.ResultCode.NoCandidateWithinDelayPeriod, result.getResult());

    promoteBean.setSchedule(CronTenAMPerDay);
    result = promoter.computePromoteDeployResult(environBean, currentDeploy, 10, promoteBean);
    Assert.assertEquals(PromoteResult.ResultCode.NoCandidateWithinDelayPeriod, result.getResult());
  }

  /* Autopromote enabled for any new build. Have previous deploy but from different deploy*/
  @Test
  public void testPredDeployDelayPromote() throws Exception{
    DateTime now = DateTime.now();
    PromoteBean promoteBean = new PromoteBean();
    promoteBean.setPred_stage("pred");
    promoteBean.setDelay(10); //minutes
    predEnvironBean.setDeploy_id("deploy1");
    DeployBean prevDeploy = new DeployBean();
    prevDeploy.setDeploy_id("deploy1");
    prevDeploy.setEnv_id(predEnvironBean.getEnv_id());
    prevDeploy.setStart_date(now.minusMinutes(60).getMillis());
    DeployBean currentDeploy = new DeployBean();
    currentDeploy.setDeploy_id("deploy2");
    currentDeploy.setStart_date(now.minusMinutes(25).getMillis());
    currentDeploy.setFrom_deploy(prevDeploy.getDeploy_id());

    DeployBean newDeploy = new DeployBean();
    newDeploy.setEnv_id(prevDeploy.getEnv_id());
    newDeploy.setDeploy_id("deploy3");
    allDeployBeans.add(prevDeploy);
    allDeployBeans.add(newDeploy);
    when(environDAO.getByStage(environBean.getEnv_name(), "pred")).thenReturn(predEnvironBean);
    when(deployDAO.getById(prevDeploy.getDeploy_id())).thenReturn(prevDeploy);
    when(deployDAO.getById(newDeploy.getDeploy_id())).thenReturn(newDeploy);
    when(deployDAO.getAcceptedDeploysDelayed(anyString(), anyObject())).thenAnswer(
        new Answer<List<DeployBean>>(){
          @Override
          public List<DeployBean> answer(InvocationOnMock invocationOnMock) throws Throwable {
            return getAcceptedDeploysDelayed((String)invocationOnMock.getArguments()[0],
                (Interval) invocationOnMock.getArguments()[1]);
          }
        }
    );
    AutoPromoter promoter = new AutoPromoter(context);
    //Pre deploy is 6 minutes ago, delay is 10 minutes
    newDeploy.setStart_date(now.minusMinutes(6).getMillis());
    PromoteResult result = promoter.computePromoteDeployResult(environBean, currentDeploy, 10, promoteBean);
    Assert.assertEquals(PromoteResult.ResultCode.NoCandidateWithinDelayPeriod, result.getResult());

    //Set predeploy to 11 minutes, delay is 10 minutes
    newDeploy.setStart_date(now.minusMinutes(11).getMillis());
    result = promoter.computePromoteDeployResult(environBean, currentDeploy, 10, promoteBean);
    Assert.assertEquals(PromoteResult.ResultCode.PromoteDeploy, result.getResult());

  }


  /* Autopromote enabled for any new build. Have previous deploy but from different deploy*/
  @Test
  public void testPredDeployWithDelayPromote() throws Exception{
    DateTime now = DateTime.now();
    PromoteBean promoteBean = new PromoteBean();
    promoteBean.setPred_stage("pred");
    promoteBean.setDelay(10); //minutes
    predEnvironBean.setDeploy_id("deploy1");
    DeployBean newDeploy = new DeployBean();
    newDeploy.setDeploy_id("deploy1");
    newDeploy.setEnv_id(predEnvironBean.getEnv_id());
    DeployBean currentDeploy = new DeployBean();
    currentDeploy.setDeploy_id("deploy2");
    currentDeploy.setStart_date(now.minusMinutes(25).getMillis());
    allDeployBeans.add(newDeploy);
    when(environDAO.getByStage(environBean.getEnv_name(), "pred")).thenReturn(predEnvironBean);
    when(deployDAO.getById("deploy1")).thenReturn(newDeploy);
    when(deployDAO.getAcceptedDeploysDelayed(anyString(), anyObject())).thenAnswer(
        new Answer<List<DeployBean>>(){
          @Override
          public List<DeployBean> answer(InvocationOnMock invocationOnMock) throws Throwable {
            return getAcceptedDeploysDelayed((String)invocationOnMock.getArguments()[0],
                (Interval) invocationOnMock.getArguments()[1]);
          }
        }
    );
    AutoPromoter promoter = new AutoPromoter(context);
    //Pre deploy is 6 minutes ago, delay is 10 minutes
    newDeploy.setStart_date(now.minusMinutes(6).getMillis());
    PromoteResult result = promoter.computePromoteDeployResult(environBean, currentDeploy, 10, promoteBean);
    Assert.assertEquals(PromoteResult.ResultCode.NoCandidateWithinDelayPeriod, result.getResult());

    //Set predeploy to 11 minutes, delay is 10 minutes
    newDeploy.setStart_date(now.minusMinutes(11).getMillis());
    result = promoter.computePromoteDeployResult(environBean, currentDeploy, 10, promoteBean);
    Assert.assertEquals(PromoteResult.ResultCode.PromoteDeploy, result.getResult());

  }

  @Test
  public void testScheduledPromote() throws Exception{
    DateTime now = DateTime.now();
    PromoteBean promoteBean = new PromoteBean();
    promoteBean.setSchedule(CronTenAMPerDay);
    promoteBean.setPred_stage("pred");
    promoteBean.setDelay(0); //minutes

    predEnvironBean.setDeploy_id("deploy1");
    DeployBean prevDeploy = new DeployBean();
    prevDeploy.setDeploy_id("deploy1");
    prevDeploy.setEnv_id(predEnvironBean.getEnv_id());

    DeployBean newDeploy = new DeployBean();
    newDeploy.setDeploy_id("newDeploy");
    newDeploy.setEnv_id(predEnvironBean.getEnv_id());

    DeployBean currentDeploy = new DeployBean();
    currentDeploy.setDeploy_id("deploy2");
    currentDeploy.setFrom_deploy("deploy1");

    allDeployBeans.addAll(Arrays.asList(prevDeploy, newDeploy));

    when(environDAO.getByStage(environBean.getEnv_name(), "pred")).thenReturn(predEnvironBean);
    when(deployDAO.getById(prevDeploy.getDeploy_id())).thenReturn(prevDeploy);
    when(deployDAO.getById(newDeploy.getDeploy_id())).thenReturn(newDeploy);
    when(deployDAO.getAcceptedDeploysDelayed(anyString(), anyObject())).thenAnswer(
        new Answer<List<DeployBean>>(){
          @Override
          public List<DeployBean> answer(InvocationOnMock invocationOnMock) throws Throwable {
            return getAcceptedDeploysDelayed((String)invocationOnMock.getArguments()[0],
                (Interval) invocationOnMock.getArguments()[1]);
          }
        }
    );

    if (now.getHourOfDay()>=10) {
      DateTime cuttingPoint = new DateTime(now.getYear(),now.getMonthOfYear(),now.getDayOfMonth(), 10,0,0);

      prevDeploy.setStart_date(cuttingPoint.minusHours(3).getMillis());
      currentDeploy.setStart_date(cuttingPoint.minusHours(2).getMillis());
      newDeploy.setStart_date(cuttingPoint.minusHours(1).getMillis());
      AutoPromoter promoter = new AutoPromoter(context);
      PromoteResult result = promoter.computePromoteDeployResult(environBean, currentDeploy, 10, promoteBean);
      Assert.assertEquals(PromoteResult.ResultCode.PromoteDeploy, result.getResult());
      promoteBean.setDelay(1+(int)(now.getMillis() - newDeploy.getStart_date())/60000);
      result = promoter.computePromoteDeployResult(environBean, currentDeploy, 10, promoteBean);
      Assert.assertEquals(PromoteResult.ResultCode.NoCandidateWithinDelayPeriod, result.getResult());

    }
    else{
      prevDeploy.setStart_date(now.minusHours(27).getMillis());
      currentDeploy.setStart_date(now.minusHours(26).getMillis());
      newDeploy.setStart_date(now.minusHours(25).getMillis());
      AutoPromoter promoter = new AutoPromoter(context);
      PromoteResult result = promoter.computePromoteDeployResult(environBean, currentDeploy, 10, promoteBean);
      Assert.assertEquals(PromoteResult.ResultCode.PromoteDeploy, result.getResult());
      promoteBean.setDelay(1+(int)(now.getMillis() - newDeploy.getStart_date())/60000);
      result = promoter.computePromoteDeployResult(environBean, currentDeploy, 10, promoteBean);
      Assert.assertEquals(PromoteResult.ResultCode.NoCandidateWithinDelayPeriod, result.getResult());
    }

  }


}
