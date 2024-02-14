package com.pinterest.teletraan.universal.security;

import com.pinterest.rodimus.security.dw2port.ContextAuthorizer;
import javax.ws.rs.container.ContainerRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class ScriptTokenRoleAuthorizer implements ContextAuthorizer<ScriptTokenPrincipal> {
  private static final Logger LOG = LoggerFactory.getLogger(ScriptTokenRoleAuthorizer.class);

  @Override
  public boolean authorize(
      ScriptTokenPrincipal principal, String role, ContainerRequestContext context) {
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
}
