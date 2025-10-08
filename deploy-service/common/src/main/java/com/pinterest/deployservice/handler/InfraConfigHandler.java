/**
 * Copyright (c) 2025 Pinterest, Inc.
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
package com.pinterest.deployservice.handler;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.rodimus.RodimusAutoScalingAlarm;
import com.pinterest.deployservice.rodimus.RodimusManager;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InfraConfigHandler {
    private static final Logger LOG = LoggerFactory.getLogger(InfraConfigHandler.class);
    private final RodimusManager rodimusManager;

    public InfraConfigHandler(ServiceContext serviceContext) {
        rodimusManager = serviceContext.getRodimusManager();
    }

    public void test(String clusterName) throws Exception {
        //    RodimusCluster rodimusCluster = rodimusManager.getCluster(clusterName);
        //        RodimusAutoScalingPolicies rodimusAutoScalingPolicies =
        // rodimusManager.getClusterScalingPolicies(clusterName);
        List<RodimusAutoScalingAlarm> rodimusAutoScalingAlarms =
                rodimusManager.getClusterAlarms(clusterName);
        LOG.error("rodimusAutoScalingAlarms 123: " + rodimusAutoScalingAlarms);
    }
}
