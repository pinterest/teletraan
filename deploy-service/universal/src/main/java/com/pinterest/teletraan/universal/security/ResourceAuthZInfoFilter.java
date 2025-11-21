/**
 * Copyright (c) 2024 Pinterest, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        // Check for multiple annotations first
        ResourceAuthZInfos authZInfos =
                resourceInfo.getResourceMethod().getAnnotation(ResourceAuthZInfos.class);
        if (authZInfos != null) {
            requestContext.setProperty(ResourceAuthZInfo.class.getName(), authZInfos.value());
            return;
        }

        // Fallback to single annotation for backward compatibility
        ResourceAuthZInfo authZInfo =
                resourceInfo.getResourceMethod().getAnnotation(ResourceAuthZInfo.class);
        requestContext.setProperty(ResourceAuthZInfo.class.getName(), authZInfo);
    }
}
