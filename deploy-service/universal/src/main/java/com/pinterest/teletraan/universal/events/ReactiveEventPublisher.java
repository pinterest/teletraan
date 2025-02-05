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

import reactor.core.publisher.Flux;

public interface ReactiveEventPublisher extends AppEventPublisher {
    <E extends AppEvent> Flux<E> getEventsFlux();

    @Override
    @SuppressWarnings("unchecked")
    default <E extends AppEvent> void subscribe(AppEventListener<E> listener) {
        getEventsFlux()
                .subscribe(
                        e -> {
                            if (e.getClass().equals(listener.getSupportedEventType())) {
                                listener.onEvent((E) e);
                            }
                        });
    }
}
