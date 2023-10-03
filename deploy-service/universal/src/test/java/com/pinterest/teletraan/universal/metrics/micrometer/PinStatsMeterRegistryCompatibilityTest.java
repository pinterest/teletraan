package com.pinterest.teletraan.universal.metrics.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.tck.MeterRegistryCompatibilityKit;
import java.time.Duration;

class PinStatsMeterRegistryCompatibilityTest extends MeterRegistryCompatibilityKit {

  @Override
  public MeterRegistry registry() {
    return new PinStatsMeterRegistry(PinStatsConfig.DEFAULT, new MockClock());
  }

  @Override
  public Duration step() {
    return PinStatsConfig.DEFAULT.step();
  }
}
