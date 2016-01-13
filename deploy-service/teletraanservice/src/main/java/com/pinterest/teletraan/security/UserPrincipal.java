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

import java.security.Principal;
import java.util.List;

public class UserPrincipal implements Principal {
    private String user;
    private TokenRolesBean tokenRolesBean;
    private List<String> groups;

    public UserPrincipal(String user, TokenRolesBean tokenRolesBean, List<String> groups) {
        this.user = user;
        this.tokenRolesBean = tokenRolesBean;
        this.groups = groups;
    }

    @Override
    public String getName() {
        return user;
    }

    public String getUser() {
        return user;
    }

    public List<String> getGroups() {
        return groups;
    }

    public TokenRolesBean getTokenRolesBean() {
        return tokenRolesBean;
    }
}
