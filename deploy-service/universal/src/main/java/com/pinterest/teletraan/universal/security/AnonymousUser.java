package com.pinterest.teletraan.universal.security;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

public class AnonymousUser implements Principal {
  private String user;
  private List<String> groups;

  public AnonymousUser() {
    this.user = "Anonymous";
    this.groups = Collections.emptyList();
  }

  @Override
  public String getName() {
    return user;
  }

  public String getUser() {
    return user;
  }

  public List<String> getGroups() {
    return groups;
  }
}
