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
import com.pinterest.deployservice.bean.HotfixBean;
import com.pinterest.deployservice.dao.HotfixDAO;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.container.ContainerRequestContext;

/**
 * The authentication and authorization resource is extracted based on the hotfix ID present in the
 * request's path parameters. It retrieves the corresponding environment bean from the EnvironDAO
 * and creates an AuthZResource object using the environment's name and stage name.
 */
public class HotfixPathExtractor implements AuthZResourceExtractor {
    private static final String HOTFIX_ID = "id";
    private final HotfixDAO hotfixDAO;

    public HotfixPathExtractor(ServiceContext context) {
        this.hotfixDAO = context.getHotfixDAO();
    }

    @Override
    public AuthZResource extractResource(ContainerRequestContext requestContext)
            throws ExtractionException {
        String hotfixID = requestContext.getUriInfo().getPathParameters().getFirst(HOTFIX_ID);
        if (hotfixID == null) {
            throw new ExtractionException("Failed to extract hotfix id");
        }

        HotfixBean hotfixBean;
        try {
            hotfixBean = hotfixDAO.getByHotfixId(hotfixID);
        } catch (Exception e) {
            throw new ExtractionException("Failed to get environment bean", e);
        }
        if (hotfixBean == null) {
            throw new NotFoundException(
                    String.format("Environment not found, referenced by hotfix(%s)", hotfixID));
        }
        return new AuthZResource(hotfixBean.getEnv_name(), "");
    }
}
