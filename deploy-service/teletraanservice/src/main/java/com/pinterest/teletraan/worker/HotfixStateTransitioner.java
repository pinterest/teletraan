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
import com.pinterest.deployservice.common.DeployInternalException;
import com.pinterest.deployservice.common.Jenkins;
import com.pinterest.deployservice.dao.*;
import com.pinterest.deployservice.handler.CommonHandler;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Check active deploys and push them into their final states
 */
public class HotfixStateTransitioner implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(HotfixStateTransitioner.class);

    private HotfixDAO hotfixDAO;
    private DeployDAO deployDAO;
    private BuildDAO buildDAO;
    private UtilDAO utilDAO;
    private EnvironDAO environDAO;
    private CommonHandler commonHandler;
    private String jenkinsUrl;
    private String jenkinsRemoteToken;
    // TODO make this configurable
    private static final int HOTFIX_JOB_DURATION_TIMEOUT = 60;

    public HotfixStateTransitioner(ServiceContext serviceContext) {
        hotfixDAO = serviceContext.getHotfixDAO();
        deployDAO = serviceContext.getDeployDAO();
        buildDAO = serviceContext.getBuildDAO();
        utilDAO = serviceContext.getUtilDAO();
        environDAO = serviceContext.getEnvironDAO();
        commonHandler = new CommonHandler(serviceContext);
        jenkinsUrl = serviceContext.getJenkinsUrl();
        jenkinsRemoteToken = serviceContext.getJenkinsRemoteToken();
    }

    void processBatch() throws Exception {
        // Get all current deploys, randomly pick one to work on
        List<String> hotfixIds = hotfixDAO.getOngoingHotfixIds();
        if (hotfixIds.isEmpty()) {
            LOG.info("HotfixStateTransitioner did not find any active hotfix, exiting.");
            return;
        }
        Collections.shuffle(hotfixIds);
        for (String hotfixId : hotfixIds) {
            HotfixBean hotBean = hotfixDAO.getByHotfixId(hotfixId);
            try {
                LOG.info("HotfixStateTransitioner chooses hotfix {} to work on.", hotfixId);
                transitionHotfixState(hotBean);
            } catch (Throwable t) {
                // Catch all throwable so that subsequent job not suppressed, also long error in DB
                LOG.error("HotfixStateTransitioner failed to process {} " + hotfixId, t);
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
        }
    }

    public void transitionHotfixState(HotfixBean hotBean) throws Exception {
        String hotfixLockName = String.format("HOTFIX-%s", hotBean.getId());
        Connection connection = utilDAO.getLock(hotfixLockName);
        if (connection != null) {
            LOG.info("Successfully get lock on {}", hotfixLockName);
            try {
                // Check for Hotfix for timeout
                if ((System.currentTimeMillis() - hotBean.getLast_worked_on()) > (HOTFIX_JOB_DURATION_TIMEOUT * 60000)) {
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
                Jenkins jenkins = new Jenkins(jenkinsUrl, jenkinsRemoteToken);
                DeployBean deployBean = deployDAO.getById(hotBean.getBase_deploy());
                BuildBean buildBean = buildDAO.getById(deployBean.getBuild_id());

                if (state == HotfixState.INITIAL) {
                    // Initial state, need to start job
                    String buildParams = "BASE_COMMIT=" + buildBean.getScm_commit() + "&COMMITS=" + hotBean.getCommits() +
                        "&SUFFIX=" + hotBean.getOperator() + "_" + buildBean.getScm_commit_7() +
                        "&HOTFIX_ID=" + hotBean.getId();
                    // Start job and set start time
                    jenkins.startBuild(hotBean.getJob_name(), buildParams);
                    LOG.info("Starting new Jenkins Job (hotfix-job) for hotfix id {}", hotfixId);

                    transition(hotBean);

                    // Else jobNum has not been given by job yet

                } else if (state == HotfixState.PUSHING) {
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
                            String buildName = getBuildName(hotBean);
                            String buildParams = "BRANCH=" + "hotfix_" + hotBean.getOperator() + "_" + buildBean.getScm_commit_7() +
                                "&BUILD_NAME=" + buildName + "&HOTFIX_ID=" + hotBean.getId();
                            hotBean.setJob_name(hotBean.getJob_name().replace("-hotfix-job", "-private-build"));
                            jenkins.startBuild(hotBean.getJob_name(), buildParams);
                            LOG.info("Starting new Jenkins Job (private-build) for hotfix id {}", hotfixId);

                            transition(hotBean);
                        }
                        // Jenkins job has returned a failure status
                        if (status.equals("FAILURE")) {
                            hotBean.setState(HotfixState.FAILED);
                            hotBean.setError_message("Failed to create hotfix, see " + jenkinsUrl +
                                hotBean.getJob_name() + "/" + hotBean.getJob_num() + "/console for more details");
                            hotfixDAO.update(hotfixId, hotBean);
                            LOG.warn("Jenkins returned a FAILURE status during state PUSHING for hotfix id " + hotfixId);
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
                            hotBean.setError_message("Failed to build hotfix, see " + jenkinsUrl +
                                hotBean.getJob_name() + "/" + hotBean.getJob_num() + "/console for more details");
                            hotfixDAO.update(hotfixId, hotBean);
                            LOG.warn("Jenkins returned a FAILURE status during state BUILDING for hotfix id " + hotfixId);
                        }
                    } else {
                        LOG.error("Job Num is empty for hotfix id " + hotfixId);
                    }
                } else {
                    throw new DeployInternalException("Hotfix Id " + hotBean.getId() + " has unknown state " + state);
                }
            } finally {
                utilDAO.releaseLock(hotfixLockName, connection);
                LOG.info("Successfully released lock on {}", hotfixLockName);
            }
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
            LOG.info("Hotfix Id {} has transitioned from the INITIAL state to the PUSH state.",
                hotfixId);
        } else if (state == HotfixState.PUSHING) {
            hotBean.setProgress(0);
            hotBean.setState(HotfixState.BUILDING);
            LOG.info("Hotfix Id {} has transitioned from the PUSH state to the BUILD state.",
                hotfixId);
        } else if (state == HotfixState.BUILDING) {
            hotBean.setState(HotfixState.SUCCEEDED);
            state = HotfixState.SUCCEEDED;

            // Send chat message
            List<EnvironBean> environBeans = environDAO.getByName(hotBean.getEnv_name());
            Set<String> chatroomSet = new HashSet<String>();
            for (EnvironBean environBean : environBeans) {
                chatroomSet.add(environBean.getChatroom());
            }
            String chatrooms = StringUtils.join(chatroomSet, ",");
            DeployBean deployBean = deployDAO.getById(hotBean.getBase_deploy());
            BuildBean buildBean = buildDAO.getById(deployBean.getBuild_id());
            String commit = buildBean.getScm_commit();
            String name = hotBean.getOperator();
            String branch = "hotfix_" + name;
            String message = name + " just created a hotfix in " + branch + " branch, and including commit(s) " +
                hotBean.getCommits() + " on top of commit " + commit;
            commonHandler.sendChatMessage(Constants.SYSTEM_OPERATOR, chatrooms, message, "yellow");

            LOG.info("Hotfix Id {} is finished and now in the SUCCEEDED state.", hotfixId);
        } else {
            throw new DeployInternalException("Hotfix Id " + hotfixId + " has an unknown state " + state);
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
