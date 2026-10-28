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
import com.pinterest.deployservice.bean.BuildBean;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.TagBean;
import com.pinterest.deployservice.dao.BuildDAO;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor.ExtractionException;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import org.apache.commons.lang3.StringUtils;

/** Resolves a tag to the resource whose deploy state it can affect. */
class TagResourceResolver {
    private final BuildDAO buildDAO;
    private final EnvironDAO environDAO;

    TagResourceResolver(ServiceContext context) {
        buildDAO = context.getBuildDAO();
        environDAO = context.getEnvironDAO();
    }

    AuthZResource resolve(TagBean tag, boolean stored) throws ExtractionException {
        if (tag == null) {
            throw new BadRequestException("A tag is required");
        }
        if (tag.getTarget_type() == null || StringUtils.isBlank(tag.getTarget_id())) {
            if (stored) {
                // An existing malformed tag has no trustworthy target scope. Only a caller
                // authorized to delete system-wide may clean it up.
                return AuthZResource.SYSTEM_RESOURCE;
            }
            throw new BadRequestException("A tag target type and target ID are required");
        }
        switch (tag.getTarget_type()) {
            case BUILD:
                return buildResource(tag, stored);
            case ENVIRONMENT:
                return environmentResource(tag.getTarget_id(), stored);
            case TELETRAAN:
                return AuthZResource.SYSTEM_RESOURCE;
            default:
                throw new BadRequestException("Unsupported tag target type");
        }
    }

    private AuthZResource buildResource(TagBean tag, boolean stored) throws ExtractionException {
        BuildBean build;
        if (stored) {
            // BuildTagHandler stores the build name as target_id and the original build as
            // meta_info. Use that persisted snapshot when deleting, including after build expiry.
            try {
                build = tag.deserializeTagMetaInfo(BuildBean.class);
            } catch (Exception e) {
                return AuthZResource.SYSTEM_RESOURCE;
            }
            if (build == null
                    || StringUtils.isBlank(build.getBuild_name())
                    || !tag.getTarget_id().equals(build.getBuild_name())) {
                // Missing or inconsistent historical metadata must not prevent system-wide
                // administrators from deleting the tag, nor grant target-scoped access.
                return AuthZResource.SYSTEM_RESOURCE;
            }
        } else {
            try {
                build = buildDAO.getById(tag.getTarget_id());
            } catch (Exception e) {
                throw new ExtractionException("Failed to resolve tagged build", e);
            }
            if (build == null) {
                throw new NotFoundException("Tagged build not found");
            }
            if (StringUtils.isBlank(build.getBuild_name())) {
                throw new ExtractionException("Invalid tagged build metadata");
            }
        }
        Map<String, String> attributes = new HashMap<>();
        attributes.put(
                AuthZResource.AttributeKeys.BUILD_ARTIFACT_URL.name(), build.getArtifact_url());
        return new AuthZResource(build.getBuild_name(), AuthZResource.Type.BUILD, attributes);
    }

    private AuthZResource environmentResource(String targetId, boolean stored)
            throws ExtractionException {
        EnvironBean environment;
        try {
            environment = environDAO.getById(targetId);
        } catch (Exception e) {
            throw new ExtractionException("Failed to resolve tagged environment", e);
        }
        if (environment == null) {
            if (stored) {
                // The target may have been deleted before its historical tags were removed.
                return AuthZResource.SYSTEM_RESOURCE;
            }
            throw new NotFoundException("Tagged environment not found");
        }
        if (StringUtils.isBlank(environment.getEnv_name())
                || StringUtils.isBlank(environment.getStage_name())) {
            throw new ExtractionException("Invalid tagged environment");
        }
        return new AuthZResource(environment.getEnv_name(), environment.getStage_name());
    }
}
