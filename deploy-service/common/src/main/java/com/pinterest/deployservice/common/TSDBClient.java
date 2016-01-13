/**
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
package com.pinterest.deployservice.common;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.util.*;

import com.google.gson.reflect.TypeToken;
import com.pinterest.arcee.bean.MetricDatumBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

public class TSDBClient {
    private static final Logger LOG = LoggerFactory.getLogger(TSDBClient.class);
    private static final String METRIC = "metric";
    private static final String TIMESTAMP = "timestamp";
    private static final String VALUE = "value";
    // TODO make this configurable in the yaml file
    private static final String tsdbServer = "https://tsd.pinadmin.com";

    public TSDBClient() {
    }

    public void export(String metricName, Map<String, String> tags, Double value, Long timestamp) throws Exception {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(METRIC, metricName);
        jsonObject.addProperty(TIMESTAMP, timestamp);
        jsonObject.addProperty(VALUE, value);

        if (tags.isEmpty()) {
            tags.put("host", InetAddress.getLocalHost().getHostName());
        }

        JsonObject tagsJson = new JsonObject();
        for (Map.Entry<String, String> entry : tags.entrySet()) {
            tagsJson.addProperty(entry.getKey(), entry.getValue());
        }
        jsonObject.add("tags", tagsJson);

        LOG.info("exporting metric to tsdb " + jsonObject);
        String url = String.format("%s/api/put?details", tsdbServer);
        HTTPClient client  = new HTTPClient();
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accepts", "application/json");
        headers.put("Accept", "*/*");
        String result = client.post(url, jsonObject.toString(), headers, 3);
    }

    public List<MetricDatumBean> getMetrics(String metricName, String start) throws Exception {
        HTTPClient httpClient = new HTTPClient();
        Map<String, String> params = new HashMap<String, String>();
        params.put("m", metricName);
        params.put("start", start);
        HashMap<String, String> headers = new HashMap<>();

        String url = String.format("%s/api/query", tsdbServer);
        String jsonPayload = httpClient.get(url, params, headers, 1);
        LOG.debug(String.format("Get metric data from source: %s", jsonPayload));

        GsonBuilder builder = new GsonBuilder();
        Map<String, Object>[] jsonMaps = builder.create().fromJson(jsonPayload, new TypeToken<HashMap<String, Object>[]>() {
        }.getType());

        List<MetricDatumBean> dataPoints = new ArrayList<>();
        for (Map<String, Object> jsonMap : jsonMaps) {
            Map<String, Double> metrics = (Map<String, Double>) jsonMap.get("dps");
            if (metrics != null) {
                for (Map.Entry<String, Double> metric : metrics.entrySet()) {
                    MetricDatumBean dataPoint = new MetricDatumBean();
                    dataPoint.setTimestamp(1000 * Long.parseLong(metric.getKey()));
                    double value = metric.getValue() == Double.MIN_VALUE ? 0 : metric.getValue();
                    dataPoint.setValue(value);
                    dataPoints.add(dataPoint);
                }
            }
        }
        return dataPoints;
    }
}
