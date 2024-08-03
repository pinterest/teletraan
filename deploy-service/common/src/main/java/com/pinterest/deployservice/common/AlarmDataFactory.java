/**
 * Copyright (c) 2016-2024 Pinterest, Inc.
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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pinterest.deployservice.bean.AlarmBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/** Serialize and deserialize List<AlarmBean> to and from a JSON string */
public class AlarmDataFactory implements PersistableJSONFactory<List<AlarmBean>> {
    private static final String ALARM_ARRAY = "alarmArray";
    private static final String NAME = "name";
    private static final String ALARM_URL = "alarmUrl";
    private static final String METRICS_URL = "metricsUrl";

    /** Convert list of alarm configs into a JSON string. */
    public String toJson(List<AlarmBean> configs) {
        JsonObject json = new JsonObject();
        JsonArray arrayOfUrlJson = new JsonArray();

        for (AlarmBean config : configs) {
            JsonObject urlJson = new JsonObject();
            urlJson.addProperty(NAME, config.getName());
            urlJson.addProperty(ALARM_URL, config.getAlarmUrl());
            urlJson.addProperty(METRICS_URL, config.getMetricsUrl());
            arrayOfUrlJson.add(urlJson);
        }
        json.add(ALARM_ARRAY, arrayOfUrlJson);

        return json.toString();
    }

    /** Take a JSON string and convert into a list of Alarm configs. */
    public List<AlarmBean> fromJson(String payload) {
        if (StringUtils.isEmpty(payload)) {
            return Collections.emptyList();
        }

        List<AlarmBean> configs = new ArrayList<>();
        JsonParser parser = new JsonParser();
        JsonObject jsonObj = (JsonObject) parser.parse(payload);
        JsonArray arrayOfUrls = jsonObj.getAsJsonArray(ALARM_ARRAY);
        for (int i = 0; i < arrayOfUrls.size(); i++) {
            JsonObject urlObj = arrayOfUrls.get(i).getAsJsonObject();
            AlarmBean config = new AlarmBean();
            config.setName(urlObj.get(NAME).getAsString());
            config.setAlarmUrl(urlObj.get(ALARM_URL).getAsString());
            JsonElement element = urlObj.get(METRICS_URL);
            if (!element.isJsonNull()) {
                config.setMetricsUrl(element.getAsString());
            }
            configs.add(config);
        }
        return configs;
    }
}
