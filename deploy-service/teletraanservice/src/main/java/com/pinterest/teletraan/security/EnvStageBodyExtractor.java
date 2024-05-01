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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.AgentBean;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.HotfixBean;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.HotfixDAO;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.container.ContainerRequestContext;

import org.glassfish.jersey.server.ContainerRequest;

public class EnvStageBodyExtractor implements AuthZResourceExtractor {
    private final EnvironDAO environDAO;
    private final HotfixDAO hotfixDAO;

    public EnvStageBodyExtractor(ServiceContext context) {
        this.environDAO = context.getEnvironDAO();
        this.hotfixDAO = context.getHotfixDAO();
    }

    @Override
    public AuthZResource extractResource(ContainerRequestContext requestContext, Class<?> beanClass)
            throws ExtractionException {
        return extractResource(requestContext, beanClass, false);
    }

    @Override
    public AuthZResource extractResource(ContainerRequestContext requestContext)
            throws ExtractionException {
        return extractResource(requestContext, Object.class, true);
    }

    AuthZResource extractResource(
            ContainerRequestContext requestContext, Class<?> beanClass, boolean tryAll)
            throws ExtractionException {
        ContainerRequest request = (ContainerRequest) requestContext;
        request.bufferEntity();
        InputStream inputStream = request.getEntityStream();
        if (EnvironBean.class.equals(beanClass) || tryAll) {
            try {
                return extractEnvironResource(inputStream);
            } catch (IOException e) {
                if (!tryAll) {
                    throw new BeanClassExtractionException(beanClass, e);
                }
            }
        }

        if (HotfixBean.class.equals(beanClass) || tryAll) {
            try {
                return extractHotfixResource(inputStream);
            } catch (IOException e) {
                if (!tryAll) {
                    throw new BeanClassExtractionException(beanClass, e);
                }
            }
        }

        if (AgentBean.class.equals(beanClass) || tryAll) {
            try {
                return extractAgentResource(inputStream);
            } catch (Exception e) {
                if (!tryAll) {
                    if (e instanceof NotFoundException) {
                        throw (NotFoundException) e;
                    }
                    throw new BeanClassExtractionException(beanClass, e);
                }
            }
        }

        if (tryAll) {
            throw new ExtractionException(
                    "Failed to extract environment resource using all supported classes");
        }
        throw new UnsupportedOperationException("Failed to extract environment resource");
    }

    private AuthZResource extractEnvironResource(InputStream inputStream) throws IOException {
        EnvironBean envBean = new ObjectMapper().readValue(inputStream, EnvironBean.class);
        return new AuthZResource(envBean.getEnv_name(), envBean.getStage_name());
    }

    private AuthZResource extractHotfixResource(InputStream inputStream) throws IOException, ExtractionException {
        HotfixBean hotfixBean = new ObjectMapper().readValue(inputStream, HotfixBean.class);
        String hotfixId = hotfixBean.getId();
        try {
            hotfixBean = hotfixDAO.getByHotfixId(hotfixId);
        } catch (Exception e) {
            throw new ExtractionException("Failed to get hotfix bean", e);
        }
        if (hotfixBean == null) {
            throw new NotFoundException(String.format("Hotfix(%s) not found", hotfixId));
        }
        return new AuthZResource(hotfixBean.getEnv_name(), "");
    }

    private AuthZResource extractAgentResource(InputStream inputStream) throws IOException, ExtractionException, NotFoundException {
        AgentBean agentBean = new ObjectMapper().readValue(inputStream, AgentBean.class);
        EnvironBean envBean;
        try {
            envBean = environDAO.getById(agentBean.getEnv_id());
        } catch (Exception e) {
            throw new ExtractionException("Failed to get environment bean", e);
        }

        if (envBean == null) {
            throw new NotFoundException(String.format("Environment(%s) not found, referenced by agent(%s)",
                    agentBean.getEnv_id(), agentBean.getHost_name()));
        }
        return new AuthZResource(envBean.getEnv_name(), envBean.getStage_name());
    }
}
