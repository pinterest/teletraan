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
package com.pinterest.deployservice.ci;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JenkinsTest {
    private static final String TEST_JOB = "testJob";
    private static final String TEST_PARAMS = "testKey1=testValue1&testKey2=testValue2";
    private static final String TEST_JENKINS_HOST = "example.com";
    private static final String TEST_JENKINS_URL =
            String.format("http://%s/testUrl", TEST_JENKINS_HOST);
    private static final String TEST_REMOTE_TOKEN = "testRemoteToken";
    private static final String TEST_BUILD_NUMBER = "testBuildNumber";
    private static final String TEST_BUILD_RESULT = "testBuildResult";
    private static final boolean TEST_BUILD_BUILDING = true;
    private static final long TEST_BUILD_TIMESTAMP = 7;
    private static final int TEST_BUILD_ESTIMATED_DURATION = 8;
    private static final int TEST_BUILD_DURATION = 9;

    private static MockWebServer mockWebServer;
    private Jenkins sut;

    @BeforeEach
    public void setUpEach() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        sut =
                new Jenkins(
                        TEST_JENKINS_URL,
                        TEST_REMOTE_TOKEN,
                        true,
                        mockWebServer.getHostName(),
                        String.valueOf(mockWebServer.getPort()),
                        "Jenkins",
                        2);
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
                    new Jenkins(
                            TEST_JENKINS_URL,
                            TEST_REMOTE_TOKEN,
                            true,
                            "localhost",
                            "invalidPort",
                            "Jenkins",
                            2);
                });
    }

    @Test
    void testStartBuildOk() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse());

        assertDoesNotThrow(
                () -> {
                    sut.startBuild(TEST_JOB, TEST_PARAMS);
                });

        assertEquals(1, mockWebServer.getRequestCount());
        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals(
                String.format(
                        "GET %s/job/%s/buildWithParameters?token=%s&%s HTTP/1.1",
                        TEST_JENKINS_URL, TEST_JOB, TEST_REMOTE_TOKEN, TEST_PARAMS),
                request.getRequestLine());
        assertEquals(ImmutableList.of(TEST_JENKINS_HOST), request.getHeaders().values("Host"));
    }

    @Test
    void testStartBuildClientError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        ClientErrorException exception =
                assertThrows(
                        ClientErrorException.class,
                        () -> {
                            sut.startBuild(TEST_JOB, TEST_PARAMS);
                        });
        assertEquals(404, exception.getResponse().getStatus());
        assertEquals(1, mockWebServer.getRequestCount());
    }

    @Test
    void testStartBuildServerError() {
        mockWebServer.setDispatcher(new ServerErrorDispatcher());
        ServerErrorException exception =
                assertThrows(
                        ServerErrorException.class,
                        () -> {
                            sut.startBuild(TEST_JOB, TEST_PARAMS);
                        });
        assertEquals(500, exception.getResponse().getStatus());
        assertEquals(3, mockWebServer.getRequestCount());
    }

    @Test
    void testGetBuildOk() throws Exception {
        JsonObject mockResponseBody = new JsonObject();
        mockResponseBody.addProperty("number", TEST_BUILD_NUMBER);
        mockResponseBody.addProperty("result", TEST_BUILD_RESULT);
        mockResponseBody.addProperty("building", TEST_BUILD_BUILDING);
        mockResponseBody.addProperty("timestamp", TEST_BUILD_TIMESTAMP);
        mockResponseBody.addProperty("estimatedDuration", TEST_BUILD_ESTIMATED_DURATION);
        mockResponseBody.addProperty("duration", TEST_BUILD_DURATION);
        mockWebServer.enqueue(new MockResponse().setBody(mockResponseBody.toString()));

        Jenkins.Build build = sut.getBuild(TEST_JOB, TEST_BUILD_NUMBER);
        assertEquals('"' + TEST_BUILD_NUMBER + '"', build.buildId);
        assertEquals('"' + TEST_BUILD_RESULT + '"', build.result);
        assertEquals(TEST_BUILD_BUILDING, build.isBuilding);
        assertEquals(TEST_BUILD_TIMESTAMP, build.startTimestamp);
        assertEquals(TEST_BUILD_ESTIMATED_DURATION, build.estimateDuration);
        assertEquals(TEST_BUILD_DURATION, build.duration);

        assertEquals(1, mockWebServer.getRequestCount());
        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals(
                String.format(
                        "GET %s/job/%s/%s/api/json HTTP/1.1",
                        TEST_JENKINS_URL, TEST_JOB, TEST_BUILD_NUMBER),
                request.getRequestLine());
        assertEquals(ImmutableList.of(TEST_JENKINS_HOST), request.getHeaders().values("Host"));
    }

    @Test
    void testGetBuildClientError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        ClientErrorException exception =
                assertThrows(
                        ClientErrorException.class,
                        () -> {
                            sut.getBuild(TEST_JOB, TEST_BUILD_NUMBER);
                        });
        assertEquals(404, exception.getResponse().getStatus());
        assertEquals(1, mockWebServer.getRequestCount());
    }

    @Test
    void testGetBuildServerError() {
        mockWebServer.setDispatcher(new ServerErrorDispatcher());
        ServerErrorException exception =
                assertThrows(
                        ServerErrorException.class,
                        () -> {
                            sut.getBuild(TEST_JOB, TEST_BUILD_NUMBER);
                        });
        assertEquals(500, exception.getResponse().getStatus());
        assertEquals(3, mockWebServer.getRequestCount());
    }

    static class ServerErrorDispatcher extends Dispatcher {
        @Override
        public MockResponse dispatch(RecordedRequest request) {
            return new MockResponse().setResponseCode(500);
        }
    }
}
