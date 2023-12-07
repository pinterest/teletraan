package com.pinterest.teletraan.universal.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitResult;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

public class GenericEventPublisher implements ReactiveEventPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(GenericEventPublisher.class);
    protected static final int BUFFER_SIZE = 128;
    private final Sinks.Many<AppEvent> eventsSink;
    private final Scheduler scheduler;
    @Getter
    private final Flux<? extends AppEvent> eventsFlux;

    public GenericEventPublisher() {
        eventsSink = Sinks.many().multicast().onBackpressureBuffer(BUFFER_SIZE);
        scheduler = Schedulers.newParallel(this.getClass().getName());
        eventsFlux = eventsSink.asFlux().publishOn(scheduler, BUFFER_SIZE);
    }

    @Override
    public <E extends AppEvent> void publishEvent(E event) {
        try {
            EmitResult result = eventsSink.tryEmitNext((AppEvent) event);
            if (result.isFailure()) {
                LOG.error("Failed to publish event, cause: " + result);
            }
        } catch (Exception ex) {
            LOG.error("Failed to publish event", ex);
        }
    }

    // for testing
    protected void terminate() {
        eventsSink.tryEmitComplete();
        eventsFlux.blockLast();
        scheduler.disposeGracefully().block();
    }

}
