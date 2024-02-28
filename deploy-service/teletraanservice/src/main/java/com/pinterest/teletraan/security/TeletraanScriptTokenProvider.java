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

import com.pinterest.deployservice.bean.TokenRolesBean;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.universal.security.ScriptTokenProvider;
import com.pinterest.teletraan.universal.security.bean.ServicePrincipal;
import com.pinterest.teletraan.universal.security.bean.ValueBasedRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class TeletraanScriptTokenProvider implements ScriptTokenProvider<ValueBasedRole> {
    private static final Logger LOG = LoggerFactory.getLogger(TeletraanScriptTokenProvider.class);

    private TeletraanServiceContext context;

    public TeletraanScriptTokenProvider(TeletraanServiceContext context) {
        this.context = context;
    }

    @Override
    public ServicePrincipal<ValueBasedRole> getPrincipal(String token) {
        try {
            TokenRolesBean tokenRolesBean = context.getTokenRolesDAO().getByToken(token);

            if (tokenRolesBean != null) {
                return new ServicePrincipal<ValueBasedRole>(
                        tokenRolesBean.getScript_name(), tokenRolesBean.getRole().getRole(), null);
            }
        } catch (Exception e) {
            LOG.error("failed to get Script token principal", e);
        }
        return null;
    }
}
