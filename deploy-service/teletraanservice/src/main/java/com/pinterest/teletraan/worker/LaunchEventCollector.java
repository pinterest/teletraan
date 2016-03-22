/*
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

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.*;
import com.pinterest.arcee.autoscaling.AutoScaleGroupManager;
import com.pinterest.arcee.bean.AsgLifecycleEventBean;
import com.pinterest.arcee.bean.GroupBean;
import com.pinterest.arcee.bean.SpotAutoScalingBean;
import com.pinterest.arcee.dao.AsgLifecycleEventDAO;
import com.pinterest.arcee.dao.GroupInfoDAO;
import com.pinterest.arcee.dao.NewInstanceReportDAO;
import com.pinterest.arcee.dao.SpotAutoScalingDAO;
import com.pinterest.deployservice.bean.AgentBean;
import com.pinterest.deployservice.bean.AgentState;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.bean.HostState;
import com.pinterest.deployservice.common.EventMessage;
import com.pinterest.deployservice.common.EventMessageParser;
import com.pinterest.deployservice.dao.AgentDAO;
import com.pinterest.deployservice.dao.GroupDAO;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.dao.UtilDAO;
import com.pinterest.teletraan.TeletraanServiceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;


public class LaunchEventCollector implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(LaunchEventCollector.class);
    private final AgentDAO agentDAO;
    private final AsgLifecycleEventDAO asgLifecycleEventDAO;
    private final GroupDAO groupDAO;
    private final GroupInfoDAO groupInfoDAO;
    private final HostDAO hostDAO;
    private final NewInstanceReportDAO newInstanceReportDAO;
    private final UtilDAO utilDAO;
    private final SpotAutoScalingDAO spotAutoScalingDAO;
    private final EventMessageParser eventMessageParser;
    private final AutoScaleGroupManager autoScaleGroupManager;
    private final AmazonSQSClient sqsClient;

    public LaunchEventCollector(TeletraanServiceContext context) {
        agentDAO = context.getAgentDAO();
        asgLifecycleEventDAO = context.getAsgLifecycleEventDAO();
        groupDAO = context.getGroupDAO();
        groupInfoDAO = context.getGroupInfoDAO();
        hostDAO = context.getHostDAO();
        newInstanceReportDAO = context.getNewInstanceReportDAO();
        utilDAO = context.getUtilDAO();
        spotAutoScalingDAO = context.getSpotAutoScalingDAO();
        eventMessageParser = new EventMessageParser();
        autoScaleGroupManager = context.getAutoScaleGroupManager();
        sqsClient = new AmazonSQSClient(context.getAwsCredentials());
        sqsClient.setEndpoint(context.getAwsConfigManager().getSqsArn());
    }

    private boolean updateGroupInfo(EventMessage eventMessage) throws Exception {
        String groupName = eventMessage.getGroupName();
        SpotAutoScalingBean spotAutoScalingBean = spotAutoScalingDAO.getClusterByAutoScalingGroup(groupName);
        if (spotAutoScalingBean != null) {
            groupName = spotAutoScalingBean.getCluster_name();
        }

        String processLockName = String.format("UPDATE-%s", groupName);
        Connection connection = utilDAO.getLock(processLockName);
        if (connection == null) {
            throw new Exception("Failed to obtain db lock for updates");
        }
        try {
            GroupBean groupBean = groupInfoDAO.getGroupInfo(groupName);
            // TODO ClusterBean clusterBean = clusterDAO.getByClusterName(groupName);
            if (groupBean == null) {
                LOG.info(String.format("The group %s information is not in the database yet.", groupName));
                return false;
            }

            if (eventMessage.getEventType().equals("autoscaling:EC2_INSTANCE_LAUNCH")) {
                LOG.debug(String.format("An new instance %s has been launched in group %s", eventMessage.getInstanceId(), groupName));

                // insert new host
                HostBean hostBean = new HostBean();
                // set default host_name equals to instance Id
                hostBean.setHost_name(eventMessage.getInstanceId());
                hostBean.setHost_id(eventMessage.getInstanceId());
                hostBean.setGroup_name(groupName);
                hostBean.setState(HostState.PROVISIONED);
                hostBean.setCreate_date(eventMessage.getTimestamp());
                hostBean.setLast_update(eventMessage.getTimestamp());
                hostDAO.insert(hostBean);

                // add to the new instance report
                List<String> envIds = groupDAO.getEnvsByGroupName(groupName);
                if (!envIds.isEmpty()) {
                    LOG.debug(String.format("Adding %d instances report for host %s", envIds.size(), eventMessage.getInstanceId()));
                    newInstanceReportDAO.addNewInstanceReport(eventMessage.getInstanceId(), eventMessage.getTimestamp(), envIds);
                }
            } else if (eventMessage.getEventType().equals("autoscaling:EC2_INSTANCE_TERMINATE")) {
                LOG.debug(String.format("An existing instance %s has been terminated in group %s", eventMessage.getInstanceId(), groupName));
                HostBean hostBean = new HostBean();
                hostBean.setState(HostState.TERMINATING);
                hostBean.setLast_update(eventMessage.getTimestamp());
                hostDAO.updateHostById(eventMessage.getInstanceId(), hostBean);
            } else if (eventMessage.getEventType().equals("autoscaling:EC2_INSTANCE_TERMINATING")) {
                // Gracefully shut down services
                String hostId = eventMessage.getInstanceId();
                String hookId = eventMessage.getLifecycleHook();
                LOG.debug(String.format("INSTANCE_TERMINATING: An instance %s is terminating in group %s with hook %s", hostId, groupName, hookId));
                AgentBean agentBean = new AgentBean();
                agentBean.setState(AgentState.STOP);
                agentBean.setLast_update(System.currentTimeMillis());
                agentDAO.updateAgentById(hostId, agentBean);

                AsgLifecycleEventBean bean = new AsgLifecycleEventBean();
                bean.setToken_id(eventMessage.getLifecycleToken());
                bean.setHook_id(hookId);
                bean.setGroup_name(groupName);
                bean.setHost_id(hostId);
                bean.setStart_date(System.currentTimeMillis());
                asgLifecycleEventDAO.insertAsgLifecycleEvent(bean);
            } else if (eventMessage.getEventType().equals("autoscaling:EC2_INSTANCE_LAUNCHING")) {
                // Directly complete the launching lifecycle action
                String hostId = eventMessage.getInstanceId();
                String hookId = eventMessage.getLifecycleHook();
                LOG.debug(String.format("INSTANCE_LAUNCHING: An instance %s is launching in group %s. Complete lifecycle hook %s", hostId, groupName, hookId));
                autoScaleGroupManager.completeLifecycleAction(hookId, eventMessage.getLifecycleToken(), groupName);

                // insert new host
                HostBean hostBean = new HostBean();
                hostBean.setHost_name(hostId);
                hostBean.setHost_id(hostId);
                hostBean.setGroup_name(groupName);
                hostBean.setState(HostState.PROVISIONED);
                hostBean.setCreate_date(eventMessage.getTimestamp());
                hostBean.setLast_update(eventMessage.getTimestamp());
                hostDAO.insert(hostBean);

                // add to the new instance report
                List<String> envIds = groupDAO.getEnvsByGroupName(groupName);
                LOG.debug(String.format("Adding %d instances report for host %s", envIds.size(), eventMessage.getInstanceId()));
                newInstanceReportDAO.addNewInstanceReport(eventMessage.getInstanceId(), eventMessage.getTimestamp(), envIds);
            }
            return true;
        } catch (Exception ex) {
            LOG.error("Failed to update group information.", ex);
            return false;
        } finally {
            utilDAO.releaseLock(processLockName, connection);
        }
    }

    private boolean processMessage(Message message) throws Exception {
        String messageBody = message.getBody();
        EventMessage event = eventMessageParser.fromJson(messageBody);
        if (event == null) {
            return true;
        } else {
            return updateGroupInfo(event);
        }
    }

    public void collectEvents() throws Exception {
        while (true) {
            ReceiveMessageRequest request = new ReceiveMessageRequest();
            request.setMaxNumberOfMessages(10);
            ReceiveMessageResult result = sqsClient.receiveMessage(request);
            List<Message> messageList = result.getMessages();
            if (messageList.isEmpty()) {
                LOG.info("No more Launch activity available at the moment.");
                return;
            }
            LOG.info(String.format("Collect %d events from AWS SQS.", messageList.size()));
            ArrayList<DeleteMessageBatchRequestEntry> entries = new ArrayList<>();
            for (Message message : messageList) {
                try {
                    boolean hasProcessed = processMessage(message);
                    if (hasProcessed) {
                        DeleteMessageBatchRequestEntry entry = new DeleteMessageBatchRequestEntry();
                        entry.setId(message.getMessageId());
                        entry.setReceiptHandle(message.getReceiptHandle());
                        entries.add(entry);
                    }
                } catch (Exception ex) {
                    LOG.error("Failed to process SQS message:", message, ex);
                }
            }

            if (!entries.isEmpty()) {
                DeleteMessageBatchRequest deleteMessageBatchRequest = new DeleteMessageBatchRequest();
                deleteMessageBatchRequest.setEntries(entries);
                LOG.debug(String.format("Successful process %d messages, deleting them from SQS.", entries.size()));
                sqsClient.deleteMessageBatch(deleteMessageBatchRequest);
            }
        }
    }

    @Override
    public void run() {
        try {
            LOG.info("Start AwsSettingsCollector process...");
            collectEvents();
        } catch (Throwable t) {
            LOG.error("Error with AwsSettingsCollector process", t);
        }
    }
}
