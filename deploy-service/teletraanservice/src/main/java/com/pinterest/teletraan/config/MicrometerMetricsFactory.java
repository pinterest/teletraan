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
package com.pinterest.teletraan.config;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pinterest.teletraan.universal.metrics.micrometer.PinStatsConfig;
import com.pinterest.teletraan.universal.metrics.micrometer.PinStatsMeterRegistry;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import io.dropwizard.metrics.MetricsFactory;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Metrics;

public class MicrometerMetricsFactory extends MetricsFactory {
    @JsonProperty("mm.uri")
    private String mm_uri;

    @JsonProperty("mm.namePrefix")
    private String mm_namePrefix;

    public String get(String propertyName) {
        try {
            propertyName = propertyName.replace('.', '_');
            return this.getClass().getDeclaredField(propertyName).get(this).toString();
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    @Override
    public void configure(LifecycleEnvironment environment, MetricRegistry registry) {
        super.configure(environment, registry);
        PinStatsConfig config = this::get;
        Metrics.addRegistry(new PinStatsMeterRegistry(config, Clock.SYSTEM));
    }
}
