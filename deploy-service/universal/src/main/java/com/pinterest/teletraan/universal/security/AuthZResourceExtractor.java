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
import javax.ws.rs.container.ContainerRequestContext;

public interface AuthZResourceExtractor {
    AuthZResource extractResource(ContainerRequestContext requestContext)
            throws ExtractionException;

    default AuthZResource extractResource(
            ContainerRequestContext requestContext, Class<?> beanClass) throws ExtractionException {
        return extractResource(requestContext);
    }

    public interface Factory {
        AuthZResourceExtractor create(ResourceAuthZInfo authZInfo);
    }

    public class ExtractionException extends RuntimeException {
        public ExtractionException(String message, Throwable cause) {
            super(message, cause);
        }

        public ExtractionException(String message) {
            super(message);
        }
    }
}
