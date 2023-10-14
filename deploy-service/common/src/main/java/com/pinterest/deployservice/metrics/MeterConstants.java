package com.pinterest.deployservice.metrics;

import static com.pinterest.teletraan.universal.metrics.micrometer.PinStatsNamingConvention.CUSTOM_NAME_PREFIX;

public class MeterConstants {
  public final static String ERROR_BUDGET_METRIC_NAME = CUSTOM_NAME_PREFIX + "error-budget.counters";
  public final static String ERROR_BUDGET_TAG_NAME_METHOD_NAME = "method_name";
  public final static String ERROR_BUDGET_TAG_NAME_RESPONSE_TYPE = "response_type";
  public final static String ERROR_BUDGET_TAG_VALUE_RESPONSE_TYPE_SUCCESS = "success";
  public final static String ERROR_BUDGET_TAG_VALUE_RESPONSE_TYPE_FAILURE = "failure";
}
