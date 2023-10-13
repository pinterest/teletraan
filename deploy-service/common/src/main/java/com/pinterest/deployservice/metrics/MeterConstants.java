package com.pinterest.deployservice.metrics;

import static com.pinterest.teletraan.universal.metrics.micrometer.PinStatsNamingConvention.CUSTOM_NAME_PREFIX;

public class MeterConstants {
  public final static String ERROR_BUDGET_METRIC_NAME = CUSTOM_NAME_PREFIX + "error-budget.counters";
  public final static String ERROR_BUDGET_TAG_NAME_METHOD_NAME = CUSTOM_NAME_PREFIX + "method_name";
  public final static String ERROR_BUDGET_TAG_NAME_RESPONSE_TYPE = CUSTOM_NAME_PREFIX + "response_type";
  public final static String ERROR_BUDGET_TAG_VALUE_RESPONSE_TYPE_SUCCESS = CUSTOM_NAME_PREFIX + "success";
  public final static String ERROR_BUDGET_TAG_VALUE_RESPONSE_TYPE_FAILURE = CUSTOM_NAME_PREFIX + "failure";
}
