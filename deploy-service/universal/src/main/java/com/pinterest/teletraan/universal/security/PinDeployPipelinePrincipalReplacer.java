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
package com.pinterest.teletraan.universal.security;

import com.google.common.collect.ImmutableList;
import com.pinterest.teletraan.universal.security.bean.EnvoyCredentials;
import com.pinterest.teletraan.universal.security.bean.ServicePrincipal;
import com.pinterest.teletraan.universal.security.bean.TeletraanPrincipal;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@RequiredArgsConstructor
public class PinDeployPipelinePrincipalReplacer implements PrincipalReplacer {
    private final ImmutableList<String> pinDeploySpiffeIds;

    @Override
    public TeletraanPrincipal replace(TeletraanPrincipal principal, EnvoyCredentials credentials) {
        if (principal != null && ServicePrincipal.class.isAssignableFrom(principal.getClass())) {
            ServicePrincipal servicePrincipal = (ServicePrincipal) principal;
            if (pinDeploySpiffeIds.contains(servicePrincipal.getName())
                    && StringUtils.isNotEmpty(credentials.getPipelineId())) {
                return new ServicePrincipal(credentials.getPipelineId());
            }
        }
        return principal;
    }
}
