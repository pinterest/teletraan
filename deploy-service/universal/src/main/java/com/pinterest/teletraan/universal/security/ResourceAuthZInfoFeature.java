/**
 * Copyright (c) 2024, Pinterest Inc. All rights reserved.
 */
package com.pinterest.teletraan.universal.security;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;

/**
 * This class is used to register the ResourceAuthZInfoFilter when the ResourceAuthZInfo annotation is
 * present on a resource method.
 */
public class ResourceAuthZInfoFeature implements DynamicFeature {
  @Override
  public void configure(ResourceInfo resourceInfo, FeatureContext context) {
    if (resourceInfo.getResourceMethod().getAnnotation(ResourceAuthZInfo.class) != null) {
      context.register(ResourceAuthZInfoFilter.class);
    }
  }
}
