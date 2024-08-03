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
package com.pinterest.teletraan.universal.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.micrometer.core.instrument.Metrics;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MetricsAsEventsListenerTest {
    private static final String CHILD_RESOURCE = "child_resource";
    private static final String RESOURCE = "resource";
    private MetricsAsEventsListener<ResourceChangedEvent> sut;
    private MetricsAsEventsListener<ChildTestEvent> childClassSut;
    private ResourceChangedEvent resourceChangedEvent;

    @BeforeAll
    static void setUpClass() {
        Metrics.globalRegistry.clear();
    }

    @BeforeEach
    void setUp() {
        sut = new MetricsAsEventsListener<ResourceChangedEvent>(ResourceChangedEvent.class);
        childClassSut = new MetricsAsEventsListener<ChildTestEvent>(ChildTestEvent.class);

        resourceChangedEvent = new ResourceChangedEvent(RESOURCE, "operator", this, 0L);
    }

    @Test
    void testGetSupportedEventType() {
        assertEquals(ResourceChangedEvent.class, sut.getSupportedEventType());
        assertEquals(ChildTestEvent.class, childClassSut.getSupportedEventType());
    }

    @Test
    void testOnEvent() {
        sut.onEvent(resourceChangedEvent);

        assertNotNull(Metrics.globalRegistry.find(RESOURCE));
    }

    class ChildTestEvent extends ResourceChangedEvent {
        public ChildTestEvent(Object source) {
            super(CHILD_RESOURCE, "operator", new Object(), 0L);
        }
    }
}
