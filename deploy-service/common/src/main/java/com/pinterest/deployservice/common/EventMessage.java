/**
 * Copyright (c) 2016-2025 Pinterest, Inc.
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

public class EventMessage {
    private String instanceId;
    private String groupName;
    private String eventType;
    private String cause;
    private long timestamp;
    private String lifecycleToken;
    private String lifecycleHook;

    public EventMessage(
            String instanceId, String groupName, String type, String cause, long timestamp) {
        this.instanceId = instanceId;
        this.groupName = groupName;
        this.eventType = type;
        this.cause = cause;
        this.timestamp = timestamp;
    }

    public EventMessage(
            String lifecycleToken,
            String lifecycleHook,
            String groupName,
            String eventType,
            String instanceId,
            long timestamp) {
        this.lifecycleToken = lifecycleToken;
        this.lifecycleHook = lifecycleHook;
        this.groupName = groupName;
        this.eventType = eventType;
        this.instanceId = instanceId;
        this.timestamp = timestamp;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getEventType() {
        return eventType;
    }

    public String getCause() {
        return cause;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getLifecycleToken() {
        return lifecycleToken;
    }

    public String getLifecycleHook() {
        return lifecycleHook;
    }
}
