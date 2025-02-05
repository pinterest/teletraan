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
package com.pinterest.deployservice.chat;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HipChatManager implements ChatManager {
    private static final Logger LOG = LoggerFactory.getLogger(HipChatManager.class);
    private static final int TOTAL_RETRY = 3;
    private static final String MESSAGE_API = "https://api.hipchat.com/v1/rooms/message";
    private static final String ROOM_ID_KEY = "room_id";
    private static final String MESSAGE_KEY = "message";
    private static final String USER_KEY = "from";
    private static final String COLOR_KEY = "color";
    private static final String ROOMS_MESSAGE_QUERY_FORMAT = "?message_format=text&auth_token=%s";
    private String requestURL;

    public HipChatManager(String token) {
        requestURL = MESSAGE_API + String.format(ROOMS_MESSAGE_QUERY_FORMAT, token);
    }

    private String constructQuery(Map<String, String> maps) {
        StringBuilder params = new StringBuilder();
        int count = 0;
        for (Map.Entry<String, String> entry : maps.entrySet()) {
            if (count == 0) {
                params.append(String.format("%s=%s", entry.getKey(), entry.getValue()));
            } else {
                params.append(String.format("&%s=%s", entry.getKey(), entry.getValue()));
            }
            count++;
        }
        return params.toString();
    }

    @Override
    public void send(String from, String room, String message, String color) throws Exception {
        HashMap<String, String> params = new HashMap<>();
        params.put(ROOM_ID_KEY, URLEncoder.encode(room, "UTF-8"));
        params.put(USER_KEY, URLEncoder.encode(from, "UTF-8"));
        params.put(MESSAGE_KEY, URLEncoder.encode(message, "UTF-8"));
        if (color != null) {
            params.put(COLOR_KEY, color);
        }

        final String paramsToSend = this.constructQuery(params);
        String url = requestURL;
        DataOutputStream output = null;
        HttpURLConnection connection = null;
        for (int i = 0; i < TOTAL_RETRY; i++) {
            try {
                URL requestUrl = new URL(url);
                connection = (HttpURLConnection) requestUrl.openConnection();
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty(
                        "Content-Length", Integer.toString(paramsToSend.getBytes().length));
                connection.setRequestProperty("Content-Language", "en-US");
                output = new DataOutputStream(connection.getOutputStream());
                output.writeBytes(paramsToSend);
                return;
            } catch (Exception e) {
                LOG.error("Failed to send Hipchat message to room " + room, e);
            } finally {
                IOUtils.closeQuietly(output);
                if (connection != null) {
                    connection.disconnect();
                }
            }
            Thread.sleep(1000);
        }
        LOG.error("Failed to send Hipchat message to room " + room);
    }

    @Override
    public void sendToUser(String from, String user, String message, String color)
            throws Exception {
        LOG.info("HipChatManager sendToUser not implemented yet!");
    }
}
