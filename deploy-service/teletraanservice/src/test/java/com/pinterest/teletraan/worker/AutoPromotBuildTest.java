package com.pinterest.teletraan.worker;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.BuildBean;
import com.pinterest.deployservice.bean.BuildTagBean;
import com.pinterest.deployservice.bean.DeployBean;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.PromoteBean;
import com.pinterest.deployservice.bean.TagBean;
import com.pinterest.deployservice.bean.TagTargetType;
import com.pinterest.deployservice.bean.TagValue;
import com.pinterest.deployservice.buildtags.BuildTagsManager;
import com.pinterest.deployservice.buildtags.BuildTagsManagerImpl;
import com.pinterest.deployservice.common.CommonUtils;
import com.pinterest.deployservice.dao.BuildDAO;
import com.pinterest.deployservice.dao.TagDAO;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;


public class AutoPromotBuildTest {

    final static String CronTenAMPerDay = "0 0 10 * * ?";
    ServiceContext context;
    DeployBean first = new DeployBean();
    DeployBean second = new DeployBean();
    EnvironBean environBean = new EnvironBean();
    BuildDAO buildDAO;
    TagDAO tagDAO;
    BuildTagsManager buildTagsManager;

    @Before
    public void setUp() throws Exception {
        context = new ServiceContext();
        buildDAO = mock(BuildDAO.class);
        tagDAO = mock(TagDAO.class);
        buildTagsManager = mock(BuildTagsManager.class);
        context.setBuildTagsManager(buildTagsManager);
        context.setBuildDAO(buildDAO);
        context.setTagDAO(tagDAO);
        first.setBuild_id("0000001");
        second.setBuild_id("0000002");
        environBean.setEnv_id("test1");

    }


    @Test
    public void testGetScheduledCheckDueTime() throws Exception {
        DeployBean currentDeploy = new DeployBean();
        AutoPromoter promoter = new AutoPromoter(context);
        DateTime now = DateTime.now(DateTimeZone.UTC);
        currentDeploy.setStart_date(now.minusDays(1).getMillis());
        DateTime due =
            new DateTime(
                promoter.getScheduledCheckDueTime(currentDeploy.getStart_date(), CronTenAMPerDay));

        if (now.getHourOfDay() < 10) {
            DateTime yesterday = now.minusDays(1);
            DateTime
                start =
                new DateTime(yesterday.getYear(), yesterday.getMonthOfYear(),
                    yesterday.getDayOfMonth(),
                    10, 0, 0);
            Assert.assertEquals(due, start);
        } else {
            DateTime
                start =
                new DateTime(now.getYear(), now.getMonthOfYear(), now.getDayOfMonth(), 10, 0, 0);
            Assert.assertEquals(due, start);
        }
    }

    @Test
    public void testGetScheduledCheckDueResult() throws Exception {
   /* PromoteBean promoteBean = new PromoteBean();
    AutoPromoter promoter = new AutoPromoter(context);
    DeployBean currentDeploy = new DeployBean();
    currentDeploy.setStart_date(DateTime.now().minusDays(1).getMillis());
    currentDeploy.setBuild_id("prev123");
    DateTime now = DateTime.now();

    promoteBean.setSchedule("* * * * * ?");
    Pair<Boolean, Long> result =
        promoter.getScheduleCheckResult(environBean, currentDeploy, promoteBean);
    Assert.assertTrue(result.getLeft().booleanValue());
    promoteBean.setSchedule(CronTenAMPerDay);
    result =
        promoter.getScheduleCheckResult(environBean, currentDeploy, promoteBean);

    if (now.getHourOfDay() < 10) {
      DateTime yesterday = now.minusDays(-1);
      DateTime
          start =
          new DateTime(yesterday.getYear(), yesterday.getMonthOfYear(), yesterday.getDayOfMonth(),
              10, 0, 0);
      Assert.assertEquals(result.getRight().longValue(), start.getMillis());
    } else {
      DateTime
          start =
          new DateTime(now.getYear(), now.getMonthOfYear(), now.getDayOfMonth(), 10, 0, 0);
      Assert.assertEquals(result.getRight().longValue(), start.getMillis());*/
        //}
    }

    /* Autopromote enabled for any new build. But no builds and no prev deploys*/
    @Test
    public void testNoBuildNoPreviousDeployPromote() throws Exception {
        PromoteBean promoteBean = new PromoteBean();
        AutoPromoter promoter = new AutoPromoter(context);
        //No build. No previous deploy
        PromoteResult
            result =
            promoter.computePromoteBuildResult(environBean, null, 1, promoteBean);
        Assert.assertEquals(PromoteResult.ResultCode.NoAvailableBuild, result.getResult());

    }

