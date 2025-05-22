/**
 * Copyright (c) 2025 Pinterest, Inc.
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

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.pinterest.deployservice.common.KeyReader;
import com.pinterest.deployservice.common.KnoxKeyReader;
import com.pinterest.teletraan.universal.http.HttpClient;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Wrapper for Buildkite API calls */
public class Buildkite extends BaseCIPlatformManager {
    private static final Logger LOG = LoggerFactory.getLogger(Buildkite.class);
    private static HttpClient httpClient;
    private static Gson gson;
    // example: curl -sS -H "Authorization: Bearer bkpt_xxxxx" -H
    // "Content-Type: application/json"
    // -d '{ "commit": "HEAD", "branch": "master", "ignore_pipeline_branch_filters": true, "env":
    // ["TEST_ENV1=ENV1", "TEST_ENV2=ENV2"],
    // "metaData": [{ "key": "metadata_var1", "value": "metadata_val1" }, { "key": "metadata_var2",
    // "value": "metadata_val2" }, { "key": "metadata_var3", "value": "metadata_val3" }] }' -X POST
    // "https://portal.buildkite.com/organizations/pinterest/portals/trigger-buildkite-agent-integration-tests"
    private String triggerBuildBodyString =
            "{\"commit\": \"%s\","
                    + "\"branch\": \"%s\","
                    + "\"message\": \"%s\","
                    + "\"metaData\": [%s]}";
    private String queryBuildStatusBodyString = "{\"uuid\": \"%s\"}";
    private static String buildkiteUrl = "https://buildkite.com/pinterest/";
    private String buildkiteApiBaseUrl = "https://api.buildkite.com/v2/organizations/pinterest/";
    private String buildkitePortalBaseUrl =
            "https://portal.buildkite.com/organizations/pinterest/portals/";

    public Buildkite(
            String buildkitePortalBaseUrl,
            String buildkiteApiBaseUrl,
            String typeName,
            Integer priority) {
        super(typeName, priority);
        this.buildkiteApiBaseUrl = buildkiteApiBaseUrl;
        this.buildkitePortalBaseUrl = buildkitePortalBaseUrl;
        HttpClient.HttpClientBuilder clientBuilder = HttpClient.builder();
        httpClient = clientBuilder.build();
        this.gson =
                new GsonBuilder()
                        .addSerializationExclusionStrategy(new CustomExclusionStrategy())
                        .create();
    }

    private static class CustomExclusionStrategy implements ExclusionStrategy {
        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            return f.getName().equals("__isset_bit_vector");
        }

