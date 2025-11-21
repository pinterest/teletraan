package com.pinterest.teletraan.security;

import com.pinterest.teletraan.universal.security.AuthZResourceExtractor;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;

import javax.ws.rs.container.ContainerRequestContext;

public class DeploySchedulePathExtractor implements AuthZResourceExtractor {
  private static final String ENV_NAME = "envName";
  private static final String STAGE_NAME = "stageName";

  @Override
  public AuthZResource extractResource(ContainerRequestContext requestContext)
    throws AuthZResourceExtractor.ExtractionException {
    String envName = requestContext.getUriInfo().getPathParameters().getFirst(ENV_NAME);
    String stageName = requestContext.getUriInfo().getPathParameters().getFirst(STAGE_NAME);
    if (envName == null) {
      throw new ExtractionException("Failed to extract environment resource");
    }
    return new AuthZResource(
      String.format("%s/%s", envName, stageName), AuthZResource.Type.DEPLOY_SCHEDULE);
  }
}
