/**
 * Copyright (c) 2024, Pinterest Inc. All rights reserved.
 */
package com.pinterest.teletraan.universal.security;

import com.pinterest.teletraan.universal.security.bean.AnonymousUser;
import java.io.IOException;
import java.security.Principal;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AnonymousAuthFilter implements ContainerRequestFilter {
  private static final AnonymousUser user = new AnonymousUser();
  private SecurityContext securityContext;

  public AnonymousAuthFilter() {
    securityContext =
        new SecurityContext() {

          @Override
          public Principal getUserPrincipal() {
            return AnonymousAuthFilter.user;
          }

          @Override
          public boolean isUserInRole(String s) {
            return true;
          }

          @Override
          public boolean isSecure() {
            return true;
          }

          @Override
          public String getAuthenticationScheme() {
            return "Anonymous";
          }
        };
  }

  @Override
  public void filter(ContainerRequestContext containerRequestContext) throws IOException {
    containerRequestContext.setSecurityContext(securityContext);
  }
}
