/**
 * Copyright (c) 2017-2024 Pinterest, Inc.
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
package com.pinterest.teletraan.worker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.pinterest.deployservice.dao.DeployDAO;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.TagDAO;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class AutoPromoteDeployTest {

    static final String CronTenAMPerDay = "0 0 10 * * ?";
    static final String CronWorkTimePerDay = "0 40 9-17 ? * *";
    ServiceContext context;
    DeployBean first = new DeployBean();
    DeployBean second = new DeployBean();
    EnvironBean environBean = new EnvironBean();
    EnvironBean predEnvironBean = new EnvironBean();
    BuildDAO buildDAO;
    EnvironDAO environDAO;
    DeployDAO deployDAO;
    TagDAO tagDAO;
    BuildTagsManager buildTagsManager;
    List<DeployBean> allDeployBeans = new ArrayList<>();

    public List<DeployBean> getAcceptedDeploysDelayed(String envId, Interval interval) {
        List<DeployBean> ret = new ArrayList<>();
        for (DeployBean bean : allDeployBeans) {
            if (bean.getEnv_id().equals(envId)
                    && interval.contains(bean.getStart_date())
                    &&
                    // contains is inclusive for beginning
                    interval.getStartMillis() != bean.getStart_date()) {
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
        tagDAO = mock(TagDAO.class);
        buildTagsManager = mock(BuildTagsManager.class);
        context.setBuildTagsManager(buildTagsManager);
        context.setTagDAO(tagDAO);
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
    public void clean() throws Exception {
        allDeployBeans.clear();
    }

    /* Autopromote enabled. But no current deploy and no prev deploys environ*/
    @Test
    public void testNoPredEnvironmentPromote() throws Exception {
        PromoteBean promoteBean = new PromoteBean();
        promoteBean.setPred_stage("pred");
        when(environDAO.getByStage(environBean.getEnv_name(), "pred")).thenReturn(null);

        AutoPromoter promoter = new AutoPromoter(context);
        // No build. No previous deploy
        PromoteResult result =
                promoter.computePromoteDeployResult(environBean, null, 10, promoteBean);
        Assert.assertEquals(PromoteResult.ResultCode.NoPredEnvironment, result.getResult());

        // Ensure same for scheduled build
        promoteBean.setSchedule(CronTenAMPerDay);
        result = promoter.computePromoteDeployResult(environBean, null, 10, promoteBean);
        Assert.assertEquals(PromoteResult.ResultCode.NoPredEnvironment, result.getResult());
    }

    @Test
    public void testOneBadDeployPromote() throws Exception {
        // pred badbuild1
        // current goodbuild2
        PromoteBean promoteBean = new PromoteBean();
        promoteBean.setPred_stage("pred");
        promoteBean.setDelay(100); // minutes
        DateTime now = DateTime.now();

        String badBuildId = "badbuild1";
        String goodBuildId = "goodBuild2";

        BuildBean badBuild = new BuildBean();
        badBuild.setBuild_id(badBuildId);
        badBuild.setBuild_name(badBuildId);
        badBuild.setCommit_date(now.minusHours(24).getMillis());
        badBuild.setPublish_date(now.minusHours(24).getMillis());
        badBuild.setScm_commit("abcde");

        TagBean tagBean = new TagBean();
        tagBean.setId(CommonUtils.getBase64UUID());
        tagBean.setTarget_type(TagTargetType.BUILD);
        tagBean.setTarget_id(badBuildId);
        tagBean.setValue(TagValue.BAD_BUILD);
        tagBean.serializeTagMetaInfo(badBuild);

        predEnvironBean.setDeploy_id("deploy1");

        DeployBean prevDeploy = new DeployBean();
        prevDeploy.setDeploy_id("deploy1");
        prevDeploy.setEnv_id(predEnvironBean.getEnv_id());
        prevDeploy.setStart_date(now.minusHours(23).getMillis());
        prevDeploy.setBuild_id(badBuildId);

        DeployBean currentDeploy = new DeployBean();
        currentDeploy.setDeploy_id("deploy2");
        currentDeploy.setFrom_deploy(prevDeploy.getDeploy_id());
        currentDeploy.setEnv_id(environBean.getEnv_id());
        currentDeploy.setStart_date(now.minusHours(22).plusMinutes(40).getMillis());
        currentDeploy.setBuild_id(goodBuildId);

        allDeployBeans.add(prevDeploy);
        allDeployBeans.add(currentDeploy);

        when(deployDAO.getById("deploy1")).thenReturn(prevDeploy);
        when(environDAO.getByStage(environBean.getEnv_name(), "pred")).thenReturn(predEnvironBean);
        when(deployDAO.getAcceptedDeploys(anyString(), any(), anyInt()))
                .thenReturn(Arrays.asList(prevDeploy));
        when(buildDAO.getBuildsFromIds(new HashSet<>(Arrays.asList(badBuildId))))
                .thenReturn(Arrays.asList(badBuild));

        when(buildTagsManager.getEffectiveTagsWithBuilds(Arrays.asList(badBuild)))
                .thenReturn(Arrays.asList(BuildTagBean.createFromTagBean(tagBean)));

        when(tagDAO.getLatestByTargetIdAndType(
                        badBuildId, TagTargetType.BUILD, BuildTagsManagerImpl.MAXCHECKTAGS))
                .thenReturn(Arrays.asList(tagBean));

        AutoPromoter promoter = new AutoPromoter(context);
        AutoPromoter promoterSpy = Mockito.spy(promoter);
        promoter.promoteDeploy(environBean, currentDeploy, 1, promoteBean);

        PromoteResult result =
                promoter.computePromoteDeployResult(environBean, currentDeploy, 1, promoteBean);
        Assert.assertEquals(
                PromoteResult.ResultCode.NoCandidateWithinDelayPeriod, result.getResult());
        // bad build, safe promote never gets called
        verify(promoterSpy, never()).safePromote(any(), anyString(), anyString(), any(), any());
    }

    /* Autopromote enabled. Has pred env but it has no deploys*/
    @Test
    public void testNoPredEnvironDeployPromote() throws Exception {
        PromoteBean promoteBean = new PromoteBean();
        promoteBean.setPred_stage("pred");
        predEnvironBean.setDeploy_id(null);
        when(environDAO.getByStage(environBean.getEnv_name(), "pred")).thenReturn(predEnvironBean);

        AutoPromoter promoter = new AutoPromoter(context);
        // No build. No previous deploy
        PromoteResult result =
                promoter.computePromoteDeployResult(environBean, null, 10, promoteBean);
        Assert.assertEquals(PromoteResult.ResultCode.NoPredEnvironmentDeploy, result.getResult());

        // Ensure same for scheduled build
        promoteBean.setSchedule(CronTenAMPerDay);
        result = promoter.computePromoteDeployResult(environBean, null, 10, promoteBean);
        Assert.assertEquals(PromoteResult.ResultCode.NoPredEnvironmentDeploy, result.getResult());
    }

    /*
    Autopromote enabled with no delay. Have pred environ and already promote one deploy.
    No current deploy available
    */
    @Test
    public void testPredDeployAlreadyPromote() throws Exception {
        DateTime now = DateTime.now();
        PromoteBean promoteBean = new PromoteBean();
        promoteBean.setPred_stage("pred");
        promoteBean.setDelay(0); // minutes
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
        when(deployDAO.getAcceptedDeploys(anyString(), any(), anyInt()))
                .thenAnswer(
                        new Answer<List<DeployBean>>() {
                            @Override
                            public List<DeployBean> answer(InvocationOnMock invocationOnMock)
                                    throws Throwable {
                                return getAcceptedDeploysDelayed(
                                        (String) invocationOnMock.getArguments()[0],
                                        (Interval) invocationOnMock.getArguments()[1]);
                            }
                        });

        AutoPromoter promoter = new AutoPromoter(context);
        // No build. No previous deploy
        PromoteResult result =
                promoter.computePromoteDeployResult(environBean, currentDeploy, 10, promoteBean);
        Assert.assertEquals(
                PromoteResult.ResultCode.NoCandidateWithinDelayPeriod, result.getResult());

        promoteBean.setSchedule(CronTenAMPerDay);
        result = promoter.computePromoteDeployResult(environBean, currentDeploy, 10, promoteBean);
        Assert.assertEquals(
                PromoteResult.ResultCode.NoCandidateWithinDelayPeriod, result.getResult());
    }

    /* Autopromote enabled for any new build. Have previous deploy but from different deploy*/
    @Test
    public void testPredDeployDelayPromote() throws Exception {
        DateTime now = DateTime.now();
        PromoteBean promoteBean = new PromoteBean();
        promoteBean.setPred_stage("pred");
        promoteBean.setDelay(10); // minutes
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
        newDeploy.setBuild_id("build3");

        BuildBean build3 = new BuildBean();
        build3.setBuild_name("buildname");
        build3.setBuild_id("build3");

        allDeployBeans.add(prevDeploy);
        allDeployBeans.add(newDeploy);
        when(environDAO.getByStage(environBean.getEnv_name(), "pred")).thenReturn(predEnvironBean);
        when(deployDAO.getById(prevDeploy.getDeploy_id())).thenReturn(prevDeploy);
        when(deployDAO.getById(newDeploy.getDeploy_id())).thenReturn(newDeploy);
        when(deployDAO.getAcceptedDeploys(anyString(), any(), anyInt()))
                .thenAnswer(
                        new Answer<List<DeployBean>>() {
                            @Override
                            public List<DeployBean> answer(InvocationOnMock invocationOnMock)
                                    throws Throwable {
                                return getAcceptedDeploysDelayed(
                                        (String) invocationOnMock.getArguments()[0],
                                        (Interval) invocationOnMock.getArguments()[1]);
                            }
                        });
        AutoPromoter promoter = new AutoPromoter(context);
        // Pre deploy is 6 minutes ago, delay is 10 minutes
        newDeploy.setStart_date(now.minusMinutes(6).getMillis());
        PromoteResult result =
                promoter.computePromoteDeployResult(environBean, currentDeploy, 1, promoteBean);
        Assert.assertEquals(
                PromoteResult.ResultCode.NoCandidateWithinDelayPeriod, result.getResult());

        when(buildDAO.getBuildsFromIds(new HashSet<>(Arrays.asList("build3"))))
                .thenReturn(Arrays.asList(build3));

        // Set predeploy to 11 minutes, delay is 10 minutes
        newDeploy.setStart_date(now.minusMinutes(11).getMillis());
        result = promoter.computePromoteDeployResult(environBean, currentDeploy, 1, promoteBean);
        Assert.assertEquals(PromoteResult.ResultCode.PromoteDeploy, result.getResult());
    }

    /* Autopromote enabled for any new build. Have previous deploy but from different deploy*/
    @Test
    public void testPredDeployWithDelayPromote() throws Exception {
        DateTime now = DateTime.now();
        PromoteBean promoteBean = new PromoteBean();
        promoteBean.setPred_stage("pred");
        promoteBean.setDelay(10); // minutes
        predEnvironBean.setDeploy_id("deploy1");

        DeployBean newDeploy = new DeployBean();
        newDeploy.setDeploy_id("deploy1");
        newDeploy.setBuild_id("build1");
        newDeploy.setEnv_id(predEnvironBean.getEnv_id());
        DeployBean currentDeploy = new DeployBean();
        currentDeploy.setDeploy_id("deploy2");

        BuildBean build1 = new BuildBean();
        build1.setBuild_name("buildname");
        build1.setBuild_id("build1");

        currentDeploy.setStart_date(now.minusMinutes(25).getMillis());
        allDeployBeans.add(newDeploy);
        when(environDAO.getByStage(environBean.getEnv_name(), "pred")).thenReturn(predEnvironBean);
        when(deployDAO.getById("deploy1")).thenReturn(newDeploy);
        when(deployDAO.getAcceptedDeploys(anyString(), any(), anyInt()))
                .thenAnswer(
                        new Answer<List<DeployBean>>() {
                            @Override
                            public List<DeployBean> answer(InvocationOnMock invocationOnMock)
                                    throws Throwable {
                                return getAcceptedDeploysDelayed(
                                        (String) invocationOnMock.getArguments()[0],
                                        (Interval) invocationOnMock.getArguments()[1]);
                            }
                        });
        AutoPromoter promoter = new AutoPromoter(context);
        // Pre deploy is 6 minutes ago, delay is 10 minutes
        newDeploy.setStart_date(now.minusMinutes(6).getMillis());
        PromoteResult result =
                promoter.computePromoteDeployResult(environBean, currentDeploy, 1, promoteBean);
        Assert.assertEquals(
                PromoteResult.ResultCode.NoCandidateWithinDelayPeriod, result.getResult());

        when(buildDAO.getBuildsFromIds(new HashSet<>(Arrays.asList("build1"))))
                .thenReturn(Arrays.asList(build1));

        // Set predeploy to 11 minutes, delay is 10 minutes
        newDeploy.setStart_date(now.minusMinutes(11).getMillis());
        result = promoter.computePromoteDeployResult(environBean, currentDeploy, 1, promoteBean);
        Assert.assertEquals(PromoteResult.ResultCode.PromoteDeploy, result.getResult());
    }

    @Test
    public void testScheduledPromote() throws Exception {
        DateTime now = DateTime.now();
        PromoteBean promoteBean = new PromoteBean();
        promoteBean.setSchedule(CronTenAMPerDay);
        promoteBean.setPred_stage("pred");
        promoteBean.setDelay(0); // minutes

        predEnvironBean.setDeploy_id("deploy1");
        DeployBean prevDeploy = new DeployBean();
        prevDeploy.setDeploy_id("deploy1");
        prevDeploy.setEnv_id(predEnvironBean.getEnv_id());

        BuildBean newBuild = new BuildBean();
        newBuild.setBuild_name("buildName");
        newBuild.setBuild_id("newBuild");
        when(buildDAO.getBuildsFromIds(new HashSet<>(Arrays.asList("newBuild"))))
                .thenReturn(Arrays.asList(newBuild));

        DeployBean newDeploy = new DeployBean();
        newDeploy.setDeploy_id("newDeploy");
        newDeploy.setBuild_id("newBuild");
        newDeploy.setEnv_id(predEnvironBean.getEnv_id());

        DeployBean currentDeploy = new DeployBean();
        currentDeploy.setDeploy_id("deploy2");
        currentDeploy.setFrom_deploy("deploy1");

        allDeployBeans.addAll(Arrays.asList(prevDeploy, newDeploy));

        when(environDAO.getByStage(environBean.getEnv_name(), "pred")).thenReturn(predEnvironBean);
        when(deployDAO.getById(prevDeploy.getDeploy_id())).thenReturn(prevDeploy);
        when(deployDAO.getById(newDeploy.getDeploy_id())).thenReturn(newDeploy);
        when(deployDAO.getAcceptedDeploys(anyString(), any(), anyInt()))
                .thenAnswer(
                        new Answer<List<DeployBean>>() {
                            @Override
                            public List<DeployBean> answer(InvocationOnMock invocationOnMock)
                                    throws Throwable {
                                return getAcceptedDeploysDelayed(
                                        (String) invocationOnMock.getArguments()[0],
                                        (Interval) invocationOnMock.getArguments()[1]);
                            }
                        });

        if (now.getHourOfDay() >= 10) {
            DateTime cuttingPoint =
                    new DateTime(
                            now.getYear(), now.getMonthOfYear(), now.getDayOfMonth(), 10, 0, 0);

            prevDeploy.setStart_date(cuttingPoint.minusHours(3).getMillis());
            currentDeploy.setStart_date(cuttingPoint.minusHours(2).getMillis());
            newDeploy.setStart_date(cuttingPoint.minusHours(1).getMillis());
            AutoPromoter promoter = new AutoPromoter(context);
            PromoteResult result =
                    promoter.computePromoteDeployResult(environBean, currentDeploy, 1, promoteBean);
            Assert.assertEquals(PromoteResult.ResultCode.PromoteDeploy, result.getResult());
            promoteBean.setDelay(1 + (int) (now.getMillis() - newDeploy.getStart_date()) / 60000);
            result =
                    promoter.computePromoteDeployResult(environBean, currentDeploy, 1, promoteBean);
            Assert.assertEquals(
                    PromoteResult.ResultCode.NoCandidateWithinDelayPeriod, result.getResult());

        } else {
            // Before 10 am. Need newdeploy to be >25 hours
            prevDeploy.setStart_date(now.minusHours(27).getMillis());
            currentDeploy.setStart_date(now.minusHours(26).getMillis());
            newDeploy.setStart_date(now.minusHours(25).getMillis());
            AutoPromoter promoter = new AutoPromoter(context);
            PromoteResult result =
                    promoter.computePromoteDeployResult(environBean, currentDeploy, 1, promoteBean);
            Assert.assertEquals(PromoteResult.ResultCode.PromoteDeploy, result.getResult());
            promoteBean.setDelay(1 + (int) (now.getMillis() - newDeploy.getStart_date()) / 60000);
            result =
                    promoter.computePromoteDeployResult(
                            environBean, currentDeploy, 10, promoteBean);
            Assert.assertEquals(
                    PromoteResult.ResultCode.NoCandidateWithinDelayPeriod, result.getResult());
        }
    }

    @Test
    public void testScheduledPromote2() throws Exception {
        DateTime now = DateTime.now();
        PromoteBean promoteBean = new PromoteBean();
        promoteBean.setSchedule(CronWorkTimePerDay);
        promoteBean.setPred_stage("pred");
        promoteBean.setDelay(10); // minutes

        predEnvironBean.setDeploy_id("deploy1");
        DeployBean prevDeploy = new DeployBean();
        prevDeploy.setDeploy_id("deploy1");
        prevDeploy.setEnv_id(predEnvironBean.getEnv_id());

        BuildBean newBuild = new BuildBean();
        newBuild.setBuild_name("buildname");
        newBuild.setBuild_id("newBuild");
        when(buildDAO.getBuildsFromIds(new HashSet<>(Arrays.asList("newBuild"))))
                .thenReturn(Arrays.asList(newBuild));

        DeployBean newDeploy = new DeployBean();
        newDeploy.setDeploy_id("newDeploy");
        newDeploy.setBuild_id("newBuild");
        newDeploy.setEnv_id(predEnvironBean.getEnv_id());

        DeployBean currentDeploy = new DeployBean();
        currentDeploy.setDeploy_id("deploy2");
        currentDeploy.setFrom_deploy("deploy1");

        allDeployBeans.addAll(Arrays.asList(prevDeploy, newDeploy));

        when(environDAO.getByStage(environBean.getEnv_name(), "pred")).thenReturn(predEnvironBean);
        when(deployDAO.getById(prevDeploy.getDeploy_id())).thenReturn(prevDeploy);
        when(deployDAO.getById(newDeploy.getDeploy_id())).thenReturn(newDeploy);
        when(deployDAO.getAcceptedDeploys(anyString(), any(), anyInt()))
                .thenAnswer(
                        new Answer<List<DeployBean>>() {
                            @Override
                            public List<DeployBean> answer(InvocationOnMock invocationOnMock)
                                    throws Throwable {
                                return getAcceptedDeploysDelayed(
                                        (String) invocationOnMock.getArguments()[0],
                                        (Interval) invocationOnMock.getArguments()[1]);
                            }
                        });

        if (now.getHourOfDay() >= 9 && now.getHourOfDay() <= 17) {
            prevDeploy.setStart_date(now.minusHours(25).getMillis());
            currentDeploy.setStart_date(now.minusHours(24).getMillis());
            newDeploy.setStart_date(now.minusHours(now.getHourOfDay()).getMillis());
            AutoPromoter promoter = new AutoPromoter(context);
            PromoteResult result =
                    promoter.computePromoteDeployResult(environBean, currentDeploy, 1, promoteBean);
            Assert.assertEquals(PromoteResult.ResultCode.PromoteDeploy, result.getResult());

            promoteBean.setDelay(1 + (int) (now.getMillis() - newDeploy.getStart_date()) / 60000);
            result =
                    promoter.computePromoteDeployResult(environBean, currentDeploy, 1, promoteBean);
            Assert.assertEquals(
                    PromoteResult.ResultCode.NoCandidateWithinDelayPeriod, result.getResult());

        } else {
            prevDeploy.setStart_date(now.minusHours(27).getMillis());
            currentDeploy.setStart_date(now.minusHours(26).getMillis());
            newDeploy.setStart_date(now.minusHours(25).getMillis());
            AutoPromoter promoter = new AutoPromoter(context);
            PromoteResult result =
                    promoter.computePromoteDeployResult(environBean, currentDeploy, 1, promoteBean);
            Assert.assertEquals(PromoteResult.ResultCode.PromoteDeploy, result.getResult());
            promoteBean.setDelay(1 + (int) (now.getMillis() - newDeploy.getStart_date()) / 60000);
            result =
                    promoter.computePromoteDeployResult(
                            environBean, currentDeploy, 10, promoteBean);
            Assert.assertEquals(
                    PromoteResult.ResultCode.NoCandidateWithinDelayPeriod, result.getResult());
        }
    }
}
