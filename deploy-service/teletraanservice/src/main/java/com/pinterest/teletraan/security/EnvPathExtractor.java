package com.pinterest.teletraan.security;

import javax.ws.rs.container.ContainerRequestContext;

import com.pinterest.teletraan.universal.security.AuthZResourceExtractor;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;

public class EnvPathExtractor implements AuthZResourceExtractor {
    @Override
    public AuthZResource extractResource(ContainerRequestContext requestContext) throws RuntimeException {
        try {
            String envName = requestContext.getUriInfo().getPathParameters().getFirst("envName");
            return new AuthZResource(envName, AuthZResource.Type.ENV);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract environment resource", e);
        }
    }
}
