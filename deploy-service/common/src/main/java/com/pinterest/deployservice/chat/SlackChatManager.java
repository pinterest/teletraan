/**
 * Copyright (c) 2016-2023 Pinterest, Inc.
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

import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.users.UsersLookupByEmailResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlackChatManager implements ChatManager {
    private static final Logger LOG = LoggerFactory.getLogger(SlackChatManager.class);
    private static final int TOTAL_RETRY = 3;
    private String url;
    private String token;
    private String domain;
    private Slack slack;

    public SlackChatManager(String token, String domain) {
        this.token = token;
        this.domain = domain;
        slack = Slack.getInstance();
    }

    private void postMessage(String from, String channel, String message) throws Exception {
        String msg = message + " (Operated by: <@" + from + ">)";
        LOG.debug("slack post message: " + msg);
        for (int i = 0; i < TOTAL_RETRY; i++) {
            try {
                ChatPostMessageResponse response =
                        slack.methods(this.token)
                                .chatPostMessage(req -> req.channel(channel).text(msg));
                if (response.isOk()) {
                    return;
                } else {
                    LOG.warn(
                            "Failed to send Slack message to "
                                    + channel
                                    + " ("
                                    + response.getError()
                                    + ")");
                }
            } catch (Exception e) {
                LOG.warn("Received exception from slack: " + e.getMessage());
            }
            Thread.sleep(1000);
        }
        LOG.error("Failed to send Slack message to " + channel + " (Retries limit reached)");
    }

    private String getUserIdFromEmail(String userHandle) {
        try {
            String email = userHandle.trim() + "@" + this.domain;
            UsersLookupByEmailResponse response =
                    slack.methods(this.token).usersLookupByEmail(req -> req.email(email));
            if (response.isOk()) {
                return response.getUser().getId();
            } else {
                String errorCode = response.getError();
                LOG.warn("Failed retrieving userId for email " + email + " (" + errorCode + ")");
            }
        } catch (SlackApiException requestFailure) {
            // Slack API responded with unsuccessful status code (= not 20x)
            LOG.warn("Slack API request returned error: " + requestFailure.getMessage());
        } catch (IOException connectivityIssue) {
            // Throwing this exception indicates your app or Slack servers had a connectivity issue.
            LOG.warn("Slack API connectivity issue: " + connectivityIssue.getMessage());
        } catch (Exception e) {
            LOG.warn("Received exception from slack API: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void send(String from, String room, String message, String color) throws Exception {
        String convertedRoom = room.replaceAll(" ", "-").toLowerCase();
        this.postMessage(from, convertedRoom, message);
    }

    // need to retrieve the user's id to send it as the channel parameter
    @Override
    public void sendToUser(String from, String user, String message, String color)
            throws Exception {
        if (user == null) {
            LOG.warn(
                    String.format(
                            "Unable to send message %s: User information was not provided",
                            message));
            return;
        }
        try {
            String userId = this.getUserIdFromEmail(user);
            if (userId != null) {
                this.postMessage(from, userId, message);
            }
        } catch (Exception e) {
            LOG.warn("Received exception while notifying slack user: " + e.getMessage());
        }
    }
}
