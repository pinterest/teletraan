/**
 * Copyright 2016 Pinterest, Inc.
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

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.*;
import com.pinterest.deployservice.buildtags.BuildTagsManager;
import com.pinterest.deployservice.buildtags.BuildTagsManagerImpl;
import com.pinterest.deployservice.common.Constants;
import com.pinterest.deployservice.common.StateMachines;
import com.pinterest.deployservice.dao.BuildDAO;
import com.pinterest.deployservice.dao.DeployDAO;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.PromoteDAO;
import com.pinterest.deployservice.dao.UtilDAO;
import com.pinterest.deployservice.handler.DeployHandler;

import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.format.ISODateTimeFormat;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.text.ParseException;
import java.util.*;
import java.util.function.Function;


/**
 * Monitor and promote deploy from one stage to another automatically
 */
public class AutoPromoter implements Runnable {

    public static final String AUTO_PROMOTER_NAME = "AutoPromoter";
    private static final Logger LOG = LoggerFactory.getLogger(AutoPromoter.class);
    public BuildDAO buildDAO;
    private EnvironDAO environDAO;
    private PromoteDAO promoteDAO;
    private DeployDAO deployDAO;
    private UtilDAO utilDAO;
    private DeployHandler deployHandler;
    private BuildTagsManager buildTagsManager;
    private final int maxCheckBuildsOrDeploys = 100;


    public AutoPromoter(ServiceContext serviceContext) {
        environDAO = serviceContext.getEnvironDAO();
        promoteDAO = serviceContext.getPromoteDAO();
        utilDAO = serviceContext.getUtilDAO();
        deployDAO = serviceContext.getDeployDAO();
        buildDAO = serviceContext.getBuildDAO();
        buildTagsManager = new BuildTagsManagerImpl(serviceContext.getTagDAO());
        deployHandler = new DeployHandler(serviceContext);
    }

    void processBatch() throws Exception {
        // Get all auto_promote enabled & normal state envs, randomly pick one to work on
        List<String> envIds = promoteDAO.getAutoPromoteEnvIds();
        if (envIds.isEmpty()) {
            LOG.debug("AutoPromoter did not find any valid env to work on, exiting.");
            return;
        }
        Collections.shuffle(envIds);
        for (String envId : envIds) {
            try {
                LOG.debug("AutoPromoter chooses env {} to work on.", envId);
                processOnce(envId);
            } catch (Throwable t) {
                // Catch all throwable so that subsequent job not suppressed
                LOG.error("AutoPromoter failed to process {}", envId, t);
            }
        }
      LOG.info("AutoPromoter processBatch finishes");
    }

    boolean isDeployFailed(DeployBean currDeployBean) {
        if (currDeployBean == null) {
            return false;
        }

        // If acceptance test failed, then deploy is definitely failed
        if (currDeployBean.getAcc_status() == AcceptanceStatus.REJECTED) {
            return true;
        }

        // Otherwise, it is up to the deploy state
        return currDeployBean.getState() == DeployState.FAILING;

    }

    long getEndTime(PromoteBean bean){
        long ret = DateTime.now().getMillis();
        if (bean.getDelay()!=null && bean.getDelay()>0){
            ret -=bean.getDelay()*60*1000;
        }
        return ret;
    }

    public <E>  E  getScheduledCheckResult(EnvironBean currEnvBean,
                                                PromoteBean promoteBean,
                                                List<E> candidates,
                                                Function<E, Long> timeSupplier) throws Exception {

        E ret = null;

        //If we have a cron schedule set, foreach candidates, we compute the earilest due
        // time per schedule for the build.
        CronExpression cronExpression = new CronExpression(promoteBean.getSchedule());
        for(E bean:candidates) {
            DateTime checkTime = new DateTime(timeSupplier.apply(bean));
            if (promoteBean.getDelay() > 0) {
                checkTime.plusMinutes(promoteBean.getDelay());
            }
            DateTime
                autoDeployDueDate =
                new DateTime(cronExpression.getNextValidTimeAfter(checkTime.toDate()));
            LOG.info("Auto deploy due time is {} for check time {} for Environment {}",
                autoDeployDueDate.toString(ISODateTimeFormat.dateTime()),
                checkTime.toString(), currEnvBean.getEnv_name());

            if (!autoDeployDueDate.isAfterNow()) {
                ret = bean;
                break;
            }
        }
        return ret;
    }

