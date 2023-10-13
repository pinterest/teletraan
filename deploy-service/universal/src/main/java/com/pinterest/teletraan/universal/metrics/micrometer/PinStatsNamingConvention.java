package com.pinterest.teletraan.universal.metrics.micrometer;

import static com.pinterest.teletraan.universal.metrics.Constants.COUNTER_PREFIX;
import static com.pinterest.teletraan.universal.metrics.Constants.GAUGES_PREFIX;
import static com.pinterest.teletraan.universal.metrics.Constants.HISTOGRAM_PREFIX;
import static com.pinterest.teletraan.universal.metrics.Constants.TIMER_PREFIX;

import io.micrometer.core.instrument.Meter.Type;
import io.micrometer.opentsdb.OpenTSDBNamingConvention;

public class PinStatsNamingConvention extends OpenTSDBNamingConvention {
  public final static String CUSTOM_NAME_PREFIX = "custom.";
  private String prefix = "mm.";

  public PinStatsNamingConvention() {
    super();
  }

  public PinStatsNamingConvention(String prefix) {
    super();
    this.prefix = prefix;
  }

  public PinStatsNamingConvention(String prefix, String timerSuffix) {
    super(timerSuffix);
    this.prefix = prefix;
  }

  @Override
  public String name(String name, Type type, String baseUnit) {
    if (name.startsWith(CUSTOM_NAME_PREFIX)) {
      return name.replaceFirst(CUSTOM_NAME_PREFIX, "");
    }

    String sanitized = super.name(name, type, baseUnit);
    switch (type) {
      case COUNTER:
        sanitized = COUNTER_PREFIX + sanitized;
        break;
      case TIMER:
        sanitized = TIMER_PREFIX + sanitized;
        break;
      case GAUGE:
        sanitized = GAUGES_PREFIX + sanitized;
        break;
      case DISTRIBUTION_SUMMARY:
        sanitized = HISTOGRAM_PREFIX + sanitized;
        break;
      case OTHER:
        // We want to keep the name as is for custom metrics
        return name;
      default:
        break;
    }
    return prefix + sanitized;
  }
}
