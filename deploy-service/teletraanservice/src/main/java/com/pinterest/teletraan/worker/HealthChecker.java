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

import com.pinterest.arcee.autoscaling.AutoScalingManager;
import com.pinterest.arcee.bean.GroupBean;
import com.pinterest.arcee.bean.HealthCheckBean;
import com.pinterest.arcee.bean.HealthCheckErrorBean;
import com.pinterest.arcee.bean.HealthCheckState;
import com.pinterest.arcee.bean.HealthCheckStatus;
import com.pinterest.arcee.bean.HealthCheckType;
import com.pinterest.arcee.bean.ImageBean;
import com.pinterest.arcee.common.HealthCheckConstants;
import com.pinterest.arcee.dao.GroupInfoDAO;
import com.pinterest.arcee.dao.HealthCheckDAO;
import com.pinterest.arcee.dao.HealthCheckErrorDAO;
import com.pinterest.arcee.dao.HostInfoDAO;
import com.pinterest.arcee.dao.ImageDAO;
import com.pinterest.arcee.handler.GroupHandler;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.AgentBean;
import com.pinterest.deployservice.bean.AgentErrorBean;
import com.pinterest.deployservice.bean.AgentStatus;
import com.pinterest.deployservice.bean.DeployStage;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.bean.HostState;
import com.pinterest.deployservice.dao.AgentDAO;
import com.pinterest.deployservice.dao.AgentErrorDAO;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.dao.UtilDAO;
import com.pinterest.deployservice.handler.CommonHandler;
import com.pinterest.deployservice.common.NotificationJob;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class HealthChecker implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(HealthChecker.class);
    private final HealthCheckDAO healthCheckDAO;
    private final HealthCheckErrorDAO healthCheckErrorDAO;
    private final GroupInfoDAO groupInfoDAO;
    private final HostInfoDAO hostInfoDAO;
    private final HostDAO hostDAO;
    private final AgentDAO agentDAO;
    private final AgentErrorDAO agentErrorDAO;
    private final ImageDAO imageDAO;
    private final UtilDAO utilDAO;
    private final AutoScalingManager autoScalingManager;
    private final GroupHandler groupHandler;
    private final CommonHandler commonHandler;
    private final ExecutorService jobPool;
    private String deployBoardUrlPrefix;
    // page oncall after FAILED_HEALTH_CHECK_BEFORE_PAGE health check failure
    private final int FAILED_HEALTH_CHECK_BEFORE_PAGE = 3;

    public HealthChecker(ServiceContext serviceContext) {
        healthCheckDAO = serviceContext.getHealthCheckDAO();
        healthCheckErrorDAO = serviceContext.getHealthCheckErrorDAO();
        groupInfoDAO = serviceContext.getGroupInfoDAO();
        hostInfoDAO = serviceContext.getHostInfoDAO();
        hostDAO = serviceContext.getHostDAO();
        agentDAO = serviceContext.getAgentDAO();
        agentErrorDAO = serviceContext.getAgentErrorDAO();
        imageDAO = serviceContext.getImageDAO();
        utilDAO = serviceContext.getUtilDAO();
        autoScalingManager = serviceContext.getAutoScalingManager();
        groupHandler = new GroupHandler(serviceContext);
        commonHandler = new CommonHandler(serviceContext);
        jobPool = serviceContext.getJobPool();
        deployBoardUrlPrefix = serviceContext.getDeployBoardUrlPrefix();
    }

    /**
     * This function is used to transit state/status
     */
    private void transistionState(HealthCheckBean healthCheckBean, HealthCheckState newState, HealthCheckStatus newStatus, String errorMessage) {
        HealthCheckBean newBean = new HealthCheckBean();
        if (!StringUtils.isEmpty(healthCheckBean.getHost_id())) {
            newBean.setHost_id(healthCheckBean.getHost_id());
        }

        if (!StringUtils.isEmpty(errorMessage)) {
            newBean.setError_message(errorMessage);
        }

        if (newStatus != null) {
            newBean.setStatus(newStatus);
        }

        newBean.setHost_launch_time(healthCheckBean.getHost_launch_time());
        newBean.setHost_terminated(healthCheckBean.getHost_terminated());
        newBean.setDeploy_start_time(healthCheckBean.getDeploy_start_time());
        newBean.setDeploy_complete_time(healthCheckBean.getDeploy_complete_time());
        newBean.setState(newState);
        newBean.setState_start_time(System.currentTimeMillis());
        newBean.setLast_worked_on(System.currentTimeMillis());
        try {
            healthCheckDAO.updateHealthCheckById(healthCheckBean.getId(), newBean);
        } catch (Exception e) {
            LOG.error("Failed to update healthCheckDAO {}", newBean.toString(), e);
        }
    }

    private boolean shouldNotifyOncall(String groupName) throws Exception {
        List<String> healthCheckBeans = healthCheckDAO.getRecentHealthCheckStatus(groupName, FAILED_HEALTH_CHECK_BEFORE_PAGE - 1);
        if (healthCheckBeans.size() < FAILED_HEALTH_CHECK_BEFORE_PAGE - 1) {
            return false;
        }

        // only notify oncall if the health check failed/timeout 3 consective times in the past.
        return !healthCheckBeans.contains(HealthCheckStatus.QUALIFIED.toString());
    }

    /**
     * This function is used to timeout the health check process
     */
    private boolean shouldTimeoutHealthCheck(HealthCheckBean healthCheckBean, GroupBean groupBean) throws Exception {
        long lastStateElapsedTime = System.currentTimeMillis() - healthCheckBean.getState_start_time();
        if (lastStateElapsedTime  >= (long) HealthCheckConstants.DEFAULT_HEALTH_CHECK_TIMEOUT * 1000) {
            LOG.info("Timeout health check id {}", healthCheckBean.getId());
            String groupName = healthCheckBean.getGroup_name();
            if (healthCheckBean.getType() != HealthCheckType.AMI_TRIGGERED) {
                try {
                    LOG.info("Disable scaling down event for group {}", groupName);
                    if (!autoScalingManager.isScalingDownEventEnabled(groupName)) {
                        LOG.info("The asg scaling down event has been disabled for group {}", groupName);
                    } else {
                        autoScalingManager.disableScalingDownEvent(groupName);
                    }
                } catch (Exception e) {
                    LOG.error("Failed to disable scaling down event for group {}", groupName, e);
                }
            }

            HealthCheckState state = HealthCheckState.COMPLETING;
            if (healthCheckBean.getState() == HealthCheckState.INIT) {
                // set state to completed to skip terminating host process
                state = HealthCheckState.COMPLETED;
            }
            String errorMessage = String.format("Health Check timeout at state %s", healthCheckBean.getState());
            transistionState(healthCheckBean, state, HealthCheckStatus.TIMEOUT, errorMessage);

            String subject = String.format("Health Check Alert - Health Check Timeout in group <%s>", groupName);
            String webLink = deployBoardUrlPrefix + String.format("/groups/health_check/%s", healthCheckBean.getId());
            String message = String.format("%s. See details: %s", errorMessage, webLink);
            String recipients = shouldNotifyOncall(groupBean.getGroup_name()) ? groupBean.getPager_recipients() : groupBean.getEmail_recipients();
            jobPool.submit(new NotificationJob(message, subject, recipients, groupBean.getChatroom(), commonHandler));
            return true;
        }
        return false;
    }

    /**
     * This function is used to send message if health check failed
     */
    private void failedHealthCheckAlertJob(HealthCheckBean healthCheckBean, GroupBean groupBean, String subject, String errorMessage) throws Exception {
        HealthCheckState state = HealthCheckState.COMPLETING;
        if (healthCheckBean.getState() == HealthCheckState.INIT) {
            // set state to completed to skip terminating host process
            state = HealthCheckState.COMPLETED;
        }

        String emailRecipient = groupBean.getEmail_recipients();
        String webLink = deployBoardUrlPrefix + String.format("/groups/health_check/%s", healthCheckBean.getId());
        String message = String.format("%s. See details: %s", errorMessage, webLink);
        // If the health check failed at Pending verify state, should send pager alert
        if (healthCheckBean.getState() == HealthCheckState.PENDING_VERIFY && shouldNotifyOncall(healthCheckBean.getGroup_name())) {
            emailRecipient = groupBean.getPager_recipients();
            message = String.format("Health check is failing for %d times. %s. See details: %s", FAILED_HEALTH_CHECK_BEFORE_PAGE, errorMessage, webLink);
        }

        transistionState(healthCheckBean, state, HealthCheckStatus.FAILED, errorMessage);
        jobPool.submit(new NotificationJob(message, subject, emailRecipient, groupBean.getChatroom(), commonHandler));
    }

    /**
     * Step 1. Launch new instance with latest ami
     * If AWS Launch Instance API call failed, send wanring message and move the state to completed to skip terminate instance process
     * If launch successfully, update hostDAO
     */
    private void processInitState(HealthCheckBean healthCheckBean, GroupBean groupBean) throws Exception {
        String groupName = groupBean.getGroup_name();
        LOG.info("Start to launch instance for group {} and healthCheck id {} at health check state {}",
            groupName, healthCheckBean.getId(), healthCheckBean.getState().toString());

        // Randomly pick a subnet to launch instance to
        List<String> subnets = Arrays.asList(groupBean.getSubnets().split(","));
        Collections.shuffle(subnets);
        String subnet = subnets.get(0);

        LOG.info("Start to launch instance with AMI ID {} to Subnet {} for group {}", healthCheckBean.getAmi_id(), subnet, groupName);
        groupBean.setImage_id(healthCheckBean.getAmi_id());
        List<HostBean> hosts = hostInfoDAO.launchEC2Instances(groupBean, 1, subnet);
        if (hosts.isEmpty()) {
            LOG.error("Failed to launch instance with AMI ID {} to Subnet {} for group {}", healthCheckBean.getAmi_id(), subnet, groupName);
            String subject = String.format("Health Check Warning - Launch Instance Failed in group <%s>", groupName);
            String errorMessage = String.format("AWS Launch Instance API call failed (AMI Id: %s, Subnet: %s) in group %s",
                healthCheckBean.getAmi_id(), subnet, groupName);
            failedHealthCheckAlertJob(healthCheckBean, groupBean, subject, errorMessage);
            return;
        }

        HostBean host = hosts.get(0);
        LOG.info("Successfully launched host id {} for group {}", host.getHost_id(), groupName);
        try {
            hostDAO.insert(host);
        } catch (Exception e) {
            LOG.error("Failed to insert new host id {} to hostDAO", host.getHost_id(), e);
        }

        healthCheckBean.setHost_id(host.getHost_id());
        healthCheckBean.setHost_launch_time(host.getCreate_date());
        healthCheckBean.setHost_terminated(false);
        transistionState(healthCheckBean, HealthCheckState.LAUNCHING, HealthCheckStatus.SUCCEEDED, "");
        LOG.info("Health Check Succeeded: id {}, group {}, state {}", healthCheckBean.getId(), groupName, healthCheckBean.getState());
    }

    /**
     * Step 2. Check whether the instance is healthy
     * If instance is terminated or stopped, send warning message
     * If instance is running, check whether its last update time exceeds the launch grace period
     * If the instance has not responsive for more than the launch grace period, send warning message
     */
    private void processLaunchingState(HealthCheckBean healthCheckBean, GroupBean groupBean) throws Exception {
        String groupName = groupBean.getGroup_name();
        LOG.info("Start to check instance state for group {} and healthCheck id {} at health check state {}",
            groupName, healthCheckBean.getId(), healthCheckBean.getState().toString());

        boolean succeeded = true;
        String hostId = healthCheckBean.getHost_id();

        // Check on AWS to make sure the instance is running
        List<String> runningIds = hostInfoDAO.getRunningInstances(Arrays.asList(hostId));
        if (runningIds.isEmpty()) {
            succeeded = false;

            Set<String> terminatedIds = hostInfoDAO.getTerminatedHosts(new HashSet<>(Arrays.asList(hostId)));
            if (!terminatedIds.isEmpty()) {
                LOG.error("Instance id {} is terminated or stopped by AWS", hostId);
                String subject = String.format("Health Check Warning - Launch Instance Failed in group <%s>", groupName);
                String errorMessage = String.format("Instance %s is terminated or stopped by AWS (AMI Id: %s) in group %s",
                    hostId, healthCheckBean.getAmi_id(), groupName);
                failedHealthCheckAlertJob(healthCheckBean, groupBean, subject, errorMessage);
                return;
            }
        } else {
            // Check whether the instance exceeds launch grace period
            HostBean host = hostDAO.getByEnvIdAndHostId(healthCheckBean.getEnv_id(), hostId);
            if (host.getState() == HostState.PROVISIONED) {
                long lastUpdateElapsedTime = System.currentTimeMillis() - host.getLast_update();
                if (lastUpdateElapsedTime >= (long) groupBean.getLaunch_latency_th() * 1000) {
                    succeeded = false;
                    String subject = String.format("Health Check Warning - Instance exceeded launch grace period in group <%s>", groupName);
                    String errorMessage = String.format("Instance %s has not been responsive for more than %d seconds since they were launched "
                        + "(AMI Id: %s) in group %s", hostId, groupBean.getLaunch_latency_th(), healthCheckBean.getAmi_id(), groupName);
                    failedHealthCheckAlertJob(healthCheckBean, groupBean, subject, errorMessage);
                }
            }
        }

        if (succeeded) {
            transistionState(healthCheckBean, HealthCheckState.PENDING_VERIFY, HealthCheckStatus.SUCCEEDED, "");
            LOG.info("Health Check Succeeded: id {}, group {}, state {}", healthCheckBean.getId(), groupName, healthCheckBean.getState());
        }
    }

    private void updateHealthCheckError(String id, AgentBean agentBean, AgentErrorBean agentErrorBean) {
        HealthCheckErrorBean bean = new HealthCheckErrorBean();
        bean.setId(id);
        bean.setEnv_id(agentBean.getEnv_id());
        bean.setDeploy_stage(agentBean.getDeploy_stage());
        bean.setAgent_state(agentBean.getState());
        bean.setAgent_status(agentBean.getStatus());
        bean.setLast_err_no(agentBean.getLast_err_no());
        bean.setFail_count(agentBean.getFail_count());

        if (!StringUtils.isEmpty(agentErrorBean.getError_msg())) {
            bean.setError_msg(agentErrorBean.getError_msg());
        }

        bean.setAgent_start_date(agentBean.getStart_date());
        bean.setAgent_last_update(agentBean.getLast_update());
        try {
            healthCheckErrorDAO.insertHealthCheckError(bean);
        } catch (Exception e) {
            LOG.error("Failed to insert healthCheckErrorDAO {}", bean.toString(), e);
        }
    }

    /**
     * Step 3. Deploy latest code and run health check script on the newly launched instance
     * If the deploy or health check script fail for a regular health check, disable sclaing down event and send alert message
     */
    private void processPendingVerifyState(HealthCheckBean healthCheckBean, GroupBean groupBean) throws Exception {
        String groupName = groupBean.getGroup_name();
        LOG.info("Start to deploy and run health check scripts for group {} and healthCheck id {} at health check state {}",
            groupName, healthCheckBean.getId(), healthCheckBean.getState().toString());
        String hostId = healthCheckBean.getHost_id();
        List<AgentBean> agents = agentDAO.getByHostId(hostId);
        if (agents.isEmpty()) {
            LOG.info("Host {} has not ping server yet", hostId);
            return;
        }

        boolean succeeded = true;
        for (AgentBean agent : agents) {
            LOG.info("Health Check Agent info {}", agent.toString());
            if (agent.getEnv_id().equals(healthCheckBean.getEnv_id())) {
                healthCheckBean.setDeploy_start_time(agent.getStart_date());
            }

            if (agent.getDeploy_stage() != DeployStage.SERVING_BUILD) {
                succeeded = false;
                if (agent.getStatus() != AgentStatus.SUCCEEDED && agent.getStatus() != AgentStatus.UNKNOWN && agent.getStatus() != AgentStatus.SCRIPT_FAILED) {
                    LOG.error("Deploy/Health Check Script failed for group {}", groupName);
                    // For both TIME and MANUALLY triggered heahth check, disable scaling down
                    if (healthCheckBean.getType() != HealthCheckType.AMI_TRIGGERED) {
                        try {
                            LOG.info("Disable scaling down event for group {}", groupName);
                            if (!autoScalingManager.isScalingDownEventEnabled(groupName)) {
                                LOG.info("The asg scaling down event has been disabled for group {}", groupName);
                            } else {
                                autoScalingManager.disableScalingDownEvent(groupName);
                            }
                        } catch (Exception e) {
                            LOG.error("Failed to disable scaling down event for group {}", groupName, e);
                        }
                    }
                    AgentErrorBean agentErrorBean = agentErrorDAO.getByHostIdAndEnvId(hostId, agent.getEnv_id());
                    updateHealthCheckError(healthCheckBean.getId(), agent, agentErrorBean);

                    String subject = String.format("Health Check Alert - Deploy/Health Check Script Failed in group <%s>", groupName);
                    String errorMessage = String.format("Deploy/Health Check Script failed (AMI ID: %s, Deploy ID: %s) in group %s",
                        healthCheckBean.getAmi_id(), healthCheckBean.getDeploy_id(), groupName);
                    failedHealthCheckAlertJob(healthCheckBean, groupBean, subject, errorMessage);
                    return;
                }
            }

            if (agent.getFirst_deploy_time() != null && agent.getEnv_id().equals(healthCheckBean.getEnv_id())) {
                healthCheckBean.setDeploy_complete_time(agent.getFirst_deploy_time());
            }
        }

        if (succeeded) {
            transistionState(healthCheckBean, HealthCheckState.COMPLETING, HealthCheckStatus.QUALIFIED, "");
            LOG.info("Health Check Succeeded: id {}, group {}, state {}", healthCheckBean.getId(), groupName, healthCheckBean.getState());
        }
    }

    /**
     * Step 4. Terminate host
     * If health check status is qualified, update launch config, imageDAO and enable scaling down event
     */
    private void processCompletingState(HealthCheckBean healthCheckBean, GroupBean groupBean) throws Exception {
        String groupName = groupBean.getGroup_name();
        LOG.info("Start to terminate instance for group {} and healthCheck id {} at health check state {}",
            groupName, healthCheckBean.getId(), healthCheckBean.getState().toString());

        if (healthCheckBean.getStatus() == HealthCheckStatus.QUALIFIED) {
            // For both TIME and MANUALLY triggered heahth check, enable terminate event
            if (healthCheckBean.getType() != HealthCheckType.AMI_TRIGGERED) {
                try {
                    LOG.info("Start to enable scaling down event for group {}", groupName);
                    if (autoScalingManager.isScalingDownEventEnabled(groupName)) {
                        LOG.info("The asg scaling down event has been enabled for group {}", groupName);
                    } else {
                        // There should not have ongoing regular health checks
                        // Already check it in HealthCheckInserter
                        autoScalingManager.enableScalingDownEvent(groupName);
                    }
                } catch (Exception e) {
                    LOG.error("Failed to enable scaling down event for group {}", groupName, e);
                }
            }

            // For both AMI and MANUALLY triggered heahth check, update asg launch config and imageDAO
            if (healthCheckBean.getType() != HealthCheckType.TIME_TRIGGERED) {
                try {
                    // Make sure the publish date of new image id is newer than current
                    ImageBean newImageBean = imageDAO.getById(healthCheckBean.getAmi_id());
                    ImageBean currImageBean = imageDAO.getById(groupBean.getImage_id());
                    if (newImageBean.getPublish_date() > currImageBean.getPublish_date()) {
                        LOG.info("Update launch config with ami id {} for group {}", healthCheckBean.getAmi_id(), groupName);
                        String lockName = String.format("UPDATEAMI-%s", groupName);
                        Connection connection = utilDAO.getLock(lockName);
                        if (connection != null) {
                            try {
                                GroupBean newBean = new GroupBean();
                                newBean.setImage_id(healthCheckBean.getAmi_id());
                                groupHandler.updateLaunchConfig(groupName, newBean);

                                newImageBean.setQualified(true);
                                imageDAO.insertOrUpdate(newImageBean);
                            } catch (Exception ex) {
                                LOG.error("Failed to upadete launch config with ami id {} from group {}",
                                    healthCheckBean.getAmi_id(), groupName, ex);
                            } finally {
                                utilDAO.releaseLock(lockName, connection);
                            }
                        } else {
                            LOG.warn(String.format("Failed to get lock: %s", lockName));
                        }
                    }
                } catch (Exception e) {
                    LOG.error("Failed to update launch config and imageDAO with ami id {}", healthCheckBean.getAmi_id(), e);
                }
            }
        }

        transistionState(healthCheckBean, HealthCheckState.COMPLETED, null, "");
        LOG.info("Health Check Succeeded: id {}, group {}, state {}, status {}",
            healthCheckBean.getId(), groupName, healthCheckBean.getState(), healthCheckBean.getStatus());
    }

    private void processHealthCheck(HealthCheckBean healthCheckBean) throws Exception {
        GroupBean groupBean = groupHandler.getGroupInfoByClusterName(healthCheckBean.getGroup_name());
        if (shouldTimeoutHealthCheck(healthCheckBean, groupBean)) {
            return;
        }

        if (healthCheckBean.getState() == HealthCheckState.INIT) {
            processInitState(healthCheckBean, groupBean);
        } else if (healthCheckBean.getState() == HealthCheckState.LAUNCHING) {
            processLaunchingState(healthCheckBean, groupBean);
        } else if (healthCheckBean.getState() == HealthCheckState.PENDING_VERIFY) {
            processPendingVerifyState(healthCheckBean, groupBean);
        } else if (healthCheckBean.getState() == HealthCheckState.COMPLETING) {
            processCompletingState(healthCheckBean, groupBean);
        }
    }

    private void processBatch() throws Exception {
        List<HealthCheckBean> healthCheckBeans = healthCheckDAO.getOngoingHealthChecks();
        if (healthCheckBeans.isEmpty()) {
            return;
        }

        Collections.shuffle(healthCheckBeans);
        for (HealthCheckBean bean : healthCheckBeans) {
            LOG.info("Start to process health check {} ", bean.toString());
            String lockName = String.format("HEALTHCHECK-%s", bean.getId());
            Connection connection = utilDAO.getLock(lockName);
            if (connection != null) {
                try {
                    processHealthCheck(bean);
                } catch (Exception ex) {
                    LOG.error("Failed to process health check {}", bean.toString(), ex);
                } finally {
                    utilDAO.releaseLock(lockName, connection);
                }
            } else {
                LOG.warn(String.format("Failed to get lock: %s", lockName));
            }
        }
    }

    @Override
    public void run() {
        try {
            LOG.info("Start to run HealthChecker");
            processBatch();
        } catch (Throwable t) {
            LOG.error("Failed to run HealthChecker");
        }
    }
}
