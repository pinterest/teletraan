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

import com.google.api.client.repackaged.com.google.common.base.Joiner;
import com.pinterest.arcee.bean.GroupBean;
import com.pinterest.arcee.bean.NewInstanceReportBean;
import com.pinterest.arcee.dao.GroupInfoDAO;
import com.pinterest.arcee.dao.NewInstanceReportDAO;
import com.pinterest.arcee.metrics.MetricSource;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.AgentBean;
import com.pinterest.deployservice.bean.AgentStatus;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.common.NotificationJob;
import com.pinterest.deployservice.dao.*;

import com.pinterest.deployservice.handler.CommonHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class LaunchLatencyUpdater implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(LaunchLatencyUpdater.class);
    private static final String COUNTER_NAME_LAUNCH_LATENCY = "autoscaling.%s.%s.launchlatency";
    private static final String COUNTER_NAME_DEPLOY_LATENCY = "autoscaling.%s.%s.deploylatency";
    private static final String TOTAL_INSTANCE_LAUNCH_COUNT = "autoscaling.%s.launch.total";
    private static final String EXCEED_THRESHOLD_LAUNCH_COUNT = "autoscaling.%s.launch.exceed_latency";

    private EnvironDAO environDAO;
    private GroupDAO groupDAO;
    private UtilDAO utilDAO;
    private AgentDAO agentDAO;
    private GroupInfoDAO groupInfoDAO;
    private NewInstanceReportDAO newInstanceReportDAO;
    private HostDAO hostDAO;
    private CommonHandler commonHandler;
    private ExecutorService jobPool;
    private MetricSource metricSource;
    private HashMap<String, String> tags;

    private class SendMetricsJob implements Callable {
        private AgentBean agent;
        private EnvironBean environ;
        private GroupBean group;
        private Long launchTime;

        public SendMetricsJob(AgentBean agentBean, EnvironBean environBean, GroupBean group, Long launchTime) {
            this.agent = agentBean;
            this.environ = environBean;
            this.launchTime = launchTime;
            this.group = group;
        }

        public Void call() {
            try {
                long serveTrafficTime = agent.getFirst_deploy_time();
                double launch_latency = serveTrafficTime - launchTime;
                double deploy_latency = serveTrafficTime - agent.getStart_date();
                LOG.debug(String.format("Instance %s launched at: %d, started to deploy at: %d, start to serve traffic at: %d, launch latency is: %f seconds, deploy launtency is:%f seconds",
                        agent.getHost_id(), launchTime, agent.getStart_date(), serveTrafficTime, launch_latency, deploy_latency));
                metricSource.export(String.format(COUNTER_NAME_LAUNCH_LATENCY, environ.getEnv_name(), environ.getStage_name()), tags, launch_latency, System.currentTimeMillis());
                metricSource.export(String.format(COUNTER_NAME_DEPLOY_LATENCY, environ.getEnv_name(), environ.getStage_name()), tags, deploy_latency, System.currentTimeMillis());
                metricSource.export(String.format(TOTAL_INSTANCE_LAUNCH_COUNT, group.getGroup_name()), tags, (double) 1, System.currentTimeMillis());
                if (launch_latency >= group.getLaunch_latency_th() * 1000) {
                    metricSource.export(String.format(EXCEED_THRESHOLD_LAUNCH_COUNT, group.getGroup_name()), tags, (double) 1, System.currentTimeMillis());
                }
            } catch (Exception ex) {
                LOG.error("Failed to send metrics to the tsd.", ex);
            } finally {
                return null;
            }
        }
    }

    public LaunchLatencyUpdater(ServiceContext context) {
        environDAO = context.getEnvironDAO();
        utilDAO = context.getUtilDAO();
        groupDAO = context.getGroupDAO();
        groupInfoDAO = context.getGroupInfoDAO();
        agentDAO = context.getAgentDAO();
        newInstanceReportDAO = context.getNewInstanceReportDAO();
        hostDAO = context.getHostDAO();
        commonHandler = new CommonHandler(context);
        metricSource = context.getMetricSource();
        jobPool = context.getJobPool();
        tags = new HashMap<>();
        try {
          tags.put("host", InetAddress.getLocalHost().getHostName());
        } catch (Exception t) {
          LOG.error("Failed to get local host name");
        }
    }

    private void processNewInstancesReport(String envId) throws Exception {
        EnvironBean envBean = environDAO.getById(envId);
        List<String> groupNames = groupDAO.getCapacityGroups(envId);
        if (groupNames.isEmpty()) {
            return;
        }
        String groupName = groupNames.get(0);
        GroupBean groupBean = groupInfoDAO.getGroupInfo(groupName);
        if (groupBean == null) {
            LOG.info(String.format("Group %s does not exist. skip.", groupName));
            return;
        }

        Integer launchTimeThreshold = groupBean.getLaunch_latency_th() * 1000;
        List<String> hostIds = newInstanceReportDAO.getNewInstanceIdsByEnv(envId);

        ArrayList<String> failingIds = new ArrayList<>();
        ArrayList<String> overtimeIds = new ArrayList<>();

        for (String hostId : hostIds) {
            String lockName = String.format("LAUNCHLATENCY-%s-%s", envId, hostId);
            Connection connection = utilDAO.getLock(lockName);
            if (connection == null) {
                LOG.info(String.format("Other thread is processing %s for %s", hostId, envId));
                continue;
            }
            try {
                NewInstanceReportBean newInstanceReportBean = newInstanceReportDAO.getByIds(hostId, envId);
                if (newInstanceReportBean == null) {
                    LOG.info(String.format("Instance report does not exist for %s. Skipping", hostId));
                    continue;
                }

                Long currentTime = System.currentTimeMillis();
                Long launchTime = newInstanceReportBean.getLaunch_time();
                AgentBean agentBean = agentDAO.getByHostEnvIds(hostId, envId);
                List<HostBean> hosts = hostDAO.getHostsByHostId(hostId);

                if (agentBean == null && hosts.isEmpty()) {
                    // case 0.1 if the host has already been terminated
                    newInstanceReportDAO.deleteNewInstanceReport(hostId, envId);
                } else if (agentBean == null) {
                    // case 1, host launched, but deploy agent has not started on the host yet.
                    LOG.info(String.format("Agents for host id: %s, env id: %s does not exist.",
                                           hostId, envId));
                    if (currentTime - launchTime > launchTimeThreshold && !newInstanceReportBean
                        .getReported()) {
                        overtimeIds.add(hostId);
                        newInstanceReportDAO.reportNewInstances(hostId, envId);
                    }
                } else if (agentBean.getFirst_deploy_time() == null) { // case 2. both host launched, and deploy agent is running, and  still in first deploy
                    if (newInstanceReportBean.getReported()) {
                        LOG.info(String.format("Instance %s has already been reported. Skipping.", newInstanceReportBean.getHost_id()));
                        continue;
                    }

                    if (agentBean.getStatus() != AgentStatus.SUCCEEDED && agentBean.getStatus() != AgentStatus.UNKNOWN && agentBean.getStatus() != AgentStatus.SCRIPT_FAILED) {
                        failingIds.add(hostId);
                        newInstanceReportDAO.reportNewInstances(hostId, envId);
                    } else if (currentTime - launchTime > launchTimeThreshold) {
                        overtimeIds.add(hostId);
                        newInstanceReportDAO.reportNewInstances(hostId, envId);
                    }
                } else { // case 3. Finished first deploy.
                    // send metrics to the open tsdb
                    jobPool.submit(new SendMetricsJob(agentBean, envBean, groupBean, newInstanceReportBean.getLaunch_time()));
                    // finish report new instance. Delete it
                    newInstanceReportDAO.deleteNewInstanceReport(hostId, envId);
                }
            } catch (Exception ex) {
                LOG.error(String.format("Failed to process host %s", hostId), ex);
            } finally {
                utilDAO.releaseLock(lockName, connection);
            }
        }

        if (failingIds.isEmpty() && overtimeIds.isEmpty()) {
            return;
        }

        String subject = String.format("Autoscaling -  Group <%s> instance launch warning", groupName);
        StringBuilder message = new StringBuilder();
        if (!overtimeIds.isEmpty()) {
            message.append(String.format("There are total %d instances: %s in group <%s> have not started to serve traffic for more than %d seconds since they were launched. \n" +
                            "It could be caused by cloud-init or failing deploy. Please take actions\n", overtimeIds.size(),
                    Joiner.on(", ").join(overtimeIds), groupName, groupBean.getLaunch_latency_th()));
        }

        if (!failingIds.isEmpty()) {
            message.append(String.format("Instances %s in group <%s> failed to launch.", Joiner.on(", ").join(failingIds), groupName));
        }
        jobPool.submit(new NotificationJob(message.toString(), subject, groupBean.getEmail_recipients(), groupBean.getChatroom(), commonHandler));
    }

    public void processBatch() throws Exception {
        List<String> envIds = environDAO.getAllEnvIds();
        Collections.shuffle(envIds);
        for (String envId : envIds) {
            try {
                LOG.info("Start to send launch latency metrics to tsd for envId: {}", envId);
                processNewInstancesReport(envId);
            } catch (Exception ex) {
                LOG.error("Failed to do launch latency metrics to tsd for envId {}", envId, ex);
            }
        }
    }

    @Override
    public void run() {
      try {
        LOG.info("Start to run LaunchLatencyUpdater.");
        processBatch();
      } catch (Throwable t) {
        LOG.error("Faile to run LaunchLatencyUpdater", t);
      }
    }
}
