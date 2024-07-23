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


import com.pinterest.deployservice.handler.CommonHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

public final class NotificationJob implements Callable<Void> {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationJob.class);
    private String message;
    private String subject;
    private String recipients;
    private String chatrooms;
    private CommonHandler commonHandler;

    public NotificationJob(String message, String subject, String recipients, String chatrooms, CommonHandler commonHandler) {
        this.message = message;
        this.subject = subject;
        this.recipients = recipients;
        this.chatrooms = chatrooms;
        this.commonHandler = commonHandler;
    }

    public Void call() {
        try {
            if (!StringUtils.isEmpty(recipients)) {
                LOG.info(String.format("%s Send email to %s", subject, recipients));
                commonHandler.sendEmailMessage(message, subject, recipients);
            }

            if (!StringUtils.isEmpty(chatrooms)) {
                LOG.info(String.format("Send message to %s", chatrooms));
                commonHandler.sendChatMessage(Constants.SYSTEM_OPERATOR, chatrooms, message, "yellow", "");
            }
        } catch (Throwable t) {
            LOG.error(String.format("%s: Failed to send notifications.", subject), t);
        }
        return null;
    }
}
