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
package com.pinterest.deployservice.bean.rodimus;

import com.pinterest.deployservice.bean.InfraBean;
import java.util.Map;
import lombok.*;

@With
@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder(toBuilder = true)
public class RodimusCluster {
    private String clusterName;
    private String cellName;
    private String archName;
    private int capacity;
    private String provider;
    private String baseImageId;
    private String baseImageName;
    private String hostType;
    private String securityZone;
    private String placement;
    private Map<String, String> configs;
    private String state;
    private String launchConfig;
    private boolean useLaunchTemplate;
    private String launchTemplateName;
    private boolean autoUpdateBaseImage;
    private Boolean useIdForBaseImageLookUp;
    private Boolean statefulStatus;
    private Boolean isManagedResource;
    private String managedResourceVersion;
    private long replacementTimeout;

    public static RodimusCluster fromInfraBean(InfraBean infraBean) {
        return RodimusCluster.builder()
                .clusterName(
                        infraBean
                                .getClusterName()) // "helloworlddummyservice-server_nfallah_iac_test_001"
                .cellName(infraBean.getCellName()) // "aws-us-east-1"
                .archName(infraBean.getArchName()) // "x86_64"
                .capacity(infraBean.getMaxCapacity()) // 2
                .provider(infraBean.getProvider().name()) // AWS
                .baseImageId(
                        infraBean.getBaseImageId()) // ??? same as hostType? ami-07cd1899f95a5eae5
                // needs to be translated to
                // "KsSJ3NdgQMejqyBAzg-asQ", rodimus table:
                // base_images
                .baseImageName(infraBean.getBaseImageName()) // "mlpuppet-18.04-ebs" (abstract name)
                .hostType(infraBean.getHostType()) // ??? how can we translate this in teletraan?
                // Should we make extra call to rodimus, add a new
                // endpoint that handles new host type, or modify
                // the existing endpoint to handle both? 4x.large
                // needs to be translated to
                // "q4EdiXPHQRWlPx8xuCxioA", not available on
                // teletraan, only on host_types table in rodimus
                .securityZone(
                        infraBean.getSecurityZone()) // ??? same as hostType? "prod-private-tools"
                // translate to JEyCGR24TmG9v48QRQnBpg from
                // rodimus's security_zones table
                .placement(
                        String.join(
                                ",",
                                infraBean.getPlacement())) // ??? same as hostType? comma separated
                // list. subnet-3f51dd65 needs to be
                // translated to "kT68vBlPT-acDklIhbC3gQ",
                // not available on teletraan, only on
                // placements table in rodimus
                .configs(
                        infraBean
                                .getUserData()) // ??? is this config_id in clusters table? couldn't
                // find a related table
//                .state("NORMAL") // ??? Hardcode to "NORMAL"
                //              .launchConfig(infraBean.con) // ??? where should this come in
                // teletraan?
                //              .useLaunchTemplate(infraBean) // ??? where should this come in
                // teletraan?
                //              .launchTemplateName(infraBean.) // !!!
                // "deploy_board-dev2-2024-03-25-195908"
                .autoUpdateBaseImage(infraBean.getAutoUpdateBaseImage()) // false
                .useIdForBaseImageLookUp(infraBean.getUseIdForBaseImageLookUp()) // null
                .statefulStatus(infraBean.getStatefulStatus()) // null
                .isManagedResource(true) // ??? hardcoded since this endpoint is used by orca plugin for now
                //              .managedResourceVersion(infraBean) // ??? ""
                .replacementTimeout(infraBean.getReplacementTimeout()) // 45
                .build();
    }
}
/*
prod: SELECT * FROM clusters LIMIT 1;
+-----------------+---------------+-------------+------------------------+------------------------+-------------------------------+------------------------+----------------------------------------------------------------------+-------------+----------------+------------------------+----------+--------------------------------+--------------------------------+----------------------------+------------------------+------------------------+--------------+---------------------------+--------------------+-----------------+--------------------------------------+------------------------+-----------------------------+------------------------+
| b'cluster_name' | b'cell_name'  | b'capacity' | b'base_image_id'       | b'host_type_id'        | b'enable_multiple_host_types' | b'security_zone_id'    | b'placement_id'                                                      | b'provider' | b'last_update' | b'config_id'           | b'state' | b'launch_config'               | b'launch_template_name'        | b'launch_template_version' | b'use_launch_template' | b'replacement_timeout' | b'arch_name' | b'auto_update_base_image' | b'stateful_status' | b'auto_refresh' | b'account_id'                        | b'is_managed_resource' | b'managed_resource_version' | b'bandwidth_weighting' |
+-----------------+---------------+-------------+------------------------+------------------------+-------------------------------+------------------------+----------------------------------------------------------------------+-------------+----------------+------------------------+----------+--------------------------------+--------------------------------+----------------------------+------------------------+------------------------+--------------+---------------------------+--------------------+-----------------+--------------------------------------+------------------------+-----------------------------+------------------------+
| abi-bot-prod    | aws-us-east-1 |           0 | KMlVY-oaRja8PDOJ7-OvVQ | 3ylAY9UURleLiQzlsnsESg |                             0 | JEyCGR24TmG9v48QRQnBpg | Dp22H9BcR7-wf0LmZcZcWw,GB2Ycl5pQXaYh-jar5YPjQ,TTlI7BhMTwyLpTZL04iI7g | AWS         |  1744749123140 | J2Ey5ylfRwS9ShcVmIKyUg | NORMAL   | abi-bot-prod-2023-05-06-073758 | abi-bot-prod-2023-05-18-213710 |                            |                      1 |                   2700 | x86_64       |                         1 | NULL               |               0 | 9de91a7b-cb73-4c9f-865a-a984cb58abca |                      0 |                             | NULL                   |
+-----------------+---------------+-------------+------------------------+------------------------+-------------------------------+------------------------+----------------------------------------------------------------------+-------------+----------------+------------------------+----------+--------------------------------+--------------------------------+----------------------------+------------------------+------------------------+--------------+---------------------------+--------------------+-----------------+--------------------------------------+------------------------+-----------------------------+------------------------+
dev: SELECT * FROM clusters WHERE cluster_name = 'deploy_board-dev2';
+-------------------+---------------+----------+------------------------+------------------------+----------------------------+---------------------+------------------------+------------------------+----------+---------------+------------------------+--------+---------------+-------------------------------------+-------------------------+---------------------+---------------------+-----------+------------------------+-----------------+--------------+--------------------------------------+------------------------------------------+--------------------------+
| cluster_name      | cell_name     | capacity | base_image_id          | host_type_id           | enable_multiple_host_types | bandwidth_weighting | security_zone_id       | placement_id           | provider | last_update   | config_id              | state  | launch_config | launch_template_name                | launch_template_version | use_launch_template | replacement_timeout | arch_name | auto_update_base_image | stateful_status | auto_refresh | account_id                           | is_managed_resource                      | managed_resource_version |
+-------------------+---------------+----------+------------------------+------------------------+----------------------------+---------------------+------------------------+------------------------+----------+---------------+------------------------+--------+---------------+-------------------------------------+-------------------------+---------------------+---------------------+-----------+------------------------+-----------------+--------------+--------------------------------------+------------------------------------------+--------------------------+
| deploy_board-dev2 | aws-us-east-1 |        0 | KsSJ3NdgQMejqyBAzg-asQ | q4EdiXPHQRWlPx8xuCxioA |                          0 | NULL                | HsMUToiGRv2w3ZjZYc9UsA | kT68vBlPT-acDklIhbC3gQ | AWS      | 1713381080326 | EqxDW50gRM-M_e_EEJaf2g | NORMAL |               | deploy_board-dev2-2024-03-25-195908 |                         |                   1 |                2700 | x86_64    |                      0 |            NULL |            0 | 7b5a8f82-2a88-41af-b978-9c4cf8872974 | 0x00                                     |                          |
+-------------------+---------------+----------+------------------------+------------------------+----------------------------+---------------------+------------------------+------------------------+----------+---------------+------------------------+--------+---------------+-------------------------------------+-------------------------+---------------------+---------------------+-----------+------------------------+-----------------+--------------+--------------------------------------+------------------------------------------+--------------------------+


SELECT * FROM security_zones WHERE id = 'JEyCGR24TmG9v48QRQnBpg';
+------------------------+--------------------+------------------+-------------+----------+---------------------------------------------------------------------------------------------------+
| b'id'                  | b'abstract_name'   | b'provider_name' | b'provider' | b'basic' | b'description'                                                                                    |
+------------------------+--------------------+------------------+-------------+----------+---------------------------------------------------------------------------------------------------+
| JEyCGR24TmG9v48QRQnBpg | prod-private-tools | sg-a37b20c7      | AWS         |        1 | this security zone is used for internal services like Teletraan, Jenkins, Phabricator, or Pinrepo |
+------------------------+--------------------+------------------+-------------+----------+---------------------------------------------------------------------------------------------------+

SELECT * FROM base_images WHERE id = 'KMlVY-oaRja8PDOJ7-OvVQ';
+------------------------+--------------------+---------------+-----------------------+-------------+----------+--------------+----------------+-----------------+--------------+--------------------------------------------------------------+--------------+-----------------+-----------------+
| b'id'                  | b'abstract_name'   | b'cell_name'  | b'provider_name'      | b'provider' | b'basic' | b'qualified' | b'description' | b'publish_date' | b'arch_name' | b'publish_info'                                              | b'publisher' | b'unused_since' | b'soft_deleted' |
+------------------------+--------------------+---------------+-----------------------+-------------+----------+--------------+----------------+-----------------+--------------+--------------------------------------------------------------+--------------+-----------------+-----------------+
| KMlVY-oaRja8PDOJ7-OvVQ | cmp_base-ebs-18.04 | aws-us-east-1 | ami-07cd1899f95a5eae5 | AWS         |        1 |            0 | NULL           |   1744315317196 | x86_64       | https://jenkins-iso-vm.pinadmin.com/job/pinterest-amis/1122/ | jenkins      | NULL            |               0 |
+------------------------+--------------------+---------------+-----------------------+-------------+----------+--------------+----------------+-----------------+--------------+--------------------------------------------------------------+--------------+-----------------+-----------------+

SELECT * FROM placements WHERE id = 'GB2Ycl5pQXaYh-jar5YPjQ';
+------------------------+------------------+---------------+------------------+-------------+----------+-------------------------+-------------+---------------------+--------------------------------------+
| b'id'                  | b'abstract_name' | b'cell_name'  | b'provider_name' | b'provider' | b'basic' | b'description'          | b'capacity' | b'assign_public_ip' | b'account_id'                        |
+------------------------+------------------+---------------+------------------+-------------+----------+-------------------------+-------------+---------------------+--------------------------------------+
| GB2Ycl5pQXaYh-jar5YPjQ | us-east-1a       | aws-us-east-1 | subnet-3f51dd65  | AWS         |        0 | private-prod-us-east-1a |         684 |                   0 | 9de91a7b-cb73-4c9f-865a-a984cb58abca |
+------------------------+------------------+---------------+------------------+-------------+----------+-------------------------+-------------+---------------------+--------------------------------------+
SELECT * FROM placements WHERE id = 'Dp22H9BcR7-wf0LmZcZcWw';
+------------------------+------------------+---------------+------------------+-------------+----------+----------------+-------------+---------------------+--------------------------------------+
| b'id'                  | b'abstract_name' | b'cell_name'  | b'provider_name' | b'provider' | b'basic' | b'description' | b'capacity' | b'assign_public_ip' | b'account_id'                        |
+------------------------+------------------+---------------+------------------+-------------+----------+----------------+-------------+---------------------+--------------------------------------+
| Dp22H9BcR7-wf0LmZcZcWw | us-east-1d       | aws-us-east-1 | subnet-46cbaf31  | AWS         |        1 |                |         610 |                   0 | 9de91a7b-cb73-4c9f-865a-a984cb58abca |
+------------------------+------------------+---------------+------------------+-------------+----------+----------------+-------------+---------------------+--------------------------------------+

 */
