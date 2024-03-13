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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import lombok.extern.slf4j.Slf4j;

/**
 * A filter for logging audit information for each request. Should be configured differently than
 * regular application logs.
 */
@Priority(Integer.MIN_VALUE)
@Slf4j
public class AuditLoggingFilter implements ContainerResponseFilter {
    private static final String PRINCIPAL = "principal";
    private static final String RESOURCE = "resource";
    private static final String METHOD = "method";
    private static final String STATUS = "status";

    @Override
    public void filter(
            ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        if (requestContext.getSecurityContext() == null
                || requestContext.getSecurityContext().getUserPrincipal() == null) {
            return;
        }
        Map<String, Object> attributes = new HashMap<>();
        try {
            attributes.put(
                    PRINCIPAL, requestContext.getSecurityContext().getUserPrincipal().getName());
            attributes.put(RESOURCE, requestContext.getUriInfo().getRequestUri().toString());
            attributes.put(METHOD, requestContext.getMethod());
            attributes.put(STATUS, responseContext.getStatus());

            String json = new ObjectMapper().writeValueAsString(attributes);
            log.info(json);
        } catch (Exception ex) {
            log.error("Failed to generate audit log for: {}", attributes, ex);
        }
    }
}
