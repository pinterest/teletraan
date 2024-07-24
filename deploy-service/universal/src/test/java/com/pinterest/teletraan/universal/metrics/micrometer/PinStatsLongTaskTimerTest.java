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

import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.LongTaskTimer.Sample;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PinStatsLongTaskTimerTest {
    private PinStatsLongTaskTimer sut;
    private MockClock clock;
    private MeterRegistry registry;

    @BeforeEach
    void setUp() {
        clock = new MockClock();
        registry =
                new SimpleMeterRegistry(SimpleConfig.DEFAULT, clock) {
                    @Override
                    protected LongTaskTimer newLongTaskTimer(
                            Meter.Id id, DistributionStatisticConfig distributionStatisticConfig) {
                        return new PinStatsLongTaskTimer(
                                id, clock, getBaseTimeUnit(), distributionStatisticConfig);
                    }
                };
        sut =
                (PinStatsLongTaskTimer)
                        LongTaskTimer.builder("my.ltt")
                                .serviceLevelObjectives(Duration.ofSeconds(10))
                                .register(registry);
    }

    @Test
    void withoutParameter_start_nowAsStartTime() {
        Instant startTime = Instant.now();
        clock.add(startTime.getEpochSecond(), TimeUnit.SECONDS);
        Sample sample = sut.start();
        clock.addSeconds(1);
        assertEquals(1000, sample.duration(TimeUnit.MILLISECONDS), 0.1);
    }

    @Test
    void withInput_start_inputAsStartTime() {
        Instant startTime = Instant.now();
        // Set the clock to 5 seconds after startTime
        clock.add(
                startTime.plusSeconds(5).minusMillis(clock.wallTime()).toEpochMilli(),
                TimeUnit.MILLISECONDS);
        Sample sample = sut.start(startTime);
        // Add another 5 seconds to the clock
        clock.addSeconds(5);
        assertEquals(10 * 1000, sample.duration(TimeUnit.MILLISECONDS), 0.1);
    }

    @Test
    void testGaugeHistogram() {
        Sample sample = sut.start();
        sut.start();
        clock.addSeconds(1);

        // both active and within SLO
        assertEquals(2, sut.activeTasks());
        assertEquals(2, sut.takeSnapshot().histogramCounts()[0].count());

        // 1 remains active
        sample.stop();
        assertEquals(1, sut.activeTasks());
        assertEquals(1, sut.takeSnapshot().histogramCounts()[0].count());

        // remaining exceeds SLO
        clock.addSeconds(10);

        assertEquals(1, sut.activeTasks());
        assertEquals(0, sut.takeSnapshot().histogramCounts()[0].count());
    }
}
