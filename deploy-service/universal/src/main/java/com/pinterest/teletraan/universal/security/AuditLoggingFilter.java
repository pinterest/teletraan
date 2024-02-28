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

import com.google.gson.Gson;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Priority(Integer.MIN_VALUE)
public class AuditLoggingFilter implements ContainerResponseFilter {
    private static final Logger LOG = LoggerFactory.getLogger(AuditLoggingFilter.class);

    @Override
    public void filter(
            ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        if (requestContext.getSecurityContext() == null
                || requestContext.getSecurityContext().getUserPrincipal() == null) {
            return;
        }
        try {
            Map<String, Object> attributes = new HashMap<>();
            attributes.put(
                    "principal", requestContext.getSecurityContext().getUserPrincipal().getName());
            attributes.put("resource", requestContext.getUriInfo().getRequestUri().toString());
            attributes.put("method", requestContext.getMethod());
            attributes.put("status", responseContext.getStatus());

            String json = new Gson().toJson(attributes);
            LOG.info(json);
        } catch (Exception ex) {
            LOG.error("Failed to generate audit log", ex);
        }
    }
}
