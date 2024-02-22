package com.pinterest.teletraan.universal.security;

import javax.annotation.Nullable;
import javax.ws.rs.container.ContainerRequestContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import com.pinterest.teletraan.universal.security.bean.PrincipalRoles;
import com.pinterest.teletraan.universal.security.bean.ServicePrincipal;

@Deprecated
public class ScriptTokenRoleAuthorizer extends BaseAuthorizer<ServicePrincipal> {
    private static final Logger LOG = LoggerFactory.getLogger(ScriptTokenRoleAuthorizer.class);
    AuthZResourceExtractor.Factory extractorFactory;

    public ScriptTokenRoleAuthorizer(AuthZResourceExtractor.Factory authZResourceExtractorFactory) {
        extractorFactory = authZResourceExtractorFactory;
    }

    @Override
    public boolean authorize(
            ServicePrincipal principal, String role, @Nullable ContainerRequestContext context) {
        ResourceAuthZInfo authZInfo = preAuthorize(principal, role, context);
        if (authZInfo == null) {
            return false;
        }

        AuthZResource requestedResource;
        try {
            requestedResource = extractorFactory.create(authZInfo).extractResource(context);
        } catch (Exception ex) {
            LOG.warn("Failed to extract resource", ex);
            return false;
        }

        if (!requestedResource.equals(principal.getResource())) {
            LOG.info("Requested resource does not match principal resource");
            return false;
        }
        if (!PrincipalRoles.valueOf(role).equals(principal.getRole())) {
            LOG.info("Requested role does not match principal role");
            return false;
        }

        LOG.debug("Authorized");
        return true;
    }
}
