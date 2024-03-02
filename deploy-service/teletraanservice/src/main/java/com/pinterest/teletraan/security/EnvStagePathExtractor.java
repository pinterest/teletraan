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

import com.pinterest.teletraan.universal.security.AuthZResourceExtractor;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import javax.ws.rs.container.ContainerRequestContext;

public class EnvStagePathExtractor implements AuthZResourceExtractor {
    @Override
    public AuthZResource extractResource(ContainerRequestContext requestContext)
            throws ExtractionException {
        try {
            String envName = requestContext.getUriInfo().getPathParameters().getFirst("envName");
            String stageName =
                    requestContext.getUriInfo().getPathParameters().getFirst("stageName");
            return new AuthZResource(envName, stageName);
        } catch (Exception e) {
            throw new ExtractionException("Failed to extract environment resource", e);
        }
    }
}