    public long getCurrentDeployStartDate(DeployBean currDeployBean, EnvironBean precededEnvBean,
                                          EnvironBean currEnvBean) throws Exception {
        long currentDeployDate = 0;
        if (currDeployBean != null) {
            String fromDeployId = currDeployBean.getFrom_deploy();
            if (fromDeployId != null) {
                DeployBean fromDeployBean = deployDAO.getById(fromDeployId);
                if (fromDeployBean.getEnv_id().equals(precededEnvBean.getEnv_id())) {
                    currentDeployDate = fromDeployBean.getStart_date();
                } else {
                    LOG.info(
                        "Current deploy {} in env {} was not promoted from pred env {}, but from "
                            + "env {}! "
                            + "Use the current deploy startDate",
                        currDeployBean.getDeploy_id(), currEnvBean.getEnv_id(),
                        precededEnvBean.getEnv_id(),
                        fromDeployBean.getEnv_id());
                    currentDeployDate = currDeployBean.getStart_date();
                }
            } else {
                LOG.info(
                    "Current deploy {} in env {} was not promoted from anywhere! Use the current "
                        + "deploy "
                        + "startDate",
                    currDeployBean.getDeploy_id(), currEnvBean.getEnv_id());
                currentDeployDate = currDeployBean.getStart_date();
            }
        }
        return currentDeployDate;
    }

    boolean isCurrentDeployRetirable(String envId, DeployBean currDeployBean) throws Exception {
        if (currDeployBean == null) {
            return true;
        }

        DeployState currDeployState = currDeployBean.getState();
        String deployId = currDeployBean.getDeploy_id();
        if (StateMachines.DEPLOY_FINAL_STATES.contains(currDeployState)) {
            // This is not very likely to happen
            LOG.info("Env {} with deploy {} is already in final state. It might happen " +
                    "other workers already handled this env. Do not retire this deploy!",
                envId, deployId);
            return false;
        }

        if (StateMachines.FINAL_ACCEPTANCE_STATUSES.contains(currDeployBean.getAcc_status())) {
            LOG.debug(
                "Env {} with deploy {} is in final accepted state, allow retire of this deploy!",
                envId, deployId);
            return true;
        }

        if (currDeployState == DeployState.FAILING) {
            LOG.debug("Env {} with deploy {} is {}, allow retire of this deploy!",
                envId, deployId, currDeployState);
            return true;
        }

        // Otherwise, the deploy can not be retired or replaced
        // This is mostly when deploy is running, or succeeding but wait for test results
        // TODO why we can not override current deploy when it is running or waiting for tests
        return false;
    }

