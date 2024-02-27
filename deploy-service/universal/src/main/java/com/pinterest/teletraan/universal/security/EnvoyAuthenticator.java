/**
 * Copyright (c) 2024, Pinterest Inc. All rights reserved.
 */
package com.pinterest.teletraan.universal.security;

import com.pinterest.teletraan.universal.security.bean.EnvoyCredentials;
import com.pinterest.teletraan.universal.security.bean.ServicePrincipal;
import com.pinterest.teletraan.universal.security.bean.TeletraanPrincipal;
import com.pinterest.teletraan.universal.security.bean.UserPrincipal;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

public class EnvoyAuthenticator implements Authenticator<EnvoyCredentials, TeletraanPrincipal> {

  @Override
  public Optional<TeletraanPrincipal> authenticate(EnvoyCredentials credentials)
      throws AuthenticationException {
    if (StringUtils.isNotBlank(credentials.getUser())) {
      return Optional.of(new UserPrincipal(credentials.getUser(), credentials.getGroups()));
    }
    if (StringUtils.isNotBlank(credentials.getSpiffeId())) {
      return Optional.of(new ServicePrincipal<>(credentials.getSpiffeId()));
    }
    return Optional.empty();
  }
}
