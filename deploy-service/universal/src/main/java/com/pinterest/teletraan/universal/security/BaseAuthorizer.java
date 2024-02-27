package com.pinterest.teletraan.universal.security;

import javax.annotation.Nullable;
import javax.ws.rs.container.ContainerRequestContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import com.pinterest.teletraan.universal.security.bean.TeletraanPrincipal;

import io.dropwizard.auth.Authorizer;

public abstract class BaseAuthorizer<P extends TeletraanPrincipal> implements Authorizer<P> {
    private static final Logger LOG = LoggerFactory.getLogger(BasePastisAuthorizer.class);
    protected final AuthZResourceExtractor.Factory extractorFactory;

    public BaseAuthorizer() {
        extractorFactory = null;
    }

    public BaseAuthorizer(AuthZResourceExtractor.Factory factory) {
        extractorFactory = factory;
    }

    @Override
    public boolean authorize(P principal, String role) {
        throw new UnsupportedOperationException("ContainerRequestContext is required for authorization");
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
            requestedResource = extractorFactory.create(safeAuthZInfo).extractResource(context, safeAuthZInfo.beanClass());
        } catch (Exception ex) {
            LOG.warn("Failed to extract resource", ex);
            return false;
        }

        return authorize(principal, role, requestedResource, context);
    }

    abstract public boolean authorize(P principal, String role, AuthZResource requestedResource, @Nullable ContainerRequestContext context);
}