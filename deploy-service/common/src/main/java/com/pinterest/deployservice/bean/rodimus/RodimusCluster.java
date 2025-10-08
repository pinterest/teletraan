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

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

@With
@AllArgsConstructor
@Data
@NoArgsConstructor
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
}
/*
RodimusCluster(
  clusterName = "deploy_board-dev2",
  cellName = "aws-us-east-1",
  archName = "x86_64",
  capacity = 0,
  provider = "AWS",
  baseImageId = "KsSJ3NdgQMejqyBAzg-asQ",
  baseImageName = "mlpuppet-18.04-ebs",
  hostType = "q4EdiXPHQRWlPx8xuCxioA",
  securityZone = "vpc-dev-app",
  placement = "kT68vBlPT-acDklIhbC3gQ",

  configs = {
    iam_role = "base-teletraan",
    pinfo_environment = "dev1",
    pinfo_team = "engprod",
    pinfo_role = "teletraan_service",
    access_role = "eng-prod",
    external_facts = "{\"deploy_service\":\"deploy_board-dev2\"}",
    nimbus_id = "nimbus://teletraan/dev1/deploy_board/dev2",
    spiffe_id = "spiffe://aws-dev-481621804342.pin220.com/teletraan/deploy_board/dev2",
    assign_public_ip = false
  },

  state = "NORMAL",
  launchConfig = "",
  useLaunchTemplate = true,
  launchTemplateName = "deploy_board-dev2-2024-03-25-195908",
  autoUpdateBaseImage = false,
  useIdForBaseImageLookUp = null,
  statefulStatus = null,
  isManagedResource = false,
  managedResourceVersion = "",
  replacementTimeout = 45
)
 */
