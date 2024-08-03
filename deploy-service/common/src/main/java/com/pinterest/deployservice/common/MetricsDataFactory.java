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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pinterest.deployservice.bean.MetricsConfigBean;
import com.pinterest.deployservice.bean.MetricsSpecBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for converting deploy metrics data into JsonObjects and Strings to be used with the Data
 * table.
 */
public class MetricsDataFactory implements PersistableJSONFactory<List<MetricsConfigBean>> {
    private static final Logger LOG = LoggerFactory.getLogger(MetricsDataFactory.class);

    /** Convert this.URLs to a JSON string. */
    public String toJson(List<MetricsConfigBean> configs) {
        JsonObject json = new JsonObject();
        JsonArray arrayOfUrlJson = new JsonArray();

        for (MetricsConfigBean met : configs) {
            JsonObject urlJson = new JsonObject();
            // Add the url to JSON
            urlJson.addProperty("url", met.getUrl());
            urlJson.addProperty("title", met.getTitle());
            // Add the segment data to JSON
            JsonArray arrayOfSpecJson = new JsonArray();

            for (MetricsSpecBean spec : met.getSpecs()) {
                JsonObject specJson = new JsonObject();
                specJson.addProperty("color", spec.getColor());
                specJson.addProperty("min", spec.getMin());
                specJson.addProperty("max", spec.getMax());
                arrayOfSpecJson.add(specJson);
            }
            // Add array of URL's metrics json data under specs key
            urlJson.add("specs", arrayOfSpecJson);
            arrayOfUrlJson.add(urlJson);
        }
        // Add json array of each url under urlMetricsData key
        json.add("urlMetricsData", arrayOfUrlJson);

        return json.toString();
    }

    /** Takes a JSON string and returns a corresponding JsonObject. */
    public List<MetricsConfigBean> fromJson(String payload) {
        if (StringUtils.isEmpty(payload)) {
            return Collections.emptyList();
        }

        List<MetricsConfigBean> configs = new ArrayList<>();
        JsonParser parser = new JsonParser();
        JsonObject jsonObj = (JsonObject) parser.parse(payload);
        JsonArray arrayOfUrls = jsonObj.getAsJsonArray("urlMetricsData");
        for (int i = 0; i < arrayOfUrls.size(); i++) {
            JsonObject urlObj = arrayOfUrls.get(i).getAsJsonObject();
            MetricsConfigBean urlConfig = new MetricsConfigBean();
            urlConfig.setSpecs(new ArrayList<>());
            urlConfig.setTitle(urlObj.get("title").getAsString());
            urlConfig.setUrl(urlObj.get("url").getAsString());
            JsonArray arrayOfSpecs = urlObj.get("specs").getAsJsonArray();
            for (int j = 0; j < arrayOfSpecs.size(); j++) {
                JsonObject specObj = arrayOfSpecs.get(j).getAsJsonObject();
                MetricsSpecBean spec = new MetricsSpecBean();
                spec.setColor(specObj.get("color").getAsString());
                spec.setMax(Double.parseDouble(specObj.get("max").getAsString()));
                spec.setMin(Double.parseDouble(specObj.get("min").getAsString()));
                urlConfig.getSpecs().add(spec);
            }
            configs.add(urlConfig);
        }
        return configs;
    }
}
