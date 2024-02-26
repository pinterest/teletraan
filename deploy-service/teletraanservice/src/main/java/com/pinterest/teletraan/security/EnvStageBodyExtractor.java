package com.pinterest.teletraan.security;

import java.io.InputStream;

import javax.ws.rs.container.ContainerRequestContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.HotfixBean;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;

public class EnvStageBodyExtractor implements AuthZResourceExtractor {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(EnvStageBodyExtractor.class);

    @Override
    public AuthZResource extractResource(ContainerRequestContext requestContext) throws RuntimeException {
        InputStream inputStream = requestContext.getEntityStream();
        try {
            EnvironBean envBean = new ObjectMapper().readValue(inputStream, EnvironBean.class);
            return new AuthZResource(String.format("%s/%s", envBean.getEnv_name(), envBean.getStage_name()),
                    AuthZResource.Type.ENV_STAGE);
        } catch (Exception e) {
            LOG.error("Failed to extract as EnvironBean", e);
        }

        try {
            HotfixBean hotfixBean = new ObjectMapper().readValue(inputStream, HotfixBean.class);
            return new AuthZResource(String.format("%s/%s", hotfixBean.getEnv_name(), null), AuthZResource.Type.ENV);
        } catch (Exception e) {
            LOG.error("Failed to extract as DeployBean", e);
        }
        throw new RuntimeException("Failed to extract environment resource");
    }
}
