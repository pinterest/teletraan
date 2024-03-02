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
package com.pinterest.teletraan.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.security.ScriptTokenRoleAuthorizer;
import com.pinterest.teletraan.security.UserRoleAuthorizer;
import com.pinterest.teletraan.universal.security.BasePastisAuthorizer;
import com.pinterest.teletraan.universal.security.bean.ServicePrincipal;
import com.pinterest.teletraan.universal.security.bean.TeletraanPrincipal;
import com.pinterest.teletraan.universal.security.bean.UserPrincipal;
import io.dropwizard.auth.Authorizer;

@JsonTypeName("composite")
public class CompositeAuthorizationFactory implements AuthorizationFactory {
    private static final String DEFAULT_PASTIS_SERVICE_NAME = "teletraan_dev";

    @JsonProperty
    private String pastisServiceName = DEFAULT_PASTIS_SERVICE_NAME;

    public void setPastisServiceName(String pastisServiceName) {
        this.pastisServiceName = pastisServiceName;
    }

    public String getPastisServiceName() {
        return pastisServiceName;
    }

    @Override
    public <P extends TeletraanPrincipal> Authorizer<P> create(TeletraanServiceContext context)
            throws Exception {
        return (Authorizer<P>) BasePastisAuthorizer.builder().factory(context.getAuthZResourceExtractorFactory())
                .serviceName(pastisServiceName).build();
    }

    @Override
    public <P extends TeletraanPrincipal> Authorizer<? extends TeletraanPrincipal> create(
            TeletraanServiceContext context, Class<P> principalClass) throws Exception {
        if (principalClass.equals(ServicePrincipal.class)) {
            return new ScriptTokenRoleAuthorizer(context.getAuthZResourceExtractorFactory());
        } else if (principalClass.equals(UserPrincipal.class)) {
            return new UserRoleAuthorizer(context, context.getAuthZResourceExtractorFactory());
        }
        return create(context);
    }
}
