package com.pinterest.teletraan.universal.events;

import reactor.core.publisher.Flux;

public interface ReactiveEventPublisher extends AppEventPublisher {
    <E extends AppEvent> Flux<E> getEventsFlux();

    @Override
    @SuppressWarnings("unchecked")
    default <E extends AppEvent> void subscribe(AppEventListener<E> listener) {
        getEventsFlux().subscribe(e -> {
            if (e.getClass().equals(listener.getSupportedEventType())) {
                listener.onEvent((E) e);
            }
        });
    }
}
