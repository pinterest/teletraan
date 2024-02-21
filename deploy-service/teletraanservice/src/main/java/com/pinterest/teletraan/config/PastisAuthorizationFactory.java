package com.pinterest.teletraan.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.pinterest.teletraan.universal.security.TeletraanPastisAuthorizer;
import com.pinterest.teletraan.universal.security.bean.TeletraanPrincipal;

import io.dropwizard.auth.Authorizer;

@JsonTypeName("pastis")
public class PastisAuthorizationFactory implements AuthorizationFactory {
  private static final String DEFAULT_PASTIS_SERVICE_NAME = "rodimus_dev";

  @JsonProperty
  private String pastisServiceName = DEFAULT_PASTIS_SERVICE_NAME;

  public void setPastisServiceName(String pastisServiceName) {
    this.pastisServiceName = pastisServiceName;
  }

  public String getPastisServiceName() {
    return pastisServiceName;
  }

  @Override
  public <P extends TeletraanPrincipal> Authorizer<P> create() throws Exception {
    return new TeletraanPastisAuthorizer<P>(pastisServiceName);
  }
}
