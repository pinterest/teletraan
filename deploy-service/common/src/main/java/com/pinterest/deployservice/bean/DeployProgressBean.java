/*
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
package com.pinterest.deployservice.bean;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.util.List;

public class DeployProgressBean {
    private List<AgentBean> agents;
    private List<String> missingHosts;
    private List<HostBean> provisioningHosts;

    public List<AgentBean> getAgents() {
        return agents;
    }

    public void setAgents(List<AgentBean> agents) {
        this.agents = agents;
    }

    public List<String> getMissingHosts() {
        return missingHosts;
    }

    public void setMissingHosts(List<String> missingHosts) {
        this.missingHosts = missingHosts;
    }

    public List<HostBean> getProvisioningHosts() {
        return provisioningHosts;
    }

    public void setProvisioningHosts(List<HostBean> provisioningHosts) {
        this.provisioningHosts = provisioningHosts;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
