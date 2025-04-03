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
package com.pinterest.deployservice.ci;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pinterest.teletraan.universal.http.HttpClient;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: make it generic
/** Wrapper for Jenkins API calls */
public class Jenkins extends BaseCIPlatformManager {
    private static final Logger LOG = LoggerFactory.getLogger(Jenkins.class);
    private final HttpClient httpClient;
    private final String jenkinsUrl;
    private final String jenkinsRemoteToken;

    public Jenkins(
            String jenkinsUrl,
            String jenkinsRemoteToken,
            boolean useProxy,
            String httpProxyAddr,
            String httpProxyPort,
            String typeName,
            int priority) {
        super(typeName, priority);
        this.jenkinsUrl = jenkinsUrl;
        this.jenkinsRemoteToken = jenkinsRemoteToken;

        int httpProxyPortInt;
        HttpClient.HttpClientBuilder clientBuilder = HttpClient.builder();
        if (useProxy) {
            try {
                httpProxyPortInt = Integer.parseInt(httpProxyPort);
            } catch (NumberFormatException exception) {
                LOG.error("Failed to parse Jenkins port: {}", httpProxyPort, exception);
                throw exception;
            }
            clientBuilder
                    .useProxy(true)
                    .httpProxyAddr(httpProxyAddr)
                    .httpProxyPort(httpProxyPortInt);
        }
        this.httpClient = clientBuilder.build();
    }

    public static class Build implements CIPlatformBuild {
        String buildId;
        String result;
        boolean isBuilding;
        long startTimestamp;
        long estimateDuration;
        long duration;

        public Build(
                String buildId,
                String result,
                boolean isBuilding,
                long startTimestamp,
                int estimateDuration,
                int duration) {
            this.buildId = buildId;
            this.result = result;
            this.isBuilding = isBuilding;
            this.startTimestamp = startTimestamp;
            this.estimateDuration = estimateDuration;
            this.duration = duration;
        }

        @Override
        public String getBuildUUID() {
            return this.buildId;
        }

        @Override
        public String getStatus() {
            if (this.isBuilding) {
                return "RUNNING";
            } else {
                return this.result;
            }
        }

        public long getEstimate_duration() {
            return this.estimateDuration;
        }

        @Override
        public int getProgress() {
            long assumedProgress = duration / estimateDuration;
            if (!this.isBuilding) {
                return 100;
            } else {
                return (int) assumedProgress;
            }
        }
    }

    public String getJenkinsUrl() {
        return jenkinsUrl;
    }

    public String startBuild(String jobName, String buildParams) throws Exception {
        String tokenString = "";
        if (this.jenkinsRemoteToken != null)
            tokenString = String.format("token=%s&", this.jenkinsRemoteToken);

        String url =
                String.format(
                        "%s/job/%s/buildWithParameters?%s%s",
                        this.jenkinsUrl, jobName, tokenString, buildParams);
        // Use GET instead, which is the same as POST but no need for token
        httpClient.get(url, null, null);
        LOG.info("Successfully post to jenkins for job " + jobName);
        return "";
    }

    @Override
    public Build getBuild(String jobName, String jobNum) throws Exception {
        String url = String.format("%s/job/%s/%s/api/json", this.jenkinsUrl, jobName, jobNum);
        LOG.debug("Calling jenkins with url " + url);
        String ret = httpClient.get(url, null, null);
        JsonObject json = (JsonObject) JsonParser.parseString(ret);
        return new Build(
                json.get("number").toString(),
                json.get("result").toString(),
                Boolean.parseBoolean(json.get("building").toString()),
                Long.parseLong(json.get("timestamp").toString()),
                Integer.parseInt(json.get("estimatedDuration").toString()),
                Integer.parseInt(json.get("duration").toString()));
    }

    @Override
    public boolean jobExist(String pipeline) throws IOException {
        String url = String.format("%s/job/%s/api/json", this.jenkinsUrl, pipeline);
        LOG.debug("Calling jenkins with url " + url);
        try {
            String ret = httpClient.get(url, null, null);
            JsonObject json = (JsonObject) JsonParser.parseString(ret);
            return json != null;
        } catch (IOException e) {
            LOG.error("Failed to get job info from jenkins", e);
            return false;
        }
    }
}
