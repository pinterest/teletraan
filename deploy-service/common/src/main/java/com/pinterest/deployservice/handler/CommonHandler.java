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
package com.pinterest.deployservice.handler;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.*;
import com.pinterest.deployservice.buildtags.BuildTagsManager;
import com.pinterest.deployservice.chat.ChatManager;
import com.pinterest.deployservice.common.StateMachines;
import com.pinterest.deployservice.common.WebhookDataFactory;
import com.pinterest.deployservice.dao.*;
import com.pinterest.deployservice.email.MailManager;
import com.pinterest.deployservice.rodimus.RodimusManager;
import com.pinterest.deployservice.events.DeployEvent;
import com.pinterest.teletraan.universal.events.AppEventPublisher;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;

public class CommonHandler {
    private static final Logger LOG = LoggerFactory.getLogger(CommonHandler.class);
    // metrics
    private final MetricRegistry metrics = new MetricRegistry();
    private DeployDAO deployDAO;
    private EnvironDAO environDAO;
    private BuildDAO buildDAO;
    private AgentDAO agentDAO;
    private UtilDAO utilDAO;
    private ScheduleDAO scheduleDAO;
    private ChatManager chatManager;
    private AppEventPublisher publisher;
    private MailManager mailManager;
    private ExecutorService jobPool;
    private DataHandler dataHandler;
    private String deployBoardUrlPrefix;
    private Counter successCounter;
    private Counter failureCounter;
    private BuildTagsManager buildTagsManager;
    private final RodimusManager rodimusManager;


    private final class FinishNotifyJob implements Callable<Void> {
        private EnvironBean envBean;
        private DeployBean deployBean;
        private DeployBean newPartialDeployBean;
        private String buildId;

        public FinishNotifyJob(EnvironBean envBean, DeployBean deployBean, DeployBean newPartialDeployBean, String buildId) {
            this.envBean = envBean;
            this.deployBean = deployBean;
            this.newPartialDeployBean = newPartialDeployBean;
            this.buildId = buildId;
        }

        private void sendMessage() {
            String operator = deployBean.getOperator();
            DeployState state = newPartialDeployBean.getState();

            String message;
            try {
                message = generateMessage(buildId, envBean, state, deployBean);
            } catch (Exception e) {
                LOG.error("Failed to genereate message", e);
                return;
            }

            String color = state == DeployState.SUCCEEDING ? "green" : "red";

            if (state == DeployState.SUCCEEDING) {
                successCounter.inc();
            }

            sendWatcherMessage(operator, envBean.getWatch_recipients(), message, color);
            sendChatMessage(operator, envBean.getChatroom(), message, color, envBean.getGroup_mention_recipients());

            if (state == DeployState.FAILING) {
                String recipients = envBean.getEmail_recipients();
                if (envBean.getNotify_authors() && !StringUtils.isEmpty(recipients)) {
                    LOG.debug(String.format("Sending emails to %s for failed deploy: %s/%s", envBean.getEmail_recipients(), envBean.getEnv_name(), envBean.getStage_name()));
                    String subject = String.format("%s/%s Deploy Failed.", envBean.getEnv_name(), envBean.getStage_name());
                    sendEmailMessage(message, subject, recipients);
                }
                failureCounter.inc();
            }
        }

        public Void call() {
            try {
                LOG.info("Start to work on FinishNotifyJob for deploy {}", deployBean.getDeploy_id());
                sendMessage();
                sendDeployEvents(deployBean, newPartialDeployBean, envBean);
                LOG.info("Completed NoitfyJob for deploy {}", deployBean.getDeploy_id());
            } catch (Throwable t) {
                LOG.error("FinishNotifyJob job failed for deploy !" + deployBean.getDeploy_id(), t);
            }
            return null;
        }
    }

