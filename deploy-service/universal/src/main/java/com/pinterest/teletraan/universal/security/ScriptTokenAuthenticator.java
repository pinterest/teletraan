package com.pinterest.teletraan.universal.security;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pinterest.teletraan.universal.security.bean.ServicePrincipal;
import com.pinterest.teletraan.universal.security.providers.MySqlScriptTokenProvider;

@Deprecated
public class ScriptTokenAuthenticator implements Authenticator<String, ServicePrincipal> {
  private static final Logger LOG = LoggerFactory.getLogger(ScriptTokenAuthenticator.class);

  private MySqlScriptTokenProvider tokenProvider;

  public ScriptTokenAuthenticator(MySqlScriptTokenProvider tokenProvider) {
    this.tokenProvider = tokenProvider;
  }

  @Override
  public Optional<ServicePrincipal> authenticate(String credentials)
      throws AuthenticationException {
    LOG.debug("Authenticating...");
    return Optional.ofNullable(tokenProvider.getScriptTokenPrincipal(credentials));
  }
}
