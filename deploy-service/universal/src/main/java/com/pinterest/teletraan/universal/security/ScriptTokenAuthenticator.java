package com.pinterest.teletraan.universal.security;

import com.pinterest.rodimus.security.providers.MySqlScriptTokenProvider;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class ScriptTokenAuthenticator implements Authenticator<String, ScriptTokenPrincipal> {
  private static final Logger LOG = LoggerFactory.getLogger(ScriptTokenAuthenticator.class);

  private MySqlScriptTokenProvider tokenProvider;

  public ScriptTokenAuthenticator(MySqlScriptTokenProvider tokenProvider) {
    this.tokenProvider = tokenProvider;
  }

  @Override
  public Optional<ScriptTokenPrincipal> authenticate(String credentials)
      throws AuthenticationException {
    LOG.debug("Authenticating...");
    return Optional.ofNullable(tokenProvider.getScriptTokenPrincipal(credentials));
  }
}
