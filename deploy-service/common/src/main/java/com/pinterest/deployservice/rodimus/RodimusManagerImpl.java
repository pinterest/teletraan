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
import com.pinterest.deployservice.common.DeployInternalException;
import com.pinterest.deployservice.knox.FileSystemKnox;
import com.pinterest.deployservice.knox.Knox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class RodimusManagerImpl implements RodimusManager {
    private static final Logger LOG = LoggerFactory.getLogger(RodimusManagerImpl.class);
    private static final int RETRIES = 3;
    private String rodimusUrl;
    private HTTPClient httpClient;
    private Map<String, String> headers;
    private Gson gson;
    private Knox fsKnox = null;
    private String cachedKey = null;

    public RodimusManagerImpl(String rodimusUrl, String knoxKey) throws Exception {
        this.rodimusUrl = rodimusUrl;
        this.httpClient = new HTTPClient();
        this.headers = new HashMap<>();
        this.headers.put("Content-Type", "application/json");
        this.headers.put("Accept", "*/*");
        if (knoxKey != null) {
            this.fsKnox = new FileSystemKnox(knoxKey);
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

    private boolean refreshCachedKey() throws Exception {
        String prevKnoxKey = this.cachedKey;

        if (this.fsKnox != null) {
            this.cachedKey = new String(this.fsKnox.getPrimaryKey());
        }

        if ( prevKnoxKey == null )
        {
            return this.cachedKey != null;
        }else{
            return ! prevKnoxKey.equals( this.cachedKey );
        }
        
    }

    private void setAuthorization() throws Exception {
        if (this.cachedKey == null) {
            this.refreshCachedKey();
        }
        this.headers.put("Authorization", String.format("token %s", this.cachedKey));
    }

    private String switchHttpClient( String verb, String url, String payload ) throws Exception {
        String res = null;

        switch(verb) {
            case "get":
                res = httpClient.get(url, payload, null, this.headers, this.RETRIES);
                break;
            case "post":
                res = httpClient.post(url, payload, this.headers, this.RETRIES);
                break;
            case "delete":
                res = httpClient.delete(url, payload, this.headers, this.RETRIES);
                break;
        }

        return res;
    }

    private String callHttpClient( String verb, String url, String payload ) throws Exception {
        String res;

        setAuthorization();
        try{
            res = switchHttpClient( verb, url, payload );
        }catch (DeployInternalException e) {
            if ( ! this.refreshCachedKey() ) throw e; // no new token? do not try again
            setAuthorization();
            res = switchHttpClient( verb, url, payload );
        }

        return res;
    }

    @Override
    public void terminateHostsByClusterName(String clusterName, Collection<String> hostIds) throws Exception {
        if (hostIds.isEmpty()) {
            return;
        }
        
        String url = String.format("%s/v1/clusters/%s/hosts", this.rodimusUrl, clusterName);
        callHttpClient( "delete", url, gson.toJson(hostIds) );
    } // terminateHostsByClusterName

    @Override
    public Collection<String> getTerminatedHosts(Collection<String> hostIds) throws Exception {
        if (hostIds.isEmpty()) {
            return Collections.emptyList();
        }

        // NOTE: it's better to call this function with single host id
        String url = String.format("%s/v1/hosts/state?actionType=%s", rodimusUrl, "TERMINATED");
        String res = callHttpClient( "post", url, gson.toJson(hostIds) );
        return gson.fromJson(res, new TypeToken<ArrayList<String>>() {}.getType());
    } // getTerminatedHosts

    @Override
    public Long getClusterInstanceLaunchGracePeriod(String clusterName) throws Exception {
        String url = String.format("%s/v1/groups/%s/config", rodimusUrl, clusterName);
        String res = callHttpClient( "get", url, null );

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
        String res = callHttpClient( "post", url, gson.toJson(hostIds) );

        return gson.fromJson(res, new TypeToken<Map<String, Map<String, String>>>(){}.getType());
    } // getEc2Tags

} // class RodimusManagerImpl
