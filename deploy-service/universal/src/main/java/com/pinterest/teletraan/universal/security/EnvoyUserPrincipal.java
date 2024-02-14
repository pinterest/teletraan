package com.pinterest.teletraan.universal.security;

import java.util.List;

public class EnvoyUserPrincipal extends EnvoyPrincipal {
  public EnvoyUserPrincipal(String username, List<String> groups) {
    super(username, groups, null);
  }
}
