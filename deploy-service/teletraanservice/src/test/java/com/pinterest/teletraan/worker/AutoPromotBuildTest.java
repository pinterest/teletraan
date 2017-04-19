package com.pinterest.teletraan.worker;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.BuildBean;
import com.pinterest.deployservice.bean.DeployBean;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.PromoteBean;
import com.pinterest.deployservice.dao.BuildDAO;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;


public class AutoPromotBuildTest {

  ServiceContext context;
  DeployBean first = new DeployBean();
  DeployBean second = new DeployBean();
  EnvironBean environBean = new EnvironBean();
  BuildDAO buildDAO;
  final static String CronTenAMPerDay = "0 0 10 * * ?";

  @Before
  public void setUp() throws Exception {
    context = new ServiceContext();
    buildDAO = mock(BuildDAO.class);
    context.setBuildDAO(buildDAO);
    first.setBuild_id("0000001");
    second.setBuild_id("0000002");
    environBean.setEnv_id("test1");

  }

  @Test
  public void testGetScheduledCheckDueTime() {
    DeployBean currentDeploy = new DeployBean();
    AutoPromoter promoter = new AutoPromoter(context);
    DateTime now = new DateTime(new Date());
    currentDeploy.setStart_date(now.minusDays(1).getMillis());
    DateTime due =
        new DateTime(promoter.getScheduledCheckDueTime(currentDeploy.getStart_date(), CronTenAMPerDay));

    if (now.getHourOfDay() < 10) {
      DateTime yesterday = now.minusDays(-1);
      DateTime
          start =
          new DateTime(yesterday.getYear(), yesterday.getMonthOfYear(), yesterday.getDayOfMonth(),
              10, 0, 0);
      Assert.assertEquals(due, start);
    } else {
      DateTime
          start =
          new DateTime(now.getYear(), now.getMonthOfYear(), now.getDayOfMonth(), 10, 0, 0);
      Assert.assertEquals(due, start);
    }
  }

  /* Autopromote enabled for any new build. But no builds and no prev deploys*/
  @Test
  public void testNoBuildNoPreviousDeployPromote() throws Exception {
    PromoteBean promoteBean = new PromoteBean();
    AutoPromoter promoter = new AutoPromoter(context);
    //No build. No previous deploy
    PromoteResult result = promoter.computePromoteBuildResult(environBean, null, 10, promoteBean);
    Assert.assertEquals(PromoteResult.ResultCode.NoAvailableBuild, result.getResult());

   }

  /* Autopromote enabled for any new build. no previous deploy and one new build*/
  @Test
  public void testOneBuildNoPreviousDeployPromote() throws Exception {
    PromoteBean promoteBean = new PromoteBean();
    AutoPromoter promoter = new AutoPromoter(context);
    //Has builds. Have previous deploy
    BuildBean build = new BuildBean();
    build.setBuild_id("123");
    build.setPublish_date(DateTime.now().minusHours(1).getMillis());
    when(buildDAO.getAcceptedBuilds(anyString(), anyString(), anyObject(), anyInt()))
        .thenReturn(Arrays.asList(build));
    PromoteResult result = promoter.computePromoteBuildResult(environBean, null, 10, promoteBean);
    Assert.assertEquals(PromoteResult.ResultCode.PromoteBuild, result.getResult());
    Assert.assertEquals("123", result.getPromotedBuild());
  }

  /* Autopromote enabled for any new build. Have previous deploy but no new build*/
  @Test
  public void testNoBuildWithPreviousDeploy() throws Exception{
    PromoteBean promoteBean = new PromoteBean();
    AutoPromoter promoter = new AutoPromoter(context);
    //Has builds. No previous deploy
    DeployBean previousDeploy = new DeployBean();
    previousDeploy.setStart_date(DateTime.now().minusHours(2).getMillis());
    previousDeploy.setBuild_id("prev123");

    BuildBean preBuild = new BuildBean();
    preBuild.setBuild_id("prev123");
    preBuild.setPublish_date(DateTime.now().minusHours(3).getMillis());

    when(buildDAO.getById("prev123")).thenReturn(preBuild);
    PromoteResult result = promoter.computePromoteBuildResult(environBean, previousDeploy, 10, promoteBean);
    Assert.assertEquals(PromoteResult.ResultCode.NoAvailableBuild, result.getResult());
  }

