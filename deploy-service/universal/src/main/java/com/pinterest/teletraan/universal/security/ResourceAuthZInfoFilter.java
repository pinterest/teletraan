/**
 * Copyright (c) 2024, Pinterest Inc. All rights reserved.
 */
package com.pinterest.teletraan.universal.security;

import java.io.IOException;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

/**
 * This filter adds the ResourceAuthZInfo annotation to the request context properties so that it
 * can be used by the AuthZResourceExtractor to extract the resource information.
 */
@Provider
@Priority(Priorities.AUTHENTICATION - 1) // Run before authentication filters
public class ResourceAuthZInfoFilter implements ContainerRequestFilter {
    @Context private ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        ResourceAuthZInfo authZInfo =
                resourceInfo.getResourceMethod().getAnnotation(ResourceAuthZInfo.class);
        requestContext.setProperty(ResourceAuthZInfo.class.getName(), authZInfo);
    }
}
