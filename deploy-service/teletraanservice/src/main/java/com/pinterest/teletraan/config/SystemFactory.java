/**
 * Copyright (c) 2016-2024 Pinterest, Inc.
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

public class SystemFactory {
    private static final String DEFAULT_DASHBOARD_URL = "http://localhost:8888";

    @JsonProperty private String dashboardUrl = DEFAULT_DASHBOARD_URL;

    @JsonProperty private String changeFeedUrl;

    @JsonProperty private long agentCountCacheTtl = 10 * 1000l;

    @JsonProperty private long maxParallelThreshold = 10000;

    @JsonProperty private boolean aclManagementEnabled = true;

    @JsonProperty
    private String aclManagementDisabledMessage =
            "ACL management is disabled. Please contact your admin for modification.";

    public String getDashboardUrl() {
        return dashboardUrl;
    }

    public void setDashboardUrl(String dashboardUrl) {
        this.dashboardUrl = dashboardUrl;
    }

    public String getChangeFeedUrl() {
        return changeFeedUrl;
    }

    public void setChangeFeedUrl(String changeFeedUrl) {
        this.changeFeedUrl = changeFeedUrl;
    }

    public Long getAgentCountCacheTtl() {
        return agentCountCacheTtl;
    }

    public void setAgentCountCacheTtl(Long agentCountCacheTtl) {
        this.agentCountCacheTtl = agentCountCacheTtl;
    }

    public Long getMaxParallelThreshold() {
        return maxParallelThreshold;
    }

    public void setMaxParallelThreshold(Long maxParallelThreshold) {
        this.maxParallelThreshold = maxParallelThreshold;
    }

    public boolean isAclManagementEnabled() {
        return aclManagementEnabled;
    }

    public void setAclManagementEnabled(boolean aclManagementEnabled) {
        this.aclManagementEnabled = aclManagementEnabled;
    }

    public String getAclManagementDisabledMessage() {
        return aclManagementDisabledMessage;
    }

    public void setAclManagementDisabledMessage(String aclManagementDisabledMessage) {
        this.aclManagementDisabledMessage = aclManagementDisabledMessage;
    }
}
