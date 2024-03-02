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

import javax.ws.rs.container.ContainerRequestContext;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfo;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;

public class TeletraanAuthZResourceExtractorFactory implements AuthZResourceExtractor.Factory {

    private static final AuthZResourceExtractor ENV_PATH_EXTRACTOR = new EnvPathExtractor();
    private static final AuthZResourceExtractor ENV_STAGE_PATH_EXTRACTOR =
            new EnvStagePathExtractor();
    private final AuthZResourceExtractor ENV_STAGE_BODY_EXTRACTOR;

    public TeletraanAuthZResourceExtractorFactory(ServiceContext serviceContext) {
        ENV_STAGE_BODY_EXTRACTOR = new EnvStageBodyExtractor(serviceContext);
    }

    @Override
    public AuthZResourceExtractor create(ResourceAuthZInfo authZInfo) {
        switch (authZInfo.type()) {
            case ENV:
                switch (authZInfo.idLocation()) {
                    case PATH:
                        return ENV_PATH_EXTRACTOR;
                    default:
                        throw new UnsupportedResourceIDLocationException(authZInfo);
                }
            case ENV_STAGE:
                switch (authZInfo.idLocation()) {
                    case PATH:
                        return ENV_STAGE_PATH_EXTRACTOR;
                    case BODY:
                        return ENV_STAGE_BODY_EXTRACTOR;
                    default:
                        throw new UnsupportedResourceIDLocationException(authZInfo);
                }
            case RATINGS:
                return new AuthZResourceExtractor() {
                    @Override
                    public AuthZResource extractResource(ContainerRequestContext requestContext)
                            throws ExtractionException {
                        return new AuthZResource("rating", AuthZResource.Type.RATINGS);
                    }

                };
            default:
                throw new IllegalArgumentException(
                        "Unsupported resource type: " + authZInfo.type());
        }
    }

    class UnsupportedResourceIDLocationException extends IllegalArgumentException {
        public UnsupportedResourceIDLocationException(ResourceAuthZInfo authZInfo) {
            super(String.format("Unsupported resource ID location %s for type %s", authZInfo.idLocation(), authZInfo.type()));
        }
    }
}
