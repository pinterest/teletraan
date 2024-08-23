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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.Interval;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class AutoPromoteBuildTest {

    static final String CronTenAMPerDay = "0 0 10 * * ?";
    ServiceContext context;
    EnvironBean environBean = new EnvironBean();
    BuildDAO buildDAO;
    TagDAO tagDAO;
    BuildTagsManager buildTagsManager;
    TestTimeProvider timeProvider = new TestTimeProvider();
    BuildBean t8AMBuildBean;
    BuildBean t9AMBuildBean;
    DateTime t9AM = new DateTime(2022, 7, 4, 9, 0, 0);
    DateTime t10AM = t9AM.plusHours(1);
    PromoteBean t10AMPromoteBean;

    class TestTimeProvider implements DateTimeUtils.MillisProvider {
        private long millis = 0L;

        public void setClock(long millis) {
            this.millis = millis;
        }

        @Override
        public long getMillis() {
            return this.millis;
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        context = new ServiceContext();
        buildDAO = mock(BuildDAO.class);
        tagDAO = mock(TagDAO.class);
        buildTagsManager = mock(BuildTagsManager.class);
        context.setBuildTagsManager(buildTagsManager);
        context.setBuildDAO(buildDAO);
        context.setTagDAO(tagDAO);
        environBean.setEnv_id("test1");

        t8AMBuildBean = new BuildBean();
        t8AMBuildBean.setBuild_id("8AMBuildId");
        t8AMBuildBean.setBuild_name("8AMBuildName");
        t8AMBuildBean.setCommit_date(t9AM.minusHours(2).getMillis());
        t8AMBuildBean.setPublish_date(t9AM.minusHours(1).getMillis());
        t8AMBuildBean.setScm_commit("fghij");

        t9AMBuildBean = new BuildBean();
        t9AMBuildBean.setBuild_id("9AMBuildId");
        t9AMBuildBean.setBuild_name("9AMBuildName");
        t9AMBuildBean.setCommit_date(t9AM.minusHours(1).getMillis());
        t9AMBuildBean.setPublish_date(t9AM.getMillis());
        t9AMBuildBean.setScm_commit("abcde");

        t10AMPromoteBean = new PromoteBean();
        t10AMPromoteBean.setEnv_id(environBean.getEnv_id());
        t10AMPromoteBean.setSchedule(CronTenAMPerDay);
        t10AMPromoteBean.setLast_update(0L);
        t10AMPromoteBean.setDelay(0);
    }

    @AfterEach
    public void tearDown() throws Exception {
        DateTimeUtils.setCurrentMillisSystem();
    }

    /* AutoPromote enabled for any new build.
    - no build
    - no previous deploys */
    @Test
    public void testNoBuildNoPreviousDeployPromote() throws Exception {
        PromoteBean promoteBean = new PromoteBean();
        AutoPromoter promoter = new AutoPromoter(context);
        // No build. No previous deploy
        PromoteResult result =
                promoter.computePromoteBuildResult(environBean, null, 1, promoteBean);
        assertEquals(PromoteResult.ResultCode.NoAvailableBuild, result.getResult());
    }

    /* AutoPromote enabled for any new build.
    - 1 new build
    - no previous deploy */
    @Test
    public void testOneBuildNoPreviousDeployPromote() throws Exception {
        PromoteBean promoteBean = new PromoteBean();
        AutoPromoter promoter = new AutoPromoter(context);
        // Has builds. Have previous deploy
        when(buildDAO.getAcceptedBuilds(any(), any(), any(Interval.class), anyInt()))
                .thenReturn(Arrays.asList(t9AMBuildBean));
        PromoteResult result =
                promoter.computePromoteBuildResult(environBean, null, 1, promoteBean);
        assertEquals(PromoteResult.ResultCode.PromoteBuild, result.getResult());
        assertEquals(t9AMBuildBean.getBuild_id(), result.getPromotedBuild());
    }

    @Test
    public void testOneBadBuildPromote() throws Exception {
        String buildName = t9AMBuildBean.getBuild_name();
        PromoteBean promoteBean = new PromoteBean();
        AutoPromoter promoter = new AutoPromoter(context);
        AutoPromoter promoterSpy = Mockito.spy(promoter);

        TagBean tagBean = new TagBean();
        tagBean.setId(CommonUtils.getBase64UUID());
        tagBean.setTarget_type(TagTargetType.BUILD);
        tagBean.setTarget_id(buildName);
        tagBean.setValue(TagValue.BAD_BUILD);
        tagBean.serializeTagMetaInfo(t9AMBuildBean);
        when(tagDAO.getLatestByTargetIdAndType(
                        buildName, TagTargetType.BUILD, BuildTagsManagerImpl.MAXCHECKTAGS))
                .thenReturn(new ArrayList<TagBean>(Arrays.asList(tagBean)));
        when(buildDAO.getAcceptedBuilds(any(), any(), any(), anyInt()))
                .thenReturn(Arrays.asList(t9AMBuildBean));
        PromoteResult result =
                promoter.computePromoteBuildResult(environBean, null, 1, promoteBean);
        assertEquals(PromoteResult.ResultCode.NoAvailableBuild, result.getResult());

        promoter.promoteBuild(environBean, null, 1, promoteBean);
        // bad build, safe promote never gets called
        verify(promoterSpy, never()).safePromote(any(), any(), any(), any(), any());
    }

    @Test
    public void testOneBadBuildOneGoodBuildPromote() throws Exception {
        // build1, bad
        // build2, good
        // build3, current
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
        tagBean.setTarget_id(build1.getBuild_name());
        tagBean.setValue(TagValue.BAD_BUILD);
        tagBean.serializeTagMetaInfo(build1);

        DeployBean currentDeploy = new DeployBean();
        currentDeploy.setEnv_id("testenv");
        currentDeploy.setBuild_id("build3");

        when(tagDAO.getLatestByTargetIdAndType(
                        build1.getBuild_name(),
                        TagTargetType.BUILD,
                        BuildTagsManagerImpl.MAXCHECKTAGS))
                .thenReturn(new ArrayList<TagBean>(Arrays.asList(tagBean)));

        when(buildDAO.getAcceptedBuilds(any(), any(), any(), anyInt()))
                .thenReturn(Arrays.asList(build1, build2));
        // build1 is bad
        when(buildTagsManager.getEffectiveTagsWithBuilds(Arrays.asList(build1)))
                .thenReturn(Arrays.asList(BuildTagBean.createFromTagBean(tagBean)));

        AutoPromoter promoter = new AutoPromoter(context);
        PromoteBean promoteBean = new PromoteBean();
        PromoteResult result =
                promoter.computePromoteBuildResult(environBean, null, 1, promoteBean);
        assertEquals(PromoteResult.ResultCode.PromoteBuild, result.getResult());
        assertEquals(build2.getBuild_id(), result.getPromotedBuild());
    }

    /* AutoPromote enabled for any new build.
    - no new build
    - 1 previous deploy */
    @Test
    public void testNoBuildWithPreviousDeploy() throws Exception {
        PromoteBean promoteBean = new PromoteBean();
        AutoPromoter promoter = new AutoPromoter(context);
        // Has builds. No previous deploy
        DeployBean previousDeploy = new DeployBean();
        previousDeploy.setStart_date(DateTime.now().minusHours(2).getMillis());
        previousDeploy.setBuild_id(t8AMBuildBean.getBuild_id());

        when(buildDAO.getById(t8AMBuildBean.getBuild_id())).thenReturn(t8AMBuildBean);
        PromoteResult result =
                promoter.computePromoteBuildResult(environBean, previousDeploy, 1, promoteBean);
        assertEquals(PromoteResult.ResultCode.NoAvailableBuild, result.getResult());
    }

    /* AutoPromote enabled for any new build.
    - 1 new build
    - 1 previous deploy */
    @Test
    public void testOneBuildWithPreviousDeploy() throws Exception {
        PromoteBean promoteBean = new PromoteBean();
        AutoPromoter promoter = new AutoPromoter(context);
        // Has builds. No previous deploy
        DeployBean previousDeploy = new DeployBean();
        previousDeploy.setStart_date(DateTime.now().minusHours(2).getMillis());
        previousDeploy.setBuild_id(t8AMBuildBean.getBuild_id());

        when(buildDAO.getById(t8AMBuildBean.getBuild_id())).thenReturn(t8AMBuildBean);
        when(buildDAO.getAcceptedBuilds(any(), any(), any(Interval.class), anyInt()))
                .thenReturn(Arrays.asList(t9AMBuildBean));
        PromoteResult result =
                promoter.computePromoteBuildResult(environBean, previousDeploy, 1, promoteBean);
        assertEquals(PromoteResult.ResultCode.PromoteBuild, result.getResult());
        assertEquals(t9AMBuildBean.getBuild_id(), result.getPromotedBuild());
    }

    /* AutoPromote scheduled for 10:00 AM daily
    - no build
    - no previous deploy */
    @Test
    public void testNoBuildNoPreviousDeployScheduledPromote() throws Exception {
        AutoPromoter promoter = new AutoPromoter(context);

        DateTimeUtils.setCurrentMillisProvider(timeProvider);
        timeProvider.setClock(t10AM.getMillis());

        // No build. No previous deploy
        PromoteResult result =
                promoter.computePromoteBuildResult(environBean, null, 1, t10AMPromoteBean);
        assertEquals(PromoteResult.ResultCode.NoAvailableBuild, result.getResult());
    }

    /* AutoPromote scheduled for 10:00 AM daily
    - 1 new build
    - no previous deploy */
    @Test
    public void testOneBuildNoPreviousDeploySchedulePromote() throws Exception {
        AutoPromoter promoter = new AutoPromoter(context);
        // Has builds. Have previous deploy

        DateTimeUtils.setCurrentMillisProvider(timeProvider);
        timeProvider.setClock(t10AM.getMillis());

        when(buildDAO.getAcceptedBuilds(any(), any(), any(), anyInt()))
                .thenReturn(Arrays.asList(t9AMBuildBean));
        PromoteResult result =
                promoter.computePromoteBuildResult(environBean, null, 1, t10AMPromoteBean);
        assertEquals(PromoteResult.ResultCode.PromoteBuild, result.getResult());
        assertEquals(t9AMBuildBean.getBuild_id(), result.getPromotedBuild());
    }

    /* AutoPromote scheduled for 10:00 AM daily
    - 1 build after schedule
    - no previous deploy */
    @Test
    public void testOneBuildNoPreviousDeploySchedulePromote2() throws Exception {
        AutoPromoter promoter = new AutoPromoter(context);
        // Has builds. Have previous deploy
        BuildBean build = new BuildBean();
        build.setBuild_name("buildName");
        build.setBuild_id("123");
        build.setPublish_date(t10AM.plusMinutes(1).getMillis());

        DateTimeUtils.setCurrentMillisProvider(timeProvider);
        timeProvider.setClock(t10AM.getMillis());

        when(buildDAO.getAcceptedBuilds(any(), any(), any(), anyInt()))
                .thenReturn(Arrays.asList(build));
        PromoteResult result =
                promoter.computePromoteBuildResult(environBean, null, 1, t10AMPromoteBean);
        assertEquals(PromoteResult.ResultCode.NoAvailableBuild, result.getResult());
    }

    /* AutoPromote scheduled for 10:00 AM daily
    - no new build
    - 1 old previous deploy */
    @Test
    public void testNoBuildWithPreviousDeploySchedule() throws Exception {
        AutoPromoter promoter = new AutoPromoter(context);
        DeployBean previousDeploy = new DeployBean();
        previousDeploy.setStart_date(t9AM.getMillis());
        previousDeploy.setBuild_id(t9AMBuildBean.getBuild_id());

        DateTimeUtils.setCurrentMillisProvider(timeProvider);
        timeProvider.setClock(t10AM.getMillis());

        when(buildDAO.getById(t9AMBuildBean.getBuild_id())).thenReturn(t9AMBuildBean);
        PromoteResult result =
                promoter.computePromoteBuildResult(
                        environBean, previousDeploy, 1, t10AMPromoteBean);
        assertEquals(PromoteResult.ResultCode.NoAvailableBuild, result.getResult());
    }

    /* AutoPromote scheduled for 10:00 AM daily
    - no new build
    - 1 recent previous deploy */
    @Test
    public void testNoBuildWithRecentPreviousDeploySchedule() throws Exception {
        AutoPromoter promoter = new AutoPromoter(context);
        DeployBean previousDeploy = new DeployBean();
        previousDeploy.setStart_date(t10AM.minusMinutes(1).getMillis());
        previousDeploy.setBuild_id(t8AMBuildBean.getBuild_id());

        DateTimeUtils.setCurrentMillisProvider(timeProvider);
        timeProvider.setClock(t10AM.getMillis());

        when(buildDAO.getById(t8AMBuildBean.getBuild_id())).thenReturn(t8AMBuildBean);
        PromoteResult result =
                promoter.computePromoteBuildResult(
                        environBean, previousDeploy, 1, t10AMPromoteBean);
        assertEquals(PromoteResult.ResultCode.NoAvailableBuild, result.getResult());
    }

    /* AutoPromote scheduled for 10:00 AM daily
    - 1 new build
    - 1 previous deploys */
    @Test
    public void testOneBuildWithRecentPreviousDeploySchedule() throws Exception {
        AutoPromoter promoter = new AutoPromoter(context);
        // Has builds & previous deploy
        DeployBean previousDeploy = new DeployBean();
        previousDeploy.setStart_date(t9AM.plusMinutes(1).getMillis());
        previousDeploy.setBuild_id(t8AMBuildBean.getBuild_id());

        DateTimeUtils.setCurrentMillisProvider(timeProvider);
        timeProvider.setClock(t10AM.getMillis());

        when(buildDAO.getById(t8AMBuildBean.getBuild_id())).thenReturn(t8AMBuildBean);
        when(buildDAO.getAcceptedBuilds(any(), any(), any(), anyInt()))
                .thenReturn(Arrays.asList(t9AMBuildBean));
        PromoteResult result =
                promoter.computePromoteBuildResult(
                        environBean, previousDeploy, 1, t10AMPromoteBean);
        assertEquals(PromoteResult.ResultCode.PromoteBuild, result.getResult());
        assertEquals(t9AMBuildBean.getBuild_id(), result.getPromotedBuild());
    }

    @Test
    public void testGetScheduledCheckResult() throws Exception {
        PromoteBean promoteBean = new PromoteBean();
        promoteBean.setDelay(1);
        AutoPromoter promoter = new AutoPromoter(context);
        DateTimeUtils.setCurrentMillisProvider(timeProvider);

        BuildBean build1 = new BuildBean();
        build1.setPublish_date(t9AM.plusMinutes(1).getMillis());
        BuildBean build2 = new BuildBean();
        build2.setPublish_date(t9AM.plusMinutes(2).getMillis());
        BuildBean build3 = new BuildBean();
        build3.setPublish_date(t10AM.getMillis());
        List<BuildBean> candidates = Arrays.asList(build3, build2, build1);

        Function<BuildBean, Long> getPublishDate = b -> b.getPublish_date();
        promoteBean.setSchedule("* * * * * ?");

        timeProvider.setClock(t9AM.getMillis());
        BuildBean buildToPromote =
                promoter.getScheduledCheckResult(
                        environBean, promoteBean, candidates, getPublishDate);
        assertNull(buildToPromote);

        // build3 is not selected due to delay is not fulfilled
        timeProvider.setClock(t10AM.getMillis());
        buildToPromote =
                promoter.getScheduledCheckResult(
                        environBean, promoteBean, candidates, getPublishDate);
        assertEquals(build2, buildToPromote);

        // build3 is selected after delay
        timeProvider.setClock(t10AM.plusMinutes(1).getMillis());
        buildToPromote =
                promoter.getScheduledCheckResult(
                        environBean, promoteBean, candidates, getPublishDate);
        assertEquals(build3, buildToPromote);

        // build3 is not selected due to schedule is set at 10AM
        promoteBean.setSchedule(CronTenAMPerDay);
        buildToPromote =
                promoter.getScheduledCheckResult(
                        environBean, promoteBean, candidates, getPublishDate);
        assertEquals(build2, buildToPromote);
    }

    @Test
    public void testPromotionOnlyHappensWithinBufferTimeWindow() throws Exception {
        int bufferTimeMinutes = 1;
        AutoPromoter promoter = new AutoPromoter(context).withBufferTimeMinutes(bufferTimeMinutes);
        AutoPromoter promoterSpy = Mockito.spy(promoter);

        when(buildDAO.getAcceptedBuilds(any(), any(), any(), anyInt()))
                .thenReturn(Arrays.asList(t9AMBuildBean));

        DateTimeUtils.setCurrentMillisProvider(timeProvider);

        // Set time to 9:01 AM, before scheduled time
        timeProvider.setClock(t9AM.plusMinutes(1).getMillis());
        PromoteResult result =
                promoter.computePromoteBuildResult(environBean, null, 1, t10AMPromoteBean);
        assertEquals(PromoteResult.ResultCode.NotInScheduledTime, result.getResult());

        // Set time to 10AM - 1ms, just before the scheduled time
        timeProvider.setClock(t10AM.getMillis() - 1);
        result = promoter.computePromoteBuildResult(environBean, null, 1, t10AMPromoteBean);
        assertEquals(PromoteResult.ResultCode.NotInScheduledTime, result.getResult());

        // Set time to 10AM, at the scheduled time
        timeProvider.setClock(t10AM.getMillis());
        result = promoter.computePromoteBuildResult(environBean, null, 1, t10AMPromoteBean);
        assertEquals(PromoteResult.ResultCode.PromoteBuild, result.getResult());
        assertEquals(t9AMBuildBean.getBuild_id(), result.getPromotedBuild());

        // Set time to the end of buffer time window
        timeProvider.setClock(t10AM.plusMinutes(bufferTimeMinutes).getMillis() - 1);
        result = promoter.computePromoteBuildResult(environBean, null, 1, t10AMPromoteBean);
        assertEquals(PromoteResult.ResultCode.PromoteBuild, result.getResult());
        assertEquals(t9AMBuildBean.getBuild_id(), result.getPromotedBuild());

        // Set time to just after the buffer time window
        timeProvider.setClock(t10AM.plusMinutes(bufferTimeMinutes).getMillis());
        result = promoter.computePromoteBuildResult(environBean, null, 1, t10AMPromoteBean);
        assertEquals(PromoteResult.ResultCode.NotInScheduledTime, result.getResult());

        promoter.promoteBuild(environBean, null, 1, t10AMPromoteBean);
        verify(promoterSpy, never()).safePromote(any(), any(), any(), any(), any());

        // Set time to tomorrow 10AM, next buffer time window
        timeProvider.setClock(t9AM.plusDays(1).plusHours(1).getMillis());
        result = promoter.computePromoteBuildResult(environBean, null, 1, t10AMPromoteBean);
        assertEquals(PromoteResult.ResultCode.PromoteBuild, result.getResult());
        assertEquals(t9AMBuildBean.getBuild_id(), result.getPromotedBuild());
    }
}
