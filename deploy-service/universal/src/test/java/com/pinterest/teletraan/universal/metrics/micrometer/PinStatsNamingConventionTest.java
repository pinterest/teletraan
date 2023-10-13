package com.pinterest.teletraan.universal.metrics.micrometer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.micrometer.core.instrument.Meter;
import org.junit.jupiter.api.Test;

public class PinStatsNamingConventionTest {
  private PinStatsNamingConvention convention = new PinStatsNamingConvention();
  private static final String METER_NAME = "test.meter";

  @Test
  void testName() {
    assertEquals("mm.counters.test_meter_total", convention.name(METER_NAME, Meter.Type.COUNTER));
    assertEquals("mm.gauges.test_meter", convention.name(METER_NAME, Meter.Type.GAUGE));
    assertEquals(METER_NAME, convention.name(METER_NAME, Meter.Type.OTHER));
    assertEquals(
        "mm.histograms.test_meter", convention.name(METER_NAME, Meter.Type.DISTRIBUTION_SUMMARY));
    assertEquals(
        "mm.test_meter_duration_seconds", convention.name(METER_NAME, Meter.Type.LONG_TASK_TIMER));
    assertEquals(
        "mm.timers.test_meter_duration_seconds", convention.name(METER_NAME, Meter.Type.TIMER));
  }

  @Test
  void testNamePrefix() {
    String prefix = "a.name.prefix.";
    PinStatsNamingConvention conventionWithPrefix = new PinStatsNamingConvention(prefix);
    assertEquals(
        prefix + "counters.test_meter_total",
        conventionWithPrefix.name(METER_NAME, Meter.Type.COUNTER));
  }

  @Test
  void testTagKey() {
    assertEquals("tag_key", convention.tagKey("tag.key"));
    assertEquals("m_0tag_key", convention.tagKey("0tag.key"));
    assertEquals("m__tag_key", convention.tagKey("*tag.key"));
  }

  @Test
  void testCustomName() {
    String customName = "abcDEF.123-xyZ";
    assertEquals(customName,
        convention.name(PinStatsNamingConvention.CUSTOM_NAME_PREFIX + customName, Meter.Type.COUNTER));
  }
}