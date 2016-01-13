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

import com.pinterest.arcee.autoscaling.AutoScaleGroupManager;
import com.pinterest.deployservice.bean.ASGStatus;
import com.pinterest.arcee.bean.GroupBean;
import com.pinterest.arcee.common.AutoScalingConstants;
import com.pinterest.arcee.dao.GroupInfoDAO;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.group.HostGroupManager;
import com.pinterest.deployservice.dao.UtilDAO;
import com.pinterest.deployservice.handler.CommonHandler;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;

public class GroupInfoCollector implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(GroupInfoCollector.class);
    private UtilDAO utilDAO;
    private GroupInfoDAO groupInfoDAO;
    private HostGroupManager hostGroupDAO;
    private CommonHandler commonHandler;
    private AutoScaleGroupManager autoScaleGroupManager;

    public GroupInfoCollector(ServiceContext context) {
        utilDAO = context.getUtilDAO();
        groupInfoDAO = context.getGroupInfoDAO();
        hostGroupDAO = context.getHostGroupDAO();
        autoScaleGroupManager = context.getAutoScaleGroupManager();
        commonHandler = new CommonHandler(context);
    }

    private void processSingleGroup(String groupName) {
        LOG.info("Start to process new group {}", groupName);
        String processLockName = String.format("CREATE-%s", groupName);
        GroupBean groupBean = new GroupBean();
        String last_instance_id;
        try {
            last_instance_id = hostGroupDAO.getLastInstanceId(groupName);
        } catch (Exception ex) {
            LOG.error("Failed to collect group  {} information from CDMB", groupName, ex);
            return;
        }

        Connection connection = utilDAO.getLock(processLockName);
        if (connection == null) {
            LOG.warn("Failed to grab CREATE_GROUP_INFO_LOCK for group = {}.", groupName);
            return;
        }
        LOG.info("Successfully get lock on {}", processLockName);
        try {
            GroupBean existingGroupBean = groupInfoDAO.getGroupInfo(groupName);
            if (existingGroupBean != null) {
                LOG.info("Group {} has already been processed.", groupName);
                groupInfoDAO.updateGroupInfo(groupName, groupBean);
            } else {
                String configId;
                String defaultImageId = commonHandler.getDefaultImageId(groupName);
                if (last_instance_id != null) {
                    configId = autoScaleGroupManager.createLaunchConfig(groupName, last_instance_id, defaultImageId);
                } else {
                    // create default launch configuration
                    GroupBean request = new GroupBean();
                    request.setGroup_name(groupName);
                    request.setImage_id(defaultImageId);
                    request.setInstance_type(AutoScalingConstants.DEFAULT_INSTANCE_TYPE);
                    request.setSecurity_group(AutoScalingConstants.DEFAULT_SECURITY_GROUP);
                    request.setLaunch_latency_th(AutoScalingConstants.DEFAULT_LAUNCH_LATENCY_THRESHOLD);
                    request.setIam_role(AutoScalingConstants.DEFAULT_IAM_ROLE);
                    request.setAssign_public_ip(false);
                    // generate default group bean
                    configId = autoScaleGroupManager.createLaunchConfig(request);
                }

                GroupBean configInfo = autoScaleGroupManager.getLaunchConfigByName(configId);
                configInfo.setGroup_name(groupName);
                configInfo.setLast_update(System.currentTimeMillis());
                configInfo.setLaunch_config_id(configId);
                if (!StringUtils.isEmpty(configInfo.getUser_data())) {
                    configInfo.setUser_data(Base64.encodeBase64String(configInfo.getUser_data().getBytes()));
                }
                configInfo.setAsg_status(ASGStatus.UNKNOWN);
                groupInfoDAO.insertGroupInfo(configInfo);
            }
        } catch (Exception e) {
            LOG.error("Failed to process group {}", groupName, e);
        } finally {
            utilDAO.releaseLock(processLockName, connection);
            LOG.info("Successfully released lock on {}", processLockName);
        }
    }

    private void processNewGroups() {
        try {
            List<String> groups = groupInfoDAO.getNewGroupNames();
            if (groups.isEmpty()) {
                LOG.info("GroupInfoCollector did not find any new groups, exiting.");
                return;
            }
            Collections.shuffle(groups);
            for (String group : groups) {
                processSingleGroup(group);
            }
        } catch (Exception ex) {
            LOG.error("Failed to process new groups", ex);
        }
    }

    public void processBatch() {
        // process all groups not in groups table yet
        processNewGroups();
    }

    public void run() {
        try {
            LOG.info("Start GroupInfoCollector process...");
            processBatch();
        } catch (Throwable t) {
            LOG.error("Failed to call GroupInfoCollector", t);
        }
    }

}
