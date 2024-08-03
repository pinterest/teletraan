/**
 * Copyright (c) 2023-2024 Pinterest, Inc.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Meter.Type;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Statistic;
import io.micrometer.core.instrument.Tags;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UtilsTest {
    @BeforeEach
    public void setUp() {
        Metrics.globalRegistry.clear();
    }

    @Test
    void testRegisterOrReplaceMeter() {
        String meterName = "meter";
        Tags tags = Tags.of("tagKey", "tagValue");
        double value1 = 1.0;
        Meter meter1 =
                Meter.builder(
                                meterName,
                                Type.GAUGE,
                                Arrays.asList(new Measurement(() -> value1, Statistic.VALUE)))
                        .tags(tags)
                        .register(Metrics.globalRegistry);

        // When registering a meter directly with the same name and tags, the old meter
        // should be returned.
        double value2 = 2.0;
        Meter meter2 =
                Meter.builder(
                                meterName,
                                Type.GAUGE,
                                Arrays.asList(new Measurement(() -> value2, Statistic.VALUE)))
                        .tags(tags)
                        .register(Metrics.globalRegistry);
        assertSame(meter1, meter2);

        // The value remains the same.
        Meter found = Metrics.globalRegistry.find(meterName).tags(tags).meter();
        assertNotNull(found);
        assertEquals(value1, found.measure().iterator().next().getValue());

        // When registering the meter with Utils.registerOrReplaceMeter, a new meter
        // should be returned.
        Meter meter3 =
                Utils.registerOrReplaceMeter(
                        Metrics.globalRegistry, meterName, tags, value2, Type.GAUGE);
        assertNotSame(meter1, meter3);

        found = Metrics.globalRegistry.find(meterName).tags(tags).meter();
        assertNotNull(found);
        assertEquals(value2, found.measure().iterator().next().getValue());
    }
}
