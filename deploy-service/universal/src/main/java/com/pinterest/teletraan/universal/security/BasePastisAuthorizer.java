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
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import com.pinterest.teletraan.universal.security.bean.ServicePrincipal;
import com.pinterest.teletraan.universal.security.bean.TeletraanPrincipal;
import com.pinterest.teletraan.universal.security.bean.UserPrincipal;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
public class BasePastisAuthorizer<P extends TeletraanPrincipal> extends BaseAuthorizer<P> {
    private static final Logger LOG = LoggerFactory.getLogger(BasePastisAuthorizer.class);
    protected final PastisAuthorizer pastis;
    protected final AuthZResourceExtractor.Factory extractorFactory;

    public BasePastisAuthorizer(String serviceName, AuthZResourceExtractor.Factory factory) {
        this(new PastisAuthorizer(serviceName), factory);
    }

    public BasePastisAuthorizer(AuthZResourceExtractor.Factory factory) {
        this("teletraan_dev", factory);
    }

    @Override
    public boolean authorize(P principal, String role, @Nullable ContainerRequestContext context) {
        ResourceAuthZInfo authZInfo = preAuthorize(principal, role, context);
        if (authZInfo == null) {
            return false;
        }

        AuthZResource resource;
        try {
            resource = extractorFactory.create(authZInfo).extractResource(context, authZInfo.beanClass());
        } catch (Exception ex) {
            LOG.warn("Failed to extract resource", ex);
            return false;
        }

        Gson gson = new Gson();
        Map<String, PastisRequest> payload = new HashMap<>();
        payload.put(
                "input",
                new PastisRequest(principal, role, resource));
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
    protected static class PastisPrincipal {
        private final String id;
        private final Type type;
        private List<String> groups;

        private enum Type {
            USER,
            SERVICE
        }
    }

    protected static class PastisRequest {
        private final PastisPrincipal principal;
        private final String method;
        private final AuthZResource resource;

        public PastisRequest(TeletraanPrincipal principal, String method, AuthZResource resource) {
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
            this.resource = resource;
        }

    }
}
