/*
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
package com.pinterest.deployservice.rodimus;


import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;

import com.pinterest.deployservice.common.HTTPClient;
import com.pinterest.deployservice.knox.CommandLineKnox;
import com.pinterest.deployservice.knox.FileSystemKnox;
import com.pinterest.deployservice.knox.Knox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class RodimusManagerImpl implements RodimusManager {
    private static final Logger LOG = LoggerFactory.getLogger(RodimusManagerImpl.class);
    private static  final int RETRIES = 3;
    private String rodimusUrl;
    private HTTPClient httpClient;
    private Map<String, String> headers;
    private Gson gson;

    public RodimusManagerImpl(String rodimusUrl, String role) {
        this.rodimusUrl = rodimusUrl;
        this.httpClient = new HTTPClient();
        this.headers = new HashMap<>();
        this.headers.put("Content-Type", "application/json");
        String token = this.getRodimusToken(role);
        if (token != null) {
            this.headers.put("Accept", "*/*");
            this.headers.put("Authorization", String.format("token %s", token));
        }
        this.gson = new GsonBuilder().addSerializationExclusionStrategy(new CustomExclusionStrategy()).create();
    }

    private static String getRodimusToken(String role) {
        try {
           LOG.info("Get rodimus token with role {}", role);
            String fileName = String.format("rodimus:%s:token", role);
            // Register key
            File file = new File("/var/lib/knox/v0/keys/" + fileName);
            if (!file.exists()) {
                CommandLineKnox cmdKnox = new CommandLineKnox(fileName, Runtime.getRuntime());

                    if (cmdKnox.register() != 0) {
                        throw new RuntimeException("Error registering keys: " + fileName);
                    }

                long startTime = System.currentTimeMillis();
                while (!file.exists() && System.currentTimeMillis() - startTime < 5000) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignore) {
                    }
                }
            }
            Knox mKnox = new FileSystemKnox(fileName);
            return new String(mKnox.getPrimaryKey(), "UTF-8");
        } catch (Exception e) {
            LOG.error("Exception while getting rodimus token from knox :" + e.getMessage());
        }
        return null;
    }

    private class CustomExclusionStrategy implements ExclusionStrategy {
        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            return f.getName().equals("__isset_bit_vector");
        }

        @Override
        public boolean shouldSkipClass(Class<?> c) {
            return false;
        }
    }

    @Override
    public void terminateHostsByClusterName(String clusterName, Collection<String> hostIds) throws Exception {
        if (hostIds.isEmpty()) {
            return;
        }
        
        String url = String.format("%s/v1/clusters/%s/hosts", rodimusUrl, clusterName);
        httpClient.delete(url, gson.toJson(hostIds), headers, RETRIES);
    }

    @Override
    public Collection<String> getTerminatedHosts(Collection<String> hostIds) throws Exception {
        if (hostIds.isEmpty()) {
            return Collections.emptyList();
        }
        // NOTE: it's better to call this function with single host id
        String url = String.format("%s/v1/hosts/state?actionType=%s", rodimusUrl, "TERMINATED");
        String res = httpClient.post(url, gson.toJson(hostIds), headers, RETRIES);
        return gson.fromJson(res, new TypeToken<ArrayList<String>>() {}.getType());
    }

    @Override
    public Long getClusterInstanceLaunchGracePeriod(String clusterName) throws Exception {
        String url = String.format("%s/v1/groups/%s/config", rodimusUrl, clusterName);
        String res = httpClient.get(url, null, null, headers, RETRIES);
        JsonObject jsonObject = gson.fromJson(res, JsonObject.class);
        if (jsonObject == null || jsonObject.isJsonNull()) {
            return null;
        }

        JsonPrimitive launchGracePeriod = jsonObject.getAsJsonPrimitive("launchLatencyTh");
        if (launchGracePeriod == null || launchGracePeriod.isJsonNull()) {
            return null;
        }

        return launchGracePeriod.getAsLong();
    }

    @Override
    public Map<String, Map<String, String>> getEc2Tags(Collection<String> hostIds) throws Exception {
        String url = String.format("%s/v1/host_ec2tags", rodimusUrl);
        String res = httpClient.post(url, gson.toJson(hostIds), headers, RETRIES);
        return gson.fromJson(res, new TypeToken<Map<String, Map<String, String>>>(){}.getType());
    }
}
