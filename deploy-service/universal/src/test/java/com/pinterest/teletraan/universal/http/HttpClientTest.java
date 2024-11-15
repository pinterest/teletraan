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
package com.pinterest.teletraan.universal.http;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableMap;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.util.Map;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.WebApplicationException;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HttpClientTest {
    private static final String TEST_PATH = "/testUrl";
    private static final String TEST_BODY = "{\"key\":\"value\"}";
    private static final Map<String, String> TEST_HEADERS = ImmutableMap.of("h1", "v1", "h2", "v2");
    private static final Map<String, String> TEST_PARAMS = ImmutableMap.of("p1", "v1", "p2", "v2");
    private static MockWebServer mockWebServer;

    private HttpClient sut;

    @BeforeAll
    public static void setUpClass() {
        Metrics.addRegistry(new SimpleMeterRegistry());
    }

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        sut = new HttpClient.HttpClientBuilder().maxRetries(3).retryInterval(10L).build();
    }

    @AfterEach
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @ParameterizedTest
    @ValueSource(ints = {429, 500, 502, 503, 504})
    void testRetry(int responseCode) throws IOException {
        mockWebServer.setDispatcher(new ServerErrorDispatcher(responseCode));
        WebApplicationException exception =
                assertThrows(
                        WebApplicationException.class,
                        () -> {
                            sut.get(
                                    mockWebServer.url(TEST_PATH).toString(),
                                    TEST_PARAMS,
                                    TEST_HEADERS);
                        });
        assertEquals(responseCode, exception.getResponse().getStatus());
        assertEquals(3, mockWebServer.getRequestCount());
    }

    @Test
    void testObservability() throws IOException {
        mockWebServer.enqueue(new MockResponse());
        sut.get(mockWebServer.url(TEST_PATH).toString(), null, TEST_HEADERS);

        assertNotNull(
                Metrics.globalRegistry
                        .get("okhttp")
                        .tag("method", "GET")
                        .tag("status", "200")
                        .tag("host", mockWebServer.getHostName())
                        .timer());
    }

    @Test
    void testProxy() throws IOException, InterruptedException {
        String host = "example.com:123";
        HttpClient sut =
                new HttpClient.HttpClientBuilder()
                        .useProxy(true)
                        .httpProxyAddr(mockWebServer.getHostName())
                        .httpProxyPort(mockWebServer.getPort())
                        .build();

        mockWebServer.enqueue(new MockResponse().setBody(TEST_BODY));

        sut.get("http://" + host + TEST_PATH, TEST_PARAMS, TEST_HEADERS);

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("/", request.getPath());
        assertEquals("GET", request.getMethod());
        assertEquals(TEST_HEADERS.get("h1"), request.getHeader("h1"));
        assertEquals(TEST_HEADERS.get("h2"), request.getHeader("h2"));
        assertEquals(host, request.getHeader("host"));
    }

    @Test
    void testAuthentication() throws IOException, InterruptedException {
        String authHeader = "Bearer token";
        HttpClient sut =
                new HttpClient.HttpClientBuilder().authorizationSupplier(() -> authHeader).build();

        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        sut.get(mockWebServer.url(TEST_PATH).toString(), TEST_PARAMS, TEST_HEADERS);

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals(authHeader, request.getHeader("Authorization"));
    }

    @Test
    void testBuildHeaders() {
        okhttp3.Headers result = sut.buildHeaders(TEST_HEADERS);
        assertEquals(2, result.size());
        assertEquals(TEST_HEADERS.get("h1"), result.get("h1"));
        assertEquals(TEST_HEADERS.get("h2"), result.get("h2"));
    }

    @Test
    void testBuildHeadersNull() {
        okhttp3.Headers headers = sut.buildHeaders(null);
        assertEquals(0, headers.size());
    }

    @Test
    void testBuildHeadersEmpty() {
        okhttp3.Headers result = sut.buildHeaders(ImmutableMap.of());
        assertEquals(0, result.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/", TEST_PATH})
    void testBuildUrl(String path) {
        String url = mockWebServer.url(path).toString();
        okhttp3.HttpUrl httpUrl = sut.buildUrl(url, TEST_PARAMS);
        assertEquals(url + "?p1=v1&p2=v2", httpUrl.toString());
    }

    @Test
    void testBuildJsonBody() throws IOException {
        okhttp3.RequestBody jsonBody = sut.buildJsonBody(TEST_BODY);
        assertEquals("application/json; charset=utf-8", jsonBody.contentType().toString());
        assertEquals(TEST_BODY.length(), jsonBody.contentLength());
    }

    @Test
    void testBuildJsonBodyNull() throws IOException {
        okhttp3.RequestBody result = sut.buildJsonBody(null);
        assertNull(result);
    }

    @Test
    void testGetSuccess() throws IOException, InterruptedException {
        mockWebServer.enqueue(new MockResponse().setBody(TEST_BODY));

        String result = sut.get(mockWebServer.url(TEST_PATH).toString(), null, TEST_HEADERS);
        assertEquals(TEST_BODY, result);

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals(TEST_PATH, request.getPath());
        assertEquals("GET", request.getMethod());
        assertEquals(TEST_HEADERS.get("h1"), request.getHeader("h1"));
        assertEquals(TEST_HEADERS.get("h2"), request.getHeader("h2"));
    }

    @Test
    void testGetClientError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        ClientErrorException exception =
                assertThrows(
                        ClientErrorException.class,
                        () -> {
                            sut.get(mockWebServer.url(TEST_PATH).toString(), null, TEST_HEADERS);
                        });
        assertEquals(404, exception.getResponse().getStatus());
    }

    @Test
    void testGetServerError() {
        mockWebServer.setDispatcher(new ServerErrorDispatcher(500));

        ServerErrorException exception =
                assertThrows(
                        ServerErrorException.class,
                        () -> {
                            sut.get(mockWebServer.url(TEST_PATH).toString(), null, TEST_HEADERS);
                        });
        assertEquals(500, exception.getResponse().getStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = {HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.POST})
    void testRequestWithBodySuccess(String method) throws IOException, InterruptedException {
        mockWebServer.enqueue(new MockResponse().setBody(TEST_BODY));

        String result = requestWithBody(method, TEST_BODY, TEST_HEADERS);
        assertEquals(TEST_BODY, result);

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals(TEST_PATH, request.getPath());
        assertEquals(method, request.getMethod());
        assertEquals(TEST_BODY, request.getBody().readUtf8());
        assertEquals(TEST_HEADERS.get("h1"), request.getHeader("h1"));
        assertEquals(TEST_HEADERS.get("h2"), request.getHeader("h2"));
    }

    @ParameterizedTest
    @ValueSource(strings = {HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.POST})
    void testRequestWithBodyClientError(String method) {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        ClientErrorException exception =
                assertThrows(
                        ClientErrorException.class,
                        () -> requestWithBody(method, "", TEST_HEADERS));
        assertEquals(404, exception.getResponse().getStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = {HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.POST})
    void testRequestWithBodyServerError(String method) {
        mockWebServer.setDispatcher(new ServerErrorDispatcher(500));

        ServerErrorException exception =
                assertThrows(
                        ServerErrorException.class,
                        () -> requestWithBody(method, "", TEST_HEADERS));
        assertEquals(500, exception.getResponse().getStatus());
    }

    String requestWithBody(String method, String body, Map<String, String> headers)
            throws IOException {
        switch (method) {
            case HttpMethod.PUT:
                return sut.put(mockWebServer.url(TEST_PATH).toString(), body, headers);
            case HttpMethod.DELETE:
                return sut.delete(mockWebServer.url(TEST_PATH).toString(), body, headers);
            case HttpMethod.POST:
                return sut.post(mockWebServer.url(TEST_PATH).toString(), body, headers);
            default:
                break;
        }
        return null;
    }

    static class ServerErrorDispatcher extends Dispatcher {
        private int responseCode;

        ServerErrorDispatcher(int responseCode) {
            this.responseCode = responseCode;
        }

        @Override
        public MockResponse dispatch(RecordedRequest request) {
            return new MockResponse().setResponseCode(responseCode).setBody(TEST_BODY);
        }
    }
}
