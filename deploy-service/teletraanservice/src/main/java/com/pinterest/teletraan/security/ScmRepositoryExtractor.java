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
import com.pinterest.deployservice.scm.SourceControlManagerProxy;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfo;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import java.util.List;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;

/** Extracts the exact SCM repository whose contents the commit APIs request. */
public class ScmRepositoryExtractor implements AuthZResourceExtractor {
    private final ServiceContext serviceContext;
    private final ResourceAuthZInfo.Location location;

    public ScmRepositoryExtractor(
            ServiceContext serviceContext, ResourceAuthZInfo.Location location) {
        if (location != ResourceAuthZInfo.Location.PATH
                && location != ResourceAuthZInfo.Location.QUERY) {
            throw new IllegalArgumentException("SCM repositories require PATH or QUERY parameters");
        }
        this.serviceContext = serviceContext;
        this.location = location;
    }

    @Override
    public AuthZResource extractResource(ContainerRequestContext requestContext)
            throws ExtractionException {
        MultivaluedMap<String, String> parameters =
                location == ResourceAuthZInfo.Location.PATH
                        ? requestContext.getUriInfo().getPathParameters()
                        : requestContext.getUriInfo().getQueryParameters();
        String repo = singleParameter(parameters, "repo");
        if (repo == null || repo.isEmpty()) {
            throw new BadRequestException("A repository is required");
        }
        if (location == ResourceAuthZInfo.Location.PATH) {
            // Match the path endpoint's legacy handling after Jersey decodes path parameters.
            repo = repo.replace("%2F", "/");
        }

        // The factory is initialized before the SCM proxy during service startup.
        SourceControlManagerProxy proxy = serviceContext.getSourceControlManagerProxy();
        if (proxy == null) {
            throw new ExtractionException("Source control manager is not configured");
        }
        String scm = singleParameter(parameters, "scm");
        return new AuthZResource(
                proxy.getRepositoryResourceName(scm, repo), AuthZResource.Type.SCM_REPOSITORY);
    }

    private static String singleParameter(MultivaluedMap<String, String> parameters, String name) {
        List<String> values = parameters.get(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        if (values.size() != 1) {
            throw new BadRequestException("Expected a single " + name + " parameter");
        }
        return values.get(0);
    }
}
