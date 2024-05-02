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

/**
 * This interface represents an AuthZResourceExtractor, which is responsible for extracting an
 * AuthZResource from a ContainerRequestContext.
 *
 * <p>Note that the extractor can map the input resource type to a different AuthZResource type. And
 * thus the authorization will be based on the mapped AuthZResource type. This can help reduce the
 * complexity of the authorization logic.
 */
public interface AuthZResourceExtractor {
    /**
     * Extracts AuthZResource from the requestContext.
     *
     * @param requestContext the ContainerRequestContext from which to extract the AuthZResource
     * @return the extracted AuthZResource
     * @throws ExtractionException if no resource can be extracted
     */
    AuthZResource extractResource(ContainerRequestContext requestContext)
            throws ExtractionException;

    /**
     * Extracts AuthZResource from the requestContext.
     *
     * @param requestContext the ContainerRequestContext from which to extract the AuthZResource
     * @param beanClass the class of the expected input resource bean
     * @return the extracted AuthZResource
     * @throws ExtractionException if no resource can be extracted
     */
    default AuthZResource extractResource(
            ContainerRequestContext requestContext, Class<?> beanClass) throws ExtractionException {
        return extractResource(requestContext);
    }

    /** This interface represents a Factory for creating AuthZResourceExtractors. */
    interface Factory {
        /**
         * Creates an AuthZResourceExtractor based on the given ResourceAuthZInfo.
         *
         * @param authZInfo the ResourceAuthZInfo used to create the AuthZResourceExtractor
         * @return the created AuthZResourceExtractor
         */
        AuthZResourceExtractor create(ResourceAuthZInfo authZInfo);
    }

    class ExtractionException extends Exception {
        public ExtractionException(String message, Throwable cause) {
            super(message, cause);
        }

        public ExtractionException(String message) {
            super(message);
        }
    }

    class BeanClassExtractionException extends ExtractionException {
        public BeanClassExtractionException(Class<?> beanClass, Throwable cause) {
            super(
                    String.format(
                            "failed to extract as %s. Check if request body is valid",
                            beanClass.getName()),
                    cause);
        }
    }
}
