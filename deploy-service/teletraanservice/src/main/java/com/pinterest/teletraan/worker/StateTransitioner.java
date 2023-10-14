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
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.handler.CommonHandler;
import com.pinterest.deployservice.metrics.MeterConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;

/**
 * Check active deploys and transition them into final states
 */
public class StateTransitioner implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(StateTransitioner.class);

    private EnvironDAO environDAO;
    private CommonHandler commonHandler;
    private Counter errorBudgetSuccess;
    private Counter errorBudgetFailure;

    public StateTransitioner(ServiceContext serviceContext) {
        environDAO = serviceContext.getEnvironDAO();
        commonHandler = new CommonHandler(serviceContext);

        errorBudgetSuccess = Metrics.counter(MeterConstants.ERROR_BUDGET_METRIC_NAME,
            MeterConstants.ERROR_BUDGET_TAG_NAME_RESPONSE_TYPE, MeterConstants.ERROR_BUDGET_TAG_VALUE_RESPONSE_TYPE_SUCCESS,
            MeterConstants.ERROR_BUDGET_TAG_NAME_METHOD_NAME, this.getClass().getSimpleName());

        errorBudgetFailure = Metrics.counter(MeterConstants.ERROR_BUDGET_METRIC_NAME,
            MeterConstants.ERROR_BUDGET_TAG_NAME_RESPONSE_TYPE, MeterConstants.ERROR_BUDGET_TAG_VALUE_RESPONSE_TYPE_FAILURE,
            MeterConstants.ERROR_BUDGET_TAG_NAME_METHOD_NAME, this.getClass().getSimpleName());
    }

    void processBatch() throws Exception {
        // Get all current deploys, randomly pick one to work on
        List<String> deployIds = environDAO.getCurrentDeployIds();
        if (deployIds.isEmpty()) {
            LOG.info("StateTransitioner did not find any active deploy, exiting.");

            errorBudgetSuccess.increment();
            return;
        }
        Collections.shuffle(deployIds);
        for (String deployId : deployIds) {
            try {
                LOG.debug("StateTransitioner chooses deploy {} to work on.", deployId);
                commonHandler.transitionDeployState(deployId, null);

                errorBudgetSuccess.increment();
            } catch (Throwable t) {
                // Catch all throwable so that subsequent job not suppressed
                LOG.error("StateTransitioner failed to process {}", deployId, t);

                errorBudgetFailure.increment();
            }
        }
    }

    @Override
    public void run() {
        try {
            LOG.info("Start StateTransitioner process...");
            processBatch();
        } catch (Throwable t) {
            // Catch all throwable so that subsequent job not suppressed
            LOG.error("Failed to call StateTransitioner.", t);

            errorBudgetFailure.increment();
        }
    }
}
