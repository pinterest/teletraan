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
package com.pinterest.teletraan.security;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.GroupRolesBean;
import com.pinterest.deployservice.bean.TeletraanPrincipalRoles;
import com.pinterest.deployservice.bean.UserRolesBean;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.GroupRolesDAO;
import com.pinterest.deployservice.dao.UserRolesDAO;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor;
import com.pinterest.teletraan.universal.security.BaseAuthorizer;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import com.pinterest.teletraan.universal.security.bean.UserPrincipal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.ws.rs.container.ContainerRequestContext;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class UserRoleAuthorizer extends BaseAuthorizer<UserPrincipal> {
    private static final Logger LOG = LoggerFactory.getLogger(UserRoleAuthorizer.class);
    private final UserRolesDAO userRolesDAO;
    private final GroupRolesDAO groupRolesDAO;
    private final EnvironDAO environDAO;

    public UserRoleAuthorizer(
            ServiceContext context, AuthZResourceExtractor.Factory authZResourceExtractorFactory) {
        super(authZResourceExtractorFactory);
        userRolesDAO = context.getUserRolesDAO();
        groupRolesDAO = context.getGroupRolesDAO();
        environDAO = context.getEnvironDAO();
    }

    @Override
    public boolean authorize(
            UserPrincipal principal,
            String role,
            AuthZResource requestedResource,
            @Nullable ContainerRequestContext context) {
        try {
            TeletraanPrincipalRoles requiredRole = TeletraanPrincipalRoles.valueOf(role);
            AuthZResource convertedRequestedResource = requestedResource;
            if (AuthZResource.Type.ENV_STAGE.equals(convertedRequestedResource.getType())) {
                // Convert to ENV for backward compatibility
                convertedRequestedResource = new AuthZResource(convertedRequestedResource.getName(),
                        AuthZResource.Type.ENV);
            }

            // Consider group role(s)
            Set<String> groupsSet = new HashSet<>();
            if (principal.getGroups() != null && !principal.getGroups().isEmpty()) {
                // Convert to Set for lookup convenience
                groupsSet.addAll(principal.getGroups());

                List<GroupRolesBean> resourceGroupBeans = groupRolesDAO.getByResource(
                        convertedRequestedResource.getName(), convertedRequestedResource.getType());
                for (GroupRolesBean resourceGroupBean : resourceGroupBeans) {
                    if (groupsSet.contains(resourceGroupBean.getGroup_name())
                            && resourceGroupBean.getRole().isEqualOrSuperior(requiredRole)) {
                        return true;
                    }
                }
            }

            // Consider user role(s)
            UserRolesBean userBean = userRolesDAO.getByNameAndResource(
                    principal.getName(),
                    convertedRequestedResource.getName(),
                    convertedRequestedResource.getType());
            if (userBean != null && userBean.getRole().isEqualOrSuperior(requiredRole)) {
                return true;
            }

            // Check SYSTEM wide group role
            if (principal.getGroups() != null && !principal.getGroups().isEmpty()) {
                List<GroupRolesBean> systemGroupBeans = groupRolesDAO.getByResource(AuthZResource.ALL,
                        AuthZResource.Type.SYSTEM);
                for (GroupRolesBean group : systemGroupBeans) {
                    if (groupsSet.contains(group.getGroup_name()) && group.getRole().isEqualOrSuperior(requiredRole)) {
                        return true;
                    }
                }
            }

            // Consider SYSTEM wide role
            UserRolesBean systemBean = userRolesDAO.getByNameAndResource(
                    principal.getName(), AuthZResource.ALL, AuthZResource.Type.SYSTEM);
            if (systemBean != null && systemBean.getRole().isEqualOrSuperior(requiredRole)) {
                return true;
            }

            // Special case for creating a new environment
            if (AuthZResource.Type.ENV.equals(requestedResource.getType())
                    && requiredRole.equals(TeletraanPrincipalRoles.WRITE)) {
                String envName = convertedRequestedResource.getName().split("/")[0];
                List<EnvironBean> environBeans = environDAO.getByName(envName);
                if (CollectionUtils.isEmpty(environBeans)) {
                    return true;
                }
            }

            if (AuthZResource.Type.RATINGS.equals(convertedRequestedResource.getType())
                    && TeletraanPrincipalRoles.WRITE.equals(requiredRole)) {
                return true;
            }

            return false;
        } catch (Exception ex) {
            LOG.error("Authorization failed", ex);
            return false;
        }
    }
}
