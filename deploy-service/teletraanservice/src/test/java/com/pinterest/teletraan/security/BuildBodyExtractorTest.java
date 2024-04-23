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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.container.ContainerRequestContext;

import org.glassfish.jersey.server.ContainerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pinterest.deployservice.bean.BuildBean;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor.ExtractionException;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;

class BuildBodyExtractorTest {
    private BuildBodyExtractor sut;
    private ContainerRequestContext requestContext;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        sut = new BuildBodyExtractor();
        requestContext = mock(ContainerRequest.class);
    }

    @Test
    void testExtractResource() throws ExtractionException, IOException {
        BuildBean buildBean = new BuildBean();
        buildBean.setBuild_name("test-build");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        objectMapper.writeValue(out, buildBean);
        InputStream inputStream = new ByteArrayInputStream(out.toByteArray());

        when(requestContext.getEntityStream()).thenReturn(inputStream);

        AuthZResource resource = sut.extractResource(requestContext);

        assertEquals("test-build", resource.getName());
        assertEquals(AuthZResource.Type.BUILD, resource.getType());
    }

    @Test
    void testExtractResourceWithInvalidInput() throws IOException {
        String invalidJson = "{ xyz }";
        InputStream inputStream = new ByteArrayInputStream(invalidJson.getBytes());

        when(requestContext.getEntityStream()).thenReturn(inputStream);

        assertThrows(ExtractionException.class, () -> sut.extractResource(requestContext));
    }
}