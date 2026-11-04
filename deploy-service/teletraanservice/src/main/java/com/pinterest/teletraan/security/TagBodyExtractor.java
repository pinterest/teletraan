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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.TagBean;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import io.dropwizard.jackson.Jackson;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.server.ContainerRequest;

/** Authorizes tag creation against the target, rather than client-supplied tag metadata. */
public class TagBodyExtractor implements AuthZResourceExtractor {
    // Match the resource parser's enum casing and unknown-field behavior.
    private static final ObjectMapper MAPPER = Jackson.newObjectMapper();
    private final TagResourceResolver resolver;

    public TagBodyExtractor(ServiceContext context) {
        resolver = new TagResourceResolver(context);
    }

    @Override
    public AuthZResource extractResource(ContainerRequestContext requestContext)
            throws ExtractionException {
        ContainerRequest request = (ContainerRequest) requestContext;
        request.bufferEntity();
        TagBean tag;
        try {
            byte[] body = IOUtils.toByteArray(request.getEntityStream());
            // Authorization runs before resource deserialization and may run more than once.
            request.setEntityStream(new ByteArrayInputStream(body));
            tag = MAPPER.readValue(body, TagBean.class);
        } catch (IOException e) {
            throw new BeanClassExtractionException(TagBean.class, e);
        }
        return resolver.resolve(tag, false);
    }
}
