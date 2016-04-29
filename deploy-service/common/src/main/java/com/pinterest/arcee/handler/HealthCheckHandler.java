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


import com.pinterest.arcee.bean.GroupBean;
import com.pinterest.arcee.bean.HealthCheckBean;
import com.pinterest.arcee.bean.HealthCheckErrorBean;
import com.pinterest.arcee.bean.HealthCheckState;
import com.pinterest.arcee.bean.HealthCheckStatus;
import com.pinterest.arcee.bean.HealthCheckType;
import com.pinterest.arcee.bean.ImageBean;
import com.pinterest.arcee.dao.GroupInfoDAO;
import com.pinterest.arcee.dao.HealthCheckDAO;
import com.pinterest.arcee.dao.HealthCheckErrorDAO;
import com.pinterest.arcee.dao.ImageDAO;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.common.CommonUtils;
import com.pinterest.deployservice.dao.EnvironDAO;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class HealthCheckHandler {
    private static final Logger LOG = LoggerFactory.getLogger(HealthCheckHandler.class);
    private HealthCheckDAO healthCheckDAO;
    private HealthCheckErrorDAO healthCheckErrorDAO;
    private GroupInfoDAO groupInfoDAO;
    private EnvironDAO environDAO;
    private ImageDAO imageDAO;
    private GroupHandler groupHandler;

    public HealthCheckHandler(ServiceContext serviceContext) {
        healthCheckDAO = serviceContext.getHealthCheckDAO();
        healthCheckErrorDAO = serviceContext.getHealthCheckErrorDAO();
        groupInfoDAO = serviceContext.getGroupInfoDAO();
        environDAO = serviceContext.getEnvironDAO();
        imageDAO = serviceContext.getImageDAO();
        groupHandler = new GroupHandler(serviceContext);
    }

    String addNewHealthCheckRecord(String groupName, String envId, String amiId, String deployId, HealthCheckType type) throws Exception {
        String id = CommonUtils.getBase64UUID();
        try {
            LOG.info("Start to add ami id {} and deploy id {} to healthCheckDAO for group {}, env id {}, type {}",
                amiId, deployId, groupName, envId, type.toString());
            HealthCheckBean healthCheckBean = new HealthCheckBean();
            healthCheckBean.setId(id);
            healthCheckBean.setGroup_name(groupName);
            healthCheckBean.setEnv_id(envId);
            healthCheckBean.setDeploy_id(deployId);
            healthCheckBean.setAmi_id(amiId);
            healthCheckBean.setState(HealthCheckState.INIT);
            healthCheckBean.setStatus(HealthCheckStatus.UNKNOWN);
            healthCheckBean.setType(type);
            healthCheckBean.setState_start_time(System.currentTimeMillis());
            healthCheckBean.setStart_time(System.currentTimeMillis());
            healthCheckBean.setLast_worked_on(System.currentTimeMillis());
            healthCheckDAO.insertHealthCheck(healthCheckBean);
        } catch (Exception e) {
            LOG.error("Failed to add ami id {} and deploy id {} to healthCheckDAO for group {}, env id {}, type {}",
                amiId, deployId, groupName, envId, type.toString(), e);
        }
        return id;
    }

    public List<String> createHealthCheck(HealthCheckBean healthCheckBean) throws Exception {
        if (healthCheckBean.getType() == null) {
            LOG.error("NULL HealthCheck type");
            return new ArrayList<>();
        }

        if (healthCheckBean.getType() == HealthCheckType.TIME_TRIGGERED || healthCheckBean.getType() == HealthCheckType.MANUALLY_TRIGGERED) {
            String groupName = healthCheckBean.getGroup_name();
            if (StringUtils.isEmpty(groupName)) {
                LOG.error("Empty group name for HealthCheck");
                return new ArrayList<>();
            }

            GroupBean group = groupHandler.getGroupInfoByClusterName(groupName);
            if (!group.getHealthcheck_state()) {
                LOG.info("Health check isn't enabled yet");
                return new ArrayList<>();
            }

            List<String> healthCheckIds = new ArrayList<>();
            if (StringUtils.isEmpty(healthCheckBean.getEnv_id())) {
                List<EnvironBean> envs = environDAO.getEnvsByGroups(Arrays.asList(groupName));
                for (EnvironBean env : envs) {
                    String id = addNewHealthCheckRecord(groupName, env.getEnv_id(), group.getImage_id(), env.getDeploy_id(), healthCheckBean.getType());
                    healthCheckIds.add(id);
                }
            } else {
                EnvironBean env = environDAO.getById(healthCheckBean.getEnv_id());
                String id = addNewHealthCheckRecord(groupName, env.getEnv_id(), group.getImage_id(), env.getDeploy_id(), healthCheckBean.getType());
                healthCheckIds.add(id);
            }
            return healthCheckIds;
        } else if (healthCheckBean.getType() == HealthCheckType.AMI_TRIGGERED) {
            String amiId = healthCheckBean.getAmi_id();
            if (StringUtils.isEmpty(amiId)) {
                LOG.error("Empty ami id for HealthCheck");
                return new ArrayList<>();
            }

            ImageBean imageBean = imageDAO.getById(amiId);
            List<GroupBean> groups = groupInfoDAO.getGroupInfoByAppName(imageBean.getApp_name());
            List<String> healthCheckIds = new ArrayList<>();
            for (GroupBean group : groups) {
                // Add new health check only when user enable health check for their group and new ami is triggered
                if (group.getHealthcheck_state()) {
                    String groupName = group.getGroup_name();
                    List<EnvironBean> envs = environDAO.getEnvsByGroups(Arrays.asList(groupName));
                    for (EnvironBean env : envs) {
                        String id = addNewHealthCheckRecord(groupName, env.getEnv_id(), amiId, env.getDeploy_id(), HealthCheckType.AMI_TRIGGERED);
                        healthCheckIds.add(id);
                    }
                }
            }
            return healthCheckIds;
        } else {
            LOG.error("Unknown HealthCheck type");
            return new ArrayList<>();
        }
    }

    public List<HealthCheckBean> getHealthChecksByGroup(String groupName, int pageIndex, int pageSize) throws Exception {
        return healthCheckDAO.getHealthChecksByGroup(groupName, pageIndex, pageSize);
    }

    public HealthCheckBean getHealthCheckById(String id) throws Exception {
        return healthCheckDAO.getHealthCheckById(id);
    }

    public HealthCheckErrorBean getHealthCheckErrorById(String id) throws Exception {
        return healthCheckErrorDAO.getHealthCheckErrorById(id);
    }

}
