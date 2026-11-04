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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.TagBean;
import com.pinterest.deployservice.bean.TagTargetType;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor.BeanClassExtractionException;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.glassfish.jersey.server.ContainerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TagBodyExtractorTest {
    private TagBodyExtractor extractor;
    private ContainerRequest request;
    private AtomicReference<InputStream> entityStream;

    @BeforeEach
    void setUp() throws Exception {
        ServiceContext context = new ServiceContext();
        EnvironDAO environDAO = mock(EnvironDAO.class);
        context.setEnvironDAO(environDAO);
        EnvironBean environment = new EnvironBean();
        environment.setEnv_name("target-env");
        environment.setStage_name("prod");
        when(environDAO.getById("env-id")).thenReturn(environment);
        extractor = new TagBodyExtractor(context);
        request = mock(ContainerRequest.class);
        entityStream = new AtomicReference<>();
        when(request.getEntityStream()).thenAnswer(call -> entityStream.get());
        doAnswer(
                        call -> {
                            entityStream.set(call.getArgument(0));
                            return null;
                        })
                .when(request)
                .setEntityStream(any(InputStream.class));
    }

    @Test
    void bodyIsPreservedForAnotherAuthorizerAndResourceDeserialization() throws Exception {
        String body =
                "{\"targetType\":\"ENVIRONMENT\",\"targetId\":\"env-id\",\"value\":\"DISABLE_ENV\"}";
        entityStream.set(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));

        AuthZResource first = extractor.extractResource(request);
        AuthZResource second = extractor.extractResource(request);
        TagBean resourceBody =
                new ObjectMapper().readValue(request.getEntityStream(), TagBean.class);

        assertEquals("target-env/prod", first.getName());
        assertEquals(first, second);
        assertEquals(TagTargetType.ENVIRONMENT, resourceBody.getTarget_type());
        assertEquals("env-id", resourceBody.getTarget_id());
        verify(request, times(2)).bufferEntity();
    }

    @Test
    void malformedJsonAndUnknownTargetTypesAreClientErrors() {
        for (String body :
                new String[] {
                    "{invalid}", "{\"targetType\":\"UNKNOWN\",\"targetId\":\"env-id\"}"
                }) {
            entityStream.set(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
            assertThrows(
                    BeanClassExtractionException.class, () -> extractor.extractResource(request));
        }
    }
}
