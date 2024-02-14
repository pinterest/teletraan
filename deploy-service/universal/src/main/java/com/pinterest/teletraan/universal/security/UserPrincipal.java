package com.pinterest.teletraan.universal.security;

import java.security.Principal;
import java.util.List;

@Deprecated
public class UserPrincipal implements Principal {
  private String user;
  private List<String> groups;

  public UserPrincipal(String user, List<String> groups) {
    this.user = user;
    this.groups = groups;
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
