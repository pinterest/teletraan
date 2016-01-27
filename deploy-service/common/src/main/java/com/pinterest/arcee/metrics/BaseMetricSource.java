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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.Map;


public abstract class BaseMetricSource implements MetricSource {
    private static final Logger LOG = LoggerFactory.getLogger(BaseMetricSource.class);
    private String writePath;
    private Integer port;

    public BaseMetricSource(String urlPath) throws Exception {
        URL url = new URL(urlPath);
        writePath = url.getHost();
        port = url.getPort();
        LOG.info(String.format("Write path: %s: %d", writePath, port));
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
