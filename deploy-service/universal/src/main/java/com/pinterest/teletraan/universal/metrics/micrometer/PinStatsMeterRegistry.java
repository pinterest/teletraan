/**
 * Copyright (c) 2023-2024 Pinterest, Inc.
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
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.util.MeterPartition;
import io.micrometer.core.instrument.util.NamedThreadFactory;
import io.micrometer.opentsdb.OpenTSDBMeterRegistry;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PinStatsMeterRegistry extends OpenTSDBMeterRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(PinStatsMeterRegistry.class);
    private static final ThreadFactory DEFAULT_THREAD_FACTORY =
            new NamedThreadFactory("pinstats-metrics-publisher");

    private final PinStatsConfig config;
    private final PinStatsPublisher publisher;

    public PinStatsMeterRegistry(PinStatsConfig config, Clock clock) {
        this(config, clock, null);
    }

    public PinStatsMeterRegistry(PinStatsConfig config, Clock clock, PinStatsPublisher publisher) {
        super(config, clock, DEFAULT_THREAD_FACTORY, null);
        config().namingConvention(new PinStatsNamingConvention(config.namePrefix()));
        this.config = config;
        this.publisher =
                publisher == null
                        ? new PinStatsPublisher(
                                config,
                                config().clock(),
                                getBaseTimeUnit(),
                                config().namingConvention())
                        : publisher;
    }

    @Override
    protected LongTaskTimer newLongTaskTimer(
            Meter.Id id, DistributionStatisticConfig distributionStatisticConfig) {
        return new PinStatsLongTaskTimer(id, clock, getBaseTimeUnit(), distributionStatisticConfig);
    }

    @Override
    protected void publish() {
        for (List<Meter> batch : MeterPartition.partition(this, config.batchSize())) {
            try {
                publisher.publish(batch);
            } catch (Throwable t) {
                LOG.warn("failed to publish metrics", t);
            }
        }
    }
}
