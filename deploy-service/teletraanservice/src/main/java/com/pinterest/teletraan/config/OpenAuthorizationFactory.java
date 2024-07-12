/**
 * Copyright (c) 2016-2024 Pinterest, Inc.
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

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.universal.security.TeletraanAuthorizer;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import com.pinterest.teletraan.universal.security.bean.TeletraanPrincipal;
import javax.annotation.Nullable;
import javax.ws.rs.container.ContainerRequestContext;

@JsonTypeName("open")
public class OpenAuthorizationFactory implements AuthorizationFactory {
    @Override
    public TeletraanAuthorizer<TeletraanPrincipal> create(TeletraanServiceContext context)
            throws Exception {
        return new TeletraanAuthorizer<TeletraanPrincipal>() {
            @Override
            public boolean authorize(TeletraanPrincipal principal, String resource) {
                return true;
            }

            @Override
            public boolean authorize(
                    TeletraanPrincipal principal,
                    String role,
                    AuthZResource requestedResource,
                    @Nullable ContainerRequestContext context) {
                return true;
            }
        };
    }
}
