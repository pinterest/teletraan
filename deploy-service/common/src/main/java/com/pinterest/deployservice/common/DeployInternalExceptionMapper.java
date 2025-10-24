package com.pinterest.deployservice.common;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class DeployInternalExceptionMapper extends BaseExceptionMapper<DeployInternalException> {
    public DeployInternalExceptionMapper() {
        super(Response.Status.BAD_REQUEST, MediaType.APPLICATION_JSON);
    }

    @Override
    public Response toResponse(DeployInternalException exception) {
        ErrorResponse error = new ErrorResponse(Response.Status.BAD_REQUEST.getStatusCode(), exception.getMessage());
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
