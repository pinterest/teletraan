/**
 * Copyright (c) 2024, Pinterest Inc. All rights reserved.
 */
package com.pinterest.deployservice.bean;

import com.pinterest.teletraan.universal.security.bean.RoleEnum;
import com.pinterest.teletraan.universal.security.bean.ValueBasedRole;


/**
 * READER:
 *      Default role, everyone who is able to use Teletraan has READER access.
 * PINGER:
 *      Role required to ping server.
 * PUBLISHER:
 *      Role required to publish artifacts.
 * OPERATOR:
 *      Role where user can modify a specific environment's config and
 *      perform deploy related actions.
 * ADMIN:
 *      Role that has the same environment specific privileges as OPERATOR
 *      plus the ability specify new OPERATORS and ADMINs for said environment.
 *      When a new environment is created the creating user is the designated the
 *      first ADMIN.
 */
public enum TeletraanPrincipalRole implements RoleEnum<ValueBasedRole> {
  READ(-1),
  READER(0), // legacy
  PINGER(1), // legacy
  PUBLISHER(1), // legacy
  EXECUTE(9),
  WRITE(9),
  DELETE(9),
  OPERATOR(10), // legacy
  ADMIN(20);

  public class Names {
    private Names() {}
    public static final String PINGER = "PINGER";
    public static final String PUBLISHER = "PUBLISHER";
    public static final String READER = "READER";
    public static final String OPERATOR = "OPERATOR";
    public static final String ADMIN = "ADMIN";

    public static final String READ = "READ";
    public static final String WRITE = "WRITE";
    public static final String EXECUTE = "EXECUTE";
    public static final String DELETE = "DELETE";
  }

  private final ValueBasedRole role;

  TeletraanPrincipalRole(int value) {
    this.role = new ValueBasedRole(value);
  }

  public ValueBasedRole getRole() {
    return role;
  }

  public boolean isEqualOrSuperior(TeletraanPrincipalRole otherRole) {
    return this.role.isEqualOrSuperior(otherRole.getRole());
  }
}
