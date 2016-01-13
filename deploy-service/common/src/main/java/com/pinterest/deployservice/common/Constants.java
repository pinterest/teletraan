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
package com.pinterest.deployservice.common;

import com.pinterest.deployservice.bean.*;

public class Constants {
    public static final DeployPriority DEFAULT_PRIORITY = DeployPriority.NORMAL;
    public static final AcceptanceStatus DEFAULT_ACCEPTANCE_STATUS = AcceptanceStatus.PENDING_DEPLOY;
    public static final PromoteType DEFAULT_PROMOTE_TYPE = PromoteType.MANUAL;
    public static final PromoteFailPolicy DEFAULT_PROMOTE_FAIL_POLICY = PromoteFailPolicy.CONTINUE;
    public static final PromoteDisablePolicy DEFAULT_PROMOTE_DISABLE_POLICY = PromoteDisablePolicy.AUTO;
    public static final AcceptanceType DEFAULT_ACCEPTANCE_TYPE = AcceptanceType.AUTO;

    public static final int DEFAULT_MAX_PARALLEL_HOSTS = 1;
    public static final int DEFAULT_SUCCESS_THRESHOLD = 100;
    public static final int DEFAULT_STUCK_THRESHOLD = 600;
    public static final String SYSTEM_OPERATOR = "system";
    public static final String BUILD_STAGE = "BUILD";
    public static final String DEFAULT_BRANCH_NAME = "master";
    public static final int DEFAULT_MAX_PROMOTE_QUEUE_SIZE = 10;
    public static final int DEFAULT_PROMOTE_QUEUE_SIZE = 10;
    public static final int DEFAULT_PROMOTE_DELAY_MINUTES = 0;
    public static final String AUTO_PROMOTER_NAME = "AutoPromoter";
    public static final int DEFAULT_DEPLOY_NUM = 5000;
    public static final int DEFAULT_DEPLOY_DAY = 365;
}
