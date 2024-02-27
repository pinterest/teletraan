package com.pinterest.teletraan.security;

import java.io.InputStream;

import javax.ws.rs.container.ContainerRequestContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pinterest.deployservice.bean.DeployBean;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.HotfixBean;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;

public class EnvStageBodyExtractor implements AuthZResourceExtractor {
    private final EnvironDAO environDAO;

    public EnvStageBodyExtractor(TeletraanServiceContext context) {
        this.environDAO = context.getEnvironDAO();
    }

    @Override
    public AuthZResource extractResource(ContainerRequestContext requestContext, Class<?> beanClass)
            throws RuntimeException {
        InputStream inputStream = requestContext.getEntityStream();
        if (beanClass.equals(EnvironBean.class)) {
            try {
                EnvironBean envBean = new ObjectMapper().readValue(inputStream, EnvironBean.class);
                return new AuthZResource(envBean.getEnv_name(), envBean.getStage_name());
            } catch (Exception e) {
                throw new ExtractionFailedException(beanClass, e);
            }
        }

        if (beanClass.equals(DeployBean.class)) {
            try {
                DeployBean deployBean = new ObjectMapper().readValue(inputStream, DeployBean.class);
                EnvironBean envBean = environDAO.getById(deployBean.getEnv_id());
                return new AuthZResource(envBean.getEnv_name(), envBean.getStage_name());
            } catch (Exception e) {
                throw new ExtractionFailedException(beanClass, e);
            }
        }

        if (beanClass.equals(HotfixBean.class)) {
            try {
                HotfixBean hotfixBean = new ObjectMapper().readValue(inputStream, HotfixBean.class);
                return new AuthZResource(hotfixBean.getEnv_name(), "");
            } catch (Exception e) {
                throw new ExtractionFailedException(beanClass, e);
            }
        }
        throw new UnsupportedOperationException("Failed to extract environment resource");
    }

    @Override
    public AuthZResource extractResource(ContainerRequestContext requestContext) throws RuntimeException {
        throw new UnsupportedOperationException("Unimplemented method 'extractResource(ContainerRequestContext)'");
    }

    class ExtractionFailedException extends RuntimeException {
        public ExtractionFailedException(Class<?> beanClass, Throwable cause) {
            super(String.format("failed to extract as %s", beanClass.getName()), cause);
        }
    }
}
