package com.pinterest.teletraan.universal.security;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.ws.rs.container.ContainerRequestContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.pinterest.commons.pastis.PastisAuthorizer;
import com.pinterest.teletraan.universal.security.bean.ServicePrincipal;
import com.pinterest.teletraan.universal.security.bean.TeletraanPrincipal;
import com.pinterest.teletraan.universal.security.bean.UserPrincipal;

import io.dropwizard.auth.Authorizer;
import lombok.AllArgsConstructor;
import lombok.Data;

public class TeletraanPastisAuthorizer<P extends TeletraanPrincipal> implements Authorizer<P> {
    private static final Logger LOG = LoggerFactory.getLogger(TeletraanPastisAuthorizer.class);
    private final PastisAuthorizer pastis;

    public TeletraanPastisAuthorizer(String serviceName) {
        this(new PastisAuthorizer(serviceName));
    }

    public TeletraanPastisAuthorizer() {
        this("teletraan_dev");
    }

    public TeletraanPastisAuthorizer(PastisAuthorizer pastis) {
        this.pastis = pastis;
    }

    @Override
    public boolean authorize(TeletraanPrincipal principal, String role, @Nullable ContainerRequestContext context) {
        LOG.debug("Authorizing...");

        if (context == null) {
            LOG.warn("ContainerRequestContext is required for authorization");
            return false;
        }

        Gson gson = new Gson();
        Map<String, PastisRequest> payload = new HashMap<>();
        payload.put(
                "input",
                new PastisRequest(principal, context.getMethod(), "/" + context.getUriInfo().getPath()));
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

    @Data
    @AllArgsConstructor
    private static class PastisPrincipal {
        private final String id;
        private final Type type;
        private List<String> groups;

        private enum Type {
            USER,
            SERVICE
        }
    }

    private static class PastisRequest {
        private final PastisPrincipal principal;
        private final String method;
        private final String path;

        public PastisRequest(TeletraanPrincipal principal, String method, String path) {
            if (principal instanceof UserPrincipal) {
                this.principal = new PastisPrincipal(principal.getName(), PastisPrincipal.Type.USER,
                        principal.getGroups());
            } else if (principal instanceof ServicePrincipal) {
                this.principal = new PastisPrincipal(principal.getName(), PastisPrincipal.Type.SERVICE,
                        principal.getGroups());
            } else {
                LOG.warn("Principal type not supported.");
                throw new IllegalArgumentException("Principal type not supported.");
            }
            this.method = method;
            this.path = path;
        }

    }

    @Override
    public boolean authorize(TeletraanPrincipal principal, String role) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'authorize'");
    }
}
