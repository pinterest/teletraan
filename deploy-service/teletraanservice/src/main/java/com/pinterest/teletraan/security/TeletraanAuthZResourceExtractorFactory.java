package com.pinterest.teletraan.security;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfo;

public class TeletraanAuthZResourceExtractorFactory implements AuthZResourceExtractor.Factory {

    private static final AuthZResourceExtractor ENV_PATH_EXTRACTOR = new EnvironmentPathExtractor();
    private static final AuthZResourceExtractor ENV_BODY_EXTRACTOR = new EnvironmentBodyExtractor();

    private final ServiceContext serviceContext;
    public TeletraanAuthZResourceExtractorFactory(ServiceContext serviceContext) {
        this.serviceContext = serviceContext;
    }

    @Override
    public AuthZResourceExtractor create(ResourceAuthZInfo authZInfo) {
        switch (authZInfo.type()) {
            case ENV:
            case ENV_STAGE:
                switch (authZInfo.IdLocation()) {
                    case PATH:
                        return ENV_PATH_EXTRACTOR;
                    case BODY:
                        return ENV_BODY_EXTRACTOR;
                    default:
                        throw new IllegalArgumentException("Unsupported resource ID location: " + authZInfo.IdLocation());
                }
            default:
                throw new IllegalArgumentException("Unsupported resource type: " + authZInfo.type());
        }
    }
}
