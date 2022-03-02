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

// TO REMOVE ???
    public void setKnoxKey(String knoxKey) throws Exception {
        this.fsKnox = new FileSystemKnox(knoxKey);
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

    @Override
    public void terminateHostsByClusterName(String clusterName, Collection<String> hostIds) throws Exception {
        if (hostIds.isEmpty()) {
            return;
        }
        
        int rtry = this.RETRIES;
        do{
//            LOG.info("terminateHostsByClusterName");
            String url = String.format("%s/v1/clusters/%s/hosts", this.rodimusUrl, clusterName);
            setAuthorization();
            try {
                httpClient.delete(url, gson.toJson(hostIds), this.headers, RETRIES);
                return;
            } catch (DeployInternalException e) {
                if ( ! this.refreshCachedKey() ) throw e;
                rtry--;
            }
        }while( rtry>0 );
        throw new DeployInternalException(
            "HTTP request to Rodimus API failed on terminateHostsByClusterName, too many retries.");
    } // terminateHostsByClusterName

    @Override
    public Collection<String> getTerminatedHosts(Collection<String> hostIds) throws Exception {
        if (hostIds.isEmpty()) {
            return Collections.emptyList();
        }

        int rtry = this.RETRIES;
        do{
//            LOG.info("getTerminatedHosts");
            // NOTE: it's better to call this function with single host id
            String url = String.format("%s/v1/hosts/state?actionType=%s", rodimusUrl, "TERMINATED");
            setAuthorization();
            try {
                String res = httpClient.post(url, gson.toJson(hostIds), this.headers, this.RETRIES);
                return gson.fromJson(res, new TypeToken<ArrayList<String>>() {}.getType());
            } catch (DeployInternalException e) {
                if ( ! this.refreshCachedKey() ) throw e;
                rtry--;
            }
        }while( rtry>0 );
        throw new DeployInternalException(
            "HTTP request to Rodimus API failed on getTerminatedHosts, too many retries.");
    } // getTerminatedHosts

    @Override
    public Long getClusterInstanceLaunchGracePeriod(String clusterName) throws Exception {
        String res = null;

        int rtry = this.RETRIES;
        do{
//            LOG.info("getClusterInstanceLaunchGracePeriod");
            String url = String.format("%s/v1/groups/%s/config", rodimusUrl, clusterName);
            setAuthorization();
            try {
                res = httpClient.get(url, null, null, this.headers, this.RETRIES);
                rtry = this.RETRIES + 1;
            } catch (DeployInternalException e) {
                if ( ! this.refreshCachedKey() ) throw e;
                rtry--;
            }
        }while( ( rtry<=this.RETRIES )&&( rtry>0 ) );
        if( rtry<=this.RETRIES ) { 
            throw new DeployInternalException(
                "HTTP request to Rodimus API failed on getClusterInstanceLaunchGracePeriod, too many retries.");
        }

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

        int rtry = this.RETRIES;
        do{
//            LOG.info("getEc2Tags");
            String url = String.format("%s/v1/host_ec2tags", this.rodimusUrl);
            setAuthorization();
            try {
                String res = httpClient.post(url, gson.toJson(hostIds), this.headers, this.RETRIES);
                return gson.fromJson(res, new TypeToken<Map<String, Map<String, String>>>(){}.getType());
            } catch (DeployInternalException e) {
                if ( ! this.refreshCachedKey() ) throw e;
                rtry--;
            }
        }while( rtry>0 );
        throw new DeployInternalException( // "HTTP request failed, too many retries.");
            "HTTP request to Rodimus API failed on getEc2Tags, too many retries.");        
    } // getEc2Tags

} // class RodimusManagerImpl
