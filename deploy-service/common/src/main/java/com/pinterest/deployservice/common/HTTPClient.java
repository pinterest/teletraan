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
package com.pinterest.deployservice.common;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// A simple HttpURLConnection wrapper
public class HTTPClient {
    private static final int TIMEOUT = 15 * 1000; // http timeout in 15 seconds
    private static final Logger LOG = LoggerFactory.getLogger(HTTPClient.class);
    public static String secretMask = "xxxxxxxxx";
    private boolean useProxy = false;
    private String httpProxyAddr;
    private int httpProxyPort;

    public HTTPClient() {
        // HTTPClient useProxy default is false
    }

    public HTTPClient(boolean useProxy, String httpProxyAddr, int httpProxyPort) {
        if (Boolean.TRUE.equals(useProxy)) {
            if (httpProxyAddr == null) {
                throw new IllegalArgumentException(
                        "useProxy was configured but missing required httpProxyAddr");
            }
            this.useProxy = useProxy;
            this.httpProxyAddr = httpProxyAddr;
            this.httpProxyPort = httpProxyPort;
        }
    }

    public boolean getUseProxy() {
        // HTTPClient useProxy default is false
        return useProxy;
    }

    public String getHttpProxyAddr() {
        return httpProxyAddr;
    }

    public int getHttpProxyPort() {
        return httpProxyPort;
    }

    private String generateUrlAndQuery(String url, Map<String, String> params, boolean scrubUrl)
            throws Exception {
        if (params == null || params.isEmpty()) {
            return url;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(url);
        String prefix = "?";
        for (Map.Entry<String, String> entry : params.entrySet()) {
            sb.append(prefix);
            prefix = "&";
            // note: scrubUrlQueryValue could be expensive with many filtered values
            // consider using it only in only a DEBUG logging context in the future
            String reportedValue =
                    scrubUrl
                            ? scrubUrlQueryValue(entry.getKey(), entry.getValue())
                            : entry.getValue();
            sb.append(
                    String.format(
                            "%s=%s", entry.getKey(), URLEncoder.encode(reportedValue, "UTF-8")));
        }
        return sb.toString();
    }

    private String scrubUrlQueryValue(String queryParamKey, String queryParamValue) {
        String[] filteredQueryKeySubstrings = {"token"};

        for (String filteredQueryKeySubstring : filteredQueryKeySubstrings) {
            if (StringUtils.containsIgnoreCase(queryParamKey, filteredQueryKeySubstring)) {
                return secretMask;
            }
        }
        return queryParamValue;
    }

    public String get(
            String url,
            String payload,
            Map<String, String> params,
            Map<String, String> headers,
            int retries)
            throws Exception {
        return internalCall(url, params, "GET", payload, headers, retries);
    }

    public String post(String url, String payload, Map<String, String> headers, int retries)
            throws Exception {
        return internalCall(url, null, "POST", payload, headers, retries);
    }

    public String put(String url, String payload, Map<String, String> headers, int retries)
            throws Exception {
        return internalCall(url, null, "PUT", payload, headers, retries);
    }

    public String delete(String url, String payload, Map<String, String> headers, int retries)
            throws Exception {
        return internalCall(url, null, "DELETE", payload, headers, retries);
    }

    private String internalCall(
            String base_url,
            Map<String, String> params,
            String method,
            String payload,
            Map<String, String> headers,
            int retries)
            throws Exception {
        HttpURLConnection conn = null;
        Exception lastException = null;

        String scrubbedUrl = generateUrlAndQuery(base_url, params, true);
        String url = generateUrlAndQuery(base_url, params, false);

        for (int i = 0; i < retries; i++) {
            try {
                URL urlObj = new URL(url);
                if (useProxy) {
                    Proxy httpProxy =
                            new Proxy(
                                    Proxy.Type.HTTP,
                                    new InetSocketAddress(httpProxyAddr, httpProxyPort));
                    conn = (HttpURLConnection) urlObj.openConnection(httpProxy);
                } else {
                    conn = (HttpURLConnection) urlObj.openConnection();
                }
                conn.setRequestMethod(method);
                conn.setRequestProperty("Accept-Charset", "UTF-8");
                conn.setConnectTimeout(TIMEOUT);
                conn.setReadTimeout(TIMEOUT);

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
                    throw new DeployInternalException(
                            "HTTP request failed, status = {}, content = {}", responseCode, ret);
                }
                LOG.info(
                        "HTTP Request returned with response code {} for URL {}",
                        responseCode,
                        scrubbedUrl);
                return ret;
            } catch (Exception e) {
                lastException = e;
                String proxyMsg = "";
                if (useProxy) {
                    proxyMsg = String.format(" via proxy %s:%s,", httpProxyAddr, httpProxyPort);
                }
                LOG.error(
                        "Failed to send HTTP Request to {},{} with method {} with payload {}, with headers {}",
                        scrubbedUrl,
                        proxyMsg,
                        method,
                        payload,
                        headers,
                        e);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
        throw lastException;
    }
}
