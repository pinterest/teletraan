package com.pinterest.teletraan.universal.security;

import javax.annotation.Nullable;
import javax.ws.rs.container.ContainerRequestContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import com.pinterest.teletraan.universal.security.bean.Role;
import com.pinterest.teletraan.universal.security.bean.RoleEnum;
import com.pinterest.teletraan.universal.security.bean.ServicePrincipal;

@Deprecated
public class ServiceRoleAuthorizer<R extends Role<R>, RE extends Enum<RE> & RoleEnum<R>, P extends ServicePrincipal<R>>
        extends BaseAuthorizer<P> {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceRoleAuthorizer.class);
    private AuthZResourceExtractor.Factory extractorFactory;
    private Class<RE> roleEnumClass;

    public ServiceRoleAuthorizer(AuthZResourceExtractor.Factory authZResourceExtractorFactory, Class<RE> roleClass) {
        extractorFactory = authZResourceExtractorFactory;
        roleEnumClass = roleClass;
    }

    @Override
    public boolean authorize(
            P principal, String role, @Nullable ContainerRequestContext context) {
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

        if (principal.getRole().isEqualOrSuperior((Enum.valueOf(roleEnumClass, role)).getRole())) {
            LOG.info("Requested role does not match principal role");
            return false;
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
