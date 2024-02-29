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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasePastisAuthorizer<P extends TeletraanPrincipal> extends BaseAuthorizer<P> {
    private static final Logger LOG = LoggerFactory.getLogger(BasePastisAuthorizer.class);
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
            P principal,
            String role,
            AuthZResource requestedResource,
            @Nullable ContainerRequestContext context) {

        Gson gson = new Gson();
        Map<String, PastisRequest> payload = new HashMap<>();
        payload.put("input", new PastisRequest(principal, role, requestedResource));
        try {
            String pastisPayload = gson.toJson(payload);
            boolean authorized = pastis.authorize(pastisPayload);
            LOG.debug("Authorized: {}", authorized);
            return authorized;
        } catch (Exception ex) {
            LOG.debug("Failed to authorized via Pastis", ex);
            // TODO: add fallback
            return false;
        }
    }

    @AllArgsConstructor
    protected static class PastisPrincipal {
        private final String id;
        private final String type;
        private List<String> groups;
    }

    protected static class PastisRequest {
        private final PastisPrincipal principal;
        private final String action;
        private final AuthZResource resource;

        public PastisRequest(TeletraanPrincipal principal, String action, AuthZResource resource) {
            this.principal =
                    new PastisPrincipal(
                            principal.getName(), principal.getType().name(), principal.getGroups());
            this.action = action;
            this.resource = resource;
        }
    }
}
