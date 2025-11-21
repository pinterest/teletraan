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

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfo;

public class TeletraanAuthZResourceExtractorFactory implements AuthZResourceExtractor.Factory {

    private static final AuthZResourceExtractor BUILD_BODY_EXTRACTOR = new BuildBodyExtractor();
    private static final AuthZResourceExtractor ENV_PATH_EXTRACTOR = new EnvPathExtractor();
    private static final AuthZResourceExtractor ENV_STAGE_BODY_EXTRACTOR =
            new EnvStageBodyExtractor();
    private static final AuthZResourceExtractor ENV_STAGE_PATH_EXTRACTOR =
            new EnvStagePathExtractor();
    private static final AuthZResourceExtractor SOX_PROPERTY_PATH_EXTRACTOR =
            new SoxPropertyPathExtractor();
    private static final AuthZResourceExtractor HOTFIX_BODY_EXTRACTOR = new HotfixBodyExtractor();
    private static final AuthZResourceExtractor DEPLOY_SCHEDULE_PATH_EXTRACTOR = new DeploySchedulePathExtractor();
    private final AuthZResourceExtractor buildPathExtractor;
    private final AuthZResourceExtractor deployPathExtractor;
    private final AuthZResourceExtractor hostPathExtractor;
    private final AuthZResourceExtractor hotfixPathExtractor;

    public TeletraanAuthZResourceExtractorFactory(ServiceContext serviceContext) {
        buildPathExtractor = new BuildPathExtractor(serviceContext);
        deployPathExtractor = new DeployPathExtractor(serviceContext);
        hotfixPathExtractor = new HotfixPathExtractor(serviceContext);
        hostPathExtractor = new HostPathExtractor(serviceContext);
    }

    @Override
    public AuthZResourceExtractor create(ResourceAuthZInfo authZInfo) {
        switch (authZInfo.idLocation()) {
            case PATH:
                switch (authZInfo.type()) {
                    case ENV:
                        return ENV_PATH_EXTRACTOR;
                    case ENV_STAGE:
                        return ENV_STAGE_PATH_EXTRACTOR;
                    case BUILD:
                        return buildPathExtractor;
                    case DEPLOY:
                        return deployPathExtractor;
                    case HOTFIX:
                        return hotfixPathExtractor;
                    case HOST:
                        return hostPathExtractor;
                    case SOX_PROPERTY:
                        return SOX_PROPERTY_PATH_EXTRACTOR;
                    case DEPLOY_SCHEDULE:
                        return DEPLOY_SCHEDULE_PATH_EXTRACTOR;
                    default:
                        throw new UnsupportedResourceInfoException(authZInfo);
                }
            case BODY:
                switch (authZInfo.type()) {
                    case BUILD:
                        return BUILD_BODY_EXTRACTOR;
                    case ENV_STAGE:
                        return ENV_STAGE_BODY_EXTRACTOR;
                    case HOTFIX:
                        return HOTFIX_BODY_EXTRACTOR;
                    default:
                        throw new UnsupportedResourceInfoException(authZInfo);
                }
            default:
                throw new IllegalArgumentException(
                        "Unsupported resource ID location: " + authZInfo.idLocation());
        }
    }

    class UnsupportedResourceInfoException extends IllegalArgumentException {
        public UnsupportedResourceInfoException(ResourceAuthZInfo authZInfo) {
            super(
                    String.format(
                            "Unsupported resource ID location %s for type %s",
                            authZInfo.idLocation(), authZInfo.type()));
        }
    }
}
