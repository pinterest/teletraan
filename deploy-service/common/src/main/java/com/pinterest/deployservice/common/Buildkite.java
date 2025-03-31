package com.pinterest.deployservice.common;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.List;
import java.util.Map;

import com.pinterest.deployservice.common.KnoxKeyReader;
import com.pinterest.teletraan.universal.http.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/** Wrapper for Buildkite API calls */
public class Buildkite {
    private static final Logger LOG = LoggerFactory.getLogger(Buildkite.class);
    private final KeyReader knoxKeyReader = new KnoxKeyReader();
    private final HttpClient httpClient;
    private final Gson gson;
    // example: curl -sS -H "Authorization: Bearer bkpt_dc9eb539b43438f707d97387a28b587a78914efc" -H "Content-Type: application/json" 
    // -d '{ "commit": "HEAD", "branch": "master", "ignore_pipeline_branch_filters": true, "env": ["TEST_ENV1=ENV1", "TEST_ENV2=ENV2"], 
    // "metaData": [{ "key": "metadata_var1", "value": "metadata_val1" }, { "key": "metadata_var2", "value": "metadata_val2" }, { "key": "metadata_var3", "value": "metadata_val3" }] }' -X POST "https://portal.buildkite.com/organizations/pinterest/portals/trigger-buildkite-agent-integration-tests"
    private String triggerBuildBodyString = 
            "{\"commit\": \"%s\","
            + "\"branch\": \"%s\","
            + "\"message\": \"%s\","
            + "\"metaData\": [\"%s\"]}";
    private String queryBuildStatusBodyString =
            "{\"uuid\": \"%s\"}";
    private String buildkiteApiBaseUrl = "https://api.buildkite.com/v2/organizations/pinterest/";
    private String buildkitePortalBaseUrl = "https://portal.buildkite.com/organizations/pinterest/portals/";

    public Buildkite(String buildkitePortalBaseUrl, String buildkiteApiBaseUrl) {
        this.buildkiteApiBaseUrl = buildkiteApiBaseUrl;
        this.buildkitePortalBaseUrl = buildkitePortalBaseUrl;
        HttpClient.HttpClientBuilder clientBuilder = HttpClient.builder();
        this.httpClient = clientBuilder.build();
        this.gson = new GsonBuilder()
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
    public static class BuildkiteBuild {
        String buildUUID;
        String buildUrl;
        String result;
        String buildStatus;
        long startTimestamp;
        long duration;

        public BuildkiteBuild(
                String buildUUID,
                String buildUrl,
                String result,
                String buildStatus,
                long startTimestamp,
                long duration) {
            this.buildUUID = buildUUID;
            this.buildUrl = buildUrl;
            this.result = result;
            this.buildStatus = buildStatus;
            this.startTimestamp = startTimestamp;
            this.duration = duration;
        }

        public String getbuildUUID() {
            return this.buildUUID;
        }

        public void setbuildUUID(String buildUUID) {
            this.buildUUID = buildUUID;
        }

        public String getBuildUrl() {
            return this.buildUrl;
        }

        public void setBuildUrl(String buildUrl) {
            this.buildUrl = buildUrl;
        }

        public String getResult() {
            return this.result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public String getBuildStatus() {
            return this.buildStatus;
        }

        public void setBuildStatus(String buildStatus) {
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
    }

    public String constructPortalEndpoint(String operation, String pipeline) {
        return buildkitePortalBaseUrl + operation + "-" + pipeline;
    }

    public String constructApiEndpoint(String pipeline) {
        return buildkiteApiBaseUrl + "pipelines/" + pipeline;
    }

    public List<String> triggerBuild(
            String pipeline,
            String commit,
            String branch,
            String message,
            HashMap<String, String> buildMetadata) throws IOException {
        
        if (commit == "") {
            commit = "HEAD";
        }
        if (branch == "") {
            branch = "master";
        }
        if (message == "") {
            message = "Triggering build from Teletraan";
        }
        String knoxKeyString = "buildkite:%s:portal:create_build";
        knoxKeyString = String.format(knoxKeyString, pipeline.replaceAll("-", "_"));
        knoxKeyReader.init(knoxKeyString);
        LOG.debug("[Buildkite] Trying to fetch key " + knoxKeyString);
        String apiToken = knoxKeyReader.getKey();
        String metadata = "";
        int count = 0;
        Set<String> keys = buildMetadata.keySet();
        for (String key : keys) {
            if (count < keys.size() - 1) {
                metadata += "{\"key\": \"" + key + "\", \"value\": \"" + buildMetadata.get(key) + "\"},";
            } else {
                metadata += "{\"key\": \"" + key + "\", \"value\": \"" + buildMetadata.get(key) + "\"}";
            }
            count++;
        }
        String bodyString = String.format(triggerBuildBodyString,
                commit,
                branch,
                message,
                metadata);
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + apiToken);
        try {
            String res = httpClient.post(constructPortalEndpoint("trigger", pipeline), bodyString, headers);
            JsonObject fullJson = gson.fromJson(res, JsonObject.class);
            if (fullJson == null || fullJson.isJsonNull()) {
                return null;
            }
            JsonObject data = fullJson.getAsJsonObject("data");
            JsonObject buildCreate = data.getAsJsonObject("buildCreate");
            JsonObject build = buildCreate.getAsJsonObject("build");
            JsonPrimitive uuid = build.getAsJsonPrimitive("uuid");
            JsonPrimitive url = build.getAsJsonPrimitive("url");
            return Arrays.asList(uuid.getAsString(), url.getAsString());
        } catch (Throwable t) {
            LOG.error("Error in triggering build for pipeline " + pipeline, t);
            return null;
        }
    }

    public String queryBuildStatus(String pipeline, String buildUUID) throws IOException {
        String knoxKeyString = "buildkite:%s:portal:query_build";
        knoxKeyReader.init(String.format(knoxKeyString, pipeline.replaceAll("-", "_")));
        String apiToken = knoxKeyReader.getKey();
        String bodyString = String.format(queryBuildStatusBodyString, buildUUID);
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + apiToken);
        headers.put("Content-Type", "application/json");
        try {
            String res = httpClient.post(constructPortalEndpoint("query", pipeline), bodyString, headers);
            JsonObject fullJson = gson.fromJson(res, JsonObject.class);
            if (fullJson == null || fullJson.isJsonNull()) {
                return null;
            }
            JsonObject data = fullJson.getAsJsonObject("data");
            JsonObject build = data.getAsJsonObject("build");
            JsonPrimitive state = build.getAsJsonPrimitive("state");
            return state.getAsString();
        } catch (Throwable t) {
            LOG.error(String.format("Error in querying build status for pipeline %s and build %s", pipeline, buildUUID), t);
            return null;
        }
    }

    public boolean jobExist(String pipeline) throws IOException{
        String knoxKeyString = "svc_buildkite:buildkite:readonly";
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
            }
            else {
                return true;
            }
        } catch (Throwable t) {
            LOG.error(String.format("Error in checking if job %s exists", pipeline), t);
            return false;
        }
    }
}
