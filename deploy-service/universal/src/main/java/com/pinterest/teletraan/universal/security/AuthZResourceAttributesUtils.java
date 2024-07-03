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

import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.container.ContainerRequestContext;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class AuthZResourceAttributesUtils {
    @SuppressWarnings("unchecked")
    public static Map<String, String> insertAuthZResourceAttributes(
            ContainerRequestContext requestContext, AuthZResource.AttributeKeys key, String value) {
        if (requestContext == null) {
            return new HashMap<>();
        }

        Object attributes = requestContext.getProperty(Constants.AUTHZ_ATTR_REQ_CXT_KEY);
        Map<String, String> attributesMap;
        if (attributes instanceof Map<?, ?>) {
            attributesMap = (Map<String, String>) attributes;
            attributesMap.put(key.name(), value);
        } else {
            attributesMap = new HashMap<>();
            attributesMap.put(key.name(), value);
            requestContext.setProperty(Constants.AUTHZ_ATTR_REQ_CXT_KEY, attributesMap);
        }
        return attributesMap;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> getAuthZResourceAttributes(
            ContainerRequestContext requestContext) {
        Object attributes = requestContext.getProperty(Constants.AUTHZ_ATTR_REQ_CXT_KEY);
        if (attributes instanceof Map<?, ?>) {
            return (Map<String, String>) attributes;
        }
        return new HashMap<>();
    }
}
