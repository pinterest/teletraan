/**
 * Copyright (c) 2023 Pinterest, Inc.
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
package com.pinterest.teletraan.universal.events;

import java.util.Map;
import java.util.stream.Stream;
import lombok.Getter;

@Getter
public class ResourceChangedEvent extends AppEvent {

    private String resource;
    private String operator;
    private String[] tags;

    public ResourceChangedEvent(Object source) {
        super(source);
    }

    public ResourceChangedEvent(
            String resource, String operator, Object source, Map<String, String> tags) {
        this(resource, operator, source, System.currentTimeMillis(), tags);
    }

    public ResourceChangedEvent(
            String resource,
            String operator,
            Object source,
            long timestamp,
            Map<String, String> tags) {
        this(
                resource,
                operator,
                source,
                timestamp,
                tags.entrySet().stream()
                        .flatMap(e -> Stream.of(e.getKey(), e.getValue()))
                        .toArray(String[]::new));
    }

    public ResourceChangedEvent(
            String resource, String operator, Object source, long timestamp, String... tags) {
        super(source, timestamp);
        this.resource = resource;
        this.operator = operator;
        this.tags = tags;

        if (tags.length % 2 != 0) {
            throw new IllegalArgumentException("Number of tags must be even.");
        }
    }
}
