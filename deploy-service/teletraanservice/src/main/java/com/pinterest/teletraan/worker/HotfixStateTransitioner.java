/**
 * Copyright (c) 2016-2024 Pinterest, Inc.
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
import com.pinterest.deployservice.ci.Buildkite;
import com.pinterest.deployservice.ci.CIPlatformManager;
import com.pinterest.deployservice.ci.CIPlatformManagerProxy;
import com.pinterest.deployservice.ci.Jenkins;
import com.pinterest.deployservice.common.Constants;
import com.pinterest.deployservice.common.DeployInternalException;
import com.pinterest.deployservice.dao.*;
import com.pinterest.deployservice.handler.CommonHandler;
import com.pinterest.teletraan.universal.metrics.ErrorBudgetCounterFactory;
import io.micrometer.core.instrument.Counter;
import java.sql.Connection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Check active deploys and push them into their final states */
public class HotfixStateTransitioner implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(HotfixStateTransitioner.class);

    private HotfixDAO hotfixDAO;
    private DeployDAO deployDAO;
    private BuildDAO buildDAO;
    private UtilDAO utilDAO;
    private EnvironDAO environDAO;
    private CommonHandler commonHandler;
    private Jenkins jenkins;
    private Buildkite buildkite;
    private CIPlatformManagerProxy ciPlatformManagerProxy;
    private Counter errorBudgetSuccess;
    private Counter errorBudgetFailure;
    // TODO make this configurable
    private static final int HOTFIX_JOB_DURATION_TIMEOUT = 180;

    public HotfixStateTransitioner(ServiceContext serviceContext) {
        hotfixDAO = serviceContext.getHotfixDAO();
        deployDAO = serviceContext.getDeployDAO();
        buildDAO = serviceContext.getBuildDAO();
        utilDAO = serviceContext.getUtilDAO();
        environDAO = serviceContext.getEnvironDAO();
        commonHandler = new CommonHandler(serviceContext);
        ciPlatformManagerProxy = serviceContext.getCIPlatformManagerProxy();
        try {
            jenkins = (Jenkins) ciPlatformManagerProxy.getCIPlatform("jenkins");
        } catch (Exception e) {
            LOG.error("Failed to initialize Jenkins CI platform", e);
            throw new RuntimeException("Failed to initialize Jenkins CI platform", e);
        }
        try {
            buildkite = (Buildkite) ciPlatformManagerProxy.getCIPlatform("buildkite");
        } catch (Exception e) {
            LOG.error("Failed to initialize Buildkite CI platform", e);
            throw new RuntimeException("Failed to initialize Buildkite CI platform", e);
        }

        errorBudgetSuccess =
                ErrorBudgetCounterFactory.createSuccessCounter(this.getClass().getSimpleName());
        errorBudgetFailure =
                ErrorBudgetCounterFactory.createFailureCounter(this.getClass().getSimpleName());
    }

    void processBatch() throws Exception {
        // Get all current deploys, randomly pick one to work on
        List<String> hotfixIds = hotfixDAO.getOngoingHotfixIds();
        if (hotfixIds.isEmpty()) {
            LOG.info("HotfixStateTransitioner did not find any active hotfix, exiting.");

            errorBudgetSuccess.increment();

            return;
        }
        Collections.shuffle(hotfixIds);
        for (String hotfixId : hotfixIds) {
            HotfixBean hotBean = hotfixDAO.getByHotfixId(hotfixId);
            try {
                LOG.info("HotfixStateTransitioner chooses hotfix {} to work on.", hotfixId);
                transitionHotfixState(hotBean);

                errorBudgetSuccess.increment();
            } catch (Throwable t) {
                // Catch all throwable so that subsequent job not suppressed, also long error in DB
                LOG.error("HotfixStateTransitioner failed to process {} " + hotfixId, t);
                hotBean.setError_message("Get Exception: " + t);
                hotfixDAO.update(hotfixId, hotBean);

                errorBudgetFailure.increment();
            }
        }
    }

    @Override
    public void run() {
        try {
            LOG.info("Start HotfixStateTransitioner process...");
            processBatch();
        } catch (Throwable t) {
            // Catch all throwable so that subsequent job not suppressed
            LOG.error("Failed to call HotfixStateTransitioner.", t);

            errorBudgetFailure.increment();
        }
    }

    public void transitionHotfixState(HotfixBean hotBean) throws Exception {
        String hotfixLockName = String.format("HOTFIX-%s", hotBean.getId());
        Connection connection = utilDAO.getLock(hotfixLockName);
        if (connection != null) {
            LOG.info("DB lock operation is successful: get lock {}", hotfixLockName);
            try {
                // Check for Hotfix for timeout
                if ((System.currentTimeMillis() - hotBean.getLast_worked_on())
                        > (HOTFIX_JOB_DURATION_TIMEOUT * 60000)) {
                    hotBean.setState(HotfixState.FAILED);
                    hotBean.setError_message("Hotfix timed out.");
                    hotfixDAO.update(hotBean.getId(), hotBean);
                    LOG.error("Hotfix " + hotBean.getId() + " has timed out!");
                    return;
                }

                int oldProgress = hotBean.getProgress();
                HotfixState state = hotBean.getState();
                String hotfixId = hotBean.getId();
                String jobNum = hotBean.getJob_num();
                DeployBean deployBean = deployDAO.getById(hotBean.getBase_deploy());
                BuildBean buildBean = buildDAO.getById(deployBean.getBuild_id());

                if (state == HotfixState.INITIAL) {
                    // Initial state, need to start job
                    String buildParams =
                            "BASE_COMMIT="
                                    + buildBean.getScm_commit()
                                    + "&COMMITS="
                                    + hotBean.getCommits()
                                    + "&SUFFIX="
                                    + hotBean.getOperator()
                                    + "_"
                                    + buildBean.getScm_commit_7()
                                    + "&HOTFIX_ID="
                                    + hotBean.getId()
                                    + "&REPO="
                                    + hotBean.getRepo();
                    // Start job and set start time
                    // jenkins.startBuild(hotBean.getJob_name(), buildParams);
                    try{
                        String buildUUID = ciPlatformManagerProxy.startBuild(
                            hotBean.getJob_name(), buildParams, "buildkite");
                        Buildkite.Build buildkiteBuild =
                                (Buildkite.Build) ciPlatformManagerProxy.getBuild(
                                        "buildkite", hotBean.getJob_name(), buildUUID);

                        String jobID = ciPlatformManagerProxy.startBuild(hotBean.getJob_name(), buildParams, "jenkins");
                        Jenkins.Build jenkinsBuild = (Jenkins.Build) ciPlatformManagerProxy.getBuild(
                                        "jenkins", hotBean.getJob_name(), jobID);

                        LOG.info("Starting new CI Jobs (hotfix-job) for hotfix id {}", hotfixId);
                    } catch (Exception e) {
                        LOG.error("Failed to start new CI Job (hotfix-job) for hotfix id {}", hotfixId, e);
                        hotBean.setState(HotfixState.FAILED);
                        hotBean.setError_message(
                                "Failed to create hotfix during batch triggering");
                        hotfixDAO.update(hotfixId, hotBean);
                        LOG.warn("CI returned a FAILURE status during state INITIAL for hotfix id "
                                            + hotfixId);
                    }
                    transition(hotBean);

                    // Else jobNum has not been given by job yet

                } else if (state == HotfixState.PUSHING) {
                    if (!StringUtils.isEmpty(jobNum)) {
                        Jenkins.Build build = (Jenkins.Build) ciPlatformManagerProxy.getBuild("jenkins", hotBean.getJob_name(), jobNum);
                        String status = build.getStatus().replace("\"", "");
                        int newProgress = build.getProgress();

                        // Update progress
                        if (oldProgress != newProgress) {
                            hotBean.setProgress(newProgress);
                            hotfixDAO.update(hotfixId, hotBean);
                        }

                        // Check if job completed or if job failed
                        if (status.equals("SUCCESS")) {
                            String buildName = getBuildName(hotBean);
                            String buildParams =
                                    "BRANCH="
                                            + "hotfix_"
                                            + hotBean.getOperator()
                                            + "_"
                                            + buildBean.getScm_commit_7()
                                            + "&BUILD_NAME="
                                            + buildName
                                            + "&HOTFIX_ID="
                                            + hotBean.getId()
                                            + "&REPO="
                                            + hotBean.getRepo();
                            hotBean.setJob_name(
                                    hotBean.getJob_name().replace("-hotfix-job", "-private-build"));
                            jenkins.startBuild(hotBean.getJob_name(), buildParams);
                            LOG.info(
                                    "Starting new Jenkins Job (private-build) for hotfix id {}",
                                    hotfixId);

                            transition(hotBean);
                        }
                        // Jenkins job has returned a failure status
                        if (status.equals("FAILURE")) {
                            hotBean.setState(HotfixState.FAILED);
                            hotBean.setError_message(
                                    "Failed to create hotfix, see "
                                            + jenkins.getJenkinsUrl()
                                            + "/"
                                            + hotBean.getJob_name()
                                            + "/"
                                            + hotBean.getJob_num()
                                            + "/console for more details");
                            hotfixDAO.update(hotfixId, hotBean);
                            LOG.warn(
                                    "Jenkins returned a FAILURE status during state PUSHING for hotfix id "
                                            + hotfixId);
                        }
                    } else {
                        LOG.error("Job Num is empty for hotfix id " + hotfixId);
                    }
                } else if (state == HotfixState.BUILDING) {
                    if (!StringUtils.isEmpty(jobNum)) {
                        Jenkins.Build build = jenkins.getBuild(hotBean.getJob_name(), jobNum);
                        String status = build.getStatus().replace("\"", "");
                        int newProgress = build.getProgress();

                        // Update progress
                        if (oldProgress != newProgress) {
                            hotBean.setProgress(newProgress);
                            hotfixDAO.update(hotfixId, hotBean);
                        }

                        // Check if job completed or if job failed
                        if (status.equals("SUCCESS")) {
                            LOG.info("Jenkins job succeeded in BUILDING for hotfix id " + hotfixId);
                            transition(hotBean);
                        }

                        // Jenkins job has returned a failure status
                        if (status.equals("FAILURE")) {
                            hotBean.setState(HotfixState.FAILED);
                            hotBean.setError_message(
                                    "Failed to build hotfix, see "
                                            + jenkins.getJenkinsUrl()
                                            + hotBean.getJob_name()
                                            + "/"
                                            + hotBean.getJob_num()
                                            + "/console for more details");
                            hotfixDAO.update(hotfixId, hotBean);
                            LOG.warn(
                                    "Jenkins returned a FAILURE status during state BUILDING for hotfix id "
                                            + hotfixId);
                        }
                    } else {
                        LOG.error("Job Num is empty for hotfix id " + hotfixId);
                    }
                } else {
                    throw new DeployInternalException(
                            "Hotfix Id " + hotBean.getId() + " has unknown state " + state);
                }
            } finally {
                utilDAO.releaseLock(hotfixLockName, connection);
                LOG.info("DB lock operation is successful: release lock {}", hotfixLockName);
            }
        } else {
            LOG.warn("DB lock operation fails: failed to get lock {}", hotfixLockName);
        }
    }

    private String getBuildName(HotfixBean hotBean) throws Exception {
        List<EnvironBean> envBeans = environDAO.getByName(hotBean.getEnv_name());
        // TODO here we assume the buildName is the same for every env
        return envBeans.get(0).getBuild_name();
    }

    void transition(HotfixBean hotBean) throws Exception {
        HotfixState state = hotBean.getState();
        String hotfixId = hotBean.getId();

        if (state == HotfixState.INITIAL) {
            hotBean.setState(HotfixState.PUSHING);
            LOG.info(
                    "Hotfix Id {} has transitioned from the INITIAL state to the PUSH state.",
                    hotfixId);
        } else if (state == HotfixState.PUSHING) {
            hotBean.setProgress(0);
            hotBean.setState(HotfixState.BUILDING);
            LOG.info(
                    "Hotfix Id {} has transitioned from the PUSH state to the BUILD state.",
                    hotfixId);
        } else if (state == HotfixState.BUILDING) {
            hotBean.setState(HotfixState.SUCCEEDED);
            state = HotfixState.SUCCEEDED;

            // Send chat message
            List<EnvironBean> environBeans = environDAO.getByName(hotBean.getEnv_name());
            Set<String> chatroomSet = new HashSet<String>();
            Set<String> groupMentionRecipientSet = new HashSet<String>();
            for (EnvironBean environBean : environBeans) {
                chatroomSet.add(environBean.getChatroom());
                groupMentionRecipientSet.add(environBean.getGroup_mention_recipients());
            }
            String chatrooms = StringUtils.join(chatroomSet, ",");
            String groupMentionRecipients = StringUtils.join(groupMentionRecipientSet, ",");
            DeployBean deployBean = deployDAO.getById(hotBean.getBase_deploy());
            BuildBean buildBean = buildDAO.getById(deployBean.getBuild_id());
            String commit = buildBean.getScm_commit();
            String name = hotBean.getOperator();
            String branch = "hotfix_" + name;
            String message =
                    name
                            + " just created a hotfix in "
                            + branch
                            + " branch, and including commit(s) "
                            + hotBean.getCommits()
                            + " on top of commit "
                            + commit;
            commonHandler.sendChatMessage(
                    Constants.SYSTEM_OPERATOR,
                    chatrooms,
                    message,
                    "yellow",
                    groupMentionRecipients);

            LOG.info("Hotfix Id {} is finished and now in the SUCCEEDED state.", hotfixId);
        } else {
            throw new DeployInternalException(
                    "Hotfix Id " + hotfixId + " has an unknown state " + state);
        }

        // Reset job number and last worked on time, and clean up the error
        if (state != HotfixState.SUCCEEDED) {
            hotBean.setJob_num("");
        }
        hotBean.setError_message("");
        hotBean.setLast_worked_on(System.currentTimeMillis());

        hotfixDAO.update(hotfixId, hotBean);
    }
}
