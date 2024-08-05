/**
 * Copyright (c) 2016 Pinterest, Inc.
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
package com.pinterest.deployservice.common;

import com.pinterest.deployservice.bean.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StateMachines {
    public static final Set<DeployState> DEPLOY_ONGOING_STATES = new HashSet<>();
    public static final Set<HotfixState> HOTFIX_ONGOING_STATES = new HashSet<>();
    public static final Set<DeployState> DEPLOY_ACTIVE_STATES = new HashSet<>();
    public static final Set<DeployType> AUTO_PROMOTABLE_DEPLOY_TYPE = new HashSet<>();
    public static final Set<DeployState> DEPLOY_FINAL_STATES = new HashSet<>();
    public static final Set<EnvState> ENV_DEPLOY_STATES = new HashSet<>();
    public static final Set<AcceptanceStatus> FINAL_ACCEPTANCE_STATUSES = new HashSet<>();
    public static final Map<DeployType, OpCode> DEPLOY_TYPE_OPCODE_MAP = new HashMap<>();
    public static final Map<DeployState, DeployState> FINAL_STATE_TRANSITION_MAP = new HashMap<>();
    public static final Map<DeployStage, DeployStage> DEPLOY_STAGE_TRANSITION_MAP = new HashMap<>();
    public static final Map<DeployType, Map<DeployStage, DeployStage>> DEPLOY_TYPE_TRANSITION_MAP =
            new HashMap<>();

    static {
        FINAL_STATE_TRANSITION_MAP.put(DeployState.RUNNING, DeployState.ABORTED);
        FINAL_STATE_TRANSITION_MAP.put(DeployState.FAILING, DeployState.ABORTED);
        FINAL_STATE_TRANSITION_MAP.put(DeployState.SUCCEEDING, DeployState.SUCCEEDED);
        FINAL_STATE_TRANSITION_MAP.put(DeployState.ABORTED, DeployState.ABORTED);
        FINAL_STATE_TRANSITION_MAP.put(DeployState.SUCCEEDED, DeployState.SUCCEEDED);

        // Map DeployType to OpCode
        DEPLOY_TYPE_OPCODE_MAP.put(DeployType.REGULAR, OpCode.DEPLOY);
        DEPLOY_TYPE_OPCODE_MAP.put(DeployType.HOTFIX, OpCode.DEPLOY);
        DEPLOY_TYPE_OPCODE_MAP.put(DeployType.ROLLBACK, OpCode.ROLLBACK);
        DEPLOY_TYPE_OPCODE_MAP.put(DeployType.RESTART, OpCode.RESTART);
        DEPLOY_TYPE_OPCODE_MAP.put(DeployType.STOP, OpCode.STOP);

        DEPLOY_ONGOING_STATES.add(DeployState.RUNNING);
        DEPLOY_ONGOING_STATES.add(DeployState.FAILING);

        HOTFIX_ONGOING_STATES.add(HotfixState.INITIAL);
        HOTFIX_ONGOING_STATES.add(HotfixState.PUSHING);
        HOTFIX_ONGOING_STATES.add(HotfixState.BUILDING);

        ENV_DEPLOY_STATES.add(EnvState.NORMAL);

        AUTO_PROMOTABLE_DEPLOY_TYPE.add(DeployType.REGULAR);
        AUTO_PROMOTABLE_DEPLOY_TYPE.add(DeployType.HOTFIX);

        FINAL_ACCEPTANCE_STATUSES.add(AcceptanceStatus.ACCEPTED);
        FINAL_ACCEPTANCE_STATUSES.add(AcceptanceStatus.REJECTED);
        FINAL_ACCEPTANCE_STATUSES.add(AcceptanceStatus.TERMINATED);

        DEPLOY_ACTIVE_STATES.add(DeployState.RUNNING);
        DEPLOY_ACTIVE_STATES.add(DeployState.FAILING);
        DEPLOY_ACTIVE_STATES.add(DeployState.SUCCEEDING);

        DEPLOY_FINAL_STATES.add(DeployState.SUCCEEDED);
        DEPLOY_FINAL_STATES.add(DeployState.ABORTED);

        DEPLOY_STAGE_TRANSITION_MAP.put(DeployStage.UNKNOWN, DeployStage.PRE_DOWNLOAD);
        DEPLOY_STAGE_TRANSITION_MAP.put(DeployStage.PRE_DOWNLOAD, DeployStage.DOWNLOADING);
        DEPLOY_STAGE_TRANSITION_MAP.put(DeployStage.DOWNLOADING, DeployStage.POST_DOWNLOAD);
        DEPLOY_STAGE_TRANSITION_MAP.put(DeployStage.POST_DOWNLOAD, DeployStage.STAGING);
        DEPLOY_STAGE_TRANSITION_MAP.put(DeployStage.STAGING, DeployStage.PRE_RESTART);
        DEPLOY_STAGE_TRANSITION_MAP.put(DeployStage.PRE_RESTART, DeployStage.RESTARTING);
        DEPLOY_STAGE_TRANSITION_MAP.put(DeployStage.RESTARTING, DeployStage.POST_RESTART);
        DEPLOY_STAGE_TRANSITION_MAP.put(DeployStage.POST_RESTART, DeployStage.SERVING_BUILD);
        DEPLOY_STAGE_TRANSITION_MAP.put(DeployStage.STOPPING, DeployStage.STOPPED);

        DEPLOY_TYPE_TRANSITION_MAP.put(DeployType.HOTFIX, DEPLOY_STAGE_TRANSITION_MAP);
        DEPLOY_TYPE_TRANSITION_MAP.put(DeployType.REGULAR, DEPLOY_STAGE_TRANSITION_MAP);
        DEPLOY_TYPE_TRANSITION_MAP.put(DeployType.ROLLBACK, DEPLOY_STAGE_TRANSITION_MAP);
        DEPLOY_TYPE_TRANSITION_MAP.put(DeployType.RESTART, DEPLOY_STAGE_TRANSITION_MAP);
        DEPLOY_TYPE_TRANSITION_MAP.put(DeployType.STOP, DEPLOY_STAGE_TRANSITION_MAP);
    }

    public static DeployStage getFirstStage() {
        return DeployStage.PRE_DOWNLOAD;
    }

    public static DeployStage getDownloadStage() {
        return DeployStage.DOWNLOADING;
    }
}
