/**
 * Copyright (c) 2024 Pinterest, Inc.
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
import com.pinterest.deployservice.ci.CIPlatformManager;
import com.pinterest.deployservice.ci.Jenkins;
import javax.validation.constraints.NotNull;

@JsonTypeName("jenkins")
public class JenkinsFactory implements CIPlatformFactory {
    @JsonProperty private String jenkinsUrl;

    @JsonProperty private String remoteToken;

    @JsonProperty private boolean useProxy;

    @JsonProperty private String httpProxyAddr;

    @JsonProperty private String httpProxyPort;

    @NotNull @JsonProperty private String typeName;

    @NotNull @JsonProperty private int priority;

    public String getJenkinsUrl() {
        return jenkinsUrl;
    }

    public void setJenkinsUrl(String jenkinsUrl) {
        this.jenkinsUrl = jenkinsUrl;
    }

    public String getRemoteToken() {
        return remoteToken;
    }

    public void setRemoteToken(String remoteToken) {
        this.remoteToken = remoteToken;
    }

    public boolean getUseProxy() {
        return useProxy;
    }

    public void setUseProxy(boolean useProxy) {
        this.useProxy = useProxy;
    }

    public String getHttpProxyAddr() {
        return httpProxyAddr;
    }

    public void setHttpProxyAddr(String httpProxyAddr) {
        this.httpProxyAddr = httpProxyAddr;
    }

    public String getHttpProxyPort() {
        return httpProxyPort;
    }

    public void setHttpProxyPort(String httpProxyPort) {
        this.httpProxyPort = httpProxyPort;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public CIPlatformManager create() throws Exception {
        return new Jenkins(
                jenkinsUrl,
                remoteToken,
                useProxy,
                httpProxyAddr,
                httpProxyPort,
                typeName,
                priority);
    }
}
