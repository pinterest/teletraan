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

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/*
autoRefreshConfig:
  - clusterType: DEV
    bakeTime: 1
    skipMatching: true
    instanceWarmup: 0
    minHealthyPercentage: 90
    maxHealthyPercentage: 110
    checkpointPercentages:
      - 10
      - 50
      - 100
    scaleInProtectedInstances: IGNORE
    checkpointDelay: 3600
  - clusterType: PRODUCTION
    bakeTime: 1
    skipMatching: true
    instanceWarmup: 0
    minHealthyPercentage: 90
    maxHealthyPercentage: 110
    checkpointPercentages:
      - 10
      - 50
      - 100
    scaleInProtectedInstances: IGNORE
    checkpointDelay: 3600
 */
@Setter
@Getter
public class AutoRefreshConfig {
    private ClusterType clusterType;
    private Integer bakeTime;
    private boolean skipMatching;
    private Integer instanceWarmup;
    private Integer minHealthyPercentage;
    private Integer maxHealthyPercentage;
    private List<Integer> checkpointPercentages;
    private ScaleInProtectedInstances scaleInProtectedInstances;
    private Integer checkpointDelay;
}