    //This contains the logic about if there is build should be promoted
    public PromoteResult computePromoteBuildResult(EnvironBean currEnvBean,
                                                   DeployBean currDeployBean,
                                                   int size,
                                                   PromoteBean promoteBean) throws Exception {

        Preconditions.checkArgument(size>0);

        // Figure out the current deploy build publish date, we only consider build after this date
        long startTime = 0;
        if (currDeployBean != null) {
            BuildBean buildBean = buildDAO.getById(currDeployBean.getBuild_id());
            startTime = buildBean.getPublish_date();
        }

        long endTime = getEndTime(promoteBean);

        if (endTime < startTime) {
            return new PromoteResult().withResultCode(PromoteResult.ResultCode.NoAvailableBuild);
        }


        //Get builds available between the deployed build and end time ordered by publish_date
        //It is either Long.MAX_VALUE (Get all builds) or the due time for scheduled deployment
        List<BuildBean> buildBeans =
            getBuildCandidates(currEnvBean, new Interval(startTime, endTime),
                maxCheckBuildsOrDeploys);
        if (buildBeans.size() < size) {
            return new PromoteResult()
                .withResultCode(PromoteResult.ResultCode.NoAvailableBuild);
        }

        String schedule = promoteBean.getSchedule();
        if (!StringUtils.isEmpty(schedule)) {
            Function<BuildBean, Long> getPublishDate = b-> b.getPublish_date();
            BuildBean toPromoteBuild = getScheduledCheckResult(currEnvBean,
                promoteBean, buildBeans, getPublishDate);

            if (toPromoteBuild != null) {
                return new PromoteResult().withResultCode(PromoteResult.ResultCode.PromoteBuild)
                    .withBuild(toPromoteBuild.getBuild_id());

            } else {
                return new PromoteResult()
                    .withResultCode(PromoteResult.ResultCode.NoAvailableBuild);
            }

        } else {
                //No delay. Just promote it
                return new PromoteResult().withBuild(buildBeans.get(size - 1).getBuild_id())
                    .withResultCode(PromoteResult.ResultCode.PromoteBuild);
        }

    }

    public void promoteBuild(EnvironBean currEnvBean, DeployBean currDeployBean, int size,
                             PromoteBean promoteBean) throws Exception {

        PromoteResult result =
            computePromoteBuildResult(currEnvBean, currDeployBean, size, promoteBean);
        LOG.info("Promote result {} for env {}", result.getResult().toString(),
            currEnvBean.getEnv_name());
        if (result.getResult() == PromoteResult.ResultCode.PromoteBuild &&
            StringUtils.isNotEmpty(result.getPromotedBuild())) {
            safePromote(null, result.getPromotedBuild(), Constants.BUILD_STAGE, currDeployBean,
                currEnvBean);
        }
    }

    //This contains the logic about if there should be a promote deploy from the preceded
    // environment.
    public PromoteResult computePromoteDeployResult(EnvironBean currEnvBean,
                                                    DeployBean currDeployBean, int size,
                                                    PromoteBean promoteBean) throws Exception {
        String precededStage = promoteBean.getPred_stage();
        // Special case when there is no preceded environment
        EnvironBean precededEnvBean =
            environDAO.getByStage(currEnvBean.getEnv_name(), precededStage);
        if (precededEnvBean == null) {
            LOG.warn("Pred env {}/{} does not exist, bail out!", currEnvBean.getEnv_name(),
                precededStage);
            return new PromoteResult().withResultCode(PromoteResult.ResultCode.NoPredEnvironment);
        }

        String predDeployId = precededEnvBean.getDeploy_id();
        if (predDeployId == null) {
            LOG.debug("Pred env {}/{} does not have deploy yet, bail out!",
                currEnvBean.getEnv_name(), precededStage);
            return new PromoteResult()
                .withResultCode(PromoteResult.ResultCode.NoPredEnvironmentDeploy);
        }

        //Get the start time to find a deploy in preceded environment. If current deploy is promoted
        //from preceded environment, use the last promoted deploy startDate in preceded
        // environment.
        //Otherwise (current deploy is not promoted from preceded environment), use the current
        // deploy
        // startDate
        long startTime =
            getCurrentDeployStartDate(currDeployBean, precededEnvBean, currEnvBean);

        long endTime = getEndTime(promoteBean);
        if (endTime < startTime) {
            return new PromoteResult()
                .withResultCode(PromoteResult.ResultCode.NoCandidateWithinDelayPeriod);
        }

        //Get all deploys in preceded environment order by start dese
        List<DeployBean> deployCandidates = getDeployCandidates(precededEnvBean.getEnv_id(),
            new Interval(startTime, endTime), maxCheckBuildsOrDeploys);

        if (deployCandidates.size() < size) {
            return new PromoteResult()
                .withResultCode(PromoteResult.ResultCode.NoCandidateWithinDelayPeriod);
        }
        String schedule = promoteBean.getSchedule();
        if (!StringUtils.isEmpty(schedule)) {
            Function<DeployBean, Long> getSucceedDate = b-> b.getStart_date();
            DeployBean toPromoteDeploy = getScheduledCheckResult(currEnvBean,
                promoteBean, deployCandidates, getSucceedDate);

            if (toPromoteDeploy != null) {
                return new PromoteResult().withResultCode(PromoteResult.ResultCode.PromoteDeploy)
                    .withPredDeployBean(toPromoteDeploy, precededEnvBean);

            } else {
                return new PromoteResult()
                    .withResultCode(PromoteResult.ResultCode.NoRegularDeployWithinDelayPeriod);
            }

        }else {
            return new PromoteResult().withResultCode(PromoteResult.ResultCode.PromoteDeploy)
                .withPredDeployBean(deployCandidates.get(size-1), precededEnvBean);
        }

    }


