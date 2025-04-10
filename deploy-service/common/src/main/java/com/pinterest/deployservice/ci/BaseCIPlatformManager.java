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
package com.pinterest.deployservice.ci;

import java.io.IOException;

public abstract class BaseCIPlatformManager implements CIPlatformManager {
    private String typeName;
    private Integer priority;

    public BaseCIPlatformManager(String typeName, Integer priority) {
        this.typeName = typeName;
        this.priority = priority;
    }

    @Override
    public String getTypeName() {
        return typeName;
    }

    @Override
    public Integer getPriority() {
        return priority;
    }

    @Override
    public String startBuild(String pipelineName, String buildParams) throws IOException {
        return "";
    }

    @Override
    public Object getBuild(String pipelineName, String buildId) throws Exception {
        return null;
    }

    @Override
    public boolean jobExist(String jobName) throws Exception {
        return false;
    }
}
