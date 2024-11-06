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
package com.pinterest.deployservice.rodimus;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RodimusManagerImplTest {
    private static final String TEST_CLUSTER = "cluster1";
    private static final List<String> HOST_IDS = Collections.singletonList("i-001");
    private static final String TEST_PATH = "/testUrl";

    private static MockWebServer mockWebServer;
    private RodimusManagerImpl sut;

    @BeforeEach
    public void setUpEach() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        sut = new RodimusManagerImpl(mockWebServer.url(TEST_PATH).toString(), null, false, "", "");
    }

    @AfterEach
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void testConstructorInValidProxyConfig() {
        assertThrows(
                NumberFormatException.class,
                () -> {
                    new RodimusManagerImpl(TEST_PATH, null, true, "localhost", "invalidPort");
                });
    }

    @Test
    void testNullKnoxKeyUsesDefaultKey() throws Exception {
        // return 401 to trigger authentication flow
        mockWebServer.enqueue(new MockResponse().setResponseCode(401));
        mockWebServer.enqueue(new MockResponse().setBody("[]"));
        sut.getTerminatedHosts(Collections.singletonList("testHost"));

        // discard first request
        RecordedRequest request = mockWebServer.takeRequest();
        request = mockWebServer.takeRequest();
        assertEquals("token defaultKeyContent", request.getHeader("Authorization"));
    }

    @Test
    void testInvalidKnoxKeyThrowsException() throws Exception {
        // return 401 to trigger authentication flow
        mockWebServer.enqueue(new MockResponse().setResponseCode(401));
        RodimusManagerImpl sut =
                new RodimusManagerImpl(
                        mockWebServer.url(TEST_PATH).toString(),
                        "invalidRodimusKnoxKey",
                        false,
                        "",
                        "");
        assertThrows(IllegalStateException.class, () -> sut.getTerminatedHosts(HOST_IDS));
        assertEquals(1, mockWebServer.getRequestCount());
    }

    @Test
    void testTerminateHostsByClusterNameOk() {
        mockWebServer.enqueue(new MockResponse());

        assertDoesNotThrow(
                () -> {
                    sut.terminateHostsByClusterName(TEST_CLUSTER, HOST_IDS);
                });
    }

    @Test
    void testTerminateHostsByClusterNameEmptyHosts() {
        assertDoesNotThrow(
                () -> {
                    sut.terminateHostsByClusterName(TEST_CLUSTER, Collections.emptyList());
                });
    }

    @Test
    void testTerminateHostsByClusterNameClientError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        ClientErrorException exception =
                assertThrows(
                        ClientErrorException.class,
                        () -> {
                            sut.terminateHostsByClusterName(TEST_CLUSTER, HOST_IDS);
                        });
        assertEquals(404, exception.getResponse().getStatus());
        assertEquals(1, mockWebServer.getRequestCount());
    }

    @Test
    void testTerminateHostsByClusterNameServerError() {
        mockWebServer.setDispatcher(new ServerErrorDispatcher());
        ServerErrorException exception =
                assertThrows(
                        ServerErrorException.class,
                        () -> {
                            sut.terminateHostsByClusterName(TEST_CLUSTER, HOST_IDS);
                        });
        assertEquals(500, exception.getResponse().getStatus());
        assertEquals(3, mockWebServer.getRequestCount());
    }

    @Test
    void testGetTerminatedHostsOk() throws Exception {
        mockWebServer.enqueue(new MockResponse().setBody(HOST_IDS.toString()));

        Collection<String> terminatedHosts = sut.getTerminatedHosts(HOST_IDS);
        assertArrayEquals(HOST_IDS.toArray(), terminatedHosts.toArray());
    }

    @Test
    void testGetTerminatedHostEmptyHostIds() throws Exception {
        Collection<String> terminatedHosts = sut.getTerminatedHosts(Collections.emptyList());
        assertArrayEquals(new String[] {}, terminatedHosts.toArray());
    }

    @Test
    void testGetClusterInstanceLaunchGracePeriodOk() throws Exception {
        mockWebServer.enqueue(new MockResponse().setBody("{\"launchLatencyTh\": 300}"));

        Long gracePeriod = sut.getClusterInstanceLaunchGracePeriod(TEST_CLUSTER);
        assertEquals(300L, gracePeriod);
    }

    @Test
    void testGetClusterInstanceLaunchGracePeriodNullResponse() throws Exception {
        mockWebServer.enqueue(new MockResponse());

        Long gracePeriod = sut.getClusterInstanceLaunchGracePeriod(TEST_CLUSTER);
        assertEquals(null, gracePeriod);
    }

    @Test
    void testGetClusterInstanceLaunchGracePeriodNoLaunchLatencyTh() throws Exception {
        mockWebServer.enqueue(new MockResponse().setBody("{}"));

        Long gracePeriod = sut.getClusterInstanceLaunchGracePeriod(TEST_CLUSTER);
        assertEquals(null, gracePeriod);
    }

    @Test
    void testGetEc2TagsOk() throws Exception {
        String responseBody = "{\"i-001\": {\"Name\": \"test-instance\"}}";
        mockWebServer.enqueue(new MockResponse().setBody(responseBody));

        Map<String, Map<String, String>> ec2Tags = sut.getEc2Tags(HOST_IDS);
        assertEquals("test-instance", ec2Tags.get("i-001").get("Name"));
    }

    @Test
    void testGetEc2TagsEmptyResponse() throws Exception {
        mockWebServer.enqueue(new MockResponse().setBody("{}"));

        Map<String, Map<String, String>> ec2Tags = sut.getEc2Tags(HOST_IDS);
        assertTrue(ec2Tags.isEmpty());
    }

    static class ServerErrorDispatcher extends Dispatcher {
        @Override
        public MockResponse dispatch(RecordedRequest request) {
            return new MockResponse().setResponseCode(500);
        }
    }
}
