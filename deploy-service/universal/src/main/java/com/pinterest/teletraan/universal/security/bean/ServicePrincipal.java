package com.pinterest.teletraan.universal.security.bean;

import com.google.common.collect.Lists;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
@RequiredArgsConstructor
public class ServicePrincipal implements TeletraanPrincipal {
  private static final Logger LOG = LoggerFactory.getLogger(ServicePrincipal.class);

  private final String name;
  @Deprecated private List<TokenRolesBean> tokenRoles;
  @Deprecated private String group;

  @Deprecated
  public ServicePrincipal(String name, List<TokenRolesBean> tokenRoles, String group) {
    this.name = name;
    this.tokenRoles = tokenRoles;
    this.group = group;
  }

  @Deprecated
  public boolean hasPermission(String resoure_id, Role requiredRole) {
    for (TokenRolesBean tokenRole : tokenRoles) {
      if (tokenRole.getResource_id().equals("*") || tokenRole.getResource_id().equals(resoure_id)) {
        Role effective = new Role(tokenRole.getRoles());
        LOG.debug(
            "Find match role setting with resource {} mask {} required role mask {}",
            tokenRole.getResource_id(),
            effective.getAccessLevel(),
            requiredRole.getAccessLevel());
        if (effective.isAuthorized(requiredRole)) {
          return true;
        }
      }
    }
    return false;
  }

  @Deprecated
  public boolean hasPermission(String resourceId, String requiredRole) {
    Role role = PrincipalRoles.valueOf(requiredRole).getRole();
    return hasPermission(resourceId, role);
  }

  @Deprecated
  public List<String> getGroups() {
    // If group isn't there, return an empty list (i.e. no privilege at all).
    if (StringUtils.isBlank(group)) {
      return Lists.newArrayList();
    } else {
      return Lists.newArrayList(group);
    }
  }
}
