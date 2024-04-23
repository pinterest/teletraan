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

import javax.ws.rs.NotFoundException;
import javax.ws.rs.container.ContainerRequestContext;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.BuildBean;
import com.pinterest.deployservice.dao.BuildDAO;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;

public class BuildPathExtractor implements AuthZResourceExtractor {
    private static final String BUILD_ID = "id";
    private final BuildDAO buildDAO;

    public BuildPathExtractor(ServiceContext context) {
        this.buildDAO = context.getBuildDAO();
    }

    @Override
    public AuthZResource extractResource(ContainerRequestContext requestContext)
            throws ExtractionException {
        String buildId = requestContext.getUriInfo().getPathParameters().getFirst(BUILD_ID);
        if (buildId == null) {
            throw new ExtractionException("Failed to extract build id");
        }

        BuildBean buildBean;
        try {
            buildBean = buildDAO.getById(buildId);
        } catch (Exception e) {
            throw new ExtractionException("Failed to get build bean", e);
        }

        if (buildBean == null) {
            throw new NotFoundException(String.format("Build %s not found", buildId));
        }
        return new AuthZResource(buildBean.getBuild_name(), AuthZResource.Type.BUILD);
    }
}
