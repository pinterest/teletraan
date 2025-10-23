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
        return Response.status(status)
                .entity(exception.getMessage())
                .type(mediaType)
                .build();
    }
}

