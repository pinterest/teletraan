package com.pinterest.deployservice.common;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class DeployInternalExceptionMapper extends BaseExceptionMapper<DeployInternalException> {
    public DeployInternalExceptionMapper() {
        super(Response.Status.BAD_REQUEST, "text/plain");
    }
}
