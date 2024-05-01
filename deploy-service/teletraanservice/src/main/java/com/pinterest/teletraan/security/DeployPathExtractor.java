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
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import java.sql.SQLException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.container.ContainerRequestContext;

/**
 * The authentication and authorization resource is extracted based on the deploy ID present in the
 * request's path parameters. It retrieves the corresponding environment bean from the EnvironDAO
 * and creates an AuthZResource object using the environment's name and stage name.
 */
public class DeployPathExtractor implements AuthZResourceExtractor {
    private static final String DEPLOY_ID = "id";
    private final EnvironDAO environDAO;

    public DeployPathExtractor(ServiceContext context) {
        this.environDAO = context.getEnvironDAO();
    }

    @Override
    public AuthZResource extractResource(ContainerRequestContext requestContext)
            throws ExtractionException {
        String deployID = requestContext.getUriInfo().getPathParameters().getFirst(DEPLOY_ID);
        if (deployID == null) {
            throw new ExtractionException("Failed to extract deploy id");
        }

        EnvironBean envBean;
        try {
            envBean = environDAO.getByDeployId(deployID);
        } catch (SQLException e) {
            throw new ExtractionException("Failed to get environment bean", e);
        }
        if (envBean == null) {
            throw new NotFoundException(
                    String.format("Environment not found, referenced by deploy(%s)", deployID));
        }
        return new AuthZResource(envBean.getEnv_name(), envBean.getStage_name());
    }
}
