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

import com.pinterest.deployservice.common.DeployInternalException;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.exception.TeletaanInternalException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.core.UriInfo;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class TokenAuthFilter implements ContainerRequestFilter {

    private static final String AUTHENTICATION_HEADER = "Authorization";
    private static final String TELETRAAN_TOKEN_SCHEME = "token";
    private static final Logger LOG = LoggerFactory.getLogger(TokenAuthFilter.class);

    private UserDataHelper userDataHelper;

    public TokenAuthFilter(String userDataUrl, String groupDataUrl, String userNameKey, Boolean extractUserNameFromEmail, String tokenCacheSpec, TeletraanServiceContext context) throws Exception {
        userDataHelper = new UserDataHelper(userDataUrl, groupDataUrl, userNameKey, extractUserNameFromEmail, tokenCacheSpec, context);
    }

    @Override
    public void filter(ContainerRequestContext context) throws IOException {
        if(!context.getMethod().equals("OPTIONS")) {
            SecurityContext securityContext;

            try {
                securityContext = authenticate(context);
            } catch (Exception e) {
                LOG.info("Authentication failed. Reason: " + e.getMessage());
                throw new TeletaanInternalException(Response.Status.UNAUTHORIZED,
                        "Failed to authenticate user. " + e.getMessage());
            }
            context.setSecurityContext(securityContext);
        }
    }

    private SecurityContext authenticate(ContainerRequestContext context) throws Exception {
        String authCredentials = context.getHeaderString(AUTHENTICATION_HEADER);
        UriInfo uriInfo = context.getUriInfo();
        if (StringUtils.isEmpty(authCredentials)) {
            if (!uriInfo.getAbsolutePath().equals("healthcheck") &&
                !uriInfo.getAbsolutePath().equals("/healthcheck") &&
                !uriInfo.getPath().equals("healthcheck") &&
                !uriInfo.getPath().equals("/healthcheck")) {
                    throw new DeployInternalException("Can not find HTTP header: Authorization!");
            }
            return null;
        }

        String[] schemeAndToken = authCredentials.trim().split(" ");
        String scheme = schemeAndToken[0].trim();
        String token = schemeAndToken[1].trim();

        if (scheme.equalsIgnoreCase(TELETRAAN_TOKEN_SCHEME)) {
            return userDataHelper.getUserSecurityContext(token);
        }

        throw new DeployInternalException("Authorization scheme " + scheme + " is not supported!");
    }
}
