package com.pinterest.teletraan.security;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfo;

public class TeletraanAuthZResourceExtractorFactory implements AuthZResourceExtractor.Factory {

    private static final AuthZResourceExtractor ENV_PATH_EXTRACTOR = new EnvPathExtractor();
    private static final AuthZResourceExtractor ENV_STAGE_PATH_EXTRACTOR = new EnvStagePathExtractor();
    private static final AuthZResourceExtractor ENV_STAGE_BODY_EXTRACTOR = new EnvStageBodyExtractor();

    private final ServiceContext serviceContext;
    public TeletraanAuthZResourceExtractorFactory(ServiceContext serviceContext) {
        this.serviceContext = serviceContext;
    }

    @Override
    public AuthZResourceExtractor create(ResourceAuthZInfo authZInfo) {
        switch (authZInfo.type()) {
            case ENV:
                switch (authZInfo.IdLocation()) {
                    case PATH:
                        return ENV_PATH_EXTRACTOR;
                    default:
                        throw new UnsupportedResourceIDLocationException(authZInfo);
                }
            case ENV_STAGE:
                switch (authZInfo.IdLocation()) {
                    case PATH:
                        return ENV_STAGE_PATH_EXTRACTOR;
                    case BODY:
                        return ENV_STAGE_BODY_EXTRACTOR;
                    default:
                        throw new UnsupportedResourceIDLocationException(authZInfo);
                }
            default:
                throw new IllegalArgumentException("Unsupported resource type: " + authZInfo.type());
        }
    }

    class UnsupportedResourceIDLocationException extends RuntimeException {
        public UnsupportedResourceIDLocationException(ResourceAuthZInfo authZInfo) {
            super("Unsupported resource ID location: " + authZInfo.IdLocation());
        }
    }
}
