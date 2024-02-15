package com.pinterest.teletraan.universal.security;

import java.util.Collections;

public class AnonymousUser extends UserPrincipal {
  public AnonymousUser() {
    super("Anonymous", Collections.emptyList());
  }
}
