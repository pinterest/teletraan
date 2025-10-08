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
import com.pinterest.deployservice.bean.rodimus.RodimusCluster;
import com.pinterest.deployservice.rodimus.RodimusManager;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InfraConfigHandler {
    private static final Logger LOG = LoggerFactory.getLogger(InfraConfigHandler.class);
    private final RodimusManager rodimusManager;

    public InfraConfigHandler(ServiceContext serviceContext) {
        rodimusManager = serviceContext.getRodimusManager();
    }

    public void test(String clusterName) throws Exception {
        LOG.error("cluster 123: init");
        clusterName = "helloworlddummyservice-server-nfallah-iac-test-001";
        String envName = "helloworlddummyservice-server";
        String stageName = "nfallah-iac-test-001";
        Map<String, String> configs = new HashMap<>();
        configs.put("iam_role", "base-teletraan");
        configs.put("pinfo_environment", "dev1");
        configs.put("pinfo_team", "engprod");
        configs.put("pinfo_role", "teletraan_service");
        configs.put("access_role", "eng-prod");
        configs.put("external_facts", "{\"deploy_service\":\"deploy_board-dev2\"}");
        configs.put("nimbus_id", "nimbus://teletraan/dev1/deploy_board/dev2");
        configs.put(
                "spiffe_id",
                "spiffe://aws-dev-481621804342.pin220.com/teletraan/deploy_board/dev2");
        configs.put("assign_public_ip", "false");
        RodimusCluster newRodimusCluster =
                new RodimusCluster(
                        clusterName, // clusterName
                        "aws-us-east-1", // cellName
                        "x86_64", // archName
                        0, // capacity
                        "AWS", // provider
                        "KsSJ3NdgQMejqyBAzg-asQ", // baseImageId
                        "mlpuppet-18.04-ebs", // baseImageName
                        "q4EdiXPHQRWlPx8xuCxioA", // hostType
                        "vpc-dev-app", // securityZone
                        "kT68vBlPT-acDklIhbC3gQ", // placement
                        configs, // configs
                        "NORMAL", // state
                        "", // launchConfig
                        true, // useLaunchTemplate
                        "deploy_board-dev2-2024-03-25-195908", // launchTemplateName
                        false, // autoUpdateBaseImage
                        null, // useIdForBaseImageLookUp
                        null, // statefulStatus
                        false, // isManagedResource
                        "", // managedResourceVersion
                        45 // replacementTimeout
                        );
        LOG.error("cluster 123: going to create");
        rodimusManager.createCluster(clusterName, envName, stageName, newRodimusCluster);
        LOG.error("cluster 123: done creating");
        RodimusCluster rodimusCluster = rodimusManager.getCluster(clusterName);
        LOG.error("cluster 123: got created: " + rodimusCluster);
        newRodimusCluster.setReplacementTimeout(60);
        LOG.error("cluster 123: done updating");
        rodimusCluster = rodimusManager.getCluster(clusterName);
        LOG.error("cluster 123: got updated: " + rodimusCluster);

        //    RodimusCluster rodimusCluster = rodimusManager.getCluster(clusterName);
        //        RodimusAutoScalingPolicies rodimusAutoScalingPolicies =
        // rodimusManager.getClusterScalingPolicies(clusterName);
        //        List<RodimusAutoScalingAlarm> rodimusAutoScalingAlarms =
        //                rodimusManager.getClusterAlarms(clusterName);
        //        List<RodimusScheduledAction> rodimusScheduledActions =
        //                rodimusManager.getClusterScheduledActions(clusterName);
        //        LOG.error("rodimusScheduledActions 123: " + rodimusScheduledActions);
    }
}
