/**
 * Copyright (c) 2024, Pinterest Inc. All rights reserved.
 */
package com.pinterest.teletraan.universal.security;

import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import com.pinterest.teletraan.universal.security.bean.TeletraanPrincipal;
import io.dropwizard.auth.Authorizer;
import javax.annotation.Nullable;
import javax.ws.rs.container.ContainerRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseAuthorizer<P extends TeletraanPrincipal> implements Authorizer<P> {
  private static final Logger LOG = LoggerFactory.getLogger(BaseAuthorizer.class);
  protected final AuthZResourceExtractor.Factory extractorFactory;

  protected BaseAuthorizer() {
    extractorFactory = null;
  }

  protected BaseAuthorizer(AuthZResourceExtractor.Factory factory) {
    extractorFactory = factory;
  }

  @Override
  public boolean authorize(P principal, String role) {
    throw new UnsupportedOperationException(
        "ContainerRequestContext is required for authorization");
  }

  @Override
  public boolean authorize(P principal, String role, @Nullable ContainerRequestContext context) {
    LOG.debug("Authorizing...");

    if (context == null) {
      LOG.warn("ContainerRequestContext is required for authorization");
      return false;
    }

    Object authZInfo = context.getProperty(ResourceAuthZInfo.class.getName());
    if (authZInfo == null) {
      LOG.warn("ResourceAuthZInfo is required for authorization");
      return false;
    }

    if (!(authZInfo instanceof ResourceAuthZInfo)) {
      LOG.warn("authZInfo type not supported");
      return false;
    }

    ResourceAuthZInfo safeAuthZInfo = (ResourceAuthZInfo) authZInfo;

    AuthZResource requestedResource;
    try {
      requestedResource =
          extractorFactory
              .create(safeAuthZInfo)
              .extractResource(context, safeAuthZInfo.beanClass());
    } catch (Exception ex) {
      LOG.warn("Failed to extract resource", ex);
      return false;
    }

    return authorize(principal, role, requestedResource, context);
  }

  public abstract boolean authorize(
      P principal,
      String role,
      AuthZResource requestedResource,
      @Nullable ContainerRequestContext context);
}
