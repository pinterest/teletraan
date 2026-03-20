/**
 * Copyright (c) 2026 Pinterest, Inc.
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

public class UdmDataUpdateServiceFactory {
    @JsonProperty private String udmDataUpdateServiceUrl;

    @JsonProperty private String httpProxyAddr;

    @JsonProperty private String httpProxyPort;

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

    public String getUdmDataUpdateServiceUrl() {
        return udmDataUpdateServiceUrl;
    }

    public void setUdmDataUpdateServiceUrl(String udmDataUpdateServiceUrl) {
        this.udmDataUpdateServiceUrl = udmDataUpdateServiceUrl;
    }
}
