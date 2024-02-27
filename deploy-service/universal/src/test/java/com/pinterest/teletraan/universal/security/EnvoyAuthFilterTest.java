/**
 * Copyright (c) 2024, Pinterest Inc. All rights reserved.
 */
package com.pinterest.teletraan.universal.security;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.junit.jupiter.api.Test;

public class EnvoyAuthFilterTest {
  private static final String CERT_HEADER =
      "C=US\";URI=spiffe://pin220.com/k8s/jupyter/fgac-test/username/testuser;DNS=fa2a9e01-dc86-477a-9661-d6ca997556ec.k8s.pin220.com";
  private static final String SPIFFE_ID =
      "spiffe://pin220.com/k8s/jupyter/fgac-test/username/testuser";

  @Test
  public void getSpiffeId_null() {
    String spiffeId = EnvoyAuthFilter.getSpiffeId(null);
    assertNull(spiffeId);
  }

  @Test
  public void getSpiffeId_valid() {
    String spiffeId = EnvoyAuthFilter.getSpiffeId(CERT_HEADER);
    assertEquals(SPIFFE_ID, spiffeId);
  }

  @Test
  public void getSpiffeId_invalid() {
    String spiffeId = EnvoyAuthFilter.getSpiffeId("random stuff");
    assertNull(spiffeId);
  }

  @Test
  public void getGroups_space() {
    String groups = "group1 group2   group3";
    List<String> groupsList = EnvoyAuthFilter.getGroups(groups);
    assertNotNull(groupsList);
    assertEquals(3, groupsList.size());
  }

  @Test
  public void getGroups_comma() {
    String groups = "group1,group2,,group3";
    List<String> groupsList = EnvoyAuthFilter.getGroups(groups);
    assertNotNull(groupsList);
    assertEquals(3, groupsList.size());
  }

  @Test
  public void getGroups_commaAndSpace() {
    String groups = "group1, group2,,   group3";
    List<String> groupsList = EnvoyAuthFilter.getGroups(groups);
    assertNotNull(groupsList);
    assertEquals(3, groupsList.size());
  }

  @Test
  public void getGroups_null() {
    List<String> groupsList = EnvoyAuthFilter.getGroups(null);
    assertNull(groupsList);
  }
}
