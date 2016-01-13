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
package com.pinterest.arcee.handler;

import com.pinterest.arcee.autoscaling.AutoScaleGroupManager;
import com.pinterest.deployservice.bean.ASGStatus;
import com.pinterest.arcee.bean.GroupBean;
import com.pinterest.arcee.dao.GroupInfoDAO;
import com.pinterest.arcee.dao.HostInfoDAO;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.bean.HostState;
import com.pinterest.deployservice.common.NotificationJob;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.dao.UtilDAO;
import com.pinterest.deployservice.handler.CommonHandler;
import com.pinterest.deployservice.handler.ConfigHistoryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.*;
import java.util.concurrent.ExecutorService;


/**
 * Launch/Terminate instances handler
 */
public class ProvisionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ProvisionHandler.class);
    private CommonHandler commonHandler;
    private ConfigHistoryHandler configHistoryHandler;
    private HostDAO hostDAO;
    private HostInfoDAO hostInfoDAO;
    private GroupInfoDAO groupInfoDAO;
    private AutoScaleGroupManager asgDAO;
    private UtilDAO utilDAO;
    private ExecutorService jobPool;

    public ProvisionHandler(ServiceContext serviceContext) {
        commonHandler = new CommonHandler(serviceContext);
        configHistoryHandler = new ConfigHistoryHandler(serviceContext);
        hostDAO = serviceContext.getHostDAO();
        hostInfoDAO = serviceContext.getHostInfoDAO();
        groupInfoDAO = serviceContext.getGroupInfoDAO();
        asgDAO = serviceContext.getAutoScaleGroupManager();
        utilDAO = serviceContext.getUtilDAO();
        jobPool = serviceContext.getJobPool();
    }

    public void terminateHost(String hostId, boolean decreaseSize, String operator) throws Exception {
        LOG.info(String.format("Start to terminate the host %s, decrease size %b", hostId, decreaseSize));
        boolean terminateSucceeded = true;
        String lockName = String.format("PROCESS-HOSTID-%s", hostId);
        Connection connection = utilDAO.getLock(lockName);
        if (connection != null) {
            try {
                Collection<String> asgIds = asgDAO.instancesInAutoScalingGroup(Arrays.asList(hostId));
                if (decreaseSize && !asgIds.isEmpty()) {
                    try {
                        LOG.info(String.format("Terminate the host %s and decrease fleet size in auto scaling group", hostId));
                        asgDAO.terminateInstanceInAutoScalingGroup(hostId, decreaseSize);
                    } catch (Exception ex) {
                        LOG.error("Failed to terminate instance in autoscaling group", ex);
                    }
                } else {
                    LOG.info(String.format("Terminate the host %s", hostId));
                    hostInfoDAO.terminateHost(hostId);
                }

                long current_time = System.currentTimeMillis();
                HostBean hostBean = new HostBean();
                hostBean.setState(HostState.TERMINATING);
                hostBean.setLast_update(current_time);
                hostDAO.updateHostById(hostId, hostBean);
            } catch (Exception e) {
                terminateSucceeded = false;
                LOG.error("Failed to terminate host {}", hostId, e);
            } finally {
                utilDAO.releaseLock(lockName, connection);
            }
        } else {
            terminateSucceeded = false;
            LOG.warn(String.format("Failed to get lock: %s", lockName));
        }

        if (!terminateSucceeded) {
            return;
        }

        // Send notifications & save changes in config history
        List<HostBean> hosts = hostDAO.getHostsByHostId(hostId);
        for (HostBean host : hosts) {
            String groupName = host.getGroup_name();
            GroupBean group = groupInfoDAO.getGroupInfo(groupName);
            if (group != null) {
                String subject = String.format("Host Termination - %s/%s has been teriminated", host.getHost_name(), hostId);
                String message = String.format("A host %s/%s has been manually terminated by operator %s.", host.getHost_name(), hostId, operator);
                if (decreaseSize) {
                    message += String.format(" Decreased fleet size for Auto Scaling Group %s.", groupName);
                }
                jobPool.submit(new NotificationJob(message, subject, group.getEmail_recipients(), group.getChatroom(), commonHandler));

                String configChange = String.format("Instance Id : %s", hostId);
                configHistoryHandler.updateConfigHistory(groupName, configHistoryHandler.getTypeHostTerminate(), configChange, operator);
            }
        }
    }

    public List<String> launchNewInstances(String groupName, int instanceCnt, String subnet, String operator) throws Exception {
        GroupBean groupBean = groupInfoDAO.getGroupInfo(groupName);
        if (groupBean != null) {
            if (groupBean.getAsg_status() == ASGStatus.ENABLED) {  // AutoScaling is enabled, increase the ASG capacity
                LOG.info(String.format("Launch %d EC2 instances in AutoScalingGroup %s", instanceCnt, groupName));
                try {
                    asgDAO.increaseASGDesiredCapacityBySize(groupName, instanceCnt);
                } catch (Exception ex) {
                    LOG.error("Failed to increase desired capacity for auto scaling group {}.", groupName, ex);
                }

                String subject = String.format("Host Launch for AutoScalingGroup \"%s\"", groupName);
                String message = String.format("%d hosts have been manually launched to group %s by operator %s.", instanceCnt, groupName, operator);
                jobPool.submit(new NotificationJob(message, subject, groupBean.getEmail_recipients(), groupBean.getChatroom(), commonHandler));

                String configChange = String.format("ASG desired capacity increased by %d", instanceCnt);
                configHistoryHandler.updateConfigHistory(groupName, configHistoryHandler.getTypeHostLaunch(), configChange, operator);

                String hostId = String.format("Please check the group %s page.", groupName);
                return Collections.singletonList(hostId);
            } else if (groupBean.getAsg_status() == ASGStatus.DISABLED) { // AutoScaling is disabled, do nothing
                LOG.info(String.format("AutoScalingGroup %s is disabled. Prohibit from launching instances", groupName));
                return new ArrayList<>();
            } else {   // No ASG for this group, directly launch EC2 instances
                LOG.info(String.format("Launch %d EC2 instances in group %s and subnet %s", instanceCnt, groupName, subnet));
                List<HostBean> hosts = hostInfoDAO.launchEC2Instances(groupBean, instanceCnt, subnet);
                List<String> hostIds = new ArrayList<>();
                for (HostBean host : hosts) {
                    try {
                        LOG.debug(String.format("An new instance %s has been launched in group %s", host.getHost_id(), groupName));
                        hostIds.add(host.getHost_id());
                        hostDAO.insert(host);
                    } catch (Exception e) {
                        LOG.error("Failed to insert host {} into hosts", host.getHost_id(), e);
                    }
                }

                String subject = String.format("Host Launch for group \"%s\"", groupName);
                String message = String.format("Hosts %s have been launched in group %s by operator %s.", hostIds, groupName, operator);
                jobPool.submit(new NotificationJob(message, subject, groupBean.getEmail_recipients(), groupBean.getChatroom(), commonHandler));

                String configChange = String.format("%s launched", hostIds);
                configHistoryHandler.updateConfigHistory(groupName, configHistoryHandler.getTypeHostLaunch(), configChange, operator);

                return hostIds;
            }
        }
        LOG.debug(String.format("Failed to launch %d new instances in non-existing group %s", instanceCnt, groupName));
        return new ArrayList<>();
    }
}
