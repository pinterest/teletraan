/**
 * Copyright (c) 2024 Pinterest, Inc.
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
package com.pinterest.teletraan.universal.metrics.micrometer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pinterest.teletraan.universal.metrics.Utils;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Meter.Type;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PinStatsPublisherTest {
    private static final Logger LOG = LoggerFactory.getLogger(PinStatsPublisherTest.class);

    private static final PinStatsConfig config = PinStatsConfig.DEFAULT;
    private final MockClock clock = new MockClock();
    private final PinStatsMeterRegistry meterRegistry = new PinStatsMeterRegistry(config, clock);
    private final PinStatsPublisher publisher =
            new PinStatsPublisher(config, clock, TimeUnit.SECONDS, new PinStatsNamingConvention());
    private final Tags tags = Tags.of("tag", "value");

    @Test
    void writeCounter() {
        String counterName = "my.counter";
        meterRegistry.counter(counterName, "t1", "v1", "t2", "v2");
        Counter counter = meterRegistry.get(counterName).counter();
        counter.increment();
        assertEquals(1, publisher.writeCounter(counter).count());
        assertEquals(
                "put mm.counters.my_counter_total 1 " + clock.wallTime() + " t1=v1 t2=v2\n",
                publisher.writeCounter(counter).collect(Collectors.joining()));
    }

    @Test
    void writeGauge() {
        meterRegistry.gauge("my.gauge", 1d);
        Gauge gauge = meterRegistry.get("my.gauge").gauge();
        assertEquals(1, publisher.writeGauge(gauge).count());
    }

    @Test
    void writeTimeGauge() {
        AtomicReference<Double> obj = new AtomicReference<>(1d);
        meterRegistry
                .more()
                .timeGauge(
                        "my.time.gauge", Tags.empty(), obj, TimeUnit.SECONDS, AtomicReference::get);
        TimeGauge timeGauge = meterRegistry.get("my.time.gauge").timeGauge();
        assertEquals(1, publisher.writeTimeGauge(timeGauge).count());
    }

    @Test
    void writeFunctionCounter() {
        FunctionCounter counter =
                FunctionCounter.builder("my.function_counter", 1d, Number::doubleValue)
                        .register(meterRegistry);
        clock.add(config.step());
        assertEquals(1, publisher.writeFunctionCounter(counter).count());
    }

    @Test
    void histogramBucketsHaveCorrectBaseUnit() {
        Timer timer =
                Timer.builder("my.timer")
                        .publishPercentileHistogram()
                        .serviceLevelObjectives(Duration.ofMillis(900), Duration.ofSeconds(1))
                        .register(meterRegistry);

        timer.record(1, TimeUnit.SECONDS);
        clock.add(config.step());

        publisher.writeTimer(timer).forEach(LOG::debug);

        assertTrue(
                publisher
                        .writeTimer(timer)
                        .anyMatch(
                                t ->
                                        t.equals(
                                                "put mm.timers.my_timer_duration_seconds.bucket 60001 1 le=1.0\n")));
        assertTrue(
                publisher
                        .writeTimer(timer)
                        .anyMatch(
                                t ->
                                        t.equals(
                                                "put mm.timers.my_timer_duration_seconds.bucket 60001 0 le=0.9\n")));
    }

    @Test
    void longTaskTimer() {
        LongTaskTimer timer = LongTaskTimer.builder("my.timer").tags(tags).register(meterRegistry);
        publisher.writeLongTaskTimer(timer).forEach(LOG::debug);

        assertTrue(
                publisher
                        .writeLongTaskTimer(timer)
                        .anyMatch(
                                t ->
                                        t.equals(
                                                "put mm.my_timer_duration_seconds.active_count 1 0 tag=value\n")));
        assertTrue(
                publisher
                        .writeLongTaskTimer(timer)
                        .anyMatch(
                                t ->
                                        t.equals(
                                                "put mm.my_timer_duration_seconds.duration_sum 1 0 tag=value\n")));
        assertTrue(
                publisher
                        .writeLongTaskTimer(timer)
                        .anyMatch(
                                t ->
                                        t.equals(
                                                "put mm.my_timer_duration_seconds.max 1 0 tag=value\n")));
    }

    @Test
    void writeCustomMetric() {
        String name = "my.meter";
        double value = 1.0;

        Meter m = Utils.registerOrReplaceMeter(meterRegistry, name, tags, value);
        String putString = publisher.writeCustomMetric(m).collect(Collectors.joining());
        assertEquals("put my.meter 1 1 statistics=VALUE tag=value\n", putString);

        m = Utils.registerOrReplaceMeter(meterRegistry, name, tags, value, Type.GAUGE);
        putString = publisher.writeCustomMetric(m).collect(Collectors.joining());
        assertEquals("put mm.gauges.my_meter 1 1 statistics=VALUE tag=value\n", putString);
    }
}
