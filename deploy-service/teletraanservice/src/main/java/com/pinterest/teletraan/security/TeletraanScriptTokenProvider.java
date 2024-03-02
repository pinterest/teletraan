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
import com.pinterest.deployservice.bean.TokenRolesBean;
import com.pinterest.teletraan.universal.security.ScriptTokenProvider;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import com.pinterest.teletraan.universal.security.bean.ScriptTokenPrincipal;
import com.pinterest.teletraan.universal.security.bean.ValueBasedRole;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TeletraanScriptTokenProvider implements ScriptTokenProvider<ValueBasedRole> {
    private static final Logger LOG = LoggerFactory.getLogger(TeletraanScriptTokenProvider.class);

    private ServiceContext context;

    public TeletraanScriptTokenProvider(ServiceContext context) {
        this.context = context;
    }

    @Override
    public Optional<ScriptTokenPrincipal<ValueBasedRole>> getPrincipal(String token) {
        try {
            TokenRolesBean tokenRolesBean = context.getTokenRolesDAO().getByToken(token);

            if (tokenRolesBean != null) {
                return Optional.of(new ScriptTokenPrincipal<ValueBasedRole>(
                        tokenRolesBean.getScript_name(), tokenRolesBean.getRole().getRole(),
                        new AuthZResource(tokenRolesBean.getResource_id(), tokenRolesBean.getResource_type())));
            }
        } catch (Exception e) {
            LOG.error("failed to get Script token principal", e);
        }
        return Optional.empty();
    }
}