    /* Autopromote enabled for any new build. no previous deploy and one new build*/
    @Test
    public void testOneBuildNoPreviousDeployPromote() throws Exception {
        PromoteBean promoteBean = new PromoteBean();
        AutoPromoter promoter = new AutoPromoter(context);
        //Has builds. Have previous deploy
        BuildBean build = new BuildBean();
        build.setBuild_name("xxx");
        build.setBuild_id("123");
        build.setPublish_date(DateTime.now().minusHours(1).getMillis());
        when(buildDAO.getAcceptedBuilds(anyString(), anyString(), anyObject(), anyInt()))
            .thenReturn(Arrays.asList(build));
        PromoteResult
            result =
            promoter.computePromoteBuildResult(environBean, null, 1, promoteBean);
        Assert.assertEquals(PromoteResult.ResultCode.PromoteBuild, result.getResult());
        Assert.assertEquals("123", result.getPromotedBuild());
    }

    @Test
    public void testOneBadBuildPromote() throws Exception {
        String buildName = "xxx";
        String buildId = "123";
        PromoteBean promoteBean = new PromoteBean();
        AutoPromoter promoter = new AutoPromoter(context);
        AutoPromoter promoterSpy = Mockito.spy(promoter);
        BuildBean build = new BuildBean();
        build.setBuild_id(buildId);
        build.setBuild_name(buildName);
        build.setCommit_date(DateTime.now().minusHours(1).getMillis());
        build.setPublish_date(DateTime.now().plusHours(1).getMillis());
        build.setScm_commit("abcde");

        TagBean tagBean = new TagBean();
        tagBean.setId(CommonUtils.getBase64UUID());
        tagBean.setTarget_type(TagTargetType.BUILD);
        tagBean.setTarget_id(build.getBuild_id());
        tagBean.setValue(TagValue.BAD_BUILD);
        tagBean.serializeTagMetaInfo(build);
        when(tagDAO.getLatestByTargetIdAndType(buildName, TagTargetType.BUILD,
            BuildTagsManagerImpl.MAXCHECKTAGS))
            .thenReturn(new ArrayList<TagBean>(Arrays.asList(tagBean)));
        when(buildDAO.getAcceptedBuilds(anyString(), anyString(), anyObject(), anyInt()))
            .thenReturn(Arrays.asList(build));
        PromoteResult
            result =
            promoter.computePromoteBuildResult(environBean, null, 1, promoteBean);
        Assert.assertEquals(PromoteResult.ResultCode.NoAvailableBuild, result.getResult());

        promoter.promoteBuild(environBean, null, 1, promoteBean);
        // bad build, safe promote never gets called
        verify(promoterSpy, never())
            .safePromote(anyObject(), anyString(), anyString(), anyObject(), anyObject());
    }

    @Test
    public void testOneBadBuildOneGoodBuildPromote() throws Exception {
//    build1, bad
//    build2, good
//    build3, current
        BuildBean build1 = new BuildBean();
        build1.setBuild_id("build1bad");
        build1.setBuild_name("build1bad");
        build1.setCommit_date(DateTime.now().minusHours(10).getMillis());
        build1.setPublish_date(DateTime.now().minusHours(10).getMillis());
        build1.setScm_commit("abcde");

        BuildBean build2 = new BuildBean();
        build2.setBuild_id("build2good");
        build2.setBuild_name("build2good");
        build2.setCommit_date(DateTime.now().minusHours(9).getMillis());
        build2.setPublish_date(DateTime.now().minusHours(9).getMillis());
        build2.setScm_commit("abcdxe");

        TagBean tagBean = new TagBean();
        tagBean.setId(CommonUtils.getBase64UUID());
        tagBean.setTarget_type(TagTargetType.BUILD);
        tagBean.setTarget_id(build1.getBuild_id());
        tagBean.setValue(TagValue.BAD_BUILD);
        tagBean.serializeTagMetaInfo(build1);

        DeployBean currentDeploy = new DeployBean();
        currentDeploy.setEnv_id("testenv");
        currentDeploy.setBuild_id("build3");

        when(tagDAO.getLatestByTargetIdAndType("build1bad", TagTargetType.BUILD,
            BuildTagsManagerImpl.MAXCHECKTAGS))
            .thenReturn(new ArrayList<TagBean>(Arrays.asList(tagBean)));

        when(buildDAO.getAcceptedBuilds(anyString(), anyString(), anyObject(), anyInt()))
            .thenReturn(Arrays.asList(build1, build2));
        // build1 is bad
        when(buildTagsManager.getEffectiveTagsWithBuilds(Arrays.asList(build1)))
            .thenReturn(Arrays.asList(BuildTagBean.createFromTagBean(tagBean)));

        AutoPromoter promoter = new AutoPromoter(context);
        PromoteBean promoteBean = new PromoteBean();
        PromoteResult
            result =
            promoter.computePromoteBuildResult(environBean, null, 1, promoteBean);
        Assert.assertEquals(PromoteResult.ResultCode.PromoteBuild, result.getResult());
        Assert.assertEquals("build2good", result.getPromotedBuild());
    }

