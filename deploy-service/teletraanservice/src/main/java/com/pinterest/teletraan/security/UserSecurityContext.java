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

import com.pinterest.deployservice.bean.TokenRolesBean;

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.List;

public class UserSecurityContext implements SecurityContext {
    private UserPrincipal userPrincipal;

    public UserSecurityContext(String user, TokenRolesBean tokenRolesBean, List<String> ldapGroups) {
        userPrincipal = new UserPrincipal(user, tokenRolesBean, ldapGroups);
    }

    @Override
    public Principal getUserPrincipal() {
        return userPrincipal;
    }

    @Override
    public boolean isUserInRole(String requiredRole) {
        return false;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public String getAuthenticationScheme() {
        return null;
    }
}
