package com.pinterest.teletraan.universal.events;

import java.util.Map;
import java.util.stream.Stream;
import lombok.Getter;

@Getter
public class ResourceChangedEvent extends AppEvent {

  private String resource;
  private String operator;
  private String[] tags;

  public ResourceChangedEvent(Object source) {
    super(source);
  }

  public ResourceChangedEvent(
      String resource, String operator, Object source, Map<String, String> tags) {
    this(
        resource,
        operator,
        source,
        System.currentTimeMillis(),
        tags);
  }

  public ResourceChangedEvent(
      String resource, String operator, Object source, long timestamp, Map<String, String> tags) {
    this(
        resource,
        operator,
        source,
        timestamp,
        tags.entrySet().stream()
            .flatMap(e -> Stream.of(e.getKey(), e.getValue()))
            .toArray(String[]::new));
  }

  public ResourceChangedEvent(
      String resource, String operator, Object source, long timestamp, String... tags) {
    super(source, timestamp);
    this.resource = resource;
    this.operator = operator;
    this.tags = tags;

    if (tags.length % 2 != 0) {
      throw new IllegalArgumentException("Number of tags must be even.");
    }
  }
}
