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

import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Meter.Type;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Statistic;
import io.micrometer.core.instrument.Tag;
import java.util.Arrays;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class Utils {

    /**
     * Register a meter with the given registry, name, tags and value.
     *
     * <p>The meter is built with a single measurement of type VALUE. This provides a workaround
     * that micrometer doesn't support dynamic tags. It's done by removing the existing meter and
     * creating a new one with the same name and tags. Otherwise, the new value will not be reported
     * because the registry only keeps 1 meter and the measurement is immutable after creation.
     *
     * @param registry where to find and register the meter
     * @param meterName name of the meter
     * @param tags tags of the meter
     * @param value value of the meter
     */
    public static Meter registerOrReplaceMeter(
            MeterRegistry registry, String meterName, Iterable<Tag> tags, Double value) {
        return registerOrReplaceMeter(registry, meterName, tags, value, Type.OTHER);
    }

    public static Meter registerOrReplaceMeter(
            MeterRegistry registry, String meterName, Iterable<Tag> tags, Double value, Type type) {
        Meter existing = registry.find(meterName).tags(tags).meter();
        if (existing != null) {
            registry.remove(existing);
        }
        return Meter.builder(
                        meterName,
                        type,
                        Arrays.asList(new Measurement(() -> value, Statistic.VALUE)))
                .tags(tags)
                .register(registry);
    }
}