    DeployBean promoteDeploy(EnvironBean currEnvBean, DeployBean currDeployBean, int size,
                             PromoteBean promoteBean) throws Exception {
        PromoteResult result =
            computePromoteDeployResult(currEnvBean, currDeployBean, size, promoteBean);
        LOG.info("Promote result {} for env {}", result.getResult().toString(),
            currEnvBean.getEnv_name());
        if (result.getResult() == PromoteResult.ResultCode.PromoteDeploy
            && result.getPredDeployInfo() != null) {
            safePromote(result.getPredDeployInfo().getLeft(), null,
                result.getPredDeployInfo().getRight().getStage_name(),
                currDeployBean, currEnvBean);
            return result.predDeployInfo.getLeft();
        }
        return null;
    }

    void handleFailedPromote(PromoteBean promoteBean, EnvironBean currEnvBean, String deployId)
        throws Exception {
        PromoteFailPolicy autoPromotePolicy = promoteBean.getFail_policy();
        if (autoPromotePolicy == PromoteFailPolicy.DISABLE) {
            LOG.info("Disable auto deploy for env {}/{} since its current deploy {} failed, and " +
                    " and its current auto deploy fail policy is DISABLE!",
                currEnvBean.getEnv_name(), currEnvBean.getStage_name(), deployId);
            deployHandler.disableAutoPromote(currEnvBean, AUTO_PROMOTER_NAME, true);
            return;
        }
        if (autoPromotePolicy == PromoteFailPolicy.ROLLBACK) {
            LOG.info(
                "Rollback deploy {} and disable auto deploy for env {}/{} since deploy failed and"
                    + " env "
                    + "fail policy is ROLLBACK!",
                currEnvBean.getEnv_name(), currEnvBean.getStage_name(), deployId);
            deployHandler.rollback(currEnvBean, null, null, AUTO_PROMOTER_NAME);
            deployHandler.disableAutoPromote(currEnvBean, AUTO_PROMOTER_NAME, true);
        }
        LOG.error("Unexpected policy {} for Env {}", autoPromotePolicy, currEnvBean.getEnv_id());
    }

