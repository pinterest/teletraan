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

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.*;
import com.pinterest.arcee.bean.GroupBean;
import com.pinterest.arcee.dao.GroupInfoDAO;
import com.pinterest.arcee.dao.NewInstanceReportDAO;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.bean.HostState;
import com.pinterest.deployservice.common.LaunchEvent;
import com.pinterest.deployservice.common.LaunchEventParser;
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
    private AmazonSQSClient sqsClient;
    private LaunchEventParser launchEventParser;
    private GroupInfoDAO groupInfoDAO;
    private UtilDAO utilDAO;
    private HostDAO hostDAO;
    private GroupDAO groupDAO;
    private NewInstanceReportDAO newInstanceReportDAO;

    public LaunchEventCollector(TeletraanServiceContext context) {
        sqsClient = new AmazonSQSClient(context.getAwsCredentials());
        sqsClient.setEndpoint(context.getAwsConfigManager().getSqsArn());
        groupInfoDAO = context.getGroupInfoDAO();
        newInstanceReportDAO = context.getNewInstanceReportDAO();
        utilDAO = context.getUtilDAO();
        hostDAO = context.getHostDAO();
        groupDAO = context.getGroupDAO();
        launchEventParser = new LaunchEventParser();
    }

    private boolean updateGroupInfo(LaunchEvent launchEvent) throws Exception {
        String groupName = launchEvent.getGroupName();
        String processLockName = String.format("UPDATE-%s", launchEvent.getGroupName());
        Connection connection = utilDAO.getLock(processLockName);
        if (connection == null) {
            throw new Exception("Failed to obtain db lock for updates");
        }
        try {
            GroupBean groupBean = groupInfoDAO.getGroupInfo(groupName);
            if (groupBean == null) {
                LOG.info(String.format("The group %s information is not in the database yet.", groupName));
                return false;
            }

            if (launchEvent.getEventType().equals("autoscaling:EC2_INSTANCE_LAUNCH")) {
                LOG.debug(String.format("An new instance %s has been launched in group %s", launchEvent.getInstanceId(), launchEvent.getGroupName()));

                // insert new host
                HostBean hostBean = new HostBean();
                // set default host_name equals to instance Id
                hostBean.setHost_name(launchEvent.getInstanceId());
                hostBean.setHost_id(launchEvent.getInstanceId());
                hostBean.setGroup_name(launchEvent.getGroupName());
                hostBean.setState(HostState.PROVISIONED);
                hostBean.setCreate_date(launchEvent.getTimestamp());
                hostBean.setLast_update(launchEvent.getTimestamp());
                hostDAO.insert(hostBean);

                // add to the new instance report
                List<String> envIds = groupDAO.getEnvsByGroupName(groupName);
                LOG.debug(String.format("Adding %d instances report for host %s", envIds.size(), launchEvent.getInstanceId()));
                newInstanceReportDAO.addNewInstanceReport(launchEvent.getInstanceId(), launchEvent.getTimestamp(), envIds);
            } else if (launchEvent.getEventType().equals("autoscaling:EC2_INSTANCE_TERMINATE")) {
                LOG.debug(String.format("An existing instance %s has been terminated in group %s", launchEvent.getInstanceId(), launchEvent.getGroupName()));
                HostBean hostBean = new HostBean();
                hostBean.setState(HostState.TERMINATING);
                hostBean.setLast_update(launchEvent.getTimestamp());
                hostDAO.updateHostById(launchEvent.getInstanceId(), hostBean);
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
        LaunchEvent event = launchEventParser.fromJson(messageBody);
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
