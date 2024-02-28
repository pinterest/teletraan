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
import com.pinterest.deployservice.bean.DeployBean;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.HotfixBean;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import java.io.InputStream;
import javax.ws.rs.container.ContainerRequestContext;

public class EnvStageBodyExtractor implements AuthZResourceExtractor {
    private final EnvironDAO environDAO;

    public EnvStageBodyExtractor(TeletraanServiceContext context) {
        this.environDAO = context.getEnvironDAO();
    }

    @Override
    public AuthZResource extractResource(ContainerRequestContext requestContext, Class<?> beanClass)
            throws ExtractionException {
        InputStream inputStream = requestContext.getEntityStream();
        if (beanClass.equals(EnvironBean.class)) {
            try {
                EnvironBean envBean = new ObjectMapper().readValue(inputStream, EnvironBean.class);
                return new AuthZResource(envBean.getEnv_name(), envBean.getStage_name());
            } catch (Exception e) {
                throw new BeanClassExtractionException(beanClass, e);
            }
        }

        if (beanClass.equals(DeployBean.class)) {
            try {
                DeployBean deployBean = new ObjectMapper().readValue(inputStream, DeployBean.class);
                EnvironBean envBean = environDAO.getById(deployBean.getEnv_id());
                return new AuthZResource(envBean.getEnv_name(), envBean.getStage_name());
            } catch (Exception e) {
                throw new BeanClassExtractionException(beanClass, e);
            }
        }

        if (beanClass.equals(HotfixBean.class)) {
            try {
                HotfixBean hotfixBean = new ObjectMapper().readValue(inputStream, HotfixBean.class);
                return new AuthZResource(hotfixBean.getEnv_name(), "");
            } catch (Exception e) {
                throw new BeanClassExtractionException(beanClass, e);
            }
        }
        throw new UnsupportedOperationException("Failed to extract environment resource");
    }

    @Override
    public AuthZResource extractResource(ContainerRequestContext requestContext)
            throws ExtractionException {
        throw new UnsupportedOperationException(
                "Unimplemented method 'extractResource(ContainerRequestContext)'");
    }

    class BeanClassExtractionException extends ExtractionException {
        public BeanClassExtractionException(Class<?> beanClass, Throwable cause) {
            super(String.format("failed to extract as %s", beanClass.getName()), cause);
        }
    }
}
