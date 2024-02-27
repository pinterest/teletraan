package com.pinterest.teletraan.security;

import javax.ws.rs.container.ContainerRequestContext;

import com.pinterest.teletraan.universal.security.AuthZResourceExtractor;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;

public class EnvStagePathExtractor implements AuthZResourceExtractor {
    @Override
    public AuthZResource extractResource(ContainerRequestContext requestContext) throws RuntimeException {
        try {
            String envName = requestContext.getUriInfo().getPathParameters().getFirst("envName");
            String stageName = requestContext.getUriInfo().getPathParameters().getFirst("stageName");
            return new AuthZResource(envName, stageName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract environment resource", e);
        }
    }
}
