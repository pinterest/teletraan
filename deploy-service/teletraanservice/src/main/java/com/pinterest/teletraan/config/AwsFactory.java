package com.pinterest.teletraan.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;

public class AwsFactory {

  private String eventBridgeEndPoint;

  private String eventBridgeEventBusName;

  @JsonProperty
  private List<String> accountAllowList;
  
  public EventBridgeAsyncClient buildEventBridgeClient() {
    return EventBridgeAsyncClient.builder().region(Region.US_EAST_1).build();
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

  public List<String> getAccountAllowList() {
    return this.accountAllowList;
  }

  public void setAccountAllowList(List<String> accountAllowList) {
    this.accountAllowList = accountAllowList;
  }
}
