package com.pinterest.teletraan.universal.security.bean;

import com.google.common.collect.Lists;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
@Data
@RequiredArgsConstructor
public class ServicePrincipal implements TeletraanPrincipal {

  private final String name;
  @Deprecated private String group;
  @Deprecated private PrincipalRoles role;
  @Deprecated private AuthZResource resource;

  @Deprecated
  public ServicePrincipal(String name, PrincipalRoles role, AuthZResource resource) {
    this.name = name;
    this.role = role;
    this.resource = resource;
  }

  @Deprecated
  public List<String> getGroups() {
    if (StringUtils.isBlank(group)) {
      return Lists.newArrayList();
    } else {
      return Lists.newArrayList(group);
    }
  }
}
