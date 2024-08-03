/**
 * Copyright (c) 2016-2023 Pinterest, Inc.
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

public class RodimusFactory {
    @JsonProperty private String rodimusUrl;

    @JsonProperty private String knoxKey;

    @JsonProperty private boolean useProxy;

    @JsonProperty private String httpProxyAddr;

    @JsonProperty private String httpProxyPort;

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

    public String getRodimusUrl() {
        return rodimusUrl;
    }

    public void setRodimusUrl(String rodimusUrl) {
        this.rodimusUrl = rodimusUrl;
    }

    public String getKnoxKey() {
        return this.knoxKey;
    }

    public void setKnoxKey(String keyName) {
        this.knoxKey = keyName;
    }
}
