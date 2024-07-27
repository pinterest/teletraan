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
package com.pinterest.teletraan.universal.security;

import com.pinterest.teletraan.universal.security.bean.AnonymousUser;
import java.io.IOException;
import java.security.Principal;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

/**
 * A filter for authenticating and authorizing any request as an anonymous user. For development use
 * only.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AnonymousAuthFilter implements ContainerRequestFilter {
    public static final AnonymousUser USER = new AnonymousUser();
    private SecurityContext securityContext;

    public AnonymousAuthFilter() {
        securityContext =
                new SecurityContext() {

                    @Override
                    public Principal getUserPrincipal() {
                        return AnonymousAuthFilter.USER;
                    }

                    @Override
                    public boolean isUserInRole(String s) {
                        return true;
                    }

                    @Override
                    public boolean isSecure() {
                        return false;
                    }

                    @Override
                    public String getAuthenticationScheme() {
                        return "Anonymous";
                    }
                };
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        containerRequestContext.setSecurityContext(securityContext);
    }
}
