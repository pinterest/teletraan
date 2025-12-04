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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pinterest.commons.pastis.PastisAuthorizer;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import com.pinterest.teletraan.universal.security.bean.TeletraanPrincipal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.ws.rs.container.ContainerRequestContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/** A base class for authorizers that use Pastis for authorization. Pinterest only */
@Slf4j
public class BasePastisAuthorizer extends BaseAuthorizer<TeletraanPrincipal> {
    private static final String INPUT = "input";
    protected final PastisAuthorizer pastis;

    @Builder
    private BasePastisAuthorizer(
            String serviceName,
            PastisAuthorizer pastis,
            AuthZResourceExtractor.Factory factory,
            @NonNull Integer pastisTimeout) {
        super(factory);
        if (pastis == null) {
            if (serviceName == null) {
                throw new IllegalArgumentException(
                        "PastisAuthorizer and serviceName cannot both be null");
            }
            this.pastis = new PastisAuthorizer(serviceName, pastisTimeout);
        } else {
            this.pastis = pastis;
        }
    }

    public BasePastisAuthorizer(PastisAuthorizer pastis, AuthZResourceExtractor.Factory factory) {
        super(factory);
        this.pastis = pastis;
    }

    @Override
    public boolean authorize(
            TeletraanPrincipal principal,
            String role,
            AuthZResource requestedResource,
            @Nullable ContainerRequestContext context) {

        // Convert resource types that Pastis doesn't directly support to ENV_STAGE
        AuthZResource convertedResource = convertResourceForPastis(requestedResource);

        log.info(
                "Authorizing principal={}, role={}, originalResource={}:{}, convertedResource={}:{}",
                principal.getName(),
                role,
                requestedResource.getType(),
                requestedResource.getName(),
                convertedResource.getType(),
                convertedResource.getName());

        Map<String, PastisRequest> payload = new HashMap<>();
        payload.put(INPUT, new PastisRequest(principal, role, convertedResource));
        try {
            String pastisPayload = new ObjectMapper().writeValueAsString(payload);
            boolean authorized = pastis.authorize(pastisPayload);
            log.info("Pastis authorization result: {}", authorized);
            return authorized;
        } catch (Exception ex) {
            return handleAuthorizationException(ex);
        }
    }

    /**
     * Convert resource types to formats that Pastis understands. DEPLOY_SCHEDULE uses the same
     * permissions as ENV_STAGE.
     */
    private AuthZResource convertResourceForPastis(AuthZResource resource) {
        if (AuthZResource.Type.DEPLOY_SCHEDULE.equals(resource.getType())) {
            // DEPLOY_SCHEDULE format is "envName/stageName", convert to ENV_STAGE
            String[] parts = resource.getName().split("/");
            if (parts.length == 2) {
                return new AuthZResource(parts[0], parts[1], resource.getAttributes());
            }
        }
        return resource;
    }

    /**
     * Handle an exception that occurred while authorizing a request
     *
     * <p>Subclasses can override this method to provide custom handling of authorization exceptions
     *
     * @param ex
     * @return true if authorized
     */
    protected boolean handleAuthorizationException(Exception ex) {
        log.error("Failed to authorize request", ex);
        return false;
    }

    @Value
    @AllArgsConstructor
    protected static class PastisPrincipal {
        String id;
        String type;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<String> groups;
    }

    @Value
    protected static class PastisRequest {
        PastisPrincipal principal;
        String action;
        AuthZResource resource;

        public PastisRequest(TeletraanPrincipal principal, String action, AuthZResource resource) {
            this.principal =
                    new PastisPrincipal(
                            principal.getName(), principal.getType().name(), principal.getGroups());
            this.action = action;
            this.resource = resource;
        }
    }
}
