package com.pinterest.teletraan.universal.security;

import javax.ws.rs.container.ContainerRequestContext;

import com.pinterest.teletraan.universal.security.bean.AuthZResource;

public interface AuthZResourceExtractor {
    AuthZResource extractResource(ContainerRequestContext requestContext) throws RuntimeException;
    default AuthZResource extractResource(ContainerRequestContext requestContext, Class<?> beanClass) throws RuntimeException {
        return extractResource(requestContext);
    }

    public interface Factory {
        AuthZResourceExtractor create(ResourceAuthZInfo authZInfo);
    }
}
