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
import com.pinterest.deployservice.bean.EnvWebHookBean;
import com.pinterest.deployservice.bean.WebHookBean;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Serialize and deserialize EnvWebHookBean to and from a JSON string */
public class WebhookDataFactory implements PersistableJSONFactory<EnvWebHookBean> {
    private static final Logger LOG = LoggerFactory.getLogger(WebhookDataFactory.class);
    private static final String METHOD = "method";
    private static final String URL = "url";
    private static final String VERSION = "version";
    private static final String HEADERS = "headers";
    private static final String BODY = "payload";
    private static final String PREHOOKS = "PreHooks";
    private static final String POSTHOOKS = "PostHooks";

    private void addProperties(JsonObject hookJson, WebHookBean hook) {
        hookJson.addProperty(METHOD, hook.getMethod());
        hookJson.addProperty(URL, hook.getUrl());
        hookJson.addProperty(VERSION, hook.getVersion());
        hookJson.addProperty(HEADERS, hook.getHeaders());
        hookJson.addProperty(BODY, hook.getBody());
    }

    private String getString(JsonObject hookObj, String name) {
        JsonElement element = hookObj.get(name);
        if (element.isJsonNull()) {
            return null;
        } else {
            return element.getAsString();
        }
    }

    private void setProperties(JsonObject hookObj, WebHookBean hook) {
        hook.setMethod(getString(hookObj, METHOD));
        hook.setUrl(getString(hookObj, URL));
        hook.setVersion(getString(hookObj, VERSION));
        hook.setHeaders(getString(hookObj, HEADERS));
        hook.setBody(getString(hookObj, BODY));
    }

    /** Convert an EnvWebHookBean object into a JSON string */
    public String toJson(EnvWebHookBean envWebhooks) {
        JsonObject json = new JsonObject();
        JsonArray arrayOfPreHookJson = new JsonArray();
        JsonArray arrayOfPostHookJson = new JsonArray();

        List<WebHookBean> preHooks = envWebhooks.getPreDeployHooks();
        List<WebHookBean> postHooks = envWebhooks.getPostDeployHooks();

        if (preHooks != null) {
            for (WebHookBean hook : preHooks) {
                JsonObject hookJson = new JsonObject();
                addProperties(hookJson, hook);
                arrayOfPreHookJson.add(hookJson);
            }
        }

        if (postHooks != null) {
            for (WebHookBean hook : postHooks) {
                JsonObject hookJson = new JsonObject();
                addProperties(hookJson, hook);
                arrayOfPostHookJson.add(hookJson);
            }
        }

        json.add(POSTHOOKS, arrayOfPostHookJson);
        json.add(PREHOOKS, arrayOfPreHookJson);
        return json.toString();
    }

    /** Take a JSON string and convert into an EnvWebHookBean object */
    public EnvWebHookBean fromJson(String json) {
        EnvWebHookBean EnvWebHookBean = new EnvWebHookBean();
        if (StringUtils.isEmpty(json)) {
            return EnvWebHookBean;
        }

        JsonParser parser = new JsonParser();
        JsonObject jsonObj = (JsonObject) parser.parse(json);
        JsonArray arrayOfPreHookJson = jsonObj.getAsJsonArray(PREHOOKS);
        JsonArray arrayOfPostHookJson = jsonObj.getAsJsonArray(POSTHOOKS);

        List<WebHookBean> listPre = new ArrayList<>();
        for (int i = 0; i < arrayOfPreHookJson.size(); i++) {
            JsonObject hookObj = arrayOfPreHookJson.get(i).getAsJsonObject();
            WebHookBean hook = new WebHookBean();
            setProperties(hookObj, hook);
            listPre.add(hook);
        }

        List<WebHookBean> listPost = new ArrayList<>();
        for (int j = 0; j < arrayOfPostHookJson.size(); j++) {
            JsonObject hookObj = arrayOfPostHookJson.get(j).getAsJsonObject();
            WebHookBean hook = new WebHookBean();
            setProperties(hookObj, hook);
            listPost.add(hook);
        }

        EnvWebHookBean.setPreDeployHooks(listPre);
        EnvWebHookBean.setPostDeployHooks(listPost);
        return EnvWebHookBean;
    }
}
