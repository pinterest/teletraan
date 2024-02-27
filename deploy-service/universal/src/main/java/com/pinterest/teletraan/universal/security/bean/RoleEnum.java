/**
 * Copyright (c) 2024, Pinterest Inc. All rights reserved.
 */
package com.pinterest.teletraan.universal.security.bean;

public interface RoleEnum<R extends Role<R>> {
  R getRole();
}
