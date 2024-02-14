package com.pinterest.teletraan.universal.security;

import java.util.Arrays;

public class EnvoyServicePrincipal extends EnvoyPrincipal {
  public EnvoyServicePrincipal(String spiffeId) {
    // TODO: remove the hard-coded group "engineering" once we migrate the
    // IAM role and security zone validations. This is added so the principal can
    // pass them.
    super(spiffeId, Arrays.asList("engineering"), spiffeId);
  }
}
