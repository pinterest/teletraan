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
