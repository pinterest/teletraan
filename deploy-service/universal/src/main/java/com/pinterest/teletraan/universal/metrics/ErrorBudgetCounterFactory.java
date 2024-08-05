/**
 * Copyright (c) 2024 Pinterest, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pinterest.teletraan.universal.metrics;

import static com.pinterest.teletraan.universal.metrics.micrometer.PinStatsNamingConvention.CUSTOM_NAME_PREFIX;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

public class ErrorBudgetCounterFactory {

    public static final String ERROR_BUDGET_METRIC_NAME =
            CUSTOM_NAME_PREFIX + "error-budget.counters";
    public static final String ERROR_BUDGET_TAG_NAME_METHOD_NAME = "method_name";
    public static final String ERROR_BUDGET_TAG_NAME_RESPONSE_TYPE = "response_type";
    public static final String ERROR_BUDGET_TAG_VALUE_RESPONSE_TYPE_SUCCESS = "success";
    public static final String ERROR_BUDGET_TAG_VALUE_RESPONSE_TYPE_FAILURE = "failure";

    private ErrorBudgetCounterFactory() {}

    public static Counter createCounter(
            MeterRegistry registry, String methodName, boolean success) {
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
        return createCounter(
                io.micrometer.core.instrument.Metrics.globalRegistry, methodName, true);
    }

    public static Counter createFailureCounter(String methodName) {
        return createCounter(
                io.micrometer.core.instrument.Metrics.globalRegistry, methodName, false);
    }
}