    public CommonHandler(ServiceContext serviceContext) {
        deployDAO = serviceContext.getDeployDAO();
        environDAO = serviceContext.getEnvironDAO();
        buildDAO = serviceContext.getBuildDAO();
        agentDAO = serviceContext.getAgentDAO();
        utilDAO = serviceContext.getUtilDAO();
        scheduleDAO = serviceContext.getScheduleDAO();
        publisher = serviceContext.getAppEventPublisher();
        chatManager = serviceContext.getChatManager();
        mailManager = serviceContext.getMailManager();
        buildTagsManager = serviceContext.getBuildTagsManager();
        rodimusManager = serviceContext.getRodimusManager();
        jobPool = serviceContext.getJobPool();
        dataHandler = new DataHandler(serviceContext);
        deployBoardUrlPrefix = serviceContext.getDeployBoardUrlPrefix();
        initializeMetrics();
    }

    private void initializeMetrics() {
        successCounter = metrics.counter("deploys.success.count");
        failureCounter = metrics.counter("deploys.failure.count");
      }

    public String getDeployAction(DeployType deployType) {
        String action = "deploy of";
        if (deployType == DeployType.ROLLBACK) {
            action = "rollback to";
        } else if (deployType == DeployType.RESTART) {
            action = "restart of";
        }
        return action;
    }

    String generateMessage(String buildId, EnvironBean envBean, DeployState state, DeployBean deployBean) throws Exception {
        DeployType deployType = deployBean.getDeploy_type();
        BuildBean buildBean = buildDAO.getById(buildId);
        String webLink = deployBoardUrlPrefix + String.format("/env/%s/%s/deploy/",
            envBean.getEnv_name(),
            envBean.getStage_name());

        TagBean tagBean = buildTagsManager.getEffectiveBuildTag(buildBean);

        String action = getDeployAction(deployType);
        if (state == DeployState.SUCCEEDING) {
            // TODO this is Slack specific, screw hipchat for now

            String template = (tagBean != null && tagBean.getValue() == TagValue.BAD_BUILD) ?
                "WARNING: %s/%s: %s %s/%s completed successfully, but running on bad build. See details <%s>" :
                "%s/%s: %s %s/%s completed successfully. See details <%s>";

            return String.format(template,
                envBean.getEnv_name(),
                envBean.getStage_name(),
                action,
                buildBean.getScm_branch(),
                buildBean.getScm_commit_7(),
                webLink);
        } else {
            // TODO this is Slack specific, screw hipchat for now
            String tagMessage = (tagBean == null) ? "NOT SET" : tagBean.getValue().toString();
            if (deployBean.getSuc_date() != null && deployBean.getSuc_date() != 0L) {
                // This is failure after previous success
                return String.format("%s/%s: can not deploy to all the newly provisioned hosts. See details <%s>. This build is currently marked as %s.",
                    envBean.getEnv_name(),
                    envBean.getStage_name(),
                    webLink,
                    tagMessage);
            } else {
                return String.format("%s/%s: %s %s/%s failed. See details <%s>. This build is currently marked as %s.",
                    envBean.getEnv_name(),
                    envBean.getStage_name(),
                    action,
                    buildBean.getScm_branch(),
                    buildBean.getScm_commit_7(),
                    webLink,
                    tagMessage);
            }
        }
    }

    public void sendChatMessage(String from, String rooms, String message, String color, String recipients) {
        if (StringUtils.isEmpty(rooms)) {
            return;
        }
        List<String> chatrooms = Arrays.asList(rooms.split(","));

        for (String chatroom : chatrooms) {
            try {
                if (!StringUtils.isEmpty(recipients)) {
                    List<String> targets = Arrays.asList(recipients.split(","));
                    for (String target : targets) {
                        message = "<!subteam^" + target + "> " + message;
                    }
                }
                chatManager.send(from, chatroom.trim(), message, color);
            } catch (Exception e) {
                LOG.error(String.format("Failed to send message '%s' to chatroom %s", message, chatroom), e);
            }
        }
    }

    void sendWatcherMessage(String operator, String watcherStr, String message, String color) {
        if (StringUtils.isEmpty(watcherStr)) {
            return;
        }
        List<String> watchers = Arrays.asList(watcherStr.split(","));
        for (String watcher : watchers) {
            try {
                // TODO verify that send to peoper actually works
                chatManager.sendToUser(operator, watcher.trim(), message, color);
            } catch (Exception e) {
                LOG.error(String.format("Failed to send message '%s' to watcher %s",
                    message, watcher), e);
            }
        }
    }

