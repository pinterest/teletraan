/**
 * Copyright 2016 Pinterest, Inc.
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
import com.pinterest.deployservice.bean.*;
import com.pinterest.deployservice.dao.GroupRolesDAO;
import com.pinterest.deployservice.dao.UserRolesDAO;
import com.pinterest.teletraan.exception.TeletaanInternalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.*;

public class RoleAuthorizer implements Authorizer {
    private static final Logger LOG = LoggerFactory.getLogger(RoleAuthorizer.class);
    private UserRolesDAO userRolesDAO;
    private GroupRolesDAO groupRolesDAO;

    public RoleAuthorizer(ServiceContext context, String roleCacheSpec) throws Exception {
        this.userRolesDAO = context.getUserRolesDAO();
        this.groupRolesDAO = context.getGroupRolesDAO();
    }

    public void checkUserPermission(String userName, Resource resource, List<String> groups, Role requiredRole) throws Exception {
        // Consider group role(s)
        Set<String> groupsSet = new HashSet<>();
        if(groups != null && !groups.isEmpty()) {
            // Convert to Set for lookup convenience
            groupsSet.addAll(groups);

            List<GroupRolesBean> resourceGroupBeans = groupRolesDAO.getByResource(resource.getId(), resource.getType());
            for (GroupRolesBean resourceGroupBean : resourceGroupBeans) {
                if (groupsSet.contains(resourceGroupBean.getGroup_name())) {
                    if(resourceGroupBean.getRole().isAuthorized(requiredRole)) {
                        return;
                    }
                }
            }
        }

        // Consider user role(s)
        UserRolesBean userBean = userRolesDAO.getByNameAndResource(userName, resource.getId(), resource.getType());
        if(userBean != null) {
            if(userBean.getRole().isAuthorized(requiredRole)) {
                return;
            }
        }

        // Check SYSTEM wide group role
        if(groups != null && !groups.isEmpty()) {
            List<GroupRolesBean> systemGroupBeans = groupRolesDAO.getByResource(Resource.ALL, Resource.Type.SYSTEM);
            for(GroupRolesBean group : systemGroupBeans) {
                if(groupsSet.contains(group.getGroup_name())) {
                    if (group.getRole().isAuthorized(requiredRole)) {
                        return;
                    }
                }
            }
        }

        // Consider SYSTEM wide role
        UserRolesBean systemBean = userRolesDAO.getByNameAndResource(userName, Resource.ALL, Resource.Type.SYSTEM);
        if (systemBean != null) {
            if(systemBean.getRole().isAuthorized(requiredRole)) {
                return;
            }
        }

        // Otherwise not authorized
        throw new TeletaanInternalException(Response.Status.FORBIDDEN, "Not authorized!");
    }

    public void checkAPITokenPermission(TokenRolesBean bean, Resource requiredResource, Role requiredRole) throws Exception {
        Resource myResource = new Resource(bean.getResource_id(), bean.getResource_type());

        if ((myResource.equals(requiredResource) && bean.getRole().isAuthorized(requiredRole))) {
            // An exact match
            return;
        }

        if (myResource.getType() == Resource.Type.SYSTEM && bean.getRole().isAuthorized(requiredRole)) {
            // More than enough
            return;
        }

        // Otherwise, no way
        throw new TeletaanInternalException(Response.Status.FORBIDDEN, "Not authorized!");
    }

    @Override
    public void authorize(SecurityContext securityContext, Resource resource, Role requiredRole) throws Exception {
        UserPrincipal principal = (UserPrincipal) securityContext.getUserPrincipal();

        // Check if script token
        if (principal.getTokenRolesBean() != null) {
            checkAPITokenPermission(principal.getTokenRolesBean(), resource, requiredRole);
        }
        // Check user roles if not a script
        else {
            checkUserPermission(principal.getName(), resource, principal.getGroups(), requiredRole);
        }
    }
}
