/**
 * Copyright (c) 2025 Pinterest, Inc.
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
package com.pinterest.deployservice.common;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public abstract class BaseExceptionMapper<T extends Throwable> implements ExceptionMapper<T> {
    private final Response.Status status;
    private final String mediaType;

    protected BaseExceptionMapper(Response.Status status, String mediaType) {
        this.status = status;
        this.mediaType = mediaType;
    }

    @Override
    public Response toResponse(T exception) {
        return Response.status(status).entity(exception.getMessage()).type(mediaType).build();
    }
}
