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

import com.pinterest.teletraan.universal.security.AuthZResourceExtractor.BeanClassExtractionException;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor.ExtractionException;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import com.pinterest.teletraan.universal.security.bean.TeletraanPrincipal;
import javax.annotation.Nullable;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * A base class for authorizers. Every Teletraan authorizer should extend this class.
 *
 * @param <P> the principal type
 */
@AllArgsConstructor
@Slf4j
public abstract class BaseAuthorizer<P extends TeletraanPrincipal>
        implements TeletraanAuthorizer<P> {
    protected final AuthZResourceExtractor.Factory extractorFactory;

    @Override
    public boolean authorize(P principal, String role) {
        throw new UnsupportedOperationException(
                "ContainerRequestContext is required for authorization");
    }

    @Override
    public boolean authorize(P principal, String role, @Nullable ContainerRequestContext context) {
        log.debug("Authorizing...");

        if (context == null) {
            log.warn("ContainerRequestContext is required for authorization");
            return false;
        }

        Object authZInfo = context.getProperty(ResourceAuthZInfo.class.getName());
        if (authZInfo == null) {
            return authorize(principal, role, AuthZResource.UNSPECIFIED_RESOURCE, context);
        }

        if (!(authZInfo instanceof ResourceAuthZInfo)) {
            log.warn("authZInfo type not supported");
            return false;
        }

        ResourceAuthZInfo safeAuthZInfo = (ResourceAuthZInfo) authZInfo;

        if (AuthZResource.Type.SYSTEM.equals(safeAuthZInfo.type())) {
            return authorize(principal, role, AuthZResource.SYSTEM_RESOURCE, context);
        } else {
            AuthZResource requestedResource;
            try {
                requestedResource =
                        extractorFactory
                                .create(safeAuthZInfo)
                                .extractResource(context, safeAuthZInfo.beanClass());
            } catch (BeanClassExtractionException ex) {
                // Although within the authorization process, if we cannot extract the bean from
                // the request body, it is a client error.
                throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
            } catch (ExtractionException ex) {
                // Otherwise, it is a server error.
                throw new WebApplicationException(
                        "Failed to extract resource. Did you forget to annotate the resource with @ResourceAuthZInfo?",
                        ex);
            }
            return authorize(principal, role, requestedResource, context);
        }
    }
}
