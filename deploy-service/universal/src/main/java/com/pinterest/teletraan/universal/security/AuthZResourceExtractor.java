/**
 * Copyright (c) 2024, Pinterest Inc. All rights reserved.
 */
package com.pinterest.teletraan.universal.security;

import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import javax.ws.rs.container.ContainerRequestContext;

public interface AuthZResourceExtractor {
  AuthZResource extractResource(ContainerRequestContext requestContext) throws ExtractionException;

  default AuthZResource extractResource(ContainerRequestContext requestContext, Class<?> beanClass)
      throws ExtractionException {
    return extractResource(requestContext);
  }

  public interface Factory {
    AuthZResourceExtractor create(ResourceAuthZInfo authZInfo);
  }

  public class ExtractionException extends RuntimeException {
    public ExtractionException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
