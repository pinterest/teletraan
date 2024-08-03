/**
 * Copyright (c) 2023 Pinterest, Inc.
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

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class Constants {
    public static final String COUNTER_PREFIX = "counters.";
    public static final String MM_DOUBLE_COUNTER_PREFIX = "double_counters.";
    public static final String GAUGES_PREFIX = "gauges.";
    public static final String HISTOGRAM_PREFIX = "histograms.";
    public static final String TIMER_PREFIX = "timers.";
}
