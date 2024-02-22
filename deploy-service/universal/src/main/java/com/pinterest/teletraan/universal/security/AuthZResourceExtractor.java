package com.pinterest.teletraan.universal.security;

import javax.ws.rs.container.ContainerRequestContext;

import com.pinterest.teletraan.universal.security.bean.AuthZResource;

public interface AuthZResourceExtractor {
    AuthZResource extractResource(ContainerRequestContext requestContext) throws RuntimeException;

    public interface Factory {
        AuthZResourceExtractor create(ResourceAuthZInfo authZInfo);
    }
}
