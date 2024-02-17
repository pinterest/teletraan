package com.pinterest.teletraan.universal.security.bean;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class UserPrincipal implements TeletraanPrincipal {
  private final String name;
  private final List<String> groups;
  @Deprecated private TokenRolesBean tokenRolesBean;

  @Deprecated
  public UserPrincipal(String user, TokenRolesBean tokenRolesBean, List<String> groups) {
    this.name = user;
    this.tokenRolesBean = tokenRolesBean;
    this.groups = groups;
  }

  @Deprecated
  public String getUser() {
    return name;
  }
}
