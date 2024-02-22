package com.pinterest.teletraan.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.security.UserRoleAuthorizer;
import com.pinterest.teletraan.universal.security.BasePastisAuthorizer;
import com.pinterest.teletraan.universal.security.ServiceRoleAuthorizer;
import com.pinterest.teletraan.universal.security.bean.ServicePrincipal;
import com.pinterest.teletraan.universal.security.bean.TeletraanPrincipal;
import com.pinterest.teletraan.universal.security.bean.TeletraanPrincipalRoles;
import com.pinterest.teletraan.universal.security.bean.UserPrincipal;
import com.pinterest.teletraan.universal.security.bean.ValueBasedRole;

import io.dropwizard.auth.Authorizer;

@JsonTypeName("composite")
public class CompositeAuthorizationFactory implements AuthorizationFactory {
  private static final String DEFAULT_PASTIS_SERVICE_NAME = "teletraan_dev";

  @JsonProperty
  private String pastisServiceName = DEFAULT_PASTIS_SERVICE_NAME;

  public void setPastisServiceName(String pastisServiceName) {
    this.pastisServiceName = pastisServiceName;
  }

  public String getPastisServiceName() {
    return pastisServiceName;
  }

  @Override
  public <P extends TeletraanPrincipal> Authorizer<P> create(TeletraanServiceContext context) throws Exception {
    return new BasePastisAuthorizer<P>(pastisServiceName, context.getAuthZResourceExtractorFactory());
  }

  @Override
  public <P extends TeletraanPrincipal> Authorizer<P> create(TeletraanServiceContext context, String className)
      throws Exception {
    if (className.equals(ServiceRoleAuthorizer.class.getSimpleName())) {
      return (Authorizer<P>) new ServiceRoleAuthorizer<ValueBasedRole, TeletraanPrincipalRoles, ServicePrincipal<ValueBasedRole>>(
          context.getAuthZResourceExtractorFactory(), TeletraanPrincipalRoles.class);
    } else if (className.equals(UserRoleAuthorizer.class.getSimpleName())) {
      return (Authorizer<P>) new UserRoleAuthorizer<UserPrincipal>(context, context.getAuthZResourceExtractorFactory());
    }
    return create(context);
  }
}