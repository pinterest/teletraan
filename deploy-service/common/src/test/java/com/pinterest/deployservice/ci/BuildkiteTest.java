/**
 * Copyright (c) 2025 Pinterest, Inc.
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.pinterest.deployservice.common.KeyReader;
import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class BuildkiteTest {

    private Buildkite buildkite;
    private KeyReader mockKeyReader;
    private OkHttpClient httpClient;
    private static MockWebServer mockWebServer;
    private static final String BUILDKITE_PORTAL_BASE_URL =
            "https://portal.buildkite.com/organizations/test/portals/";
    private static final String BUILDKITE_API_BASE_URL =
            "https://api.buildkite.com/v2/organizations/test/";
    private static final String BUILDKITE_URL = "https://buildkite.com/test/";

    @BeforeEach
    public void setUp() throws Exception {
        mockKeyReader = mock(KeyReader.class);
        buildkite =
                new Buildkite(BUILDKITE_PORTAL_BASE_URL, BUILDKITE_API_BASE_URL, "buildkite", 1);
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        httpClient = new OkHttpClient();
    }

    @AfterEach
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    public void testStartBuildOk() throws IOException, InterruptedException {
        String pipelineName = "test-pipeline";
        String buildParams = "key1=value1&key2=value2";
        String expectedUUID = "test-uuid";
        String responseBody =
                "{\"data\":{\"buildCreate\":{\"build\":{\"number\": \"123\", \"url\":"
                        + BUILDKITE_URL
                        + "\""
                        + pipelineName
                        + "/builds/123\" ,\"uuid\":\""
                        + expectedUUID
                        + "\"}}}}";

        when(mockKeyReader.getKey()).thenReturn("mock-portal-token");

        // Mock the server response
        mockWebServer.enqueue(new MockResponse().setBody(responseBody).setResponseCode(200));
        String result = buildkite.startBuild(pipelineName, buildParams);
        // Verify the request sent to the mock server
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertTrue(recordedRequest.getPath().contains("/trigger"));
        assertTrue(recordedRequest.getBody().readUtf8().contains("commit"));
        assertTrue(recordedRequest.getBody().readUtf8().contains("branch"));
        assertTrue(recordedRequest.getBody().readUtf8().contains("message"));

        // Assert the result
        assertEquals(expectedUUID, result);
    }

    @Test
    public void testGetBuildOk() throws IOException, InterruptedException {
        String pipelineName = "test-pipeline";
        String buildUUID = "test-uuid";
        String expectedStatus = "PASSED";
        String expectedUrl = BUILDKITE_URL + pipelineName + "/builds/123";
        String responseBody =
                "{\"data\":{\"build\":{\"state\":\""
                        + expectedStatus
                        + "\",\"url\":\""
                        + expectedUrl
                        + "\"}}}";

        // Mock the server response
        mockWebServer.enqueue(new MockResponse().setBody(responseBody).setResponseCode(200));

        Buildkite.Build build = buildkite.getBuild(pipelineName, buildUUID);

        // Verify the request sent to the mock server
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertTrue(recordedRequest.getPath().contains("/query-build-generic"));
        assertTrue(recordedRequest.getBody().readUtf8().contains(buildUUID));

        // Assert the result
        assertNotNull(build);
        assertEquals(expectedStatus, build.getStatus());
        assertEquals(expectedUrl, build.getBuildUrl());
    }

    @Test
    public void testJobExist() throws Exception {
        String pipelineName = "test-pipeline";
        String responseBody = "{\"some\":\"response\"}";

        // Mock the server response
        mockWebServer.enqueue(new MockResponse().setBody(responseBody).setResponseCode(200));

        when(mockKeyReader.getKey()).thenReturn("mock-readonly-token");

        boolean result = buildkite.jobExist(pipelineName);

        // Verify the request sent to the mock server
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("GET", recordedRequest.getMethod());
        assertTrue(recordedRequest.getPath().contains("/pipelines/" + pipelineName));

        // Assert the result
        assertTrue(result);
    }

    @Test
    public void testJobNotExist() throws Exception {
        String pipelineName = "non-existent-pipeline";

        // Mock the server response with a 404 status code
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        when(mockKeyReader.getKey()).thenReturn("mock-readonly-token");

        boolean result = buildkite.jobExist(pipelineName);

        // Verify the request sent to the mock server
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("GET", recordedRequest.getMethod());
        assertTrue(recordedRequest.getPath().contains("/pipelines/" + pipelineName));

        // Assert the result
        assertFalse(result);
    }
}
