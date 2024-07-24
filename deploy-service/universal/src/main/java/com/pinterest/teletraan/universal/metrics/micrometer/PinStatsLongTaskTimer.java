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

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.internal.DefaultLongTaskTimer;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This is a custom implementation of {@link LongTaskTimer} to support user supplied start time. */
public class PinStatsLongTaskTimer extends DefaultLongTaskTimer {
    private static final Logger LOG = LoggerFactory.getLogger(PinStatsLongTaskTimer.class);

    private final Clock clock;

    public PinStatsLongTaskTimer(
            Id id,
            Clock clock,
            TimeUnit baseTimeUnit,
            DistributionStatisticConfig distributionStatisticConfig) {
        super(id, clock, baseTimeUnit, distributionStatisticConfig, false);
        this.clock = clock;
    }

    /**
     * Start the timer with user supplied start time.
     *
     * <p>This method can only provide an approximation of the start time. Therefore it is not
     * suitable for high precision use cases. You shouldn't use a {@link LongTaskTimer} for tracking
     * high precision durations anyways.
     *
     * <p>If for any reason the start time cannot be set, the current time will be used.
     *
     * @param startTime start time
     * @return the sample with specified start time
     */
    public Sample start(Instant startTime) {
        Sample sample = start();
        try {
            long timeLapsed = clock.wallTime() - startTime.toEpochMilli();
            long monotonicStartTime = clock.monotonicTime() - timeLapsed * 1000000;
            // The class `SampleImpl` is not visible, so we have to use reflection to set
            // the start time.
            Class<?> sampleImplClass = sample.getClass();
            Field field = sampleImplClass.getDeclaredField("startTime");
            field.setAccessible(true);
            field.set(sample, monotonicStartTime);
        } catch (Exception e) {
            LOG.error("Failed to set start time, use current time instead", e);
        }
        return sample;
    }
}
