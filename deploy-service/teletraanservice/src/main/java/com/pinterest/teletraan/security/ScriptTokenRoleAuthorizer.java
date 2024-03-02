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

import com.pinterest.deployservice.bean.TeletraanPrincipalRoles;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor;
import com.pinterest.teletraan.universal.security.BaseAuthorizer;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import com.pinterest.teletraan.universal.security.bean.ScriptTokenPrincipal;
import com.pinterest.teletraan.universal.security.bean.ValueBasedRole;
import javax.annotation.Nullable;
import javax.ws.rs.container.ContainerRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScriptTokenRoleAuthorizer extends BaseAuthorizer<ScriptTokenPrincipal<ValueBasedRole>> {
    private static final Logger LOG = LoggerFactory.getLogger(ScriptTokenRoleAuthorizer.class);

    public ScriptTokenRoleAuthorizer(AuthZResourceExtractor.Factory authZResourceExtractorFactory) {
        super(authZResourceExtractorFactory);
    }

    @Override
    public boolean authorize(
            ScriptTokenPrincipal<ValueBasedRole> principal,
            String role,
            AuthZResource requestedResource,
            @Nullable ContainerRequestContext context) {
        if (!principal
                .getRole()
                .isEqualOrSuperior(TeletraanPrincipalRoles.valueOf(role).getRole())) {
            LOG.info("Requested role does not match principal role");
            return false;
        }

        if (requestedResource.getType().equals(AuthZResource.Type.ENV_STAGE)) {
            // Convert to ENV for backward compatibility
            requestedResource =
                    new AuthZResource(requestedResource.getName(), AuthZResource.Type.ENV);
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
