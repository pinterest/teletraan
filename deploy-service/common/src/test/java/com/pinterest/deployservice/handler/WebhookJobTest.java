/**
 * Copyright (c) 2024-2025 Pinterest, Inc.
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
package com.pinterest.deployservice.handler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.pinterest.deployservice.bean.DeployBean;
import com.pinterest.deployservice.bean.DeployState;
import com.pinterest.deployservice.bean.DeployType;
import com.pinterest.deployservice.bean.WebHookBean;
import java.util.List;
import java.util.Map;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WebhookJobTest {
    private static final String TEST_PATH = "/testPath?testKey1=testVal1&testKey2=testVal2";
    private static final String TEST_DEPLOY_ID = "testDeployId";
    private static final DeployState TEST_DEPLOY_STATE = DeployState.SUCCEEDING;
    private static final DeployType TEST_DEPLOY_TYPE = DeployType.REGULAR;
    private static final Long TEST_DEPLOY_START_DATE = 5L;
    private static final TypeReference<Map<String, String>> STRING_MAP_TYPE =
            new TypeReference<Map<String, String>>() {};
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static MockWebServer mockWebServer;

    private WebhookJob sut;

    @BeforeEach
    public void setUpEach() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Create test DeployBean
        DeployBean deployBean = new DeployBean();
        deployBean.setDeploy_id(TEST_DEPLOY_ID);
        deployBean.setState(TEST_DEPLOY_STATE);
        deployBean.setStart_date(TEST_DEPLOY_START_DATE);
        deployBean.setDeploy_type(TEST_DEPLOY_TYPE);

        // Create test WebHookBean
        WebHookBean webhook = new WebHookBean();
        webhook.setMethod("PUT");
        webhook.setUrl(mockWebServer.url(TEST_PATH).toString());
        webhook.setHeaders("Host:example.com; Accept: text/plain");
        webhook.setVersion("1.1");

        Map<String, String> bodyMap =
                ImmutableMap.of(
                        "deployId", "$TELETRAAN_DEPLOY_ID",
                        "deployStart", "$TELETRAAN_DEPLOY_START",
                        "deployState", "$TELETRAAN_NUMERIC_DEPLOY_STATE",
                        "deployType", "$TELETRAAN_DEPLOY_TYPE");
        webhook.setBody(objectMapper.writeValueAsString(bodyMap));

        List<WebHookBean> webhooks = ImmutableList.of(webhook);
        sut = new WebhookJob(webhooks, deployBean);
    }

    @AfterEach
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void testWebhookOk() throws Exception {
        mockWebServer.enqueue(new MockResponse());

        assertDoesNotThrow(
                () -> {
                    sut.call();
                });

        assertEquals(1, mockWebServer.getRequestCount());
        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals(TEST_PATH, request.getPath());
        assertEquals(ImmutableList.of("example.com"), request.getHeaders().values("Host"));
        assertEquals(ImmutableList.of("text/plain"), request.getHeaders().values("Accept"));
        Map<String, String> requestBodyMap =
                objectMapper.readValue(request.getBody().readUtf8(), STRING_MAP_TYPE);
        assertEquals(4, requestBodyMap.size());
        assertEquals(String.valueOf(TEST_DEPLOY_ID), requestBodyMap.get("deployId"));
        assertEquals(String.valueOf(TEST_DEPLOY_START_DATE), requestBodyMap.get("deployStart"));
        assertEquals(
                String.valueOf(TEST_DEPLOY_STATE.ordinal()), requestBodyMap.get("deployState"));
        assertEquals(String.valueOf(TEST_DEPLOY_TYPE), requestBodyMap.get("deployType"));
    }

    @Test
    void testWebhookClientError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        assertDoesNotThrow(
                () -> {
                    sut.call();
                });

        assertEquals(1, mockWebServer.getRequestCount());
    }

    @Test
    void testWebhookServerError() {
        mockWebServer.setDispatcher(new ServerErrorDispatcher());

        assertDoesNotThrow(
                () -> {
                    sut.call();
                });
        assertEquals(3, mockWebServer.getRequestCount());
    }

    static class ServerErrorDispatcher extends Dispatcher {
        @Override
        public MockResponse dispatch(RecordedRequest request) {
            return new MockResponse().setResponseCode(500);
        }
    }
}
