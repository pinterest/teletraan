package com.pinterest.teletraan.universal.security;

import java.security.Principal;
import java.util.List;

public class EnvoyPrincipal implements Principal {

  private final String username;
  private final List<String> groups;
  private final String spiffe;

  public EnvoyPrincipal(String username, List<String> groups, String spiffeId) {
    this.username = username;
    this.groups = groups;
    this.spiffe = spiffeId;
  }

  @Override
  public String getName() {
    return username != null ? username : spiffe;
  }

  public String getUserName() {
    return username;
  }

  public String getSpiffe() {
    return spiffe;
  }

  public List<String> getGroups() {
    return groups;
  }
}
