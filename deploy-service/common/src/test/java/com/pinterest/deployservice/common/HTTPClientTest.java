/**
 * Copyright (c) 2021 Pinterest, Inc.
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
package com.pinterest.deployservice.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class HTTPClientTest {;
    @Test
    public void testScrubUrlQueryValue() throws Exception {
        HTTPClient httpClient = new HTTPClient();
        // Future: there should be some better way to test HTTPClient private methods rather than
        // use reflection on private methods
        Method scrubUrlQueryValue =
                HTTPClient.class.getDeclaredMethod(
                        "scrubUrlQueryValue", String.class, String.class);
        scrubUrlQueryValue.setAccessible(true);

        assertEquals(
                HTTPClient.secretMask,
                scrubUrlQueryValue.invoke(httpClient, "access_token", "dangerous_stuff"));
        assertEquals(
                HTTPClient.secretMask,
                scrubUrlQueryValue.invoke(httpClient, "token", "dangerous_stuff"));
        assertEquals("some_stuff", scrubUrlQueryValue.invoke(httpClient, "s", "some_stuff"));
    }

    @Test
    public void testGenerateUrlAndQuery() throws Exception {
        HTTPClient httpClient = new HTTPClient();
        // Future: there should be some better way to test HTTPClient private methods rather than
        // use reflection on private methods
        Method generateUrlAndQuery =
                HTTPClient.class.getDeclaredMethod(
                        "generateUrlAndQuery", String.class, Map.class, boolean.class);
        generateUrlAndQuery.setAccessible(true);

        String baseUrl = "example.com/example/tests/";

        Map<String, String> filteredParams1 = new HashMap<>();
        filteredParams1.put("access_token", "dangerous_stuff");

        Map<String, String> filteredParams2 = new HashMap<>();
        filteredParams2.put("token", "dangerous_stuff");

        Map<String, String> unfilteredParams1 = new HashMap<>();
        unfilteredParams1.put("s", "some_stuff");

        // only pass 1 param to avoid flaky tests due to unordered Map
        // Future: Consider how handle multiple query parameter cases when testing HTTPClient
        assertEquals(
                "example.com/example/tests/?access_token=" + HTTPClient.secretMask,
                generateUrlAndQuery.invoke(httpClient, baseUrl, filteredParams1, true));
        assertEquals(
                "example.com/example/tests/?token=" + HTTPClient.secretMask,
                generateUrlAndQuery.invoke(httpClient, baseUrl, filteredParams2, true));
        assertEquals(
                "example.com/example/tests/?access_token=dangerous_stuff",
                generateUrlAndQuery.invoke(httpClient, baseUrl, filteredParams1, false));
        assertEquals(
                "example.com/example/tests/?s=some_stuff",
                generateUrlAndQuery.invoke(httpClient, baseUrl, unfilteredParams1, true));
    }
}
