/**
 * Copyright (c) 2016-2025 Pinterest, Inc.
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
package com.pinterest.teletraan.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pinterest.deployservice.events.DeployEvent;
import com.pinterest.teletraan.universal.events.AppEventPublisher;
import com.pinterest.teletraan.universal.events.GenericEventPublisher;
import com.pinterest.teletraan.universal.events.MetricsAsEventsListener;
import com.pinterest.teletraan.universal.events.ResourceChangedEvent;

public class AppEventFactory {
    @JsonProperty private Boolean enableMetricsAsEvent = true;

    @JsonProperty private Boolean publishDeployEvents = true;

    public AppEventPublisher createEventPublisher() {
        AppEventPublisher publisher = new GenericEventPublisher();
        if (enableMetricsAsEvent) {
            publisher.subscribe(
                    new MetricsAsEventsListener<ResourceChangedEvent>(ResourceChangedEvent.class));
            if (publishDeployEvents) {
                publisher.subscribe((new MetricsAsEventsListener<DeployEvent>(DeployEvent.class)));
            }
        }
        return publisher;
    }
}