    void processOnce(String envId) throws Exception {
        EnvironBean currEnvBean = environDAO.getById(envId);
        if (currEnvBean == null || currEnvBean.getEnv_state() != EnvState.NORMAL ||
            currEnvBean.getState() != EnvironState.NORMAL) {
            LOG.info("Env {} has just been disabled or paused or deleted, bail out!", envId);
            return;
        }

        PromoteBean promoteBean = promoteDAO.getById(envId);
        if (promoteBean == null || promoteBean.getType() == PromoteType.MANUAL) {
            LOG.info("Env {} auto promote has just been disabled, bail out!", envId);
            return;
        }

        if (StringUtils.isEmpty(promoteBean.getPred_stage())) {
            LOG.info("Env {} pred stage has just been deleted, bail out!", envId);
            return;
        }

        DeployBean currDeployBean = null;
        String deployId = currEnvBean.getDeploy_id();
        if (deployId != null) {
            currDeployBean = deployDAO.getById(deployId);
        }

        if (!isCurrentDeployRetirable(envId, currDeployBean)) {
            LOG.debug("Env {} current deploy is not ready to be retired, bail out!", envId);
            return;
        }

        // Special case when deploy failed, apply promote fail policy here
        if (isDeployFailed(currDeployBean)) {
            if (promoteBean.getFail_policy() != PromoteFailPolicy.CONTINUE) {
                handleFailedPromote(promoteBean, currEnvBean, deployId);
                return;
            } else {
                LOG.info("Env {} current deploy {} failed but since promote fail policy is " +
                    "CONTINUE, let us continue to promote!", envId, deployId);
            }
        }

        // How many candidates should we check
        int size = promoteBean.getQueue_size();

        // Promote build if preStage is BUILD
        if (promoteBean.getPred_stage().equals(Constants.BUILD_STAGE)) {
            promoteBuild(currEnvBean, currDeployBean, size, promoteBean);
        } else {
            // Otherwise, promote from pred env deploys
            promoteDeploy(currEnvBean, currDeployBean, size, promoteBean);
        }
    }

    boolean autoDeployDue(DeployBean deployBean, String cronExpressionString) {
        Date date = new Date();
        return !date
            .before(getScheduledCheckDueTime(deployBean.getStart_date(), cronExpressionString));
    }

    public Date getScheduledCheckDueTime(long start_date, String cronExpressionString) {
        Date ret = new Date(0);
        try {
            if (!CronExpression.isValidExpression(cronExpressionString)) {
                LOG.error(
                    String.format("Cron expression %s is not valid. Ignore it.",
                        cronExpressionString));
                return ret;
            }
            CronExpression cronExpression = new CronExpression(cronExpressionString);
            Date lastCheckDate = new Date(start_date);
            ret = cronExpression.getNextValidTimeAfter(lastCheckDate);
            LOG.info("Get cron {} due time is {} for check time {}",
                cronExpressionString,
                new DateTime(ret).toString(ISODateTimeFormat.dateTime()),
                new DateTime(lastCheckDate).toString(ISODateTimeFormat.dateTime()));

            return ret;
        } catch (ParseException e) {
            LOG.error(String.format("Failed to parse cron expression: %s. Reason: %s",
                cronExpressionString, e.getMessage()));
            return ret;
        } catch (Exception e) {
            LOG.error(String.format("Failed to validate date. Reason: %s", e.getMessage()));
            return ret;
        }
    }

    /**
     * get a list of available builds, and filter out the BAD_BUILD builds
     * @param envBean
     * @param interval
     * @param size
     * @return
     * @throws Exception
     */
    List<BuildBean> getBuildCandidates(EnvironBean envBean, Interval interval, int size) throws Exception {
        // By default, buildName is the same as envName
        String buildName = envBean.getBuild_name();
        String scmBranch = envBean.getBranch();
        List<BuildBean> taggedGoodBuilds = new ArrayList<BuildBean>();

        List<BuildBean> availableBuilds = buildDAO.getAcceptedBuilds(buildName, scmBranch, interval, size);
        LOG.info("Env {} stage {} has {} accepted builds with name {} branch {} between {} and {}", envBean.getEnv_name(),
            envBean.getStage_name(), availableBuilds.size(), buildName, scmBranch, interval.getStart().toString(), interval.getEnd().toString());
        if(!availableBuilds.isEmpty()) {
            List<BuildTagBean> buildTagBeanList = buildTagsManager.getEffectiveTagsWithBuilds(availableBuilds);
            for(BuildTagBean buildTagBean: buildTagBeanList) {
                if(buildTagBean.getTag() != null && buildTagBean.getTag().getValue() == TagValue.BAD_BUILD) {
                    // bad build,  do not include
                    LOG.info("Env {} Build {} is tagged as BAD_BUILD, ignore", envBean.getEnv_id(), buildTagBean.getBuild());
                } else {
                    taggedGoodBuilds.add(buildTagBean.getBuild());
                }
            }
        }
        // should order build bean ORDER BY publish_date DESC
        if(taggedGoodBuilds.size() > 0) {
            Collections.sort(taggedGoodBuilds, new Comparator<BuildBean>() {
                @Override
                public int compare(final BuildBean d1, final BuildBean d2) {
                    return Long.compare(d2.getPublish_date(), d1.getPublish_date());
                }
            });
            LOG.info("Env {} the first build candidate is {}", envBean.getEnv_id(), taggedGoodBuilds.get(0).getBuild_id());
        }
        return taggedGoodBuilds;
    }

