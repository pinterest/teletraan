/**
 * Copyright (c) 2016-2020 Pinterest, Inc.
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

public class Constants {
    public static final DeployPriority DEFAULT_PRIORITY = DeployPriority.NORMAL;
    public static final AcceptanceStatus DEFAULT_ACCEPTANCE_STATUS =
            AcceptanceStatus.PENDING_DEPLOY;
    public static final PromoteType DEFAULT_PROMOTE_TYPE = PromoteType.MANUAL;
    public static final PromoteFailPolicy DEFAULT_PROMOTE_FAIL_POLICY = PromoteFailPolicy.CONTINUE;
    public static final PromoteDisablePolicy DEFAULT_PROMOTE_DISABLE_POLICY =
            PromoteDisablePolicy.AUTO;
    public static final OverridePolicy DEFAULT_OVERRIDE_POLICY = OverridePolicy.OVERRIDE;
    public static final AcceptanceType DEFAULT_ACCEPTANCE_TYPE = AcceptanceType.AUTO;
    public static final EnvType DEFAULT_STAGE_TYPE = EnvType.DEFAULT;

    public static final int DEFAULT_MAX_PARALLEL_HOSTS = 1;
    public static final int DEFAULT_SUCCESS_THRESHOLD = 100;
    public static final int DEFAULT_STUCK_THRESHOLD = 600;
    public static final String SYSTEM_OPERATOR = "system";
    public static final String BUILD_STAGE = "BUILD";
    public static final String DEFAULT_BRANCH_NAME = "master";
    public static final int DEFAULT_MAX_PROMOTE_QUEUE_SIZE = 10;
    public static final int DEFAULT_PROMOTE_QUEUE_SIZE = 1;
    public static final int DEFAULT_PROMOTE_DELAY_MINUTES = 0;
    public static final String AUTO_PROMOTER_NAME = "AutoPromoter";
    public static final int DEFAULT_DEPLOY_NUM = 5000;
    public static final int DEFAULT_DEPLOY_DAY = 365;

    // TODO this is a hack to use NULL represent host with not group info
    // Ideally we use database value NULL, but it will break a lot of existing
    // functions, need to revisit
    public static final String NULL_HOST_GROUP = "NULL";

    public static final String CONFIG_TYPE_ENV = "Deploy Env Config Change";
    public static final String TYPE_ENV_GENERAL = "Env General Config";
    public static final String TYPE_ENV_PROMOTE = "Env Promote Config";
    public static final String TYPE_ENV_SCRIPT = "Env Script Config";
    public static final String TYPE_ENV_ADVANCED = "Env Advanced Config";
    public static final String TYPE_ENV_HOST_CAPACITY = "Env Host Capacity Config";
    public static final String TYPE_ENV_GROUP_CAPACITY = "Env Group Capacity Config";
    public static final String TYPE_ENV_METRIC = "Env Metrics Config";
    public static final String TYPE_ENV_ALARM = "Env Alarm Config";
    public static final String TYPE_ENV_WEBHOOK = "Env Webhook Config";
    public static final String TYPE_ENV_ACTION = "Env Action";
    public static final String TYPE_HOST_ACTION = "Host Action";

    public static final String CLIENT_ERROR_SHORT = "short";
}
