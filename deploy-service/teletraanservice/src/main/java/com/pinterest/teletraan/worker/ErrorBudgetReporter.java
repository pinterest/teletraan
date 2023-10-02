package com.pinterest.teletraan.worker;

import io.micrometer.core.instrument.Metrics;

// It could be the same class for Teletraan Worker and Rodimus Worker, however the nimbus_uuid will be different
public class ErrorBudgetReporter {
    private static final String TELETRAAN_WORKER_ERROR_BUDGET_METRIC_NAME = "error-budget.counters.<nimbus_uuid>";

    private final Object caller;

    public ErrorBudgetReporter(Object caller) {
        this.caller = caller;
    }

    public void SendMetric(boolean isSuccess) {
        String result = (isSuccess) ? "success" : "failure";

        Metrics.counter(TELETRAAN_WORKER_ERROR_BUDGET_METRIC_NAME,
                "response_type", result,
                "method_name", caller.getClass().getSimpleName())
                .increment();
    }
}