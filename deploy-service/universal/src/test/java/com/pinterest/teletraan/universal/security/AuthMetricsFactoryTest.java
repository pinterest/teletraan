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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.pinterest.teletraan.universal.security.AuthMetricsFactory.PrincipalType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AuthMetricsFactoryTest {
    private static final String AUTHN_AUTH_METRICS_FACTORY_TEST = "authn.AuthMetricsFactoryTest";
    private static final String[] EXTRA_TAGS = {"tagK", "tagV"};

    @BeforeAll
    public static void setup() {
        Metrics.globalRegistry.add(new SimpleMeterRegistry());
    }

    @Test
    void testCreateAuthNCounterBuilder() {
        Counter.Builder counterBuilder =
                AuthMetricsFactory.createAuthNCounterBuilder(
                        AuthMetricsFactoryTest.class, false, PrincipalType.USER, EXTRA_TAGS);

        assertNotNull(counterBuilder);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testCreateAuthNCounter(Boolean success) {
        AuthMetricsFactory.createAuthNCounter(
                AuthMetricsFactoryTest.class, success, PrincipalType.USER, EXTRA_TAGS);
        Counter counter =
                Metrics.globalRegistry
                        .find(AUTHN_AUTH_METRICS_FACTORY_TEST)
                        .tag(AuthMetricsFactory.SUCCESS, success.toString())
                        .tag(
                                AuthMetricsFactory.TYPE,
                                AuthMetricsFactory.PrincipalType.USER.toString())
                        .counter();

        assertNotNull(counter);
        assertEquals(AUTHN_AUTH_METRICS_FACTORY_TEST, counter.getId().getName());
        assertEquals(success.toString(), counter.getId().getTag(AuthMetricsFactory.SUCCESS));
        assertEquals("USER", counter.getId().getTag(AuthMetricsFactory.TYPE));
        assertEquals(EXTRA_TAGS[1], counter.getId().getTag(EXTRA_TAGS[0]));

        counter.increment();
        assertEquals(1.0, counter.count());
    }
}