    /**
     * get a list of available deploys, and filter out the deploys with BAD_BUILD builds
     * @param envId
     * @param interval
     * @param size
     * @return
     * @throws Exception
     */
    List<DeployBean> getDeployCandidates(String envId, Interval interval, int size) throws Exception {
        return deployHandler.getDeployCandidates(envId, interval, size, true);
    }

    // Lock, double check and promote
    void safePromote(DeployBean predDeployBean, String buildId, String predStageName,
                     DeployBean currDeployBean, EnvironBean currEnvBean) throws Exception {
        String promoteLockName = String.format("PROMOTE-%s", currEnvBean.getEnv_id());
        Connection connection = utilDAO.getLock(promoteLockName);
        if (connection != null) {
            LOG.info("Successfully get lock on {}", promoteLockName);
            try {
                // Read the env again, make sure the current deploy is still the same deploy we
                // think it is
                currEnvBean = environDAO.getById(currEnvBean.getEnv_id());
                if ((currDeployBean == null && currEnvBean.getDeploy_id() != null) ||
                    (currDeployBean != null && !currEnvBean.getDeploy_id()
                        .equals(currDeployBean.getDeploy_id()))) {
                    LOG.info(
                        "Env {} has a new deploy already, previously was {}, now is {}, no need "
                            + "to promote,"
                            + " bail out!",
                        currEnvBean.getEnv_id(),
                        currDeployBean == null ? "NULL" : currDeployBean.getDeploy_id(),
                        currEnvBean.getDeploy_id());
                    return;
                }

                // otherwise, safe to promote
                if (predDeployBean != null) {
                    String description = "Auto promote deploy " + predDeployBean.getDeploy_id();
                    String
                        newDeployId =
                        deployHandler
                            .promote(currEnvBean, predDeployBean.getDeploy_id(), description,
                                AUTO_PROMOTER_NAME);
                    LOG.info(
                        "Auto promoted deploy {} from deploy {}, from stage {} to {} for env {}",
                        newDeployId, predDeployBean.getDeploy_id(), predStageName,
                        currEnvBean.getStage_name(),
                        currEnvBean.getEnv_name());
                } else {
                    String desc = "Auto promote build " + buildId;
                    String
                        newDeployId =
                        deployHandler.deploy(currEnvBean, buildId, desc, AUTO_PROMOTER_NAME);
                    LOG.info(
                        "Auto promoted deploy {} from build {}, from stage {} to {} for env {}",
                        newDeployId, buildId, predStageName, currEnvBean.getStage_name(),
                        currEnvBean.getEnv_name());
                }
            } catch (Exception e) {
                LOG.warn("Failed to promote for env {}.", currEnvBean.getEnv_id(), e);
            } finally {
                utilDAO.releaseLock(promoteLockName, connection);
                LOG.info("Successfully released lock on {}", promoteLockName);
            }
        } else {
            LOG.warn("Failed to grab PROMOTE_LOCK for env = {}.", currEnvBean.getEnv_id());
        }
    }

    @Override
    public void run() {
        try {
            LOG.info("Start AutoPromoter process...");
            processBatch();
        } catch (Throwable t) {
            // Catch all throwable so that subsequent job not suppressed
            LOG.error("Failed to call AutoPromoter.", t);
        }
    }
}
