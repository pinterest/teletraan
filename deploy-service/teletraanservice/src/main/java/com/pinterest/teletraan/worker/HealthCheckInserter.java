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


import com.pinterest.arcee.bean.GroupBean;
import com.pinterest.arcee.bean.HealthCheckBean;
import com.pinterest.arcee.bean.HealthCheckType;
import com.pinterest.arcee.dao.GroupInfoDAO;
import com.pinterest.arcee.dao.HealthCheckDAO;
import com.pinterest.arcee.handler.HealthCheckHandler;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.ASGStatus;
import com.pinterest.deployservice.dao.UtilDAO;

import com.google.common.base.Joiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;

public class HealthCheckInserter implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(HealthCheckInserter.class);
    private final GroupInfoDAO groupInfoDAO;
    private final HealthCheckDAO healthCheckDAO;
    private final UtilDAO utilDAO;
    private final HealthCheckHandler healthCheckHandler;

    public HealthCheckInserter(ServiceContext serviceContext) {
        groupInfoDAO = serviceContext.getGroupInfoDAO();
        healthCheckDAO = serviceContext.getHealthCheckDAO();
        utilDAO = serviceContext.getUtilDAO();
        healthCheckHandler = new HealthCheckHandler(serviceContext);
    }

    void processEachGroup(String groupName) throws Exception {
        GroupBean groupBean = groupInfoDAO.getGroupInfo(groupName);
        String lockName = String.format("HEALTHCHECK-GROUP-%s", groupName);
        Connection connection = utilDAO.getLock(lockName);
        if (connection != null) {
            try {
                List<HealthCheckBean> ongiongHealthChecks = healthCheckDAO.getOngoingRegularHealthChecksByGroup(groupName);
                if (!ongiongHealthChecks.isEmpty()) {
                    LOG.info("There are ongoing regular health checks for group {}. Do not add new check.", groupName);
                    return;
                }

                long timeToCheck = System.currentTimeMillis() - groupBean.getHealthcheck_period() * 1000;
                List<HealthCheckBean> healthCheckBeans = healthCheckDAO.getRegularHealthChecksByGroupAndTime(groupName, timeToCheck);
                if (!healthCheckBeans.isEmpty()) {
                    LOG.info("There are regular health checks for group {} within {} seconds. Do not add new check.",
                        groupName, groupBean.getHealthcheck_period());
                    return;
                }

                HealthCheckBean healthCheckBean = new HealthCheckBean();
                healthCheckBean.setGroup_name(groupName);
                healthCheckBean.setType(HealthCheckType.TIME_TRIGGERED);
                List<String> healthCheckIds = healthCheckHandler.createHealthCheck(healthCheckBean);
                LOG.info("Added regular health checks, ids: {}", Joiner.on(",").join(healthCheckIds));
            } catch (Exception ex) {
                LOG.error("Failed to add a regular health check for {}", groupName, ex);
            } finally {
                utilDAO.releaseLock(lockName, connection);
            }
        } else {
            LOG.warn(String.format("Failed to get lock: %s", lockName));
        }
    }

    void processBatch() throws Exception {
        // Get all enabled health check groups
        List<String> groupNames = groupInfoDAO.getEnabledHealthCheckGroupNames();
        Collections.shuffle(groupNames);
        for (String groupName : groupNames) {
            LOG.info("Start to process enabled health check group {} for regular health check", groupName);
            processEachGroup(groupName);
        }
    }

    @Override
    public void run() {
        try {
            LOG.info("Start to run HealthCheckInserter");
            processBatch();
        } catch (Throwable t) {
            LOG.error("Failed to run HealthCheckInserter");
        }
    }
}
