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
package com.pinterest.teletraan.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.pinterest.deployservice.ci.Buildkite;
import com.pinterest.deployservice.ci.CIPlatformManager;

@JsonTypeName("buildkite")
public class BuildkiteFactory implements CIPlatformFactory {
    @JsonProperty private String buildkitePortalBaseUrl;
    @JsonProperty private String buildkiteApiBaseUrl;
    @JsonProperty private String typeName;
    @JsonProperty private Integer priority;

    public String getBuildkitePortalBaseUrl() {
        return this.buildkitePortalBaseUrl;
    }

    public void setBuildkitePortalBaseUrl(String buildkitePortalBaseUrl) {
        this.buildkitePortalBaseUrl = buildkitePortalBaseUrl;
    }

    public String getBuildkiteApiBaseUrl() {
        return this.buildkiteApiBaseUrl;
    }

    public void setBuildkiteApiBaseUrl(String buildkiteApiBaseUrl) {
        this.buildkiteApiBaseUrl = buildkiteApiBaseUrl;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    @Override
    public CIPlatformManager create() throws Exception {
        if (typeName == null || typeName.isEmpty()) {
            typeName = "buildkite";
        }
        if (priority == null) {
            priority = 1;
        }
        return new Buildkite(buildkitePortalBaseUrl, buildkiteApiBaseUrl, typeName, priority);
    }
}
