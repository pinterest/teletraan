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

import com.pinterest.arcee.metrics.MetricSource;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.dao.AgentDAO;
import com.pinterest.deployservice.dao.EnvironDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class NewInstanceChecker implements Runnable {
    private static Logger LOG = LoggerFactory.getLogger(NewInstanceChecker.class);
    private static String FIRST_DEPLOY_TOTAL_COUNT_STR = "autoscaling.%s.%s.first_deploy.total";
    private static String FIRST_DEPLOY_FAILED_COUNT_STR = "autoscaling.%s.%s.first_deploy.failed";
    private static String FIRST_DEPLOY_FAILED_RATE_STR = "autoscaling.%s.%s.first_deploy.success_rate";
    private AgentDAO agentDAO;
    private EnvironDAO environDAO;
    private MetricSource metricSource;
    private HashMap<String, String> tags;

    public NewInstanceChecker(ServiceContext serviceContext) {
        agentDAO = serviceContext.getAgentDAO();
        environDAO = serviceContext.getEnvironDAO();
        metricSource = serviceContext.getMetricSource();
        tags = new HashMap<>();
    }

    private void processEachEnvironment(String envId) throws Exception {
        EnvironBean environBean = environDAO.getById(envId);
        String envName = environBean.getEnv_name();
        String stageName = environBean.getStage_name();
        long firstDeployCount = agentDAO.countFirstDeployingAgent(envId);
        long firstDeployFailedCount = agentDAO.countFailedFirstDeployingAgent(envId);
        Long currentTime = System.currentTimeMillis();
        double rate = firstDeployCount == 0 ? 1 : (firstDeployCount - firstDeployFailedCount) / (double)firstDeployCount;
        LOG.info(String.format("Checking env: %s, first deploy count: %d, failed count: %d, failure rate: %f",
                        envName, firstDeployCount, firstDeployFailedCount, rate));
        metricSource.export(String.format(FIRST_DEPLOY_TOTAL_COUNT_STR, envName, stageName), tags, (double)firstDeployCount, currentTime);
        metricSource.export(String.format(FIRST_DEPLOY_FAILED_COUNT_STR, envName, stageName), tags, (double)firstDeployFailedCount, currentTime);
        metricSource.export(String.format(FIRST_DEPLOY_FAILED_RATE_STR,envName, stageName), tags, rate, currentTime);
    }

    public void run()  {
        try {
            List<String> envIds = environDAO.getAllEnvIds();
            Collections.shuffle(envIds);
            for (String envId : envIds) {
                processEachEnvironment(envId);
            }
        } catch (Throwable t) {
            LOG.error("Failed to new instance checker.", t);
        }
    }
}
