package com.pinterest.teletraan.security;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.ws.rs.container.ContainerRequestContext;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.GroupRolesBean;
import com.pinterest.deployservice.bean.UserRolesBean;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.GroupRolesDAO;
import com.pinterest.deployservice.dao.UserRolesDAO;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor;
import com.pinterest.teletraan.universal.security.BaseAuthorizer;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfo;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import com.pinterest.teletraan.universal.security.bean.TeletraanPrincipalRoles;
import com.pinterest.teletraan.universal.security.bean.UserPrincipal;

@Deprecated
public class UserRoleAuthorizer<P extends UserPrincipal> extends BaseAuthorizer<UserPrincipal> {
    private static final Logger LOG = LoggerFactory.getLogger(UserRoleAuthorizer.class);
    private final AuthZResourceExtractor.Factory extractorFactory;
    private final UserRolesDAO userRolesDAO;
    private final GroupRolesDAO groupRolesDAO;
    private final EnvironDAO environDAO;

    public UserRoleAuthorizer(ServiceContext context, AuthZResourceExtractor.Factory authZResourceExtractorFactory) {
        extractorFactory = authZResourceExtractorFactory;
        userRolesDAO = context.getUserRolesDAO();
        groupRolesDAO = context.getGroupRolesDAO();
        environDAO = context.getEnvironDAO();
    }

    @Override
    public boolean authorize(UserPrincipal principal, String role, @Nullable ContainerRequestContext context) {
        ResourceAuthZInfo authZInfo = preAuthorize(principal, role, context);
        if (authZInfo == null) {
            return false;
        }

        AuthZResource requestedResource;
        try {
            requestedResource = extractorFactory.create(authZInfo).extractResource(context, authZInfo.beanClass());
        } catch (Exception ex) {
            LOG.warn("Failed to extract resource", ex);
            return false;
        }

        try {
            TeletraanPrincipalRoles requiredRole = TeletraanPrincipalRoles.valueOf(role);

            // Consider group role(s)
            Set<String> groupsSet = new HashSet<>();
            if (principal.getGroups() != null && !principal.getGroups().isEmpty()) {
                // Convert to Set for lookup convenience
                groupsSet.addAll(principal.getGroups());

                List<GroupRolesBean> resourceGroupBeans = groupRolesDAO.getByResource(requestedResource.getName(),
                        requestedResource.getType());
                for (GroupRolesBean resourceGroupBean : resourceGroupBeans) {
                    if (groupsSet.contains(resourceGroupBean.getGroup_name())) {
                        if (resourceGroupBean.getRole().isEqualOrSuperior(requiredRole)) {
                            return true;
                        }
                    }
                }
            }

            // Consider user role(s)
            UserRolesBean userBean = userRolesDAO.getByNameAndResource(principal.getName(), requestedResource.getName(),
                    requestedResource.getType());
            if (userBean != null) {
                if (userBean.getRole().isEqualOrSuperior(requiredRole)) {
                    return true;
                }
            }

            // Check SYSTEM wide group role
            if (principal.getGroups() != null && !principal.getGroups().isEmpty()) {
                List<GroupRolesBean> systemGroupBeans = groupRolesDAO.getByResource(AuthZResource.ALL,
                        AuthZResource.Type.SYSTEM);
                for (GroupRolesBean group : systemGroupBeans) {
                    if (groupsSet.contains(group.getGroup_name())) {
                        if (group.getRole().isEqualOrSuperior(requiredRole)) {
                            return true;
                        }
                    }
                }
            }

            // Consider SYSTEM wide role
            UserRolesBean systemBean = userRolesDAO.getByNameAndResource(principal.getName(), AuthZResource.ALL,
                    AuthZResource.Type.SYSTEM);
            if (systemBean != null) {
                if (systemBean.getRole().isEqualOrSuperior(requiredRole)) {
                    return true;
                }
            }

            // Special case for creating a new environment
            if (requestedResource.getType().equals(AuthZResource.Type.ENV_STAGE)
                    && requiredRole.equals(TeletraanPrincipalRoles.WRITE)) {
                String envName = requestedResource.getName().split("/")[0];
                List<EnvironBean> environBeans = environDAO.getByName(envName);
                if (CollectionUtils.isEmpty(environBeans)) {
                    // This is the first stage for this env, let's make operator ADMIN of this env
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            LOG.error("Authorization failed", ex);
            return false;
        }
    }
}
