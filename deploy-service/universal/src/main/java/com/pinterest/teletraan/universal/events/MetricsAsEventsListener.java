/**
 * Copyright (c) 2023-2025 Pinterest, Inc.
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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@RequiredArgsConstructor
public class MetricsAsEventsListener<E extends ResourceChangedEvent>
        implements AppEventListener<E> {
    private final MeterRegistry registry;
    private final Class<E> eventType;

    public MetricsAsEventsListener(Class<E> eventType) {
        this(Metrics.globalRegistry, eventType);
    }

    @Override
    public void onEvent(ResourceChangedEvent event) {
        String name = event.getResource();

        Counter.builder(name)
                .tag("operator", StringUtils.defaultString(event.getOperator()))
                .tags(event.getTags())
                .register(registry)
                .increment();
    }

    @Override
    public Class<E> getSupportedEventType() {
        return eventType;
    }
}
