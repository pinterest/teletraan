package com.pinterest.deployservice.events;

import com.pinterest.deployservice.bean.BuildBean;
import com.pinterest.teletraan.universal.events.AppEventListener;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

public class BuildEventListener implements AppEventListener<BuildEvent> {

  public static final String TELETRAAN_SOURCE = "teletraan.build";
  public static final String DETAIL_TYPE = "Teletraan Build Action";

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final EventBridgeAsyncClient eventBridgeAsyncClient;
  private final String eventBusName;
  private static final Logger logger = LoggerFactory.getLogger(BuildEventListener.class);

  public BuildEventListener(EventBridgeAsyncClient eventBridgeAsyncClient, String eventBusName) {
    this.eventBridgeAsyncClient = eventBridgeAsyncClient;
    this.eventBusName = eventBusName;
  }

  @Override
  public void onEvent(BuildEvent event) {
    PutEventsRequestEntry entry = PutEventsRequestEntry.builder()
        .eventBusName(eventBusName)
        .source(TELETRAAN_SOURCE)
        .detail(buildEventDetailJson(event.getBuildBean(), event.getAction()))
        .detailType(DETAIL_TYPE)
        .build();

    try {
      eventBridgeAsyncClient.putEvents(e -> e.entries(entry).build()).whenCompleteAsync((response, err) -> {
        if (err != null) {
          logger.error("Failed to publish event to Event Bridge: {}", entry, err);
        }
      });
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

  @Override
  public Class<BuildEvent> getSupportedEventType() {
    return BuildEvent.class;
  }
}