  /* Autopromote enabled for any new build. Have previous deploy and one new build*/
  @Test
  public void testOneBuildwithPreviousDeploy() throws Exception {
    PromoteBean promoteBean = new PromoteBean();
    AutoPromoter promoter = new AutoPromoter(context);
    //Has builds. No previous deploy
    DeployBean previousDeploy = new DeployBean();
    previousDeploy.setStart_date(DateTime.now().minusHours(2).getMillis());
    previousDeploy.setBuild_id("prev123");

    BuildBean preBuild = new BuildBean();
    preBuild.setBuild_id("prev123");
    preBuild.setPublish_date(DateTime.now().minusHours(3).getMillis());

    BuildBean build = new BuildBean();
    build.setBuild_id("123");
    build.setPublish_date(DateTime.now().minusHours(1).getMillis());
    when(buildDAO.getById("prev123")).thenReturn(preBuild);
    when(buildDAO.getAcceptedBuilds(anyString(), anyString(), anyObject(), anyInt()))
        .thenReturn(Arrays.asList(build));
    PromoteResult result = promoter.computePromoteBuildResult(environBean, previousDeploy, 10, promoteBean);
    Assert.assertEquals(PromoteResult.ResultCode.PromoteBuild, result.getResult());
    Assert.assertEquals("123", result.getPromotedBuild());
  }

  /* Autopromote enabled for daily schedule. But no builds and no prev deploys*/
  @Test
  public void testNoBuildNoPreviousDeployScheduledPromote() throws Exception {
    PromoteBean promoteBean = new PromoteBean();
    promoteBean.setEnv_id(environBean.getEnv_id());
    promoteBean.setSchedule(CronTenAMPerDay);
    promoteBean.setLast_update(0L);
    AutoPromoter promoter = new AutoPromoter(context);

    //No build. No previous deploy
    PromoteResult result = promoter.computePromoteBuildResult(environBean, null, 10, promoteBean);
    Assert.assertEquals(PromoteResult.ResultCode.NoAvailableBuild, result.getResult());
  }

  /* Autopromote enabled for 10:00 AM every day. no previous deploy and one new build*/
  @Test
  public void testOneBuildNoPreviousDeploySchedulePromote() throws Exception {
    PromoteBean promoteBean = new PromoteBean();
    promoteBean.setEnv_id(environBean.getEnv_id());
    promoteBean.setSchedule(CronTenAMPerDay);
    promoteBean.setLast_update(0L);

    AutoPromoter promoter = new AutoPromoter(context);
    //Has builds. Have previous deploy
    BuildBean build = new BuildBean();
    build.setBuild_id("123");
    DateTime now = DateTime.now();

    //Set build time to be before 10 am today
    if (now.getHourOfDay()>= 10){
      build.setPublish_date(now.minusHours(now.getHourOfDay()-10+1).getMillis());
    }else {
      build.setPublish_date(DateTime.now().getMillis());
    }
    when(buildDAO.getAcceptedBuilds(anyString(), anyString(), anyObject(), anyInt()))
        .thenReturn(Arrays.asList(build));
    PromoteResult result = promoter.computePromoteBuildResult(environBean, null, 10, promoteBean);
    Assert.assertEquals(PromoteResult.ResultCode.PromoteBuild, result.getResult());
    Assert.assertEquals("123", result.getPromotedBuild());
  }

  /* Autopromote enabled for 10:00 AM every day. no previous deploy and one new build after schedule*/
  @Test
  public void testOneBuildNoPreviousDeploySchedulePromote2() throws Exception {
    PromoteBean promoteBean = new PromoteBean();
    promoteBean.setEnv_id(environBean.getEnv_id());
    promoteBean.setSchedule(CronTenAMPerDay);
    promoteBean.setLast_update(0L);

    AutoPromoter promoter = new AutoPromoter(context);
    //Has builds. Have previous deploy
    BuildBean build = new BuildBean();
    build.setBuild_id("123");
    DateTime now = DateTime.now();

    //Set build time to be after 10 am today
    if (now.getHourOfDay()< 10){
      build.setPublish_date(now.plusHours(10-now.getHourOfDay()+1).getMillis());
    }else {
      build.setPublish_date(DateTime.now().getMillis());
    }
    when(buildDAO.getAcceptedBuilds(anyString(), anyString(), anyObject(), anyInt()))
        .thenReturn(Arrays.asList(build));
    PromoteResult result = promoter.computePromoteBuildResult(environBean, null, 10, promoteBean);
    Assert.assertEquals(PromoteResult.ResultCode.NotInScheduledTime, result.getResult());
  }

