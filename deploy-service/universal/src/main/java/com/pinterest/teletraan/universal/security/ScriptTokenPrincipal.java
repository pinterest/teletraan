package com.pinterest.teletraan.universal.security;

import com.google.common.collect.Lists;
import com.pinterest.rodimus.security.bean.Role;
import com.pinterest.rodimus.security.bean.TokenRolesBean;
import java.security.Principal;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A principal represents a script token. */
@Deprecated
public class ScriptTokenPrincipal implements Principal {

  private static final Logger LOG = LoggerFactory.getLogger(ScriptTokenPrincipal.class);
  private String name;
  private List<TokenRolesBean> tokenRoles;
  private String group;

  public ScriptTokenPrincipal(String name, List<TokenRolesBean> tokenRoles, String group) {
    this.name = name;
    this.tokenRoles = tokenRoles;
    this.group = group;
  }

  @Override
  public String getName() {
    return name;
  }

  public boolean hasPermission(String resoure_id, Role requiredRole) {
    for (TokenRolesBean tokenRole : tokenRoles) {
      if (tokenRole.getResource_id().equals("*") || tokenRole.getResource_id().equals(resoure_id)) {
        Role effective = new Role(tokenRole.getRoles());
        LOG.debug(
            "Find match role setting with resource {} mask {} required role mask {}",
            tokenRole.getResource_id(),
            effective.getMask(),
            requiredRole.getMask());
        if (effective.isAuthorized(requiredRole.getMask())) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean hasPermission(String resourceId, String requiredRole) {
    Role role = PrincipalRoles.valueOf(requiredRole).getRole();
    return hasPermission(resourceId, role);
  }

  public List<String> getGroups() {
    // If group isn't there, return an empty list (i.e. no privilege at all).
    if (StringUtils.isBlank(group)) {
      return Lists.newArrayList();
    } else {
      return Lists.newArrayList(group);
    }
  }
}