    /* Autopromote enabled for any new build. Have previous deploy but no new build*/
    @Test
    public void testNoBuildWithPreviousDeploy() throws Exception {
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
        PromoteResult
            result =
            promoter.computePromoteBuildResult(environBean, previousDeploy, 1, promoteBean);
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
        build.setBuild_name("buildName");
        build.setBuild_id("123");
        build.setPublish_date(DateTime.now().minusHours(1).getMillis());
        when(buildDAO.getById("prev123")).thenReturn(preBuild);
        when(buildDAO.getAcceptedBuilds(anyString(), anyString(), anyObject(), anyInt()))
            .thenReturn(Arrays.asList(build));
        PromoteResult
            result =
            promoter.computePromoteBuildResult(environBean, previousDeploy, 1, promoteBean);
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
        PromoteResult
            result =
            promoter.computePromoteBuildResult(environBean, null, 1, promoteBean);
        Assert.assertEquals(PromoteResult.ResultCode.NoAvailableBuild, result.getResult());
    }

    /* Autopromote enabled for 10:00 AM every day. no previous deploy and one new build*/
    @Test
    public void testOneBuildNoPreviousDeploySchedulePromote() throws Exception {
        PromoteBean promoteBean = new PromoteBean();
        promoteBean.setEnv_id(environBean.getEnv_id());
        promoteBean.setSchedule(CronTenAMPerDay);
        promoteBean.setLast_update(0L);
        promoteBean.setDelay(0);

        AutoPromoter promoter = new AutoPromoter(context);
        //Has builds. Have previous deploy
        BuildBean build = new BuildBean();
        build.setBuild_name("buildName");
        build.setBuild_id("123");
        DateTime now = DateTime.now();

        //Set build time to be before 10 am today
        if (now.getHourOfDay() >= 10) {
            build.setPublish_date(now.minusHours(now.getHourOfDay() - 10 + 1).getMillis());
        } else {
            build.setPublish_date(DateTime.now().minusDays(1).getMillis());
        }
        when(buildDAO.getAcceptedBuilds(anyString(), anyString(), anyObject(), anyInt()))
            .thenReturn(Arrays.asList(build));
        PromoteResult
            result =
            promoter.computePromoteBuildResult(environBean, null, 1, promoteBean);
        Assert.assertEquals(PromoteResult.ResultCode.PromoteBuild, result.getResult());
        Assert.assertEquals("123", result.getPromotedBuild());
    }

    /* Autopromote enabled for 10:00 AM every day. no previous deploy and one new build after
    schedule*/
    @Test
    public void testOneBuildNoPreviousDeploySchedulePromote2() throws Exception {
        PromoteBean promoteBean = new PromoteBean();
        promoteBean.setEnv_id(environBean.getEnv_id());
        promoteBean.setSchedule(CronTenAMPerDay);
        promoteBean.setLast_update(0L);
        promoteBean.setDelay(0);
        AutoPromoter promoter = new AutoPromoter(context);
        //Has builds. Have previous deploy
        BuildBean build = new BuildBean();
        build.setBuild_name("buildName");
        build.setBuild_id("123");
        DateTime now = DateTime.now();

        //Set build time to be after 10 am today
        if (now.getHourOfDay() < 10) {
            build.setPublish_date(now.plusHours(10 - now.getHourOfDay() + 1).getMillis());
        } else {
            build.setPublish_date(DateTime.now().getMillis());
        }
        when(buildDAO.getAcceptedBuilds(anyString(), anyString(), anyObject(), anyInt()))
            .thenReturn(Arrays.asList(build));
        PromoteResult
            result =
            promoter.computePromoteBuildResult(environBean, null, 1, promoteBean);
        Assert.assertEquals(PromoteResult.ResultCode.NoAvailableBuild, result.getResult());
    }

