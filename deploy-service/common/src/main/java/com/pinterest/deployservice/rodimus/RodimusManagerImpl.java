/**
 * Copyright (c) 2016-2023 Pinterest, Inc.
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
import com.pinterest.deployservice.bean.ClusterInfoPublicIdsBean;
import com.pinterest.deployservice.common.KeyReader;
import com.pinterest.deployservice.common.KnoxKeyReader;
import com.pinterest.teletraan.universal.http.HttpClient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RodimusManagerImpl implements RodimusManager {
    private static final Logger LOG = LoggerFactory.getLogger(RodimusManagerImpl.class);
    private final HttpClient httpClient;

    private final String rodimusUrl;
    private final Gson gson;
    private final KeyReader knoxKeyReader = new KnoxKeyReader();

    public RodimusManagerImpl(
            String rodimusUrl,
            String knoxKey,
            boolean useProxy,
            String httpProxyAddr,
            String httpProxyPort)
            throws Exception {
        this.rodimusUrl = rodimusUrl;
        int httpProxyPortInt;

        HttpClient.HttpClientBuilder clientBuilder = HttpClient.builder();
        if (useProxy) {
            try {
                httpProxyPortInt = Integer.parseInt(httpProxyPort);
            } catch (NumberFormatException exception) {
                LOG.error(httpProxyPort, exception);
                throw exception;
            }
            clientBuilder
                    .useProxy(true)
                    .httpProxyAddr(httpProxyAddr)
                    .httpProxyPort(httpProxyPortInt);
        }
        this.httpClient =
                clientBuilder.authorizationSupplier(this::fetchAuthorizationHeader).build();

        if (StringUtils.isNotBlank(knoxKey)) {
            knoxKeyReader.init(knoxKey);
        } else {
            LOG.warn("Rodimus Knox key is blank, this is only acceptable in test environment.");
        }
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

    private String fetchAuthorizationHeader() throws IllegalStateException {
        String knoxKey = knoxKeyReader.getKey();
        if (StringUtils.isBlank(knoxKey)) {
            throw new IllegalStateException("Rodimus knoxKey is blank");
        }
        return String.format("token %s", knoxKey);
    }

    @Override
    public void terminateHostsByClusterName(String clusterName, Collection<String> hostIds)
            throws Exception {
        terminateHostsByClusterName(clusterName, hostIds, true);
    }

    @Override
    public void terminateHostsByClusterName(
            String clusterName, Collection<String> hostIds, Boolean replaceHost) throws Exception {
        if (hostIds.isEmpty()) {
            return;
        }

        String url =
                String.format(
                        "%s/v1/clusters/%s/hosts?replaceHost=%s",
                        this.rodimusUrl, clusterName, replaceHost);
        httpClient.delete(url, gson.toJson(hostIds), null);
    }

    @Override
    public Collection<String> getTerminatedHosts(Collection<String> hostIds) throws Exception {
        if (hostIds.isEmpty()) {
            return Collections.emptyList();
        }

        // NOTE: it's better to call this function with single host id
        String url = String.format("%s/v1/hosts/state?actionType=%s", rodimusUrl, "TERMINATED");
        String res = httpClient.post(url, gson.toJson(hostIds), null);
        return gson.fromJson(res, new TypeToken<ArrayList<String>>() {}.getType());
    }

    @Override
    public Long getClusterInstanceLaunchGracePeriod(String clusterName) throws Exception {
        String url = String.format("%s/v1/groups/%s/config", rodimusUrl, clusterName);
        String res = httpClient.get(url, null, null);

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
    public Map<String, Map<String, String>> getEc2Tags(Collection<String> hostIds)
            throws Exception {
        String url = String.format("%s/v1/host_ec2tags", rodimusUrl);
        String res = httpClient.post(url, gson.toJson(hostIds), null);

        return gson.fromJson(res, new TypeToken<Map<String, Map<String, String>>>() {}.getType());
    }

    @Override
    public ClusterInfoPublicIdsBean getCluster(String clusterName) throws Exception {
        String url = String.format("%s/v1/clusters/%s", rodimusUrl, clusterName);
        String res = httpClient.get(url, null, null);

        return gson.fromJson(res, ClusterInfoPublicIdsBean.class);
    }

    @Override
    public void createClusterWithEnvPublicIds(
            String clusterName,
            String envName,
            String stageName,
            ClusterInfoPublicIdsBean clusterInfoPublicIdsBean)
            throws Exception {
        String url =
                String.format(
                        "%s/v1/clusters/%s/%s/%s/publicids",
                        rodimusUrl, clusterName, envName, stageName);
        httpClient.post(url, gson.toJson(clusterInfoPublicIdsBean), null);
    }

    @Override
    public void updateClusterWithPublicIds(
            String clusterName, ClusterInfoPublicIdsBean clusterInfoPublicIdsBean)
            throws Exception {
        String url = String.format("%s/v1/clusters/%s/publicids", rodimusUrl, clusterName);
        httpClient.put(url, gson.toJson(clusterInfoPublicIdsBean), null);
    }
}
