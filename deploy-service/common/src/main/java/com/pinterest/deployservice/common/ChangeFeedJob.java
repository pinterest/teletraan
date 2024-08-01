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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;


public final class ChangeFeedJob implements Callable<Void> {
    private static final Logger LOG = LoggerFactory.getLogger(ChangeFeedJob.class);
    private final int RETRIES = 3;
    private String payload;
    private String changeFeedUrl;
    private Object oriObj;
    private Object curObj;
    private HTTPClient httpClient;

    public ChangeFeedJob(String payload, String changeFeedUrl, Object oriObj, Object curObj) {
        this.payload = payload;
        this.changeFeedUrl = changeFeedUrl;
        this.oriObj = oriObj;
        this.curObj = curObj;
        this.httpClient = new HTTPClient();
    }

    private static String toStringRepresentation(Object object) throws IllegalAccessException {
        if (object == null) {
            return "None";
        }

        if (object instanceof List) {
            List<?> list = (List<?>) object;
            if (list.isEmpty()) {
                return "[]";
            }

            StringBuilder sb = new StringBuilder("[");
            for (Object element : list) {
                sb.append(toStringRepresentation(element));
                sb.append(", ");
            }

            sb.delete(sb.length() - 2, sb.length());
            sb.append(']');
            return sb.toString();
        }

        Field[] fields = object.getClass().getDeclaredFields();
        if (fields.length == 0) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder("{");
        for (Field field : fields) {
            field.setAccessible(true);
            Object fieldItem = field.get(object);
            sb.append(field.getName());
            sb.append(":");
            sb.append(fieldItem);
            sb.append(", ");
        }

        sb.delete(sb.length() - 2, sb.length());
        sb.append('}');
        return sb.toString();
    }

    private static String getConfigChangeMessage(Object ori, Object cur) {
        if (ori.getClass() != cur.getClass())
            return null;

        List<String> results = new ArrayList<>();
        try {
            if (ori instanceof List) {
                // Process list of custom object and others (e.g. List<String>)
                List<?> originalList = (List<?>) ori;
                List<?> currentList = (List<?>) cur;
                for (int i = 0; i < Math.max(originalList.size(), currentList.size()); i++) {
                    Object originalItem = i < originalList.size() ? originalList.get(i) : null;
                    Object currentItem = i < currentList.size() ? currentList.get(i) : null;
                    if (!Objects.equals(originalItem, currentItem)) {
                        Object temp = originalItem != null ? originalItem : currentItem;
                        if (temp.getClass().getName().startsWith("com.pinterest")) {
                            results.add(String.format("%-40s  %-40s  %-40s%n", i, toStringRepresentation(originalItem),
                                toStringRepresentation(currentItem)));
                        } else {
                            results.add(String.format("%-40s  %-40s  %-40s%n", i, originalItem, currentItem));
                        }
                    }
                }
            } else if (ori instanceof Map) {
                // Process Map (e.g. Map<String, String>)
                Map<?, ?> originalMap = (Map<?, ?>) ori;
                Map<?, ?> currentMap = (Map<?, ?>) cur;
                Set<String> keys = new HashSet<>();
                originalMap.keySet().stream().forEach(key -> keys.add((String) key));
                currentMap.keySet().stream().forEach(key -> keys.add((String) key));
                for (String key : keys) {
                    Object originalItem = originalMap.get(key);
                    Object currentItem = currentMap.get(key);
                    if (!Objects.equals(originalItem, currentItem)) {
                        results.add(String.format("%-40s  %-40s  %-40s%n", key, originalItem, currentItem));
                    }
                }
            } else {
                // Process other objects (e.g. custom object bean)
                Field[] fields = ori.getClass().getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                    Object oriObj = field.get(ori);
                    Object curObj = field.get(cur);
                    if (!Objects.equals(oriObj, curObj)) {
                        if (oriObj instanceof List) {
                            results.add(String.format("%-40s  %-40s  %-40s%n", field.getName(), toStringRepresentation(oriObj),
                                toStringRepresentation(curObj)));
                        } else {
                            results.add(String.format("%-40s  %-40s  %-40s%n", field.getName(), oriObj, curObj));
                        }

                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to get config message.", e);
        }

        if (results.isEmpty()) {
            return null;
        }

        StringBuilder resultBuilder = new StringBuilder();
        resultBuilder.append(String.format("%n%-40s  %-40s  %-40s%n", "Name", "Original Value", "Current Value"));
        results.stream().forEach(resultBuilder::append);
        return resultBuilder.toString();
    }

    public Void call() {
        try {
            if (oriObj == null || curObj == null) {
                return null;
            }

            String configChange = getConfigChangeMessage(oriObj, curObj);
            if ((configChange == null) || StringUtils.isEmpty(configChange)) {
                return null;
            }

            if (!StringUtils.isEmpty(changeFeedUrl)) {
                LOG.info(String.format("Send change feed %s to %s", payload, changeFeedUrl));
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                httpClient.post(changeFeedUrl, payload, headers, RETRIES);
            }
        } catch (Throwable t) {
            LOG.error(String.format("Failed to send change feed: %s", payload), t);
        }
        return null;
    }

    public String getPayload() {
        return payload;
    }
}
