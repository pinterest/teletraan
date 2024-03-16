package com.pinterest.teletraan.universal.events;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GenericEventPublisherTest {
    private AppEventListener<ResourceChangedEvent> mockResourceChangedEventListener;
    private AppEventListener<ResourceChangedEvent> mockSlowListener;
    private AppEventListener<ChildTestEvent> mockChildTestEventListener;
    private AppEventListener<SiblingTestEvent> mockSiblingTestEventListener;
    private ResourceChangedEvent resourceChangedEvent;
    private ChildTestEvent childTestEvent;
    private SiblingTestEvent siblingTestEvent;
    private GenericEventPublisher sut;

    @BeforeEach
    public void setUp() {
        sut = new GenericEventPublisher();

        resourceChangedEvent = new ResourceChangedEvent(new Object());
        childTestEvent = new ChildTestEvent(new Object());
        siblingTestEvent = new SiblingTestEvent(new Object());

        mockResourceChangedEventListener = mockEventListener(ResourceChangedEvent.class);
        mockSlowListener = mockEventListener(ResourceChangedEvent.class);
        mockChildTestEventListener = mockEventListener(ChildTestEvent.class);
        mockSiblingTestEventListener = mockEventListener(SiblingTestEvent.class);
        doAnswer(
                invocation -> {
                    TimeUnit.MILLISECONDS.sleep(2);
                    return null;
                })
                .when(mockSlowListener)
                .onEvent(any());
    }

    @SuppressWarnings("unchecked")
    private <E extends AppEvent> AppEventListener<E> mockEventListener(Class<E> eventType) {
        AppEventListener<E> eventListener = (AppEventListener<E>) mock(AppEventListener.class);
        when(eventListener.getSupportedEventType()).thenReturn(eventType);
        return eventListener;
    }

    @Test
    public void testSubscribe() {
        sut.subscribe(mockResourceChangedEventListener);
        sut.publishEvent(resourceChangedEvent);
        sut.terminate();

        verify(mockResourceChangedEventListener).onEvent(eq(resourceChangedEvent));
    }

    @Test
    public void testPublishEvent_publishThenSubscribe() {
        for (int i = 0; i < GenericEventPublisher.BUFFER_SIZE + 10; i++) {
            sut.publishEvent(resourceChangedEvent);
        }
        sut.subscribe(mockResourceChangedEventListener);
        sut.terminate();

        // Up to BUFFER_SIZE events are processed
        verify(mockResourceChangedEventListener, times(GenericEventPublisher.BUFFER_SIZE))
                .onEvent(eq(resourceChangedEvent));
    }

    @Test
    public void testPublishEvent_subscribeThenPublish() {
        int numEvents = GenericEventPublisher.BUFFER_SIZE * 2;
        sut.subscribe(mockResourceChangedEventListener);
        for (int i = 0; i < numEvents; i++) {
            sut.publishEvent(resourceChangedEvent);
        }
        sut.terminate();

        // All events are processed
        verify(mockResourceChangedEventListener, times(numEvents)).onEvent(eq(resourceChangedEvent));
    }

    @Test
    public void testMultipleListeners() {
        sut.subscribe(mockResourceChangedEventListener);
        sut.subscribe(mockSlowListener);

        int numEvents = GenericEventPublisher.BUFFER_SIZE * 2 + 10;
        for (int i = 0; i < numEvents; i++) {
            sut.publishEvent(resourceChangedEvent);
        }
        sut.terminate();

        // Up to (Back pressure buffer + publishOn prefetch) = BUFFER_SIZE x 2
        verify(mockResourceChangedEventListener, times(GenericEventPublisher.BUFFER_SIZE * 2))
                .onEvent(eq(resourceChangedEvent));
        verify(mockSlowListener, times(GenericEventPublisher.BUFFER_SIZE * 2))
                .onEvent(eq(resourceChangedEvent));
    }

    @Test
    public void testMultipleListeners_differentEventTypes() {
        sut.subscribe(mockResourceChangedEventListener);
        sut.subscribe(mockSlowListener);
        sut.subscribe(mockChildTestEventListener);
        sut.subscribe(mockSiblingTestEventListener);

        sut.publishEvent(resourceChangedEvent);
        sut.publishEvent(childTestEvent);
        sut.publishEvent(siblingTestEvent);

        sut.terminate();

        verify(mockResourceChangedEventListener).onEvent(eq(resourceChangedEvent));
        verify(mockSlowListener).onEvent(eq(resourceChangedEvent));
        verify(mockChildTestEventListener).onEvent(eq(childTestEvent));
        verify(mockSiblingTestEventListener).onEvent(eq(siblingTestEvent));
    }

    class ChildTestEvent extends ResourceChangedEvent {
        public ChildTestEvent(Object source) {
            super(source);
        }
    }

    class SiblingTestEvent extends AppEvent {
        public SiblingTestEvent(Object source) {
            super(source);
        }
    }
}
