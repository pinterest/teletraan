package com.pinterest.teletraan.universal.security.bean;

import lombok.Data;

@Data
public class Resource {
  private String name;
  private String accountId;
  private Type type;

  public enum Type {
    Environment,
    EnvStage,
    Group,
    System,
    Placement,
    BaseImage,
    SecurityZone,
    IamRole
  }
}
