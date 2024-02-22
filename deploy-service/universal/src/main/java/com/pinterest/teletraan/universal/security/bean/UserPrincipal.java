package com.pinterest.teletraan.universal.security.bean;

import java.util.List;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Builder
@Data
@RequiredArgsConstructor
public class UserPrincipal implements TeletraanPrincipal {
  private final String name;
  private final List<String> groups;


  @Deprecated
  public String getUser() {
    return name;
  }
}
