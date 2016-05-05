/*
 * Copyright 2016 Pinterest, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pinterest.teletraan.worker;

import com.google.common.base.Joiner;

import com.pinterest.arcee.autoscaling.AutoScalingManager;
import com.pinterest.arcee.dao.HostInfoDAO;
import com.pinterest.clusterservice.bean.AwsVmBean;
import com.pinterest.clusterservice.bean.ClusterBean;
import com.pinterest.clusterservice.bean.ClusterState;
import com.pinterest.clusterservice.bean.ClusterUpgradeEventBean;
import com.pinterest.clusterservice.bean.ClusterUpgradeEventState;
import com.pinterest.clusterservice.bean.ClusterUpgradeEventStatus;
import com.pinterest.clusterservice.cm.AwsVmManager;
import com.pinterest.clusterservice.cm.ClusterManager;
import com.pinterest.clusterservice.dao.ClusterDAO;
import com.pinterest.clusterservice.dao.ClusterUpgradeEventDAO;
import com.pinterest.clusterservice.handler.ClusterHandler;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.AgentBean;
import com.pinterest.deployservice.bean.AgentStatus;
import com.pinterest.deployservice.bean.DeployStage;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.common.NotificationJob;
import com.pinterest.deployservice.dao.AgentDAO;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.dao.UtilDAO;
import com.pinterest.deployservice.handler.CommonHandler;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class ClusterReplacer implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterReplacer.class);
    private static final long DEFAULT_CLUSTER_UPGRADE_EVENT_TIMEOUT = 1800;
    private static final int MAX_HOST_LAUNCH_SIZE = 50;
    private final AgentDAO agentDAO;
    private final ClusterDAO clusterDAO;
    private final ClusterUpgradeEventDAO clusterUpgradeEventDAO;
    private final EnvironDAO environDAO;
    private final HostDAO hostDAO;
    private final HostInfoDAO hostInfoDAO;
    private final UtilDAO utilDAO;
    private final AutoScalingManager autoScalingManager;
    private final ClusterHandler clusterHandler;
    private final ClusterManager clusterManager;
    private final CommonHandler commonHandler;
    private final ExecutorService jobPool;

    public ClusterReplacer(ServiceContext serviceContext) {
        agentDAO = serviceContext.getAgentDAO();
        clusterDAO = serviceContext.getClusterDAO();
        clusterUpgradeEventDAO = serviceContext.getClusterUpgradeEventDAO();
        environDAO = serviceContext.getEnvironDAO();
        hostDAO = serviceContext.getHostDAO();
        hostInfoDAO = serviceContext.getHostInfoDAO();
        utilDAO = serviceContext.getUtilDAO();
        autoScalingManager = serviceContext.getAutoScalingManager();
        clusterHandler = new ClusterHandler(serviceContext);
        clusterManager = new AwsVmManager(serviceContext);
        commonHandler = new CommonHandler(serviceContext);
        jobPool = serviceContext.getJobPool();
    }

    private void transitionState(String id, ClusterUpgradeEventBean updateBean) throws Exception {
        updateBean.setState_start_time(System.currentTimeMillis());
        updateBean.setLast_worked_on(System.currentTimeMillis());
        clusterUpgradeEventDAO.updateById(id, updateBean);
    }

    private void updateClusterState(String clusterName) throws Exception {
        ClusterBean updateBean = new ClusterBean();
        updateBean.setState(ClusterState.NORMAL);
        updateBean.setLast_update(System.currentTimeMillis());
        clusterDAO.update(clusterName, updateBean);
    }

    private void updateHostsInClusterEvent(String id, Collection<String> hostIds) throws Exception {
        ClusterUpgradeEventBean updateBean = new ClusterUpgradeEventBean();
        updateBean.setHost_ids(Joiner.on(",").join(hostIds));
        updateBean.setLast_worked_on(System.currentTimeMillis());
        clusterUpgradeEventDAO.updateById(id, updateBean);
    }

    private boolean shouldTimeoutClusterUpgradeEvent(ClusterUpgradeEventBean eventBean) throws Exception {
        String clusterName = eventBean.getCluster_name();
        long lastStateElapsedTime = System.currentTimeMillis() - eventBean.getLast_worked_on();
        if (lastStateElapsedTime >= DEFAULT_CLUSTER_UPGRADE_EVENT_TIMEOUT * 1000) {
            LOG.info(String.format("Timeout cluster upgrade event id %s for cluster %s", eventBean.getId(), clusterName));
            ClusterUpgradeEventBean updateEventBean = new ClusterUpgradeEventBean();
            if (eventBean.getState() == ClusterUpgradeEventState.COMPLETING) {
                updateEventBean.setState(ClusterUpgradeEventState.COMPLETED);
            } else {
                updateEventBean.setState(ClusterUpgradeEventState.COMPLETING);
            }
            updateEventBean.setStatus(ClusterUpgradeEventStatus.TIMEOUT);
            updateEventBean.setError_message(String.format("Cluster upgrade event timeout at state %s", eventBean.getState().toString()));
            transitionState(eventBean.getId(), updateEventBean);
            updateClusterState(clusterName);

            EnvironBean environBean = environDAO.getById(eventBean.getEnv_id());
            String message = String.format("Cluster upgrade event timeout at state %s for cluster <%s>", eventBean.getState().toString(), clusterName);
            String subject = String.format("Cluster Upgrade Event Alert - Timeout for cluster <%s>", clusterName);
            jobPool.submit(new NotificationJob(message, subject, environBean.getEmail_recipients(), environBean.getChatroom(), commonHandler));
            return true;
        }
        return false;
    }

    /**
     * Step 1. INIT state will launch hosts outside of the auto scaling group
     * The number of hosts to be launched should be max_parallel_rp
     * If launching failed, retry INIT state until timeout meets
     */
    private void processInitState(ClusterUpgradeEventBean eventBean) throws Exception {
        String clusterName = eventBean.getCluster_name();
        EnvironBean environBean = environDAO.getById(eventBean.getEnv_id());
        int totToLaunch = environBean.getMax_parallel_rp() <= 0 ? 1 : environBean.getMax_parallel_rp();
        if (!StringUtils.isEmpty(eventBean.getHost_ids())) {
            Collection<String> oldHostIds = Arrays.asList(eventBean.getHost_ids().split(","));
            totToLaunch -= oldHostIds.size();
        }

        boolean succeeded = true;
        while (totToLaunch > 0) {
            int numToLaunch = Math.min(totToLaunch, MAX_HOST_LAUNCH_SIZE);
            Collection<String> newHostIds = clusterManager.launchHosts(clusterName, numToLaunch, false);
            if (newHostIds.isEmpty()) {
                LOG.error(String.format("Failed to launch %s hosts in INIT state", numToLaunch));
                succeeded = false;
                break;
            }

            LOG.info(String.format("Successfully launched %d hosts (current replacement count %d): %s", newHostIds.size(), totToLaunch, newHostIds.toString()));
            Collection<String> updateHostIds = new ArrayList<>(newHostIds);
            if (!StringUtils.isEmpty(eventBean.getHost_ids())) {
                Collection<String> oldHostIds = Arrays.asList(eventBean.getHost_ids().split(","));
                updateHostIds.addAll(oldHostIds);
            }

            updateHostsInClusterEvent(eventBean.getId(), updateHostIds);
            totToLaunch -= newHostIds.size();
        }

        if (succeeded) {
            LOG.info("Successfully completed INIT state, move to LAUNCHING state");
            ClusterUpgradeEventBean updateBean = new ClusterUpgradeEventBean();
            updateBean.setState(ClusterUpgradeEventState.LAUNCHING);
            updateBean.setStatus(ClusterUpgradeEventStatus.SUCCEEDED);
            transitionState(eventBean.getId(), updateBean);
        }
    }

    /**
     * Step 2. LAUNCHING state should make sure all the host is in RUNNING state and serving builds
     * If some hosts are terminated or deploy failed, go back to INIT state to relaunch
     */
    private void processLaunchingState(ClusterUpgradeEventBean eventBean) throws Exception {
        String clusterName = eventBean.getCluster_name();
        Collection<String> hostIds = Arrays.asList(eventBean.getHost_ids().split(","));

        // First, make sure every host is running
        Set<String> failedIds = hostInfoDAO.getTerminatedHosts(new HashSet<>(hostIds));
        List<String> runningIds = hostInfoDAO.getRunningInstances(new ArrayList<>(hostIds));

        // Second, make sure hosts are serving builds
        boolean succeeded = true;
        for (String hostId : runningIds) {
            List<AgentBean> agents = agentDAO.getByHostId(hostId);
            if (agents.isEmpty()) {
                LOG.info(String.format("Host %s has not ping server yet", hostId));
                succeeded = false;
                continue;
            }

            // Make sure every env on the host are serving build
            for (AgentBean agent : agents) {
                if (agent.getDeploy_stage() != DeployStage.SERVING_BUILD) {
                    succeeded = false;
                    if (agent.getStatus() != AgentStatus.SUCCEEDED && agent.getStatus() != AgentStatus.UNKNOWN && agent.getStatus() != AgentStatus.SCRIPT_FAILED) {
                        LOG.info(String.format("Deploy failed on host %s", hostId));
                        failedIds.add(hostId);
                    }
                }
            }
        }

        // Third, if found failed hosts, terminate them and go back to INIT state to relaunch hosts
        if (!failedIds.isEmpty()) {
            succeeded = false;
            Collection<String> updateHostIds = Arrays.asList(eventBean.getHost_ids().split(","));
            updateHostIds.removeAll(failedIds);
            clusterManager.terminateHosts(clusterName, failedIds, true);

            ClusterUpgradeEventBean updateBean = new ClusterUpgradeEventBean();
            updateBean.setHost_ids(Joiner.on(",").join(updateHostIds));
            updateBean.setState(ClusterUpgradeEventState.INIT);
            updateBean.setStatus(ClusterUpgradeEventStatus.SUCCEEDED);
            transitionState(eventBean.getId(), updateBean);
        }

        if (succeeded) {
            LOG.info("Successfully completed LAUNCHING state, move to REPLACING state");
            ClusterUpgradeEventBean updateBean = new ClusterUpgradeEventBean();
            updateBean.setState(ClusterUpgradeEventState.REPLACING);
            updateBean.setStatus(ClusterUpgradeEventStatus.SUCCEEDED);
            transitionState(eventBean.getId(), updateBean);
        }
    }

    /**
     * Step 3. REPLACING state will guarantee that total agent count must be larger than number of hosts
     * in auto scaling group. If yes, stop the failed and can_retire host first and then stop the can_retire host
     * until there is no more can_retire host
     */
    private void processReplacingState(ClusterUpgradeEventBean eventBean) throws Exception {
        String clusterName = eventBean.getCluster_name();
        EnvironBean envBean = environDAO.getById(eventBean.getEnv_id());
        String envName = envBean.getEnv_name();
        String stageName = envBean.getStage_name();
        List<String> retiredHosts = new ArrayList<>(hostDAO.getRetiredHostIdsByGroup(clusterName));
        if (retiredHosts.isEmpty()) {
            LOG.info("Successfully completed REPLACING state, move to COMPLETING state");
            ClusterUpgradeEventBean updateBean = new ClusterUpgradeEventBean();
            updateBean.setState(ClusterUpgradeEventState.COMPLETING);
            updateBean.setStatus(ClusterUpgradeEventStatus.SUCCEEDED);
            transitionState(eventBean.getId(), updateBean);
        } else {
            AwsVmBean awsVmBean = autoScalingManager.getAutoScalingGroupInfo(clusterName);
            int curAsgCapacity = awsVmBean.getCurSize();
            // Total agent count = auto scaling capacity + non asg hosts count
            long servingAgentCnt = agentDAO.countServingTotal(envBean.getEnv_id());
            if (servingAgentCnt <= curAsgCapacity) {
                LOG.debug(String.format("Wait for enough serving agents for %s/%s", envName, stageName));
                return;
            }

            long stopCnt = servingAgentCnt - curAsgCapacity;
            // Stop failed and can_retire agents first
            List<String> retiredAndFailedHosts = new ArrayList<>(hostDAO.getRetiredAndFailedHostIdsByGroup(clusterName));
            if (!retiredAndFailedHosts.isEmpty()) {
                int numToStop = Math.min(retiredAndFailedHosts.size(), (int)stopCnt);
                clusterHandler.stopHosts(envName, stageName, retiredAndFailedHosts.subList(0, numToStop));
                LOG.info(String.format("Successfully stopped hosts %s", retiredAndFailedHosts.subList(0, numToStop)));
                stopCnt -= numToStop;
            }

            if (stopCnt <= 0) {
                ClusterUpgradeEventBean updateBean = new ClusterUpgradeEventBean();
                updateBean.setLast_worked_on(System.currentTimeMillis());
                clusterUpgradeEventDAO.updateById(eventBean.getId(), updateBean);
                return;
            }

            // Stop can_retire agents
            retiredHosts.removeAll(retiredAndFailedHosts);
            if (!retiredHosts.isEmpty()) {
                int numToStop = Math.min(retiredHosts.size(), (int)stopCnt);
                clusterHandler.stopHosts(envName, stageName, retiredHosts.subList(0, numToStop));
                LOG.info(String.format("Successfully stopped hosts %s", retiredHosts.subList(0, numToStop)));
            }
            ClusterUpgradeEventBean updateBean = new ClusterUpgradeEventBean();
            updateBean.setLast_worked_on(System.currentTimeMillis());
            clusterUpgradeEventDAO.updateById(eventBean.getId(), updateBean);
        }
    }

    /**
     * Step 4. COMPLETING state should clean up hosts launched in INIT state
     * If found failed deploy asg hosts, use non-asg host to replace it.
     * No more launch activity in this state
     * If previous state failed/timeout/abort, should come to this state to do final clean up
     */
    private void processCompletingState(ClusterUpgradeEventBean eventBean) throws Exception {
        String clusterName = eventBean.getCluster_name();
        AwsVmBean awsVmBean = autoScalingManager.getAutoScalingGroupInfo(clusterName);
        long totAgentCount = agentDAO.countAgentByEnv(eventBean.getEnv_id());
        // Total agent count = auto scaling capacity + non asg hosts count
        if (totAgentCount <= awsVmBean.getCurSize() || StringUtils.isEmpty(eventBean.getHost_ids())) {
            LOG.info("Successfully completed COMPLETING state, move to COMPLETED state");
            ClusterUpgradeEventBean updateBean = new ClusterUpgradeEventBean();
            // Do not set status, leave it
            updateBean.setState(ClusterUpgradeEventState.COMPLETED);
            transitionState(eventBean.getId(), updateBean);
            updateClusterState(clusterName);
            return;
        }

        List<String> nonAsgHosts = Arrays.asList(eventBean.getHost_ids().split(","));
        List<String> activeNonAsgHosts = hostInfoDAO.getRunningInstances(new ArrayList<>(nonAsgHosts));
        if (activeNonAsgHosts.size() != activeNonAsgHosts.size()) {
            updateHostsInClusterEvent(eventBean.getId(), activeNonAsgHosts);
        }

        Collection<String> failedHosts = hostDAO.getFailedHostIdsByGroup(clusterName);
        List<String> failedDeployNonAsgHosts = new ArrayList<>();
        List<String> failedDeployAsgHosts = new ArrayList<>();
        for (String failedHost : failedHosts) {
            if (activeNonAsgHosts.contains(failedHost)) {
                failedDeployNonAsgHosts.add(failedHost);
            } else {
                failedDeployAsgHosts.add(failedHost);
            }
        }

        if (!failedDeployNonAsgHosts.isEmpty()) {
            activeNonAsgHosts.removeAll(failedDeployNonAsgHosts);
            clusterManager.terminateHosts(clusterName, failedDeployAsgHosts, true);
            LOG.info(String.format("Successfully terminated failed deploy non-asg hosts for cluster %s: %s", clusterName, failedDeployAsgHosts.toString()));
        }

        if (!failedDeployAsgHosts.isEmpty()) {
            int num = Math.min(failedDeployAsgHosts.size(), activeNonAsgHosts.size());
            List<String> hostsToAttach = new ArrayList<>(activeNonAsgHosts.subList(0, num));
            autoScalingManager.addInstancesToAutoScalingGroup(hostsToAttach, clusterName);
            activeNonAsgHosts.removeAll(hostsToAttach);
            LOG.info(String.format("Successfully attached %d hosts to cluster %s: %s", num, clusterName, hostsToAttach.toString()));

            List<String> hostsToTerminate = new ArrayList<>(failedDeployAsgHosts.subList(0, num));
            clusterManager.terminateHosts(clusterName, hostsToTerminate, false);
            LOG.info(String.format("Successfully terminated %d hosts in cluster %s: %s", num, clusterName, hostsToTerminate.toString()));
        }

        if (!activeNonAsgHosts.isEmpty()) {
            clusterManager.terminateHosts(clusterName, activeNonAsgHosts, true);
            LOG.info(String.format("Successfully clean up remaining hosts for cluster %s: %s", clusterName, activeNonAsgHosts.toString()));
        }

        LOG.info("Successfully completed COMPLETING state, move to COMPLETED state");
        ClusterUpgradeEventBean updateBean = new ClusterUpgradeEventBean();
        updateBean.setHost_ids("");
        // Do not set status, leave it
        updateBean.setState(ClusterUpgradeEventState.COMPLETED);
        updateBean.setLast_worked_on(System.currentTimeMillis());
        transitionState(eventBean.getId(), updateBean);
        updateClusterState(clusterName);
    }

    private void processEvent(ClusterUpgradeEventBean eventBean) throws Exception {
        String clusterName = eventBean.getCluster_name();
        ClusterBean clusterBean = clusterDAO.getByClusterName(clusterName);
        if (clusterBean.getState() == ClusterState.PAUSE) {
            LOG.info(String.format("Cluster upgrade event is paused for %s", clusterName));
            ClusterUpgradeEventBean updateBean = new ClusterUpgradeEventBean();
            updateBean.setLast_worked_on(System.currentTimeMillis());
            clusterUpgradeEventDAO.updateById(eventBean.getId(), updateBean);
            return;
        }

        if (shouldTimeoutClusterUpgradeEvent(eventBean)) {
            return;
        }

        LOG.info(String.format("Start to process %s state for cluster %s: %s", eventBean.getState().toString(), clusterName, eventBean.toString()));
        if (eventBean.getState() == ClusterUpgradeEventState.INIT) {
            processInitState(eventBean);
        } else  if (eventBean.getState() == ClusterUpgradeEventState.LAUNCHING) {
            processLaunchingState(eventBean);
        } else  if (eventBean.getState() == ClusterUpgradeEventState.REPLACING) {
            processReplacingState(eventBean);
        } else if (eventBean.getState() == ClusterUpgradeEventState.COMPLETING) {
            processCompletingState(eventBean);
        }
    }

    private void processBatch() throws Exception {
        Collection<ClusterUpgradeEventBean> eventBeans = clusterUpgradeEventDAO.getOngoingEvents();
        if (eventBeans.isEmpty()) {
            return;
        }

        for (ClusterUpgradeEventBean eventBean : eventBeans) {
            LOG.info(String.format("Start to process cluster upgrade event %s", eventBean.toString()));
            String lockName = String.format("CLUSTERREPLACER-%s", eventBean.getId());
            Connection connection = utilDAO.getLock(lockName);
            if (connection != null) {
                try {
                    processEvent(eventBean);
                } catch (Exception e) {
                    LOG.error(String.format("Failed to process event %s", eventBean.toString()), e);
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
            LOG.info("Start to run ClusterReplacer");
            processBatch();
        } catch (Throwable t) {
            LOG.error("Faile to run ClusterReplacer", t);
        }
    }

}