        @Override
        public boolean shouldSkipClass(Class<?> c) {
            return false;
        }
    }

    // https://buildkite.com/docs/apis/rest-api/builds#get-a-build
    public static class Build extends BaseCIPlatformBuild {
        String pipelineName;
        String buildUUID;
        String buildUrl;
        String buildStatus;
        long startTimestamp;
        long duration;
        private String queryLatestBuildsBodyString = "{\"pipeline_slug\" : \"%s\"}";

        public Build(
                String pipelineName,
                String buildUUID,
                String buildUrl,
                String buildStatus,
                long startTimestamp,
                long duration) {
            super(buildUUID, buildStatus, startTimestamp, duration);
            this.buildUUID = buildUUID;
            this.buildUrl = buildUrl;
            this.buildStatus = buildStatus;
            this.startTimestamp = startTimestamp;
            this.duration = duration;
            this.pipelineName = pipelineName;
        }

        @Override
        public String getBuildUUID() {
            return this.buildUUID;
        }

        public void setBuildUUID(String buildUUID) {
            this.buildUUID = buildUUID;
        }

        public String getPipelineName() {
            return this.pipelineName;
        }

        public void setPipelineName(String pipelineName) {
            this.pipelineName = pipelineName;
        }

        public String getBuildUrl() {
            return this.buildUrl;
        }

        public void setBuildUrl(String buildUrl) {
            this.buildUrl = buildUrl;
        }

        @Override
        public String getStatus() {
            return this.buildStatus;
        }

        public void setStatus(String buildStatus) {
            this.buildStatus = buildStatus;
        }

        public long getStartTimestamp() {
            return this.startTimestamp;
        }

        public void setStartTimestamp(long startTimestamp) {
            this.startTimestamp = startTimestamp;
        }

        public long getDuration() {
            return this.duration;
        }

        public void setDuration(long duration) {
            this.duration = duration;
        }

        public String getCIPlatformBaseUrl() {
            return buildkiteUrl;
        }

        @Override
        public int getProgress() {
            if (this.buildStatus.equals("passed")) {
                return 100;
            } else if (this.buildStatus.equals("failed")) {
                return 100;
            } else if (this.buildStatus.equals("canceled")) {
                return 100;
            } else if (this.buildStatus.equals("blocked")) {
                return 100;
            } else if (this.buildStatus.equals("skipped")) {
                return 0;
            } else {
                return (int)
                        (duration / getLastBuildsAverageTime("pinterest/" + this.pipelineName));
            }
        }

        private long getLastBuildsAverageTime(String pipelineName) {
            KeyReader knoxKeyReader = new KnoxKeyReader();
            String knoxKeyString = "buildkite:query_latest_builds:portal:query_build";
            knoxKeyReader.init(knoxKeyString);
            String queryToken = knoxKeyReader.getKey();
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Authorization", "Bearer " + queryToken);
            String bodyString =
                    String.format(queryLatestBuildsBodyString, "pinterest/" + pipelineName);
            try {
                String res =
                        httpClient.post(
                                "https://portal.buildkite.com/organizations/pinterest/portals/query-latest-builds",
                                bodyString,
                                headers);
                JsonObject fullJson = gson.fromJson(res, JsonObject.class);
                if (fullJson == null || fullJson.isJsonNull()) {
                    return 0L;
                }
                JsonArray buildsEdges = null;
                JsonObject dataJson = fullJson.getAsJsonObject("data");
                if (dataJson != null && !dataJson.isJsonNull()) {
                    LOG.error("dataJson: " + dataJson);
                    JsonObject pipelineJson = dataJson.getAsJsonObject("pipeline");
                    if (pipelineJson != null && !pipelineJson.isJsonNull()) {
                        JsonObject buildsJson = pipelineJson.getAsJsonObject("builds");
                        if (buildsJson != null && !buildsJson.isJsonNull()) {
                            buildsEdges = buildsJson.getAsJsonArray("edges");
                        } else {
                            return 0L;
                        }
                    } else {
                        throw new IOException("Pipeline JSON is null");
                    }
                }

                int buildsCount = buildsEdges.size();
                long totalDuration = 0;
                for (JsonElement build : buildsEdges) {
                    JsonObject node = build.getAsJsonObject().getAsJsonObject("node");

                    String createdAt = node.get("createdAt").getAsString();
                    long createdAtTimestamp = Instant.parse(createdAt).toEpochMilli();
                    String finishedAt = node.get("finishedAt").getAsString();
                    long finishedAtTimestamp = Instant.parse(finishedAt).toEpochMilli();
                    long duration = finishedAtTimestamp - createdAtTimestamp;
                    totalDuration += duration;
                }
                return totalDuration / buildsCount;

            } catch (IOException t) {
                LOG.error("Error in querying latest builds for pipeline " + pipelineName, t);
                return 0L;
            }
        }
    }

    private String constructPortalEndpoint(String operation, String pipeline) {
        return buildkitePortalBaseUrl + operation + "-" + pipeline;
    }

    private String constructApiEndpoint(String pipeline) {
        return buildkiteApiBaseUrl + "pipelines/" + pipeline;
    }

    private String getPipelineDefaultBranch(String pipeline) {
        String knoxKeyString = "svc_buildkite:buildkite:readonly";
        KeyReader knoxKeyReader = new KnoxKeyReader();
        knoxKeyReader.init(knoxKeyString);
        String apiToken = knoxKeyReader.getKey();
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + apiToken);
        headers.put("Content-Type", "application/json");
        String defaultBranch = "";
        try {
            String res = httpClient.get(constructApiEndpoint(pipeline), null, headers);
            JsonObject fullJson = gson.fromJson(res, JsonObject.class);
            if (fullJson == null || fullJson.isJsonNull()) {
                return "";
            }
            if (fullJson.has("default_branch") && !fullJson.get("default_branch").isJsonNull()) {
                defaultBranch = fullJson.getAsJsonPrimitive("default_branch").getAsString();
            }
            return defaultBranch;
        } catch (Throwable t) {
            LOG.error(String.format("Error in querying pipeline %s default branch", pipeline), t);
            return "";
        }
    }

    @Override
    public String startBuild(String pipelineName, String buildParams) throws IOException {
        HashMap<String, String> buildMetadata = new HashMap<>();

        if (buildParams != null && !buildParams.isEmpty()) {
            String[] buildParamPairs = buildParams.split("&"); // Split by "&"
            for (String pair : buildParamPairs) {
                String[] keyValue = pair.split("=", 2); // Split by "=", limit to 2 parts
                if (keyValue.length == 2) {
                    String key = keyValue[0];
                    String value = keyValue[1];
                    buildMetadata.put(key, value);
                } else if (keyValue.length == 1) {
                    // This handles the case where there is a key without a value (e.g., "key=")
                    buildMetadata.put(keyValue[0], "");
                }
            }
        }
        return startBuild(pipelineName, "", "", "", buildMetadata);
    }

    private String startBuild(
            String pipeline,
            String commit,
            String branch,
            String message,
            HashMap<String, String> buildMetadata)
            throws IOException {

        if (commit == "") {
            commit = "HEAD";
        }
        if (branch == "") {
            branch = getPipelineDefaultBranch(pipeline);
        }
        if (message == "") {
            message = "Triggering build from Teletraan";
        }
        String knoxKeyString = "buildkite:%s:portal:create_build";
        knoxKeyString = String.format(knoxKeyString, pipeline.replaceAll("-", "_"));
        KeyReader knoxKeyReader = new KnoxKeyReader();
        knoxKeyReader.init(knoxKeyString);
        String apiToken = knoxKeyReader.getKey();
        String metadata = "";
        int count = 0;
        Set<String> keys = buildMetadata.keySet();
        for (String key : keys) {
            if (count < keys.size() - 1) {
                metadata +=
                        "{\"key\": \""
                                + key
                                + "\", \"value\": \""
                                + buildMetadata.get(key)
                                + "\"},";
            } else {
                metadata +=
                        "{\"key\": \"" + key + "\", \"value\": \"" + buildMetadata.get(key) + "\"}";
            }
            count++;
        }
        String bodyString =
                String.format(triggerBuildBodyString, commit, branch, message, metadata);
        LOG.debug("[Buildkite][startBuild] bodyString " + bodyString);
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + apiToken);
        try {
            String res =
                    httpClient.post(
                            constructPortalEndpoint("trigger", pipeline), bodyString, headers);
            LOG.debug(
                    "[Buildkite][startBuild] portal url "
                            + constructPortalEndpoint("trigger", pipeline));
            JsonObject fullJson = gson.fromJson(res, JsonObject.class);
            if (fullJson == null || fullJson.isJsonNull()) {
                LOG.error("Something went wrong triggering build for pipeline " + pipeline);
            }
            JsonObject data = fullJson.getAsJsonObject("data");
            JsonObject buildCreate = data.getAsJsonObject("buildCreate");
            JsonObject build = buildCreate.getAsJsonObject("build");
            JsonPrimitive url = build.getAsJsonPrimitive("url");
            JsonPrimitive uuid = build.getAsJsonPrimitive("uuid");
            LOG.debug("[Buildkite] Successfully triggered build for pipeline " + url.getAsString());
            String[] delimitStrings = url.getAsString().split("/");
            String jobNum = delimitStrings[delimitStrings.length - 1];
            return jobNum;
        } catch (Exception t) {
            LOG.error("Error in triggering build for pipeline " + pipeline, t);
            return "";
        }
    }

    @Override
    public Build getBuild(String pipeline, String buildUUID) throws IOException {
        LOG.debug("[Buildkite][getBuild] pipeline name: " + pipeline);
        String knoxKeyString = "svc_buildkite:buildkite:readonly";
        KeyReader knoxKeyReader = new KnoxKeyReader();
        knoxKeyReader.init(knoxKeyString);
        String apiToken = knoxKeyReader.getKey();
        String bodyString = String.format(queryBuildStatusBodyString, buildUUID);
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + apiToken);
        headers.put("Content-Type", "application/json");
        String url = "";
        String state = "";
        String startedAt = "";
        String finishedAt = "";
        long startTimestamp = 0L;
        long finishedTimestamp = 0L;
        long duration = 0L;
        try {
            String res =
                    httpClient.get(
                            constructApiEndpoint(pipeline) + "/builds/" + buildUUID, null, headers);
            JsonObject fullJson = gson.fromJson(res, JsonObject.class);
            // {"message": "Not Found"} indicates 404, but since we use httpclient within Teletraan
            // library, it doesn't throw an error when 404
            if (fullJson == null
                    || fullJson.isJsonNull()
                    || (fullJson.has("message")
                            && fullJson.get("message").getAsString().equals("Not Found"))) {
                return null;
            }
            state = fullJson.getAsJsonPrimitive("state").getAsString();
            url = fullJson.getAsJsonPrimitive("url").getAsString();
            startedAt = fullJson.getAsJsonPrimitive("started_at").getAsString();
            startTimestamp = Instant.parse(startedAt).toEpochMilli();
            if (fullJson.has("finished_at") && !fullJson.get("finished_at").isJsonNull()) {
                finishedAt = fullJson.getAsJsonPrimitive("finished_at").getAsString();
                finishedTimestamp = Instant.parse(finishedAt).toEpochMilli();
                duration = finishedTimestamp - startTimestamp;
            } else {
                startTimestamp = System.currentTimeMillis();
                duration = 0L;
            }
            return new Build(pipeline, buildUUID, url, state, startTimestamp, duration);
        } catch (Throwable t) {
            LOG.error(
                    String.format(
                            "Error in querying build status for pipeline %s and build %s",
                            pipeline, buildUUID),
                    t);
            return null;
        }
    }

    @Override
    public boolean jobExist(String pipeline) throws Exception {
        String knoxKeyString = "svc_buildkite:buildkite:readonly";
        KeyReader knoxKeyReader = new KnoxKeyReader();
        knoxKeyReader.init(knoxKeyString);
        String readOnlyToken = knoxKeyReader.getKey();
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + readOnlyToken);
        headers.put("Content-Type", "application/json");
        try {
            String res = httpClient.get(constructApiEndpoint(pipeline), null, headers);
            JsonObject fullJson = gson.fromJson(res, JsonObject.class);
            if (fullJson == null || fullJson.isJsonNull()) {
                return false;
            } else {
                return true;
            }
        } catch (Throwable t) {
            LOG.error(String.format("Error in checking if job %s exists", pipeline), t);
            return false;
        }
    }
}
