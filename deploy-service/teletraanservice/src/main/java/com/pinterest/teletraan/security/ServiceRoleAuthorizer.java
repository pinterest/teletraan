package com.pinterest.teletraan.security;

import javax.annotation.Nullable;
import javax.ws.rs.container.ContainerRequestContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pinterest.deployservice.bean.TeletraanPrincipalRoles;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor;
import com.pinterest.teletraan.universal.security.BaseAuthorizer;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import com.pinterest.teletraan.universal.security.bean.ServicePrincipal;
import com.pinterest.teletraan.universal.security.bean.ValueBasedRole;

public class ServiceRoleAuthorizer extends BaseAuthorizer<ServicePrincipal<ValueBasedRole>> {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceRoleAuthorizer.class);

    public ServiceRoleAuthorizer(AuthZResourceExtractor.Factory authZResourceExtractorFactory) {
        super(authZResourceExtractorFactory);
    }

    @Override
    public boolean authorize(
            ServicePrincipal<ValueBasedRole> principal, String role, AuthZResource requestedResource,
            @Nullable ContainerRequestContext context) {
        if (!principal.getRole().isEqualOrSuperior(TeletraanPrincipalRoles.valueOf(role).getRole())) {
            LOG.info("Requested role does not match principal role");
            return false;
        }

        if (requestedResource.getType().equals(AuthZResource.Type.ENV_STAGE)) {
            // Convert to ENV for backward compatibility
            requestedResource = new AuthZResource(requestedResource.getName(), AuthZResource.Type.ENV);
        }

        if (requestedResource.equals(principal.getResource())
                || principal.getResource().getType().equals(AuthZResource.Type.SYSTEM)) {
            LOG.debug("Authorized");
            return true;
        }

        LOG.info("Requested resource does not match principal resource");
        return false;
    }
}
