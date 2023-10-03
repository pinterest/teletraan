package com.pinterest.teletraan.universal.events;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

public class ResourceChangedEventTest {
    private static final String OPERATOR = "operator";
    private static final String RESOURCE = "resource";

    @Test
    public void testConstructor_oddTags() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ResourceChangedEvent(RESOURCE, OPERATOR, this, 0L, ""));
    }

    @Test
    public void testConstructor_evenTags() {
        assertDoesNotThrow(() -> new ResourceChangedEvent(RESOURCE, OPERATOR, this, 0L, "t1", "v1"));
    }

    @Test
    public void testConstructor_noTag() {
        assertDoesNotThrow(() -> new ResourceChangedEvent(RESOURCE, OPERATOR, this, 0L));
    }

    @Test
    public void testConstructor_tagsAsMap() {
        Map<String, String> tags = ImmutableMap.of("t1", "v1");
        assertDoesNotThrow(() -> new ResourceChangedEvent(RESOURCE, OPERATOR, this, 0L, tags));
    }

    @Test
    public void testConstructor_nullSource() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ResourceChangedEvent(RESOURCE, OPERATOR, null, 0L));
    }
}
