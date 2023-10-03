package com.pinterest.teletraan.universal.events;

public interface AppEventPublisher {
    default <E extends AppEvent> void publishEvent(E event) {}

    default <E extends AppEvent> void subscribe(AppEventListener<E> listener) {}
}
