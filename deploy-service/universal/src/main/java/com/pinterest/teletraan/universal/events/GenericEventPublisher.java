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

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    @Getter private final Flux<? extends AppEvent> eventsFlux;

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
