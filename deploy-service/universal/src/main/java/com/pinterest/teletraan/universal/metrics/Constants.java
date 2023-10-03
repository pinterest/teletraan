package com.pinterest.teletraan.universal.metrics;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class Constants {
  public static final String COUNTER_PREFIX = "counters.";
  public static final String MM_DOUBLE_COUNTER_PREFIX = "double_counters.";
  public static final String GAUGES_PREFIX = "gauges.";
  public static final String HISTOGRAM_PREFIX = "histograms.";
  public static final String TIMER_PREFIX = "timers.";
}