    public void sendEmailMessage(String message, String subject, String recipients) {
        List<String> emailRecipients = Arrays.asList(recipients.split(","));
        for (String recipient : emailRecipients) {
            try {
                mailManager.send(recipient, subject, message);
                LOG.info("Successfully send email to {}", recipient);
            } catch (Exception e) {
                LOG.error("Failed to send email to {}", recipient, e);
            }
        }
    }

    void transitionSchedule(EnvironBean envBean) throws Exception {
        String scheduleId = envBean.getSchedule_id();
        if (scheduleId == null) {
            return;
        }
        ScheduleBean schedule = scheduleDAO.getById(scheduleId);
        String hostNumbers = schedule.getHost_numbers();
        String cooldownTimes = schedule.getCooldown_times();
        Integer currentSession = schedule.getCurrent_session();
        Integer totalSessions = schedule.getTotal_sessions();
        String[] hostNumbersList = hostNumbers.split(",");
        String[] cooldownTimesList = cooldownTimes.split(",");
        int totalHosts = 0;
        for (int i = 0; i < currentSession; i++) {
            totalHosts+=Integer.parseInt(hostNumbersList[i]);
        }
        if (schedule.getState() == ScheduleState.COOLING_DOWN) {
            // check if cooldown period is over
            if (System.currentTimeMillis() - schedule.getState_start_time() > Integer.parseInt(cooldownTimesList[currentSession-1]) * 60000) {
                ScheduleBean updateScheduleBean = new ScheduleBean();
                updateScheduleBean.setId(schedule.getId());
                if (totalSessions == currentSession) {
                    updateScheduleBean.setState(ScheduleState.FINAL);
                    LOG.debug("Env {} is now going into final deloy stage and will deploy on the rest of all of the hosts.", envBean.getEnv_id());
                } else {
                    updateScheduleBean.setState(ScheduleState.RUNNING);
                    updateScheduleBean.setCurrent_session(currentSession+1);
                    LOG.debug("Env {} has finished cooling down and will now start resume deploy by running session {}", envBean.getEnv_id(), currentSession+1);
                }
                updateScheduleBean.setState_start_time(System.currentTimeMillis());
                scheduleDAO.update(updateScheduleBean, schedule.getId());
            }
        } else if (schedule.getState() == ScheduleState.RUNNING && agentDAO.countFinishedAgentsByDeploy(envBean.getDeploy_id()) >= totalHosts) {
            ScheduleBean updateScheduleBean = new ScheduleBean();
            updateScheduleBean.setId(schedule.getId());
            updateScheduleBean.setState(ScheduleState.COOLING_DOWN);
            updateScheduleBean.setState_start_time(System.currentTimeMillis());
            scheduleDAO.update(updateScheduleBean, schedule.getId());
            LOG.debug("Env {} has finished running session {} and will now begin cooling down", envBean.getEnv_id(), currentSession);
        }
    }

