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
package com.pinterest.teletraan.handler;

import static com.pinterest.teletraan.resource.EnvCapacities.*;

import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.InfraBean;
import com.pinterest.deployservice.bean.rodimus.RodimusCluster;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.rodimus.RodimusManager;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.resource.Utils;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.core.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InfraConfigHandler {

    private static final Logger LOG = LoggerFactory.getLogger(InfraConfigHandler.class);
    private final EnvironDAO environDAO;
    private final RodimusManager rodimusManager;
    private final EnvironmentHandler environmentHandler;

    public InfraConfigHandler(TeletraanServiceContext context) {
        environDAO = context.getEnvironDAO();
        rodimusManager = context.getRodimusManager();
        environmentHandler = new EnvironmentHandler(context);
    }

    public void test(String envName, String stageName, String clusterName) throws Exception {
        LOG.error("cluster 123: init");
//        clusterName = envName + "_" + stageName;
        //        String envName = "helloworlddummyservice-server";
        //        String stageName = "nfallah-iac-test-001";
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
//        LOG.error("cluster 123: going to create");
//        rodimusManager.createCluster(clusterName, envName, stageName, newRodimusCluster);
//        LOG.error("cluster 123: done creating");
        RodimusCluster rodimusCluster = rodimusManager.getCluster(clusterName);
        LOG.error("cluster 123: got created: " + rodimusCluster);
//        newRodimusCluster.setReplacementTimeout(60);
//        rodimusManager.updateCluster(clusterName, newRodimusCluster);
//        LOG.error("cluster 123: done updating");
//        rodimusCluster = rodimusManager.getCluster(clusterName);
//        LOG.error("cluster 123: got updated: " + rodimusCluster);

        //    RodimusCluster rodimusCluster = rodimusManager.getCluster(clusterName);
        //        RodimusAutoScalingPolicies rodimusAutoScalingPolicies =
        // rodimusManager.getClusterScalingPolicies(clusterName);
        //        List<RodimusAutoScalingAlarm> rodimusAutoScalingAlarms =
        //                rodimusManager.getClusterAlarms(clusterName);
        //        List<RodimusScheduledAction> rodimusScheduledActions =
        //                rodimusManager.getClusterScheduledActions(clusterName);
        //        LOG.error("rodimusScheduledActions 123: " + rodimusScheduledActions);
    }

    public void test(SecurityContext sc, String envName, String stageName, InfraBean infraBean)
            throws Exception {
        String clusterName = infraBean.getClusterName();
        RodimusCluster rodimusCluster = rodimusManager.getCluster(clusterName);
        if (rodimusCluster == null) {
            // Create cluster: cluster doesn't exist
            LOG.info(
                    "Creating cluster and supporting teletraan records for cluster: {}",
                    clusterName);
            EnvironBean originEnvironBean = Utils.getEnvStage(environDAO, envName, stageName);
            try {
                EnvironBean updateEnvironBean =
                        originEnvironBean; // .toBuilder().cluster_name(clusterName).build();
                environmentHandler.updateEnvironment(sc, envName, stageName, updateEnvironBean);
                environmentHandler.createCapacityForHostOrGroup(
                        sc,
                        envName,
                        stageName,
                        Optional.of(CapacityType.GROUP),
                        clusterName,
                        originEnvironBean);
                rodimusManager.createCluster(clusterName, envName, stageName, rodimusCluster);
            } catch (Exception e) {
                environmentHandler.updateEnvironment(sc, envName, stageName, originEnvironBean);
                environmentHandler.deleteCapacityForHostOrGroup(sc, envName, stageName);
                throw e;
            }
        } else {
            // Update cluster: Other successful GET responses only; this is checked in the client

            LOG.info("Updating cluster: {}", clusterName);
            RodimusCluster updateRodimusCluster = rodimusCluster.toBuilder().build(); // rod 0.0
            Map<String, String> responseConfig = updateRodimusCluster.getConfigs();
            if ((responseConfig != null) && (updateRodimusCluster.getConfigs() != null)) {
                if (responseConfig.containsKey("spiffe_id")) {
                    updateRodimusCluster
                            .getConfigs()
                            .put("spiffe_id", responseConfig.get("spiffe_id")); // rod 0.0.0
                }
                if (responseConfig.containsKey("nimbus_id")) {
                    updateRodimusCluster
                            .getConfigs()
                            .put("nimbus_id", responseConfig.get("nimbus_id")); // rod 0.0.1
                }
            }
            //          actualManagedResourceVersion =
            // updateRodimusCluster.getManagedResourceVersion(); // rod 0.0.2
            //          try {
            //            rodimusManager.updateCluster( // rod 2, not implemented in Teletraan
            //                    clusterName,
            //
            // updateRodimusCluster.withManagedResourceVersion(actualManagedResourceVersion)); //
            // rod 0.0.2.0
            //          } catch (Exception e) {
            //            registry
            //                    .counter(registryId.withTag("exception",
            // e.getClass().getCanonicalName()))
            //                    .increment();
            //            throw e;
            //          }
        }
    }
}
