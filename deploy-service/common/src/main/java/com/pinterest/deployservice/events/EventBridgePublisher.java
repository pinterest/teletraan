/**
 * Copyright (c) 2023-2024 Pinterest, Inc.
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pinterest.deployservice.bean.BuildBean;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

public class EventBridgePublisher implements BuildEventPublisher {

    public static final String TELETRAAN_SOURCE = "teletraan.build";
    public static final String DETAIL_TYPE = "Teletraan Build Action";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EventBridgeAsyncClient eventBridgeAsyncClient;
    private final String eventBusName;
    private static final Logger logger = LoggerFactory.getLogger(EventBridgePublisher.class);
    private static final String ORIGIN_PREFIX = "origin/";

    public EventBridgePublisher(
            EventBridgeAsyncClient eventBridgeAsyncClient, String eventBusName) {
        this.eventBridgeAsyncClient = eventBridgeAsyncClient;
        this.eventBusName = eventBusName;
    }

    @Override
    public void publish(BuildBean buildBean, String action) {
        final String originalBranch = buildBean.getScm_branch();

        // Some legacy CI jobs still use remote-tracking branch (with prefix "origin/" added to
        // branch name).
        // Remove this prefix before publishing.
        if (StringUtils.startsWithIgnoreCase(originalBranch, ORIGIN_PREFIX)
                && !StringUtils.equalsIgnoreCase(originalBranch, ORIGIN_PREFIX)) {
            final String localBranch = buildBean.getScm_branch().substring(ORIGIN_PREFIX.length());
            buildBean.setScm_branch(localBranch);
        }

        PutEventsRequestEntry entry =
                PutEventsRequestEntry.builder()
                        .eventBusName(eventBusName)
                        .source(TELETRAAN_SOURCE)
                        .detail(buildEventDetailJson(buildBean, action))
                        .detailType(DETAIL_TYPE)
                        .build();

        try {
            eventBridgeAsyncClient
                    .putEvents(e -> e.entries(entry).build())
                    .whenCompleteAsync(
                            (response, err) -> {
                                if (err != null) {
                                    logger.error(
                                            "Failed to publish event to Event Bridge: {}",
                                            entry,
                                            err);
                                }
                            });
            logger.info("Published build event {}", entry);
        } catch (Exception e) {
            logger.error("Failed to publish event to Event Bridge: {}", entry, e);
        }

        // set branch name back to its original value in case it's expected.
        buildBean.setScm_branch(originalBranch);
    }

    private String buildEventDetailJson(BuildBean buildBean, String action) {
        JsonNode buildBeanJsonNode = objectMapper.valueToTree(buildBean);
        ((ObjectNode) buildBeanJsonNode).put("action-type", action);
        return buildBeanJsonNode.toString();
    }
}
