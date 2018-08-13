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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

// A simple HttpURLConnection wrapper
public class HTTPClient {
    private static final Logger LOG = LoggerFactory.getLogger(HTTPClient.class);

    private String generateUrlAndQuery(String url, Map<String, String> params) throws Exception {
        if (params == null || params.isEmpty()) {
            return url;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(url);
        String prefix = "?";
        for (Map.Entry<String, String> entry : params.entrySet()) {
            sb.append(prefix);
            prefix = "&";
            sb.append(String.format("%s=%s", entry.getKey(),
                URLEncoder.encode(entry.getValue(), "UTF-8")));
        }
        return sb.toString();
    }

    public String get(String url, String payload, Map<String, String> params, Map<String, String> headers, int retries) throws Exception {
        String urlAndQuery = generateUrlAndQuery(url, params);
        return internalCall(urlAndQuery, "GET", payload, headers, retries);
    }

    public String post(String url, String payload, Map<String, String> headers, int retries) throws Exception {
        return internalCall(url, "POST", payload, headers, retries);
    }

    public String put(String url, String payload, Map<String, String> headers, int retries) throws Exception {
        return internalCall(url, "PUT", payload, headers, retries);
    }

    public String delete(String url, String payload, Map<String, String> headers, int retries) throws Exception {
        return internalCall(url, "DELETE", payload, headers, retries);
    }

    private String internalCall(String url, String method, String payload, Map<String, String> headers, int retries) throws Exception {
        HttpURLConnection conn = null;
        Exception lastException = null;

        for (int i = 0; i < retries; i++) {
            try {
                URL urlObj = new URL(url);
                conn = (HttpURLConnection) urlObj.openConnection();
                conn.setRequestMethod(method);
                conn.setRequestProperty("Accept-Charset", "UTF-8");
                conn.setConnectTimeout(15000)

                if (headers != null) {
                    for (Map.Entry<String, String> entry : headers.entrySet()) {
                        conn.setRequestProperty(entry.getKey(), entry.getValue());
                    }
                }

                if (StringUtils.isNotEmpty(payload)) {
                    conn.setDoOutput(true);
                    OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
                    writer.write(payload);
                    writer.flush();
                    writer.close();
                }

                String ret = IOUtils.toString(conn.getInputStream(), "UTF-8");
                int responseCode = conn.getResponseCode();
                if (responseCode >= 400) {
                    throw new DeployInternalException("HTTP request failed, status = {}, content = {}",
                        responseCode, ret);
                }
                LOG.info("HTTP Request returned with response code {} for URL {}", responseCode, url);
                return ret;
            } catch (Exception e) {
                lastException = e;
                LOG.error("Failed to send HTTP Request to {}, with payload {}, with headers {}",
                    url, payload, headers, e);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
        throw lastException;
    }
}
