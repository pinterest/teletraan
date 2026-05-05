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
package com.pinterest.deployservice.udm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.teletraan.universal.http.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UdmDataUpdateService {
    private static final Logger LOG = LoggerFactory.getLogger(UdmDataUpdateService.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final String udmDataUpdateServiceUrl;
    private final HttpClient httpClient;

    public UdmDataUpdateService(
            String udmDataUpdateServiceUrl, String httpProxyAddr, String httpProxyPort)
            throws Exception {

        this.udmDataUpdateServiceUrl = udmDataUpdateServiceUrl;
        this.httpClient =
                HttpClient.builder()
                        .useProxy(true)
                        .httpProxyAddr(httpProxyAddr)
                        .httpProxyPort(parseHttpProxyPort(httpProxyPort))
                        .build();
    }

    public void notifyStageCreated(EnvironBean environBean) throws Exception {
        String envName = environBean.getEnv_name();
        String stageName = environBean.getStage_name();

        try {
            LOG.info("Notifying udm of stage creation: {}/{}", envName, stageName);

            String url =
                    String.format(
                            "%s/v1/teletraan_stage/%s/%s",
                            udmDataUpdateServiceUrl, envName, stageName);
            String body = mapper.writeValueAsString(environBean);
            httpClient.put(url, body, null);
        } catch (Exception e) {
            LOG.error("Failed to notify udm of stage creation {}/{}", envName, stageName, e);
        }
    }

    public void notifyStageDeleted(String envName, String stageName) throws Exception {
        try {
            LOG.info("Notifying udm of stage deletion: {}/{}", envName, stageName);

            String url =
                    String.format(
                            "%s/v1/teletraan_stage/%s/%s",
                            udmDataUpdateServiceUrl, envName, stageName);
            httpClient.delete(url, null, null);
        } catch (Exception e) {
            LOG.error("Failed to notify udm of stage deletion {}/{}", envName, stageName, e);
        }
    }

    private int parseHttpProxyPort(String httpProxyPort) {
        try {
            return Integer.parseInt(httpProxyPort);
        } catch (NumberFormatException e) {
            LOG.error("Invalid http proxy port {} for udm data update service", httpProxyPort, e);
            throw e;
        }
    }
}
