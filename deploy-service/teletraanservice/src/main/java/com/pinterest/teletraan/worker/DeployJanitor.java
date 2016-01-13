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

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.dao.DeployDAO;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.UtilDAO;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;

/**
 * Removed unused/old deploys.
 */
public class DeployJanitor implements Job {
    private static final Logger LOG = LoggerFactory.getLogger(StateTransitioner.class);
    private static final long MILLIS_PER_DAY = 86400000;

    private EnvironDAO environDAO;
    private DeployDAO deployDAO;
    private UtilDAO utilDAO;

    public DeployJanitor() {
        // If using the Job interface, must keep constructor empty.
    }

    void processDeploys() throws Exception {
        List<String> envIds = environDAO.getAllEnvIds();
        Collections.shuffle(envIds);

        for (String envId : envIds) {
            EnvironBean envBean = environDAO.getById(envId);
            long timeThreshold = System.currentTimeMillis() - (long) envBean.getMax_deploy_day() * MILLIS_PER_DAY;
            long numToDelete = deployDAO.countDeploysByEnvId(envId) - (long) envBean.getMax_deploy_num();

            if (numToDelete > 0) {
                String deployLockName = String.format("DEPLOYJANITOR-%s", envId);
                Connection connection = utilDAO.getLock(deployLockName);

                if (connection != null) {
                    LOG.debug("Successfully to get the lock: %s", deployLockName);
                    try {
                        deployDAO.deleteUnusedDeploys(envId, timeThreshold, numToDelete);
                        LOG.info("Successfully removed deploys: %s before %d milliseconds has %d.",
                            envId, timeThreshold, numToDelete);
                    } catch (Exception e) {
                        LOG.error("Failed to delete builds from tables.", e);
                    } finally {
                        utilDAO.releaseLock(deployLockName, connection);
                        LOG.debug("Successfully released the lock: %s", deployLockName);
                    }
                } else {
                    LOG.warn("Failed to get the lock: %s", deployLockName);
                }
            }
        }
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        SchedulerContext schedulerContext;

        try {
            schedulerContext = context.getScheduler().getContext();
        } catch (SchedulerException e) {
            LOG.error("Cannot retrive job context!", e);
            return;
        }

        ServiceContext serviceContext = (ServiceContext) schedulerContext.get("serviceContext");
        environDAO = serviceContext.getEnvironDAO();
        deployDAO = serviceContext.getDeployDAO();
        utilDAO = serviceContext.getUtilDAO();

        try {
            LOG.info("Start deploy janitor process...");
            processDeploys();
            LOG.info("Stop deploy janitor process...");
        } catch (Throwable t) {
            LOG.error("Failed to call deploy janitor.", t);
        }
    }
}
