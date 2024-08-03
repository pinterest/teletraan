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
package com.pinterest.deployservice.group;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.common.CommonUtils;
import com.pinterest.deployservice.common.HTTPClient;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CMDBHostGroupManager implements HostGroupManager {
    private static int TOTAL_RETRY = 10;
    private static final Logger LOG = LoggerFactory.getLogger(CMDBHostGroupManager.class);
    private String cmdbServer = null;
    private Gson gson = new Gson();

    public CMDBHostGroupManager(String cmdbServer) {
        this.cmdbServer = cmdbServer;
    }

    @Override
    public Map<String, HostBean> getHostIdsByGroup(String groupName) throws Exception {
        HashMap<String, HostBean> hosts = new HashMap<>();
        String url = String.format("%s/v2/query", cmdbServer);

        // construct data
        String query =
                String.format(
                        "state:running AND (facts.deploy_service:\"%s\" facts.puppet_groups:\"%s\")",
                        groupName, groupName);
        Map<String, String> data = new HashMap<>();
        data.put("query", query);
        data.put("fields", "id,state,config.name,config.internal_address,created_time");

        // construct head
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        HTTPClient client = new HTTPClient();
        String result = client.post(url, gson.toJson(data), headers, TOTAL_RETRY);
        JsonParser parser = new JsonParser();
        JsonElement elements = parser.parse(result);
        if (elements.isJsonNull()) {
            LOG.info("CMDB query: {} returns empty list", query);
            return hosts;
        }

        JsonArray jsonObjects = (JsonArray) elements;
        for (JsonElement element : jsonObjects) {
            JsonObject object = element.getAsJsonObject();
            if (object.get("state").getAsString().equals("running") && object.has("config.name")) {
                HostBean hostBean = new HostBean();
                String hostId = object.get("id").getAsString();
                hostBean.setHost_name(object.get("config.name").getAsString());
                hostBean.setHost_id(hostId);
                hostBean.setIp(object.get("config.internal_address").getAsString());
                hostBean.setCreate_date(getDateTime(object.get("created_time").getAsString()));
                hosts.put(hostId, hostBean);
            }
        }
        LOG.info("Fetched {} hosts for group {} in CMDB", hosts.size(), groupName);
        return hosts;
    }

    @Override
    public String getLastInstanceId(String groupName) throws Exception {
        String url = String.format("%s/v2/query", cmdbServer);

        // construct data
        String query =
                String.format(
                        "state:running AND (facts.deploy_service:\"%s\" facts.puppet_groups:\"%s\")",
                        groupName, groupName);
        Map<String, String> data = new HashMap<>();
        data.put("query", query);
        data.put("fields", "state,id,location,created_time");

        // construct head
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        HTTPClient client = new HTTPClient();
        String result = client.post(url, gson.toJson(data), headers, TOTAL_RETRY);
        JsonParser parser = new JsonParser();
        JsonElement elements = parser.parse(result);

        if (elements.isJsonNull()) {
            LOG.info("CMDB query: {} returns empty list", query);
            return null;
        }

        JsonArray jsonObjects = (JsonArray) elements;
        List<Pair<Long, String>> results = new LinkedList<>();
        for (JsonElement element : jsonObjects) {
            JsonObject object = element.getAsJsonObject();
            if (object.get("state").getAsString().equals("running")) {
                String id = object.get("id").getAsString();
                Long timestamp = getDateTime(object.get("created_time").getAsString());
                results.add(new Pair<>(timestamp, id));
            }
        }

        Collections.sort(
                results,
                new Comparator<Pair<Long, String>>() {
                    @Override
                    public int compare(Pair<Long, String> o1, Pair<Long, String> o2) {
                        return o2.getKey().compareTo(o1.getKey());
                    }
                });

        return results.get(0).getValue();
    }

    private long getDateTime(String dateString) {
        try {
            return CommonUtils.convertDateStringToMilliseconds(dateString);
        } catch (Exception ex) {
            LOG.warn(String.format("Failed to parse date string: %s", dateString));
            return 0l;
        }
    }

    private String constructQuery(Map<String, String> params) throws Exception {
        if (params == null || params.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean firstTime = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (firstTime) {
                firstTime = false;
            } else {
                sb.append("&");
            }

            sb.append(
                    String.format(
                            "%s=%s", entry.getKey(), URLEncoder.encode(entry.getValue(), "UTF-8")));
        }
        return sb.toString();
    }

    private static class Pair<K, V> {
        private final K key;
        private final V val;

        private Pair(K key, V val) {
            this.key = key;
            this.val = val;
        }

        private K getKey() {
            return key;
        }

        private V getValue() {
            return val;
        }
    }
}
