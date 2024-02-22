package com.pinterest.teletraan.universal.security.bean;

/**
 * READER:
 * Default role, everyone who is able to use Teletraan has READER access.
 * PINGER:
 * Role required to ping server.
 * PUBLISHER:
 * Role required to publish artifacts.
 * OPERATOR:
 * Role where user can modify a specific environment's config and
 * perform deploy related actions.
 * ADMIN:
 * Role that has the same environment specific privileges as OPERATOR
 * plus the ability specify new OPERATORS and ADMINs for said environment.
 * When a new environment is created the creating user is the designated the
 * first ADMIN.
 */
public enum TeletraanPrincipalRoles implements RoleEnum<ValueBasedRole> {
  READER(0),
  PINGER(1),
  PUBLISHER(1),
  OPERATOR(10),
  SYSTEM_OPERATOR(15),
  ADMIN(20);

  public class Names {
    public static final String ADMIN = "ADMIN";
    public static final String READER = "READER";
    public static final String OPERATOR = "OPERATOR";
    public static final String DETERMINED_BY_PASTIS = "DETERMINED_BY_PASTIS";

    public static final String WRITE = "WRITE";
    public static final String EXECUTE = "EXECUTE";
    public static final String DELETE = "DELETE";
    public static final String SYSTEM_WRITE = "SYSTEM_WRITE";
    public static final String SYSTEM_EXECUTE = "SYSTEM_EXECUTE";
    public static final String SYSTEM_DELETE = "SYSTEM_DELETE";
  }

  private ValueBasedRole role;

  TeletraanPrincipalRoles(int value) {
    this.role = new ValueBasedRole(value);
  }

  public ValueBasedRole getRole() {
    return role;
  }

  public boolean isEqualOrSuperior(TeletraanPrincipalRoles otherRole) {
    return this.role.isEqualOrSuperior(otherRole.getRole());
  }
}