  /* Autopromote enabled for 10:00 AM every day. Have old previous deploy but no new build*/
  @Test
  public void testNoBuildWithPreviousDeploySchedule() throws Exception{
    PromoteBean promoteBean = new PromoteBean();
    promoteBean.setEnv_id(environBean.getEnv_id());
    promoteBean.setSchedule(CronTenAMPerDay);
    promoteBean.setLast_update(0L);
    AutoPromoter promoter = new AutoPromoter(context);

    DeployBean previousDeploy = new DeployBean();
    previousDeploy.setStart_date(DateTime.now().minusDays(1).getMillis());
    previousDeploy.setBuild_id("prev123");

    BuildBean preBuild = new BuildBean();
    preBuild.setBuild_id("prev123");
    preBuild.setPublish_date(DateTime.now().minusHours(3).getMillis());

    when(buildDAO.getById("prev123")).thenReturn(preBuild);
    PromoteResult result = promoter.computePromoteBuildResult(environBean, previousDeploy, 10, promoteBean);
    Assert.assertEquals(PromoteResult.ResultCode.NoAvailableBuild, result.getResult());
  }

  /* Autopromote enabled for 10:00 AM every day. Have a very recent previous deploy and no new build*/
  @Test
  public void testNoBuildWithPreviousDeploySchedule2() throws Exception{
    PromoteBean promoteBean = new PromoteBean();
    promoteBean.setEnv_id(environBean.getEnv_id());
    promoteBean.setSchedule(CronTenAMPerDay);

    AutoPromoter promoter = new AutoPromoter(context);

    DeployBean previousDeploy = new DeployBean();
    previousDeploy.setStart_date(DateTime.now().minusMinutes(1).getMillis());
    previousDeploy.setBuild_id("prev123");

    BuildBean preBuild = new BuildBean();
    preBuild.setBuild_id("prev123");
    preBuild.setPublish_date(DateTime.now().minusHours(3).getMillis());

    when(buildDAO.getById("prev123")).thenReturn(preBuild);
    PromoteResult result = promoter.computePromoteBuildResult(environBean, previousDeploy, 10, promoteBean);
    Assert.assertEquals(PromoteResult.ResultCode.NotInScheduledTime, result.getResult());
  }


  @Test
  public void testOneBuildwithPreviousDeploySchedule() throws Exception {
    PromoteBean promoteBean = new PromoteBean();
    promoteBean.setEnv_id(environBean.getEnv_id());
    promoteBean.setSchedule(CronTenAMPerDay);
    promoteBean.setLast_update(0L);
    AutoPromoter promoter = new AutoPromoter(context);
    //Has builds. No previous deploy
    DeployBean previousDeploy = new DeployBean();
    previousDeploy.setStart_date(DateTime.now().minusDays(1).getMillis());
    previousDeploy.setBuild_id("prev123");

    BuildBean preBuild = new BuildBean();
    preBuild.setBuild_id("prev123");
    preBuild.setPublish_date(DateTime.now().minusHours(25).getMillis());

    BuildBean build = new BuildBean();
    build.setBuild_id("123");
    DateTime now = DateTime.now();
    if (now.getHourOfDay()>10) {
      build.setPublish_date((now.minusHours(now.getHourOfDay()-10+1)).getMillis());
    }else{
      build.setPublish_date(DateTime.now().minusHours(1).getMillis());
    }
    when(buildDAO.getById("prev123")).thenReturn(preBuild);
    when(buildDAO.getAcceptedBuilds(anyString(), anyString(), anyObject(), anyInt()))
        .thenReturn(Arrays.asList(build));
    PromoteResult result = promoter.computePromoteBuildResult(environBean, previousDeploy, 10, promoteBean);
    Assert.assertEquals(PromoteResult.ResultCode.PromoteBuild, result.getResult());
    Assert.assertEquals("123", result.getPromotedBuild());
  }
}
