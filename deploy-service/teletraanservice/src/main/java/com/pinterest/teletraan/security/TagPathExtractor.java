/**
 * Copyright (c) 2026 Pinterest, Inc.
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
import com.pinterest.deployservice.bean.TagBean;
import com.pinterest.deployservice.dao.TagDAO;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.container.ContainerRequestContext;
import org.apache.commons.lang3.StringUtils;

/** Resolves tag deletion using the persisted tag rather than caller-controlled target fields. */
public class TagPathExtractor implements AuthZResourceExtractor {
    private final TagDAO tagDAO;
    private final TagResourceResolver resolver;

    public TagPathExtractor(ServiceContext context) {
        tagDAO = context.getTagDAO();
        resolver = new TagResourceResolver(context);
    }

    @Override
    public AuthZResource extractResource(ContainerRequestContext requestContext)
            throws ExtractionException {
        String id = requestContext.getUriInfo().getPathParameters().getFirst("id");
        if (StringUtils.isBlank(id)) {
            throw new ExtractionException("Failed to extract tag ID");
        }
        TagBean tag;
        try {
            tag = tagDAO.getById(id);
        } catch (Exception e) {
            throw new ExtractionException("Failed to get tag", e);
        }
        if (tag == null) {
            throw new NotFoundException(String.format("Tag %s does not exist.", id));
        }
        return resolver.resolve(tag, true);
    }
}
