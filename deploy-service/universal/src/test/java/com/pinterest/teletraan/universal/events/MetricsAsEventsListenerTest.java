package com.pinterest.teletraan.universal.events;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Metrics;

public class MetricsAsEventsListenerTest {
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
