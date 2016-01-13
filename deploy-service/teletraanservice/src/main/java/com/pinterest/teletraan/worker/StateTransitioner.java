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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Check active deploys and transition them into final states
 */
public class StateTransitioner implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(StateTransitioner.class);

    private EnvironDAO environDAO;
    private CommonHandler commonHandler;

    public StateTransitioner(ServiceContext serviceContext) {
        environDAO = serviceContext.getEnvironDAO();
        commonHandler = new CommonHandler(serviceContext);
    }

    void processBatch() throws Exception {
        // Get all current deploys, randomly pick one to work on
        List<String> deployIds = environDAO.getCurrentDeployIds();
        if (deployIds.isEmpty()) {
            LOG.info("StateTransitioner did not find any active deploy, exiting.");
            return;
        }
        Collections.shuffle(deployIds);
        for (String deployId : deployIds) {
            try {
                LOG.debug("StateTransitioner chooses deploy {} to work on.", deployId);
                commonHandler.transitionDeployState(deployId, null);
            } catch (Throwable t) {
                // Catch all throwable so that subsequent job not suppressed
                LOG.error("StateTransitioner failed to process {}", deployId, t);
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
        }
    }
}
