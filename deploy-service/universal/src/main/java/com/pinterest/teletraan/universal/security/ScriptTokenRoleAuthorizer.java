package com.pinterest.teletraan.universal.security;

import javax.annotation.Nullable;
import javax.ws.rs.container.ContainerRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pinterest.teletraan.universal.security.bean.PrincipalRoles;
import com.pinterest.teletraan.universal.security.bean.ServicePrincipal;

import io.dropwizard.auth.Authorizer;

@Deprecated
public class ScriptTokenRoleAuthorizer implements Authorizer<ServicePrincipal> {
  private static final Logger LOG = LoggerFactory.getLogger(ScriptTokenRoleAuthorizer.class);

  @Override
  public boolean authorize(
      ServicePrincipal principal, String role, @Nullable ContainerRequestContext context) {
    LOG.debug("Authorizing...");
    if (principal == null) {
      return false;
    }
    if (role.equals(PrincipalRoles.Names.DETERMINED_BY_PASTIS)) {
      // No need to authorize for DETERMINED_BY_PASTIS "role"
      return true;
    }
    String wildCardResourceID = "*";
    boolean authorized = principal.hasPermission(wildCardResourceID, role);
    LOG.debug("Authorized: {}", authorized);
    return authorized;
  }

  @Override
  public boolean authorize(ServicePrincipal principal, String role) {
    throw new UnsupportedOperationException("Unimplemented method 'authorize'");
  }
}
