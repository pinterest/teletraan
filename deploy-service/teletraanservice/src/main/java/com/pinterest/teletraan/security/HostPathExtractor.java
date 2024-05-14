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
import com.pinterest.deployservice.dao.HostAgentDAO;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import java.sql.SQLException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.container.ContainerRequestContext;

public class HostPathExtractor implements AuthZResourceExtractor {
    private static final String HOST_ID = "hostId";
    private final HostAgentDAO hostAgentDAO;

    public HostPathExtractor(ServiceContext context) {
        this.hostAgentDAO = context.getHostAgentDAO();
    }

    @Override
    public AuthZResource extractResource(ContainerRequestContext requestContext)
            throws ExtractionException {
        String hostId = requestContext.getUriInfo().getPathParameters().getFirst(HOST_ID);
        if (hostId == null) {
            throw new ExtractionException("Failed to extract host id");
        }

        EnvironBean envBean;
        try {
            envBean = hostAgentDAO.getMainEnvByHostId(hostId);
        } catch (SQLException e) {
            throw new ExtractionException(
                    "Failed to get the main environment with host ID: " + hostId, e);
        }

        if (envBean == null) {
            throw new NotFoundException(
                    "Failed to get the main environment with host ID: " + hostId);
        }
        return new AuthZResource(envBean.getEnv_name(), envBean.getStage_name());
    }
}
