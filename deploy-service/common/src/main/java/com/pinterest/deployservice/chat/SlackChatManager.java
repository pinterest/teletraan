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
package com.pinterest.deployservice.chat;

import in.ashwanthkumar.slack.webhook.SlackMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlackChatManager implements ChatManager {
    private static final Logger LOG = LoggerFactory.getLogger(SlackChatManager.class);
    private static final int TOTAL_RETRY = 3;
    private String url;

    public SlackChatManager(String url) {
        this.url = url;
    }

    @Override
    public void send(String from, String room, String message, String color) throws Exception {
        for (int i = 0; i < TOTAL_RETRY; i++) {
            try {
                in.ashwanthkumar.slack.webhook.Slack slack = new in.ashwanthkumar.slack.webhook.Slack(url);
                String convertedRoom = room.replaceAll(" ", "-").toLowerCase();
                slack.sendToChannel(convertedRoom)
                    .displayName(from)
                    .push(new SlackMessage(message));
                return;
            } catch (Exception e) {
                LOG.error("Failed to send Slack message to " + room, e);
            }
            Thread.sleep(1000);
        }
        LOG.error("Failed to send Slack message to " + room);
    }
}
