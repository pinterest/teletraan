package com.pinterest.teletraan.universal.metrics.micrometer;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.internal.CumulativeHistogramLongTaskTimer;

/**
 * This is a custom implementation of {@link LongTaskTimer}
 * to support user supplied start time.
 */
public class PinStatsLongTaskTimer extends CumulativeHistogramLongTaskTimer {
  private static final Logger LOG = LoggerFactory.getLogger(PinStatsLongTaskTimer.class);

  private final Clock clock;

  public PinStatsLongTaskTimer(Id id, Clock clock, TimeUnit baseTimeUnit,
      DistributionStatisticConfig distributionStatisticConfig) {
    super(id, clock, baseTimeUnit, distributionStatisticConfig);
    this.clock = clock;
  }

  /**
   * Start the timer with user supplied start time.
   *
   * This method can only provide an approximation of the start time. Therefore it
   * is not suitable for high precision use cases. You shouldn't use a
   * {@link LongTaskTimer} for tracking high precision durations anyways.
   *
   * If for any reason the start time cannot be set, the current time will be
   * used.
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
