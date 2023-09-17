package com.pinterest.deployservice.events;

import com.pinterest.deployservice.bean.BuildBean;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

public class EventBridgePublisher implements BuildEventPublisher {

  public static final String TELETRAAN_SOURCE = "teletraan.build";
  public static final String DETAIL_TYPE = "Teletraan Build Action";

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final EventBridgeClient eventBridgeClient;
  private final String eventBusName;
  private static final Logger logger = LoggerFactory.getLogger(EventBridgePublisher.class);

  public EventBridgePublisher(EventBridgeClient eventBridgeClient, String eventBusName) {
    this.eventBridgeClient = eventBridgeClient;
    this.eventBusName = eventBusName;
  }

  @Override
  public void publish(BuildBean buildBean, String action) {
    PutEventsRequestEntry entry = PutEventsRequestEntry.builder()
        .eventBusName(eventBusName)
        .source(TELETRAAN_SOURCE)
        .detail(buildEventDetailJson(buildBean, action))
        .detailType(DETAIL_TYPE)
        .build();

    try {
      eventBridgeClient.putEvents(PutEventsRequest.builder().entries(entry).build());
      logger.info("Published build event {}", entry);
    } catch (Exception e) {
      logger.error("Failed to publish event to Event Bridge: {}", entry, e);
    }
  }

  private String buildEventDetailJson(BuildBean buildBean, String action) {
    JsonNode buildBeanJsonNode = objectMapper.valueToTree(buildBean);
    ((ObjectNode) buildBeanJsonNode).put("action-type", action);
    return buildBeanJsonNode.toString();
  }
}
