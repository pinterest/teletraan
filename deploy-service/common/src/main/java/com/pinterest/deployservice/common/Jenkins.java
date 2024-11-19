/**
 * Copyright (c) 2016-2017 Pinterest, Inc.
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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pinterest.teletraan.universal.http.HttpClient;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: make it generic
/** Wrapper for Jenkins API calls */
public class Jenkins {
    private static final Logger LOG = LoggerFactory.getLogger(Jenkins.class);
    private static final HttpClient httpClient = HttpClient.builder().build();
    private String jenkinsUrl;
    private String jenkinsRemoteToken;

    public Jenkins(String jenkinsUrl, String jenkinsRemoteToken) {
        this.jenkinsUrl = jenkinsUrl;
        this.jenkinsRemoteToken = jenkinsRemoteToken;
    }

    public static class Build {
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

        public int getProgress() {
            long assumedProgress = duration / estimateDuration;
            if (!this.isBuilding) {
                return 100;
            } else {
                return (int) assumedProgress;
            }
        }
    }

    public void startBuild(String jobName, String buildParams) throws Exception {
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
    }

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
}
