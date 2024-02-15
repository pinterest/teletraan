package com.pinterest.teletraan.universal.security.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

/** Description of a script token role on a resource */
public class TokenRolesBean {

  @NotEmpty
  @JsonProperty("name")
  private String script_name;

  @JsonProperty("resource")
  private String resource_id;

  @JsonProperty("token")
  private String token;

  @JsonProperty("roles")
  private long roles;

  @JsonProperty("expireDate")
  private Long expire_date;

  @JsonProperty("groupName")
  private String group_name;

  public String getResource_id() {
    return resource_id;
  }

  public void setResource_id(String resource_id) {
    this.resource_id = resource_id;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public Long getExpire_date() {
    return expire_date;
  }

  public void setExpire_date(Long expire_date) {
    this.expire_date = expire_date;
  }

  public String getScript_name() {
    return script_name;
  }

  public void setScript_name(String script_name) {
    this.script_name = script_name;
  }

  public long getRoles() {
    return roles;
  }

  public void setRoles(long roles) {
    this.roles = roles;
  }

  public String getGroup_name() {
    return group_name;
  }

  public void setGroup_name(String group_name) {
    this.group_name = group_name;
  }
}
