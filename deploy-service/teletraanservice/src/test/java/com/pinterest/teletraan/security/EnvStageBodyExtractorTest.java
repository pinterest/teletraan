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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.AgentBean;
import com.pinterest.deployservice.bean.DeployBean;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.HotfixBean;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.HotfixDAO;
import com.pinterest.teletraan.fixture.EnvironBeanFixture;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor.BeanClassExtractionException;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor.ExtractionException;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.container.ContainerRequestContext;

import org.glassfish.jersey.server.ContainerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class EnvStageBodyExtractorTest {

    private ContainerRequestContext context;
    private EnvStageBodyExtractor sut;
    private EnvironDAO environDAO;
    private HotfixDAO hotfixDAO;
    private InputStream inputStream;
    private ObjectMapper objectMapper = new ObjectMapper();
    private static final Class<?>[] BEAN_CLASSES = {
        EnvironBean.class, HotfixBean.class, DeployBean.class, AgentBean.class
    };

    @BeforeEach
    void setUp() {
        environDAO = mock(EnvironDAO.class);
        hotfixDAO = mock(HotfixDAO.class);
        context = mock(ContainerRequest.class);

        ServiceContext serviceContext = new ServiceContext();
        serviceContext.setEnvironDAO(environDAO);
        serviceContext.setHotfixDAO(hotfixDAO);
        sut = new EnvStageBodyExtractor(serviceContext);
    }

    @Test
    void testExtractResource_nothing_exception() throws Exception {
        inputStream = mock(InputStream.class);
        when(context.getEntityStream()).thenReturn(inputStream);

        assertThrows(ExtractionException.class, () -> sut.extractResource(context));
    }

    @ParameterizedTest
    @MethodSource("getSupportedClassed")
    void testExtractResource_specificBeanClass_emptyStream(Class<?> beanClass) throws Exception {
        inputStream = mock(InputStream.class);
        when(context.getEntityStream()).thenReturn(inputStream);

        assertThrows(
                BeanClassExtractionException.class, () -> sut.extractResource(context, beanClass));
    }

    @ParameterizedTest
    @MethodSource("getSupportedClassed")
    void testExtractResource_environBean_success(Class<?> beanClass) throws Exception {
        EnvironBean envBean = EnvironBeanFixture.createRandomEnvironBean();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        objectMapper.writeValue(out, envBean);
        inputStream = new ByteArrayInputStream(out.toByteArray());

        when(context.getEntityStream()).thenReturn(inputStream);

        if (beanClass.equals(EnvironBean.class)) {
            AuthZResource resource = sut.extractResource(context, EnvironBean.class);
            assertTrue(resource.getName().contains(envBean.getEnv_name()));
            assertTrue(resource.getName().contains(envBean.getStage_name()));
            assertEquals(AuthZResource.Type.ENV_STAGE, resource.getType());
        } else {
            assertThrows(
                    BeanClassExtractionException.class,
                    () -> sut.extractResource(context, beanClass));
        }
    }

    @ParameterizedTest
    @MethodSource("getSupportedClassed")
    void testExtractResource_hotFixBean_success(Class<?> beanClass) throws Exception {
        HotfixBean hotfixBean = new HotfixBean();
        hotfixBean.setEnv_name("hotfix_env");
        hotfixBean.setId("hotfix_id");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        objectMapper.writeValue(out, hotfixBean);
        inputStream = new ByteArrayInputStream(out.toByteArray());

        when(context.getEntityStream()).thenReturn(inputStream);

        if (beanClass.equals(HotfixBean.class)) {
            when(hotfixDAO.getByHotfixId(hotfixBean.getId())).thenReturn(hotfixBean);

            AuthZResource resource = sut.extractResource(context, HotfixBean.class);
            assertTrue(resource.getName().contains(hotfixBean.getEnv_name()));
            assertEquals(AuthZResource.Type.ENV_STAGE, resource.getType());
        } else {
            assertThrows(
                    BeanClassExtractionException.class,
                    () -> sut.extractResource(context, beanClass));
        }
    }

    @ParameterizedTest
    @MethodSource("getSupportedClassed")
    void testExtractResource_deployBean_success(Class<?> beanClass) throws Exception {
        EnvironBean envBean = EnvironBeanFixture.createRandomEnvironBean();
        DeployBean deployBean = new DeployBean();
        String envId = "env_id";
        deployBean.setEnv_id(envId);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        objectMapper.writeValue(out, deployBean);
        inputStream = new ByteArrayInputStream(out.toByteArray());

        when(context.getEntityStream()).thenReturn(inputStream);

        if (beanClass.equals(DeployBean.class)) {
            when(environDAO.getById(envId)).thenReturn(null);
            assertThrows(NotFoundException.class, () -> sut.extractResource(context, DeployBean.class));

            inputStream = new ByteArrayInputStream(out.toByteArray());
            when(environDAO.getById(envId)).thenReturn(envBean);
            when(context.getEntityStream()).thenReturn(inputStream);

            AuthZResource resource = sut.extractResource(context, DeployBean.class);
            assertTrue(resource.getName().contains(envBean.getEnv_name()));
            assertTrue(resource.getName().contains(envBean.getStage_name()));
            assertEquals(AuthZResource.Type.ENV_STAGE, resource.getType());
        } else {
            assertThrows(
                    BeanClassExtractionException.class,
                    () -> sut.extractResource(context, beanClass));
        }
    }

    @ParameterizedTest
    @MethodSource("getSupportedClassed")
    void testExtractResource_agentBean_success(Class<?> beanClass) throws Exception {
        EnvironBean envBean = EnvironBeanFixture.createRandomEnvironBean();
        AgentBean agentBean = new AgentBean();
        String envId = "env_id";
        agentBean.setEnv_id(envId);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        objectMapper.writeValue(out, agentBean);
        inputStream = new ByteArrayInputStream(out.toByteArray());

        when(context.getEntityStream()).thenReturn(inputStream);

        if (beanClass.equals(AgentBean.class)) {
            when(environDAO.getById(envId)).thenReturn(null);
            assertThrows(NotFoundException.class, () -> sut.extractResource(context, AgentBean.class));

            inputStream = new ByteArrayInputStream(out.toByteArray());
            when(environDAO.getById(envId)).thenReturn(envBean);
            when(context.getEntityStream()).thenReturn(inputStream);

            AuthZResource resource = sut.extractResource(context, AgentBean.class);
            assertTrue(resource.getName().contains(envBean.getEnv_name()));
            assertTrue(resource.getName().contains(envBean.getStage_name()));
            assertEquals(AuthZResource.Type.ENV_STAGE, resource.getType());
        } else {
            assertThrows(
                    BeanClassExtractionException.class,
                    () -> sut.extractResource(context, beanClass));
        }
    }

    static Class<?>[] getSupportedClassed() {
        return BEAN_CLASSES;
    }
}
