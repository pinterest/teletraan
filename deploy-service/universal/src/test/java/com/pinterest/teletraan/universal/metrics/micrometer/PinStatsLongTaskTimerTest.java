package com.pinterest.teletraan.universal.metrics.micrometer;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.LongTaskTimer.Sample;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

public class PinStatsLongTaskTimerTest {
    private PinStatsLongTaskTimer sut;
    private MockClock clock;
    private MeterRegistry registry;

    @BeforeEach
    void setUp() {
        clock = new MockClock();
        registry = new SimpleMeterRegistry(SimpleConfig.DEFAULT, clock) {
            @Override
            protected LongTaskTimer newLongTaskTimer(Meter.Id id,
                    DistributionStatisticConfig distributionStatisticConfig) {
                return new PinStatsLongTaskTimer(id, clock, getBaseTimeUnit(), distributionStatisticConfig);
            }
        };
        sut = (PinStatsLongTaskTimer) LongTaskTimer.builder("my.ltt").register(registry);
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
        clock.add(startTime.plusSeconds(5).minusMillis(clock.wallTime()).toEpochMilli(), TimeUnit.MILLISECONDS);
        Sample sample = sut.start(startTime);
        // Add another 5 seconds to the clock
        clock.addSeconds(5);
        assertEquals(10 * 1000, sample.duration(TimeUnit.MILLISECONDS), 0.1);
    }
}
