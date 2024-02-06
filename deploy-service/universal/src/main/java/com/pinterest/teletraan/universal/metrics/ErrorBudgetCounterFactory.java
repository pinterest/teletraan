package com.pinterest.teletraan.universal.metrics;

import static com.pinterest.teletraan.universal.metrics.micrometer.PinStatsNamingConvention.CUSTOM_NAME_PREFIX;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

public class ErrorBudgetCounterFactory {

  public final static String ERROR_BUDGET_METRIC_NAME = CUSTOM_NAME_PREFIX + "error-budget.counters";
  public final static String ERROR_BUDGET_TAG_NAME_METHOD_NAME = "method_name";
  public final static String ERROR_BUDGET_TAG_NAME_RESPONSE_TYPE = "response_type";
  public final static String ERROR_BUDGET_TAG_VALUE_RESPONSE_TYPE_SUCCESS = "success";
  public final static String ERROR_BUDGET_TAG_VALUE_RESPONSE_TYPE_FAILURE = "failure";

  private ErrorBudgetCounterFactory() {}

  public static Counter createCounter(MeterRegistry registry, String methodName, boolean success) {
    return Counter.builder(ERROR_BUDGET_METRIC_NAME)
        .tag(
            ERROR_BUDGET_TAG_NAME_RESPONSE_TYPE,
            success
                ? ERROR_BUDGET_TAG_VALUE_RESPONSE_TYPE_SUCCESS
                : ERROR_BUDGET_TAG_VALUE_RESPONSE_TYPE_FAILURE)
        .tag(ERROR_BUDGET_TAG_NAME_METHOD_NAME, methodName)
        .register(registry);
  }

  public static Counter createSuccessCounter(String methodName) {
    return createCounter(io.micrometer.core.instrument.Metrics.globalRegistry, methodName, true);
  }

  public static Counter createFailureCounter(String methodName) {
    return createCounter(io.micrometer.core.instrument.Metrics.globalRegistry, methodName, false);
  }
}
