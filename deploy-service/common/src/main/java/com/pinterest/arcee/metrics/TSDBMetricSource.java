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

import com.google.gson.GsonBuilder;


import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.Socket;
import java.io.OutputStreamWriter;
import java.util.*;
import com.google.gson.reflect.TypeToken;
import com.pinterest.arcee.bean.MetricDatumBean;
import com.pinterest.deployservice.common.HTTPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TSDBMetricSource implements MetricSource {
    private static final Logger LOG = LoggerFactory.getLogger(com.pinterest.arcee.metrics.TSDBMetricSource.class);
    private String writePath;
    private String readPath;
    private Integer port;

    public TSDBMetricSource(String urlpath, String readUrl) throws Exception {
        URL url = new URL(urlpath);
        writePath = url.getHost();
        port = url.getPort();
        readPath = readUrl;
        LOG.info(String.format("set up tsdb server as: %s : %d, read path: %s", writePath, port, readPath));
}

    @Override
    public void export(String metricName, Map<String, String> tags, Double value, Long timestamp) throws Exception {
        if (tags.isEmpty()) {
            tags.put("host", InetAddress.getLocalHost().getHostName());
        }
        StringBuilder sb = new StringBuilder();
        String prefix = "";
        for (Map.Entry<String, String> entry : tags.entrySet()) {
            sb.append(prefix);
            prefix = " ";
            sb.append(String.format("%s=%s", entry.getKey(), entry.getValue()));
        }

        String message = String.format("put %s %d %f %s\n", metricName, timestamp, value, sb.toString());
        LOG.info("exporting metric to tsdb:  " + message);
        sendMessage(message, 3);
    }

    @Override
    public Collection<MetricDatumBean> getMetrics(String metricName, String start) throws Exception {
        HTTPClient httpClient = new HTTPClient();
        Map<String, String> params = new HashMap<>();
        params.put("m", metricName);
        params.put("start", start);
        HashMap<String, String> headers = new HashMap<>();

        String url = String.format("%s/api/query", readPath);
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

    private boolean sendMessage(String message, int times) throws Exception {
        Exception lastException = null;
        for (int i = 0; i < times; ++i) {
            Socket socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(writePath, port));
                OutputStreamWriter writer = new OutputStreamWriter(socket.getOutputStream());
                writer.write(message);
                writer.flush();
                return true;
            } catch (Exception ex) {
                LOG.error("Failed to send message to tsd server", ex);
                lastException = ex;
            } finally {
                if (!socket.isClosed()) {
                    socket.close();
                }
            }
        }

        throw lastException;
    }
}
