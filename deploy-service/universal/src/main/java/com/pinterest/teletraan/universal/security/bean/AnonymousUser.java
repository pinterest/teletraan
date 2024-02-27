/**
 * Copyright (c) 2024, Pinterest Inc. All rights reserved.
 */
package com.pinterest.teletraan.universal.security.bean;

import java.util.Collections;

public class AnonymousUser extends UserPrincipal {
  public AnonymousUser() {
    super("Anonymous", Collections.emptyList());
  }
}
