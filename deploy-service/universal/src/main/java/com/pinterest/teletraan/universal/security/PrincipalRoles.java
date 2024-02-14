package com.pinterest.teletraan.universal.security;

import com.pinterest.rodimus.security.bean.Role;

public enum PrincipalRoles {
  ADMIN(0xFFFFFFFFL),
  READER(0x1L),
  OPERATOR(0x3L);

  public class Names {
    public static final String ADMIN = "ADMIN";
    public static final String READER = "READER";
    public static final String OPERATOR = "OPERATOR";
    public static final String DETERMINED_BY_PASTIS = "DETERMINED_BY_PASTIS";
  }

  private Role role;

  PrincipalRoles(long mask) {
    this.role = new Role(mask);
  }

  public Role getRole() {
    return role;
  }

  public String getName() {
    return role.getName();
  }
}
