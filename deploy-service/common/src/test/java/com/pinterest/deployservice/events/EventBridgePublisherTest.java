package com.pinterest.deployservice.events;

import static com.pinterest.deployservice.events.EventBridgePublisher.DETAIL_TYPE;
import static com.pinterest.deployservice.events.EventBridgePublisher.TELETRAAN_SOURCE;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pinterest.deployservice.bean.BuildBean;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

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

  private final EventBridgeAsyncClient eventBridgeAsyncClient = mock(EventBridgeAsyncClient.class);
  private final EventBridgePublisher eventBridgePublisher = new EventBridgePublisher(eventBridgeAsyncClient, EVENT_BUS_NAME);
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void publish() throws JsonProcessingException {
    ArgumentCaptor<Consumer> putEventsRequestArgumentCaptor = ArgumentCaptor.forClass(Consumer.class);
    when(eventBridgeAsyncClient.putEvents(any(Consumer.class))).thenReturn(mock(CompletableFuture.class));
    BuildBean buildBean = generateBuild();

    eventBridgePublisher.publish(buildBean, ACTION);

    Mockito.verify(eventBridgeAsyncClient).putEvents(putEventsRequestArgumentCaptor.capture());

    PutEventsRequestEntry entry = PutEventsRequestEntry.builder()
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
