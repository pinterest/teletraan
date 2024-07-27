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
import com.pinterest.deployservice.common.KeyReader;
import com.pinterest.deployservice.common.KnoxKeyReader;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RodimusManagerImpl implements RodimusManager {
    private static final Logger LOG = LoggerFactory.getLogger(RodimusManagerImpl.class);
    private static final int RETRIES = 3;

    protected static enum Verb {
        GET, POST, DELETE
    };

    private String rodimusUrl;
    private HTTPClient httpClient;
    private Map<String, String> headers;
    private Gson gson;
    private KeyReader knoxKeyReader = new KnoxKeyReader();

    public RodimusManagerImpl(String rodimusUrl, String knoxKey, boolean useProxy, String httpProxyAddr,
            String httpProxyPort) throws Exception {
        this.rodimusUrl = rodimusUrl;
        this.httpClient = new HTTPClient();
        this.headers = new HashMap<>();
        this.headers.put("Content-Type", "application/json");
        this.headers.put("Accept", "*/*");
        int httpProxyPortInt;

        if (Boolean.TRUE.equals(useProxy)) {
            try {
                httpProxyPortInt = Integer.parseInt(httpProxyPort);
            } catch (NumberFormatException exception) {
                LOG.error(httpProxyPort, exception);
                throw exception;
            }
            this.httpClient = new HTTPClient(useProxy, httpProxyAddr, httpProxyPortInt);
        } else {
            this.httpClient = new HTTPClient();
        }

        if (StringUtils.isNotBlank(knoxKey)) {
            knoxKeyReader.init(knoxKey);
        } else {
            LOG.warn("Rodimus Knox key is blank, this is only acceptable in test environment.");
        }
        this.gson = new GsonBuilder().addSerializationExclusionStrategy(new CustomExclusionStrategy()).create();
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

    private void setAuthorization() throws Exception {
        String knoxKey = knoxKeyReader.getKey();
        if (knoxKey == null) {
            throw new IllegalStateException("Rodimus knoxKey is null");
        }
        this.headers.put("Authorization", String.format("token %s", knoxKey));
    }

    private String switchHttpClient(Verb verb, String url, String payload) throws Exception {
        String res = null;

        switch (verb) {
            case GET:
                res = httpClient.get(url, payload, null, this.headers, RETRIES);
                break;
            case POST:
                res = httpClient.post(url, payload, this.headers, RETRIES);
                break;
            case DELETE:
                res = httpClient.delete(url, payload, this.headers, RETRIES);
                break;
        }

        return res;
    }

    protected String callHttpClient(Verb verb, String url, String payload) throws Exception {
        setAuthorization();
        return switchHttpClient(verb, url, payload);
    }

    @Override
    public void terminateHostsByClusterName(String clusterName, Collection<String> hostIds) throws Exception {
        terminateHostsByClusterName(clusterName, hostIds, true);
    }

    @Override
    public void terminateHostsByClusterName(String clusterName, Collection<String> hostIds, Boolean replaceHost)
            throws Exception {
        if (hostIds.isEmpty()) {
            return;
        }

        String url = String.format("%s/v1/clusters/%s/hosts?replaceHost=%s", this.rodimusUrl, clusterName, replaceHost);
        callHttpClient(Verb.DELETE, url, gson.toJson(hostIds));
    } // terminateHostsByClusterName

    @Override
    public Collection<String> getTerminatedHosts(Collection<String> hostIds) throws Exception {
        if (hostIds.isEmpty()) {
            return Collections.emptyList();
        }

        // NOTE: it's better to call this function with single host id
        String url = String.format("%s/v1/hosts/state?actionType=%s", rodimusUrl, "TERMINATED");
        String res = callHttpClient(Verb.POST, url, gson.toJson(hostIds));
        return gson.fromJson(res, new TypeToken<ArrayList<String>>() {
        }.getType());
    } // getTerminatedHosts

    @Override
    public Long getClusterInstanceLaunchGracePeriod(String clusterName) throws Exception {
        String url = String.format("%s/v1/groups/%s/config", rodimusUrl, clusterName);
        String res = callHttpClient(Verb.GET, url, null);

        JsonObject jsonObject = gson.fromJson(res, JsonObject.class);
        if (jsonObject == null || jsonObject.isJsonNull()) {
            return null;
        }

        JsonPrimitive launchGracePeriod = jsonObject.getAsJsonPrimitive("launchLatencyTh");
        if (launchGracePeriod == null || launchGracePeriod.isJsonNull()) {
            return null;
        }

        return launchGracePeriod.getAsLong();
    } // getClusterInstanceLaunchGracePeriod

    @Override
    public Map<String, Map<String, String>> getEc2Tags(Collection<String> hostIds) throws Exception {
        String url = String.format("%s/v1/host_ec2tags", rodimusUrl);
        String res = callHttpClient(Verb.POST, url, gson.toJson(hostIds));

        return gson.fromJson(res, new TypeToken<Map<String, Map<String, String>>>() {
        }.getType());
    } // getEc2Tags

} // class RodimusManagerImpl
