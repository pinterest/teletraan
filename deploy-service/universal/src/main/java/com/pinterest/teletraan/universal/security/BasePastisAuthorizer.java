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
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/** A base class for authorizers that use Pastis for authorization. Pinterest only */
@Slf4j
public class BasePastisAuthorizer extends BaseAuthorizer<TeletraanPrincipal> {
    private static final String INPUT = "input";
    protected final PastisAuthorizer pastis;

    @Builder
    private BasePastisAuthorizer(
            String serviceName, PastisAuthorizer pastis, AuthZResourceExtractor.Factory factory) {
        super(factory);
        if (pastis == null) {
            if (serviceName == null) {
                throw new IllegalArgumentException(
                        "PastisAuthorizer and serviceName cannot both be null");
            }
            this.pastis = new PastisAuthorizer(serviceName);
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

        Map<String, PastisRequest> payload = new HashMap<>();
        payload.put(INPUT, new PastisRequest(principal, role, requestedResource));
        try {
            String pastisPayload = new ObjectMapper().writeValueAsString(payload);
            boolean authorized = pastis.authorize(pastisPayload);
            log.debug("Authorized: {}", authorized);
            return authorized;
        } catch (Exception ex) {
            return handleAuthorizationException(ex);
        }
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
