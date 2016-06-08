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
import com.pinterest.deployservice.common.Constants;
import com.pinterest.deployservice.common.StateMachines;
import com.pinterest.deployservice.dao.*;
import com.pinterest.deployservice.handler.DeployHandler;
import org.apache.commons.lang.StringUtils;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Monitor and promote deploy from one stage to another automatically
 */
public class AutoPromoter implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(AutoPromoter.class);
    public static final String AUTO_PROMOTER_NAME = "AutoPromoter";

    private EnvironDAO environDAO;
    private PromoteDAO promoteDAO;
    private DeployDAO deployDAO;
    private BuildDAO buildDAO;
    private UtilDAO utilDAO;
    private DeployHandler deployHandler;

    public AutoPromoter(ServiceContext serviceContext) {
        environDAO = serviceContext.getEnvironDAO();
        promoteDAO = serviceContext.getPromoteDAO();
        utilDAO = serviceContext.getUtilDAO();
        deployDAO = serviceContext.getDeployDAO();
        buildDAO = serviceContext.getBuildDAO();
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
        if (currDeployBean.getState() == DeployState.FAILING) {
            return true;
        }

        return false;
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
            LOG.debug("Env {} with deploy {} is in final accepted state, allow retire of this deploy!",
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

    void promoteBuild(EnvironBean currEnvBean, DeployBean currDeployBean, int size) throws Exception {
        // Figure out the current deploy build publish date, we only consider build after this date
        long currentBuildDate = 0;
        if (currDeployBean != null) {
            BuildBean buildBean = buildDAO.getById(currDeployBean.getBuild_id());
            currentBuildDate = buildBean.getPublish_date();
        }
        BuildBean buildBean = getBuildCandidate(currEnvBean, currentBuildDate, size);
        if (buildBean != null) {
            safePromote(null, buildBean.getBuild_id(), Constants.BUILD_STAGE, currDeployBean, currEnvBean);
        }
    }

    void promoteDeploy(EnvironBean currEnvBean, DeployBean currDeployBean, int size, PromoteBean predPromoteBean) throws Exception {
        String predStage = predPromoteBean.getPred_stage();
        // Special case when there is no pred deploy
        EnvironBean predEnvBean = environDAO.getByStage(currEnvBean.getEnv_name(), predStage);
        if (predEnvBean == null) {
            LOG.warn("Pred env {}/{} does not exist, bail out!", currEnvBean.getEnv_name(), predStage);
            return;
        }

        String predDeployId = predEnvBean.getDeploy_id();
        if (predDeployId == null) {
            LOG.debug("Pred env {}/{} does not have deploy yet, bail out!", currEnvBean.getEnv_name(), predStage);
            return;
        }

        long currentDeployDate = 0;
        if (currDeployBean != null) {
            String fromDeployId = currDeployBean.getFrom_deploy();
            if (fromDeployId != null) {
                DeployBean fromDeployBean = deployDAO.getById(fromDeployId);
                if (fromDeployBean.getEnv_id().equals(predEnvBean.getEnv_id())) {
                    currentDeployDate = fromDeployBean.getStart_date();
                } else {
                    LOG.info("Current deploy {} in env {} was not promoted from pred env {}, but from env {}! Use the current deploy startDate",
                        currDeployBean.getDeploy_id(), currEnvBean.getEnv_id(), predEnvBean.getEnv_id(), fromDeployBean.getEnv_id());
                    currentDeployDate = currDeployBean.getStart_date();
                }
            } else {
                LOG.info("Current deploy {} in env {} was not promoted from anywhere! Use the current deploy startDate",
                    currDeployBean.getDeploy_id(), currEnvBean.getEnv_id());
                currentDeployDate = currDeployBean.getStart_date();
            }
        }

        DeployBean predDeployBean;
        if (predPromoteBean.getDelay() != 0) {
            long before = (System.currentTimeMillis() - predPromoteBean.getDelay() * 60 * 1000);
            predDeployBean = getDeployCandidateDelayed(predEnvBean.getEnv_id(), before, currentDeployDate);
            if (predDeployBean == null) {
                LOG.debug("Could not find any deploy candidate within delay period for {}/{}", predEnvBean.getEnv_name(), predEnvBean.getStage_name());
                return;
            }
            Long count = deployDAO.countNonRegularDeploys(predEnvBean.getEnv_id(), predDeployBean.getStart_date());
            if (count != 0) {
                LOG.debug("not deploying due to nonregular deploy in delay period for {}/{}", predEnvBean.getEnv_name(), predEnvBean.getStage_name());
                return;
            }
        } else {
            predDeployBean = getDeployCandidate(predEnvBean.getEnv_id(), currentDeployDate, size);
        }
        if (predDeployBean != null) {
            safePromote(predDeployBean, null, predEnvBean.getStage_name(), currDeployBean, currEnvBean);
        }
    }

    void handleFailedPromote(PromoteBean promoteBean, EnvironBean currEnvBean, String deployId) throws Exception {
        PromoteFailPolicy autoPromotePolicy = promoteBean.getFail_policy();
        if (autoPromotePolicy == PromoteFailPolicy.DISABLE) {
            LOG.info("Disable auto deploy for env {}/{} since its current deploy {} failed, and " +
                " and its current auto deploy fail policy is DISABLE!",
                currEnvBean.getEnv_name(), currEnvBean.getStage_name(), deployId);
            deployHandler.disableAutoPromote(currEnvBean, AUTO_PROMOTER_NAME, true);
            return;
        }
        if (autoPromotePolicy == PromoteFailPolicy.ROLLBACK) {
            LOG.info("Rollback deploy {} and disable auto deploy for env {}/{} since deploy failed and env fail policy is ROLLBACK!",
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
        // check if there is a currDeployBean -> ask if they would like to continue
        

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

        // If the service owner specify the cron expression, check if it's due for auto promote.
        String schedule = promoteBean.getSchedule();
        if (!StringUtils.isEmpty(schedule)) {
            if (!autoDeployDue(currDeployBean, schedule)) {
                return;
            }
            // For cron auto promote, we only choose the latest one.
            size = Math.min(size, 1);
        }

        // Promote build if preStage is BUILD
        if (promoteBean.getPred_stage().equals(Constants.BUILD_STAGE)) {
            promoteBuild(currEnvBean, currDeployBean, size);
            return;
        }

        // Otherwise, promote from pred env deploys
        promoteDeploy(currEnvBean, currDeployBean, size, promoteBean);
    }

    boolean autoDeployDue(DeployBean deployBean, String cronExpressionString) {
        Date date = new Date();
        try {
            if (!CronExpression.isValidExpression(cronExpressionString)) {
                LOG.error(String.format("Cron expression %s is not valid. Ignore it.", cronExpressionString));
                return true;
            }
            CronExpression cronExpression = new CronExpression(cronExpressionString);
            if (deployBean == null) {
                return true;
            }
            Date lastDeloyDate = new Date(deployBean.getStart_date());
            Date nextDeployDate = cronExpression.getNextValidTimeAfter(lastDeloyDate);
            // Only run the cron deploy when the current date is equal or after the scheduled deploy date
            // since last deploy.
            //
            // If current date is before the next scheduled deploy date since last deploy, return false.
            if (date.before(nextDeployDate)) {
                LOG.info(String.format("The next scheduled deploy after last deploy %tc is %tc, now is: %tc",
                    nextDeployDate, lastDeloyDate, date));
                return false;
            } else {
                return true;
            }
        } catch (ParseException e) {
            LOG.error(String.format("Failed to parse cron expression: %s. Reason: %s",
                cronExpressionString, e.getMessage()));
            return true;
        } catch (Exception e) {
            LOG.error(String.format("Failed to validate date. Reason: %s", e.getMessage()));
            return true;
        }
    }

    BuildBean getBuildCandidate(EnvironBean envBean, long after, int size) throws Exception {
        // By default, buildName is the same as envName
        String buildName = envBean.getBuild_name();
        String scmBranch = envBean.getBranch();

        List<BuildBean> buildBeans = buildDAO.getAcceptedBuilds(buildName, scmBranch, after, size);
        if (buildBeans.size() < 1) {
            LOG.debug("Looks like all builds been deployed in env {}", envBean.getEnv_id());
            return null;
        }
        return buildBeans.get(buildBeans.size() - 1);
    }

    DeployBean getDeployCandidate(String envId, long after, int size) throws Exception {
        List<DeployBean> deployBeans = deployDAO.getAcceptedDeploys(envId, after, size);
        if (deployBeans.size() < 1) {
            LOG.info("There is no accepted deploy in env {}", envId);
            return null;
        }
        return deployBeans.get(deployBeans.size() - 1);
    }

    DeployBean getDeployCandidateDelayed(String envId, long before, long after) throws Exception {
        List<DeployBean> deployBeans = deployDAO.getAcceptedDeploysDelayed(envId, before, after);
        if (deployBeans.size() < 1) {
            LOG.info("There is no accepted deploy in env {}", envId);
            return null;
        }
        return deployBeans.get(0);
    }

    // Lock, double check and promote
    void safePromote(DeployBean predDeployBean, String buildId, String predStageName, DeployBean currDeployBean, EnvironBean currEnvBean) throws Exception {
        String promoteLockName = String.format("PROMOTE-%s", currEnvBean.getEnv_id());
        Connection connection = utilDAO.getLock(promoteLockName);
        if (connection != null) {
            LOG.info("Successfully get lock on {}", promoteLockName);
            try {
                // Read the env again, make sure the current deploy is still the same deploy we think it is
                currEnvBean = environDAO.getById(currEnvBean.getEnv_id());
                if ((currDeployBean == null && currEnvBean.getDeploy_id() != null) ||
                    (currDeployBean != null && !currEnvBean.getDeploy_id().equals(currDeployBean.getDeploy_id()))) {
                    LOG.info("Env {} has a new deploy already, previously was {}, now is {}, no need to promote, bail out!",
                        new Object[] {currEnvBean.getEnv_id(), currDeployBean == null ? "NULL" : currDeployBean.getDeploy_id(), currEnvBean.getDeploy_id()});
                    return;
                }

                // otherwise, safe to promote
                if (predDeployBean != null) {
                    String description = "Auto promote deploy " + predDeployBean.getDeploy_id();
                    String newDeployId = deployHandler.promote(currEnvBean, predDeployBean.getDeploy_id(), description, AUTO_PROMOTER_NAME);
                    LOG.info("Auto promoted deploy {} from deploy {}, from stage {} to {} for env {}",
                        new Object[] {newDeployId, predDeployBean.getDeploy_id(), predStageName, currEnvBean.getStage_name(), currEnvBean.getEnv_name()});
                } else {
                    String desc = "Auto promote build " + buildId;
                    String newDeployId = deployHandler.deploy(currEnvBean, buildId, desc, AUTO_PROMOTER_NAME);
                    LOG.info("Auto promoted deploy {} from build {}, from stage {} to {} for env {}",
                        new Object[] {newDeployId, buildId, predStageName, currEnvBean.getStage_name(), currEnvBean.getEnv_name()});
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
