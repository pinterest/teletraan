/**
 * Copyright 2016 Pinterest, Inc.
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

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class HTTPClientTest {
    @Test
    public void testScrubUrlQueryValue() throws Exception {
        HTTPClient httpClient = new HTTPClient();

        assertEquals("xxxxxxxxx", httpClient.scrubUrlQueryValue("access_token", "dangerous_stuff"));
        assertEquals("xxxxxxxxx", httpClient.scrubUrlQueryValue("token", "dangerous_stuff"));
        assertEquals("some_stuff", httpClient.scrubUrlQueryValue("s", "some_stuff"));
    }

    @Test
    public void testGenerateUrlAndQuery() throws Exception {
        HTTPClient httpClient = new HTTPClient();
        String baseUrl = "example.com/example/tests/";

        Map<String, String> filteredParams1 = new HashMap<>();
        Map<String, String> filteredParams2 = new HashMap<>();
        filteredParams1.put("access_token", "dangerous_stuff");
        filteredParams2.put("token", "dangerous_stuff");

        Map<String, String> unfilteredParams1 = new HashMap<>();
        unfilteredParams1.put("s", "some_stuff");

        // only pass 1 param to avoid flaky tests due to unordered Map
        // Future: Add some other tests to handle multiple query parameter cases
        assertEquals("example.com/example/tests/?access_token=xxxxxxxxx", httpClient.generateUrlAndQuery(baseUrl, filteredParams1, true));
        assertEquals("example.com/example/tests/?token=xxxxxxxxx", httpClient.generateUrlAndQuery(baseUrl, filteredParams2, true));
        assertEquals("example.com/example/tests/?access_token=dangerous_stuff", httpClient.generateUrlAndQuery(baseUrl, filteredParams1, false));
        assertEquals("example.com/example/tests/?s=some_stuff", httpClient.generateUrlAndQuery(baseUrl, unfilteredParams1, true));
    }
}
