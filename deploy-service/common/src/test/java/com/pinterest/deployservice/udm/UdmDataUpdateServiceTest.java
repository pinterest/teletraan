/**
 * Copyright (c) 2026 Pinterest, Inc.
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
package com.pinterest.deployservice.udm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.fixture.EnvironBeanFixture;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UdmDataUpdateServiceTest {
    private static final String TEST_UDMDU_HOST = "http://example.com";

    private MockWebServer mockWebServer;
    private UdmDataUpdateService udmduService;

    @BeforeEach
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        udmduService =
                new UdmDataUpdateService(
                        TEST_UDMDU_HOST,
                        mockWebServer.getHostName(),
                        String.valueOf(mockWebServer.getPort()));
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
                    new UdmDataUpdateService(
                            TEST_UDMDU_HOST, mockWebServer.getHostName(), "invalidPort");
                });
    }

    @Test
    void testNotifyStageCreatedSuccess() throws Exception {
        String mockResponseBody = "{\"message\": \"Teletraan stage is created successfully.\"}";
        mockWebServer.enqueue(new MockResponse().setBody(mockResponseBody).setResponseCode(200));

        EnvironBean environBean = getTestEnvironBean();
        udmduService.notifyStageCreated(environBean);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("PUT", recordedRequest.getMethod());
        assertTrue(
                recordedRequest.getRequestLine().contains("/v1/teletraan_stage/testEnv/testStage"));
    }

    @Test
    void testNotifyStageCreatedFailure() throws Exception {
        EnvironBean environBean = getTestEnvironBean();
        mockWebServer.setDispatcher(new ServerErrorDispatcher());

        // The service handles the error responses
        udmduService.notifyStageCreated(environBean);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("PUT", recordedRequest.getMethod());
        assertTrue(
                recordedRequest.getRequestLine().contains("/v1/teletraan_stage/testEnv/testStage"));
        assertEquals(3, mockWebServer.getRequestCount());
    }

    @Test
    void testNotifyStageDeletedSuccess() throws Exception {
        String mockResponseBody = "{\"message\": \"Teletraan stage is deleted successfully.\"}";
        mockWebServer.enqueue(new MockResponse().setBody(mockResponseBody).setResponseCode(200));

        udmduService.notifyStageDeleted("testEnv", "testStage");

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("DELETE", recordedRequest.getMethod());
        assertTrue(
                recordedRequest.getRequestLine().contains("/v1/teletraan_stage/testEnv/testStage"));
    }

    @Test
    void testNotifyStageDeletedFailure() throws Exception {
        mockWebServer.setDispatcher(new ServerErrorDispatcher());

        // The service handles the error responses
        udmduService.notifyStageDeleted("testEnv", "testStage");

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("DELETE", recordedRequest.getMethod());
        assertTrue(
                recordedRequest.getRequestLine().contains("/v1/teletraan_stage/testEnv/testStage"));
        assertEquals(3, mockWebServer.getRequestCount());
    }

    private EnvironBean getTestEnvironBean() {
        EnvironBean environBean = EnvironBeanFixture.createRandomEnvironBean();

        environBean.setEnv_name("testEnv");
        environBean.setStage_name("testStage");

        return environBean;
    }

    static class ServerErrorDispatcher extends Dispatcher {
        @Override
        public MockResponse dispatch(RecordedRequest request) {
            return new MockResponse().setResponseCode(500);
        }
    }
}
