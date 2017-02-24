package com.pinterest.teletraan.config;


import com.fasterxml.jackson.annotation.JsonProperty;

public class JenkinsFactory {
  @JsonProperty
  private String jenkinsUrl;

  @JsonProperty
  private String remoteToken;

  public String getJenkinsUrl() {
    return jenkinsUrl;
  }

  public void setJenkinsUrl(String jenkinsUrl) {
    this.jenkinsUrl = jenkinsUrl;
  }

  public String getRemoteToken() {
    return remoteToken;
  }

  public void setRemoteToken(String remoteToken) {
    this.remoteToken = remoteToken;
  }
}
