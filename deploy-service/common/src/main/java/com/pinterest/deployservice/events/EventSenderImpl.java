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
package com.pinterest.deployservice.events;

import com.google.gson.JsonObject;
import com.pinterest.deployservice.common.DeployInternalException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class EventSenderImpl implements EventSender {
    private static final Logger LOG = LoggerFactory.getLogger(EventSenderImpl.class);
    private String URL;
    private static int TOTAL_RETRY = 3;

    public EventSenderImpl(String url) {
        this.URL = url;
    }

    public void sendDeployEvent(String what, String tags, String data) throws Exception {
        JsonObject object = new JsonObject();
        object.addProperty("what", what);
        object.addProperty("tags", tags);
        object.addProperty("data", data);
        final String paramsToSend = object.toString();
        DataOutputStream output = null;
        HttpURLConnection connection = null;
        int retry = 0;
        while (retry < TOTAL_RETRY) {
            try {
                URL requestUrl = new URL(this.URL);
                connection = (HttpURLConnection) requestUrl.openConnection();
                connection.setDoOutput(true);

                connection.setRequestProperty("Content-Type", "application/json; charset=utf8");
                connection.setRequestProperty("Content-Length", Integer.toString(paramsToSend.getBytes().length));
                connection.setRequestProperty("Content-Language", "en-US");
                connection.setRequestMethod("POST");
                output = new DataOutputStream(connection.getOutputStream());
                output.writeBytes(paramsToSend);
                output.flush();
                output.close();
                String result = IOUtils.toString(connection.getInputStream(), "UTF-8").toLowerCase();
                LOG.info("Successfully send events to the statsboard: " + result);
                return;
            } catch (Exception e) {
                LOG.error("Failed to send event", e);
            } finally {
                IOUtils.closeQuietly(output);
                if (connection != null) {
                    connection.disconnect();
                }
                retry++;
            }
        }
        throw new DeployInternalException("Failed to send event");
    }
}
