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
package com.pinterest.teletraan.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;

public class AwsFactory {

    private String eventBridgeEndPoint;

    private String eventBridgeEventBusName;

    @JsonProperty private List<String> accountAllowList;

    public EventBridgeAsyncClient buildEventBridgeClient() {
        return EventBridgeAsyncClient.builder().region(Region.US_EAST_1).build();
    }

    @JsonProperty
    public String getEventBridgeEndPoint() {
        return eventBridgeEndPoint;
    }

    @JsonProperty
    public void setEventBridgeEndPoint(String eventBridgeEndPoint) {
        this.eventBridgeEndPoint = eventBridgeEndPoint;
    }

    @JsonProperty
    public String getEventBridgeEventBusName() {
        return eventBridgeEventBusName;
    }

    @JsonProperty
    public void setEventBridgeEventBusName(String eventBridgeEventBusName) {
        this.eventBridgeEventBusName = eventBridgeEventBusName;
    }

    public List<String> getAccountAllowList() {
        return this.accountAllowList;
    }

    public void setAccountAllowList(List<String> accountAllowList) {
        this.accountAllowList = accountAllowList;
    }
}
