package com.pinterest.teletraan.security;

import java.io.InputStream;

import javax.ws.rs.container.ContainerRequestContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;

public class EnvironmentBodyExtractor implements AuthZResourceExtractor {
    @Override
    public AuthZResource extractResource(ContainerRequestContext requestContext) throws RuntimeException {
        InputStream inputStream = requestContext.getEntityStream();
        try {
            EnvironBean envBean = new ObjectMapper().readValue(inputStream, EnvironBean.class);
            return new AuthZResource(envBean.getEnv_name(), AuthZResource.Type.ENV);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract environment resource", e);
        }
    }

}
