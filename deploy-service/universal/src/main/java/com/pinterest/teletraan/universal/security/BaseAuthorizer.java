package com.pinterest.teletraan.universal.security;

import javax.annotation.Nullable;
import javax.ws.rs.container.ContainerRequestContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pinterest.teletraan.universal.security.bean.TeletraanPrincipal;

import io.dropwizard.auth.Authorizer;

public abstract class BaseAuthorizer<P extends TeletraanPrincipal> implements Authorizer<P> {
    private static final Logger LOG = LoggerFactory.getLogger(BasePastisAuthorizer.class);

    @Override
    public boolean authorize(P principal, String role) {
        throw new UnsupportedOperationException("ContainerRequestContext is required for authorization");
    }

    public ResourceAuthZInfo preAuthorize(P principal, String role, @Nullable ContainerRequestContext context) {
        LOG.debug("Authorizing...");

        if (context == null) {
            LOG.warn("ContainerRequestContext is required for authorization");
            return null;
        }

        Object authZInfo = context.getProperty(ResourceAuthZInfo.class.getName());
        if (authZInfo == null) {
            LOG.warn("ResourceAuthZInfo is required for authorization");
            return null;
        }

        if (!(authZInfo instanceof ResourceAuthZInfo)) {
            LOG.warn("authZInfo type not supported");
            return null;
        }

        return (ResourceAuthZInfo) authZInfo;
    }
}