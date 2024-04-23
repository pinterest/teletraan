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

import java.io.InputStream;

import javax.ws.rs.container.ContainerRequestContext;

import org.glassfish.jersey.server.ContainerRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pinterest.deployservice.bean.BuildBean;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;

public class BuildBodyExtractor implements AuthZResourceExtractor {
    @Override
    public AuthZResource extractResource(ContainerRequestContext requestContext)
            throws ExtractionException {
        ContainerRequest request = (ContainerRequest) requestContext;
        request.bufferEntity();
        InputStream inputStream = request.getEntityStream();
        try {
            BuildBean buildBean = new ObjectMapper().readValue(inputStream, BuildBean.class);
            return new AuthZResource(buildBean.getBuild_name(), AuthZResource.Type.BUILD);
        } catch (Exception e) {
            throw new BeanClassExtractionException(BuildBean.class, e);
        }
    }
}
