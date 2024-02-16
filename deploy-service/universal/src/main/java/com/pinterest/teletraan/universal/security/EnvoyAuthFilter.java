package com.pinterest.teletraan.universal.security;

import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.Authorizer;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;

import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Priority(Priorities.AUTHENTICATION)
public class EnvoyAuthFilter<P extends Principal> extends AuthFilter<ContainerRequestContext, P> {
  private static final Logger LOG = LoggerFactory.getLogger(EnvoyAuthFilter.class);
  protected Authorizer<Principal> Authorizer;

  @Override
  public void filter(final ContainerRequestContext requestContext) throws IOException {
    if (!authenticate(requestContext)) {
      throw unauthorizedHandler.buildException(prefix, realm);
    }
  }

    /**
     * Builder for {@link EnvoyAuthFilter}.
     * <p>An {@link Authenticator} must be provided during the building process.</p>
     *
     * @param <P> the type of the principal
     */
    public static class Builder<P extends Principal>
            extends AuthFilterBuilder<ContainerRequestContext, P, EnvoyAuthFilter<P>> {

        @Override
        protected EnvoyAuthFilter<P> newInstance() {
            return new EnvoyAuthFilter<>();
        }
    }
  /**
   * Authenticates a request with headers and setup the security context.
   *
   * @param requestContext the context of the request
   * @return {@code true}, if the request is authenticated, otherwise {@code false}
   */
  protected boolean authenticate(ContainerRequestContext requestContext) {
    LOG.debug("Authenticating...");
    EnvoyPrincipal principal;
    String user = requestContext.getHeaders().getFirst(Constants.USER_HEADER);
    String spiffeId =
        getSpiffeId(requestContext.getHeaders().getFirst(Constants.CLIENT_CERT_HEADER));

    if (StringUtils.isNotBlank(spiffeId)) {
      principal = new EnvoyServicePrincipal(spiffeId);
    } else if (StringUtils.isNotBlank(user)) {
      List<String> groups =
          getGroups(requestContext.getHeaders().getFirst(Constants.GROUPS_HEADER));
      principal = new EnvoyUserPrincipal(user, groups);
    } else {
      return false;
    }

    final SecurityContext securityContext = requestContext.getSecurityContext();
    final boolean secure = securityContext != null && securityContext.isSecure();
    requestContext.setSecurityContext(
        new SecurityContext() {
          @Override
          public Principal getUserPrincipal() {
            return principal;
          }

          @Override
          public boolean isUserInRole(String role) {
            if (Authorizer != null) {
              return Authorizer.authorize(principal, role, requestContext);
            }
            return authorizer.authorize(principal, role);
          }

          @Override
          public boolean isSecure() {
            return secure;
          }

          @Override
          public String getAuthenticationScheme() {
            return user != null ? SecurityContext.BASIC_AUTH : SecurityContext.CLIENT_CERT_AUTH;
          }
        });
    return true;
  }

  /**
   * Parses the raw value of a spiffe request header
   *
   * @return spiffe id
   */
  protected static String getSpiffeId(String value) {
    if (value == null) {
      return null;
    }
    String[] headerValues = value.split(",");
    String lastHeaderValue = headerValues[headerValues.length - 1];
    String[] pairs = lastHeaderValue.split(";");
    for (String pair : pairs) {
      String[] pairKeyAndValue = pair.split("=", 2);
      if (pairKeyAndValue[0].equals("URI")) {
        return pairKeyAndValue[1];
      }
    }
    return null;
  }

  @Nullable
  protected static List<String> getGroups(String header) {
    if (header == null) {
      return null;
    }
    return Arrays.asList(header.split("[\\s,]+"));
  }
}
