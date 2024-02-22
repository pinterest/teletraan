package com.pinterest.teletraan.universal.security;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(999) //authentication -1
public class ResourceAuthZInfoFilter implements ContainerRequestFilter {
    @Context
    private ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        ResourceAuthZInfo authZInfo = resourceInfo.getResourceMethod().getAnnotation(ResourceAuthZInfo.class);
        requestContext.setProperty(ResourceAuthZInfo.class.getName(), authZInfo);
    }
}
