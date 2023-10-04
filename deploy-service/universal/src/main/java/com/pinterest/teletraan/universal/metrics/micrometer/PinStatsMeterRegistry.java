package com.pinterest.teletraan.universal.metrics.micrometer;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.NamingConvention;
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
    this(config, clock, null, new PinStatsNamingConvention(config.namePrefix()));
  }

  public PinStatsMeterRegistry(PinStatsConfig config, Clock clock, PinStatsPublisher publisher) {
    this(config, clock, publisher, new PinStatsNamingConvention(config.namePrefix()));
  }

  public PinStatsMeterRegistry(PinStatsConfig config, Clock clock, NamingConvention namingConvention) {
    this(config, clock, null, namingConvention);
  }

  public PinStatsMeterRegistry(PinStatsConfig config, Clock clock, PinStatsPublisher publisher, NamingConvention namingConvention) {
    super(config, clock, DEFAULT_THREAD_FACTORY, null);
    config().namingConvention(namingConvention);
    this.config = config;
    this.publisher =
        publisher == null
            ? new PinStatsPublisher(
                config, config().clock(), getBaseTimeUnit(), config().namingConvention())
            : publisher;
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