    /* Autopromote enabled for 10:00 AM every day. Have old previous deploy but no new build*/
    @Test
    public void testNoBuildWithPreviousDeploySchedule() throws Exception {
        PromoteBean promoteBean = new PromoteBean();
        promoteBean.setEnv_id(environBean.getEnv_id());
        promoteBean.setSchedule(CronTenAMPerDay);
        promoteBean.setLast_update(0L);
        promoteBean.setDelay(0);
        AutoPromoter promoter = new AutoPromoter(context);

        DeployBean previousDeploy = new DeployBean();
        previousDeploy.setStart_date(DateTime.now().minusDays(1).getMillis());
        previousDeploy.setBuild_id("prev123");

        BuildBean preBuild = new BuildBean();
        preBuild.setBuild_name("buildName");
        preBuild.setBuild_id("prev123");
        preBuild.setPublish_date(DateTime.now().minusHours(3).getMillis());

        when(buildDAO.getById("prev123")).thenReturn(preBuild);
        PromoteResult
            result =
            promoter.computePromoteBuildResult(environBean, previousDeploy, 1, promoteBean);
        Assert.assertEquals(PromoteResult.ResultCode.NoAvailableBuild, result.getResult());
    }

    /* Autopromote enabled for 10:00 AM every day. Have a very recent previous deploy and no new
    build*/
    @Test
    public void testNoBuildWithPreviousDeploySchedule2() throws Exception {
        PromoteBean promoteBean = new PromoteBean();
        promoteBean.setEnv_id(environBean.getEnv_id());
        promoteBean.setSchedule(CronTenAMPerDay);
        promoteBean.setDelay(0);
        AutoPromoter promoter = new AutoPromoter(context);

        DeployBean previousDeploy = new DeployBean();
        previousDeploy.setStart_date(DateTime.now().minusMinutes(1).getMillis());
        previousDeploy.setBuild_id("prev123");

        BuildBean preBuild = new BuildBean();
        preBuild.setBuild_name("buildName");
        preBuild.setBuild_id("prev123");
        preBuild.setPublish_date(DateTime.now().minusHours(3).getMillis());

        when(buildDAO.getById("prev123")).thenReturn(preBuild);
        PromoteResult
            result =
            promoter.computePromoteBuildResult(environBean, previousDeploy, 1, promoteBean);
        Assert.assertEquals(PromoteResult.ResultCode.NoAvailableBuild, result.getResult());
    }


    @Test
    public void testOneBuildwithPreviousDeploySchedule() throws Exception {
        PromoteBean promoteBean = new PromoteBean();
        promoteBean.setEnv_id(environBean.getEnv_id());
        promoteBean.setSchedule(CronTenAMPerDay);
        promoteBean.setLast_update(0L);
        promoteBean.setDelay(0);
        AutoPromoter promoter = new AutoPromoter(context);
        //Has builds. No previous deploy
        DeployBean previousDeploy = new DeployBean();
        previousDeploy.setStart_date(DateTime.now(DateTimeZone.UTC).minusDays(1).getMillis());
        previousDeploy.setBuild_id("prev123");

        BuildBean preBuild = new BuildBean();
        preBuild.setBuild_name("buildName");
        preBuild.setBuild_id("prev123");
        preBuild.setPublish_date(DateTime.now(DateTimeZone.UTC).minusHours(48).getMillis());

        BuildBean build = new BuildBean();
        build.setBuild_name("buildName");
        build.setBuild_id("123");
        DateTime now = DateTime.now(DateTimeZone.UTC);
        if (now.getHourOfDay() > 10) {
            build.setPublish_date((now.minusHours(now.getHourOfDay() - 10 + 1)).getMillis());
        } else {
            build.setPublish_date(DateTime.now(DateTimeZone.UTC).minusDays(1).minusHours(1).getMillis());
        }
        when(buildDAO.getById("prev123")).thenReturn(preBuild);
        when(buildDAO.getAcceptedBuilds(anyString(), anyString(), anyObject(), anyInt()))
            .thenReturn(Arrays.asList(build));
        PromoteResult
            result =
            promoter.computePromoteBuildResult(environBean, previousDeploy, 1, promoteBean);
        Assert.assertEquals(PromoteResult.ResultCode.PromoteBuild, result.getResult());
        Assert.assertEquals("123", result.getPromotedBuild());
    }
}
