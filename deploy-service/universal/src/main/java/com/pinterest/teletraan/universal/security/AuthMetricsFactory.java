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
package com.pinterest.teletraan.universal.security;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;

public class AuthMetricsFactory {
    private static final String AUTHN_METRICS_NAME_TEMPLATE = "authn.%s";
    public static final String SUCCESS = "success";
    public static final String TYPE = "type";

    private AuthMetricsFactory() {}

    public static Counter.Builder createAuthNCounterBuilder(
            Class<?> clazz, Boolean success, PrincipalType type, String... extraTags) {
        return Counter.builder(String.format(AUTHN_METRICS_NAME_TEMPLATE, clazz.getSimpleName()))
                .tag(SUCCESS, success.toString())
                .tag(TYPE, type.toString())
                .tags(extraTags);
    }

    public static Counter createAuthNCounter(
            Class<?> clazz, Boolean success, PrincipalType type, String... extraTags) {
        return createAuthNCounterBuilder(clazz, success, type, extraTags)
                .register(Metrics.globalRegistry);
    }

    public enum PrincipalType {
        USER,
        SERVICE,
        NA
    }
}
