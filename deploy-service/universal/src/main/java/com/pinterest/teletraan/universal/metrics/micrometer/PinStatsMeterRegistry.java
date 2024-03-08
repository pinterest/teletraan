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
                config, config().clock(), getBaseTimeUnit(), config().namingConvention())
            : publisher;
  }

  @Override
  protected LongTaskTimer newLongTaskTimer(Meter.Id id, DistributionStatisticConfig distributionStatisticConfig) {
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