    void transition(DeployBean deployBean, DeployBean newDeployBean, EnvironBean envBean) throws Exception {
        transitionSchedule(envBean);
        String deployId = deployBean.getDeploy_id();
        String envId = envBean.getEnv_id();
        DeployState oldState = deployBean.getState();

        int sucThreshold = envBean.getSuccess_th();
        long total = agentDAO.countAgentByEnv(envId);
        LOG.debug("There are total {} agents are expected for env {}", total, envId);

        long succeeded = agentDAO.countSucceededAgent(envId, deployId);
        LOG.debug("Among them, {} agents are succeeded", succeeded);

        long stucked = agentDAO.countStuckAgent(envId, deployId);
        LOG.debug("Among them, {} agents are stuck", stucked);

        newDeployBean.setSuc_total((int) succeeded);
        newDeployBean.setFail_total((int) stucked);
        newDeployBean.setTotal((int) total);
        newDeployBean.setState(oldState);
        newDeployBean.setLast_update(System.currentTimeMillis());

        String cluster = envBean.getEnv_name() + "-" + envBean.getStage_name();
        Long capacity = null;
        try {
             capacity = rodimusManager.getClusterCapacity(cluster);
        } catch (Exception ex) {
            LOG.error("Yaqin Debug: Failed to get capacity for cluster {}, exception: {}", cluster, ex);
        }
       
        LOG.debug("Yaqin Debug: The capacity for cluster {} is {}", cluster, capacity);

        //The maximum sucThreshold is 10000 to keep precision.
        if (succeeded * 10000 >= sucThreshold * total && !(succeeded == 0 && capacity != null && capacity > 0)) {
            LOG.debug("Propose deploy {} as SUCCEEDING since {} agents are succeeded.", deployId, succeeded);
            if (deployBean.getSuc_date() == null) {
                newDeployBean.setSuc_date(System.currentTimeMillis());

                // Submit post-webhooks, if exists
                EnvWebHookBean webhooks = dataHandler.getDataById(envBean.getWebhooks_config_id(), WebhookDataFactory.class);
                if (webhooks != null && !CollectionUtils.isEmpty(webhooks.getPostDeployHooks())) {
                    jobPool.submit(new WebhookJob(webhooks.getPostDeployHooks(), deployBean, envBean));
                    LOG.info("Submitted post deploy hook job for deploy {}.", deployId);
                }

                if (envBean.getAccept_type() == AcceptanceType.AUTO) {
                    newDeployBean.setAcc_status(AcceptanceStatus.ACCEPTED);
                } else {
                    newDeployBean.setAcc_status(AcceptanceStatus.OUTSTANDING);
                }
            }
            newDeployBean.setState(DeployState.SUCCEEDING);
            LOG.info("Set deploy {} as SUCCEEDING since {} agents are succeeded.", deployId, succeeded);
            return;
        }

        if (stucked * 10000 > (10000 - sucThreshold) * total) {
            newDeployBean.setState(DeployState.FAILING);
            LOG.info("Set deploy {} as FAILING since {} agents are stuck.", deployId, stucked);
            return;
        }

        String scheduleId = envBean.getSchedule_id();
        long duration = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - deployBean.getLast_update());
        long stuckTh = envBean.getStuck_th();
        if (succeeded <= deployBean.getSuc_total() && duration >= stuckTh) {
            if (oldState == DeployState.SUCCEEDING) {
                // This is the case when deploy has been in SUCCEEDING for a while without updates
                // And a new machine being provisioned, in this case, we set status back to RUNNING
                LOG.info("Set deploy {} back to RUNNING most likely there are new hosts joining in.", deployId);
                newDeployBean.setState(DeployState.RUNNING);
                return;
            } else {
                if (scheduleId != null) { // don't change state if it's cooling down
                    ScheduleBean schedule = scheduleDAO.getById(scheduleId);
                    if (schedule.getState() == ScheduleState.COOLING_DOWN) {
                        return;
                    }
                }
                newDeployBean.setState(DeployState.FAILING);
                LOG.info("Set deploy {} as FAILING since {} seconds past without complete the deploy.", deployId, duration);

                // TODO, temp hack do NOT set lastUpdate for deploy stuck case, otherwise the
                // next round transition will convert FAILING to RUNNING since new lastUpdate
                // The better solution should be provide reason for previous transition
                newDeployBean.setLast_update(deployBean.getLast_update());
                // TODO please refactoring this StateTransition logic, it is HORRIBLE!!!
                return;
            }
        }

