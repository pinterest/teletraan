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
package com.pinterest.arcee.metrics;

import com.google.gson.*;
import com.pinterest.arcee.bean.MetricDatumBean;
import com.pinterest.deployservice.common.HTTPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class StatsboardMetricSource extends BaseMetricSource {
    private static final Logger LOG = LoggerFactory.getLogger(com.pinterest.arcee.metrics.StatsboardMetricSource.class);
    private String readPath;

    public StatsboardMetricSource(String urlpath, String readUrl) throws Exception {
        super(urlpath);
        readPath = readUrl;
        LOG.info(String.format("set up statsboard server read path as: %s", readPath));
    }

    @Override
    public Collection<MetricDatumBean> getMetrics(String metricName, String start) throws Exception {
        HTTPClient httpClient = new HTTPClient();
        Map<String, String> params = new HashMap<>();
        params.put("target", metricName);
        params.put("begin_time", start);
        HashMap<String, String> headers = new HashMap<>();

        String url = String.format("%s/api/v1/query", readPath);
        String jsonPayload = httpClient.get(url, params, headers, 1);
        LOG.debug(String.format("Get metric data from source: %s", jsonPayload));
        List<MetricDatumBean> dataPoints = new ArrayList<>();
        JsonParser parser = new JsonParser();
        JsonArray array = (JsonArray)parser.parse(jsonPayload);
        if (array.isJsonNull() || array.size() == 0) {
            LOG.info(String.format("Cannot obtain metrics %s from %s", metricName, url));
            return dataPoints;
        }

        JsonObject object = (JsonObject)array.get(0);
        JsonArray data = object.getAsJsonArray("datapoints");
        for (Iterator<JsonElement> elementIterator = data.iterator(); elementIterator.hasNext();) {
            JsonArray metric = (JsonArray)elementIterator.next();
            MetricDatumBean dataPoint = new MetricDatumBean();
            dataPoint.setTimestamp(metric.get(0).getAsLong() * 1000);
            double metricValue = metric.get(1).getAsDouble();
            metricValue = metricValue == Double.MIN_VALUE ? 0 : metricValue;
            dataPoint.setValue(metricValue);
            dataPoints.add(dataPoint);
        }
        return dataPoints;
    }

}
