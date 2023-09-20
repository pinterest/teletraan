package com.pinterest.teletraan.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;

public class AwsFactory {

  private String eventBridgeEndPoint;

  private String eventBridgeEventBusName;

  public EventBridgeAsyncClient buildEventBridgeClient() {
    return EventBridgeAsyncClient.builder().region(Region.US_EAST_1).credentialsProvider(
        ProfileCredentialsProvider.create())
        .build();
  }

  @JsonProperty
  public String getEventBridgeEndPoint() {
    return eventBridgeEndPoint;
  }

  @JsonProperty
  public void setEventBridgeEndPoint(String eventBridgeEndPoint) {
    this.eventBridgeEndPoint = eventBridgeEndPoint;
  }

  @JsonProperty
  public String getEventBridgeEventBusName() {
    return eventBridgeEventBusName;
  }

  @JsonProperty
  public void setEventBridgeEventBusName(String eventBridgeEventBusName) {
    this.eventBridgeEventBusName = eventBridgeEventBusName;
  }
}
