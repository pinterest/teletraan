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
package com.pinterest.deployservice.events;

import static com.pinterest.deployservice.events.EventBridgePublisher.DETAIL_TYPE;
import static com.pinterest.deployservice.events.EventBridgePublisher.TELETRAAN_SOURCE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pinterest.deployservice.bean.BuildBean;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

public class EventBridgePublisherTest {

    private static final long COMMIT_DATE = Instant.now().toEpochMilli();
    private static final long PUBLISH_DATE = Instant.now().toEpochMilli();

    private static final String BUILD_ID = "buildId";
    private static final String BUILD_NAME = "buildName";
    private static final String BUILD_PUBLISHER = "buildPublisher";
    private static final String SCM = "scm";
    private static final String PUBLISHER_INFO = "publisherInfo";
    private static final String COMMIT_SHA = "commitSha";
    private static final String SHORT_COMMIT_SHA = "shortCommitSha";
    private static final String BRANCH = "branch";
    private static final String ARTIFACT_URL = "artifactUrl";
    private static final String SCM_INFO = "scmInfo";
    private static final String SCM_REPO = "scmRepo";
    private static final String ACTION = "action";
    private static final String EVENT_BUS_NAME = "eventBusName";

    private final EventBridgeAsyncClient eventBridgeAsyncClient =
            mock(EventBridgeAsyncClient.class);
    private final EventBridgePublisher eventBridgePublisher =
            new EventBridgePublisher(eventBridgeAsyncClient, EVENT_BUS_NAME);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void publish() throws JsonProcessingException {
        ArgumentCaptor<Consumer> putEventsRequestArgumentCaptor =
                ArgumentCaptor.forClass(Consumer.class);
        when(eventBridgeAsyncClient.putEvents(any(Consumer.class)))
                .thenReturn(mock(CompletableFuture.class));
        BuildBean buildBean = generateBuild();

        eventBridgePublisher.publish(buildBean, ACTION);

        Mockito.verify(eventBridgeAsyncClient).putEvents(putEventsRequestArgumentCaptor.capture());

        PutEventsRequestEntry entry =
                PutEventsRequestEntry.builder()
                        .eventBusName(EVENT_BUS_NAME)
                        .source(TELETRAAN_SOURCE)
                        .detail(buildEventDetailJson(buildBean))
                        .detailType(DETAIL_TYPE)
                        .build();

        PutEventsRequest expectedRequest = PutEventsRequest.builder().entries(entry).build();

        PutEventsRequest.Builder builder = PutEventsRequest.builder();
        putEventsRequestArgumentCaptor.getValue().accept(builder);
        PutEventsRequest actualRequest = builder.build();

        Assert.assertEquals(expectedRequest, actualRequest);

        String detailJson = actualRequest.entries().get(0).detail();
        JsonNode jsonNode = objectMapper.readTree(detailJson);
        Assert.assertEquals(ACTION, jsonNode.get("action-type").textValue());
    }

    private String buildEventDetailJson(BuildBean buildBean) {
        JsonNode buildBeanJsonNode = objectMapper.valueToTree(buildBean);
        ((ObjectNode) buildBeanJsonNode).put("action-type", ACTION);
        return buildBeanJsonNode.toString();
    }

    private BuildBean generateBuild() {
        BuildBean buildBean = new BuildBean();
        buildBean.setBuild_id(BUILD_ID);
        buildBean.setBuild_name(BUILD_NAME);
        buildBean.setPublisher(BUILD_PUBLISHER);
        buildBean.setScm(SCM);
        buildBean.setPublish_info(PUBLISHER_INFO);
        buildBean.setCommit_date(COMMIT_DATE);
        buildBean.setScm_commit(COMMIT_SHA);
        buildBean.setScm_commit_7(SHORT_COMMIT_SHA);
        buildBean.setScm_branch(BRANCH);
        buildBean.setPublish_date(PUBLISH_DATE);
        buildBean.setArtifact_url(ARTIFACT_URL);
        buildBean.setScm_info(SCM_INFO);
        buildBean.setScm_repo(SCM_REPO);

        return buildBean;
    }
}
