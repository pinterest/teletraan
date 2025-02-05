/**
 * Copyright (c) 2023-2024 Pinterest, Inc.
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
package com.pinterest.deployservice.events;

import com.google.common.collect.ImmutableMap;
import com.pinterest.teletraan.universal.events.ResourceChangedEvent;

public class DeployEvent extends ResourceChangedEvent {
    private final String env;

    private final String stage;

    private final String commit;

    private final String operator;

    public DeployEvent(Object source, String env, String stage, String commit, String operator) {
        super("deployed_build", operator, source, ImmutableMap.of("env", env, "stage", stage));
        this.operator = operator;
        this.env = env;
        this.stage = stage;
        this.commit = commit;
    }

    public String getEnv() {
        return env;
    }

    public String getStage() {
        return stage;
    }

    public String getCommit() {
        return commit;
    }

    public String getOperator() {
        return operator;
    }
}