        // At this point, always set to RUNNING
        if (oldState != DeployState.RUNNING) {
            LOG.info("Set deploy {} from {} to RUNNING.", deployId, oldState);
        } else {
            LOG.debug("Deploy {} is still RUNNING...", deployId, oldState);
        }
        newDeployBean.setState(DeployState.RUNNING);
    }

    boolean shouldSendFinishMessage(DeployState oldState, DeployState newState, Long sucDate) {
        // State does not change
        if (oldState == newState) {
            return false;
        }

        // No need to notify if the deploy is still running.
        if (newState == DeployState.RUNNING) {
            return false;
        }

        // SUCCEEDING->SUCCEEDED: no need to notify, ( since we already notified when SUCCEEDING)
        if (newState == DeployState.SUCCEEDED && oldState == DeployState.SUCCEEDING) {
            return false;
        }

        // FAILING->ABORTED: not need to notify since we already notified FAILING
        if (newState == DeployState.ABORTED && oldState == DeployState.FAILING) {
            return false;
        }

        // if newState is SUCCEEDING and suc_date is set, do not report ( because we already did )
        return !(newState == DeployState.SUCCEEDING && sucDate != null);
    }

    void sendDeployEvents(DeployBean oldDeployBean, DeployBean newDeployBean, EnvironBean environBean) {
        DeployState newState = newDeployBean.getState();
        DeployState oldState = oldDeployBean.getState();
        if (newState == oldState) {
            return;
        }
        if (newState == DeployState.SUCCEEDING && oldDeployBean.getSuc_date() == null) {
            try {
                String build_id = oldDeployBean.getBuild_id();
                BuildBean buildBean = buildDAO.getById(build_id);
                String commit = buildBean.getScm_commit_7();
                String envName = environBean.getEnv_name();
                String stageName = environBean.getStage_name();
                DeployEvent event = new DeployEvent(this, envName, stageName, commit, newDeployBean.getOperator());
                publisher.publishEvent(event);
                LOG.info("Successfully sent deploy event: {}", event);
            } catch (Exception ex) {
                LOG.error("Failed to send deploy events.", ex);
            }
        }
    }

    public void transitionDeployState(String deployId, EnvironBean envBean) throws Exception {
        String lockName = String.format("STATE_TRANSITION-%s", deployId);
        Connection connection = utilDAO.getLock(lockName);
        if (connection != null) {
            LOG.info(String.format("DB lock operation is successful: get lock %s", lockName));
            try {
                internalTransition(deployId, envBean);
            } finally {
                utilDAO.releaseLock(lockName, connection);
                LOG.info(String.format("DB lock operation is successful: release lock %s", lockName));
            }
        } else {
            LOG.warn(String.format("DB lock operation fails: failed to get lock %s", lockName));
        }
    }

    void internalTransition(String deployId, EnvironBean envBean) throws Exception {
        /*
         * Check again to make sure the deploy is indeed ongoing
         */
        DeployBean deployBean = deployDAO.getById(deployId);
        if (deployBean == null) {
            LOG.error("Deploy {} does not exist.", deployId);
            return;
        }

        DeployState state = deployBean.getState();
        if (!StateMachines.DEPLOY_ACTIVE_STATES.contains(state)) {
            LOG.info("Deploy {} is currently in {} state, no need to transition.", deployId, state);
            return;
        }

        String envId = deployBean.getEnv_id();
        if (envBean == null) {
            envBean = environDAO.getById(envId);
        }
        /*
         * Make sure we do not have such a deploy which is not current but somehow not in the
         * final state. This should NOT happen, treat this as cleaning up for any potential wrong states
        */
        if (!deployId.equals(envBean.getDeploy_id())) {
            LOG.warn("Deploy {} has already been obsoleted but state {} is not final.", deployId, state);
            DeployState finalState = StateMachines.FINAL_STATE_TRANSITION_MAP.get(state);
            LOG.info("Transite deploy {} to {} state.", deployId, finalState);
            DeployBean updateBean = new DeployBean();
            updateBean.setState(finalState);
            updateBean.setLast_update(System.currentTimeMillis());
            deployDAO.update(deployId, updateBean);
            return;
        }

        DeployBean newPartialDeployBean = new DeployBean();
        transition(deployBean, newPartialDeployBean, envBean);

        if (shouldSendFinishMessage(state, newPartialDeployBean.getState(), deployBean.getSuc_date())) {
            jobPool.submit(new FinishNotifyJob(envBean, deployBean, newPartialDeployBean, deployBean.getBuild_id()));
        }


        // TODO This is not easy to maintain especially when there are new fields added,
        // it makes more sense it we implement this in DeployBean and have it examine all
        // the fields, just like equals
        if (!state.equals(newPartialDeployBean.getState()) ||
            !deployBean.getSuc_total().equals(newPartialDeployBean.getSuc_total()) ||
            !deployBean.getFail_total().equals(newPartialDeployBean.getFail_total()) ||
            !deployBean.getTotal().equals(newPartialDeployBean.getTotal())) {
            deployDAO.updateStateSafely(deployId, state.toString(), newPartialDeployBean);
            LOG.info("Updated deploy {} with deploy bean = {}.", deployId, newPartialDeployBean);
        }
    }
}
