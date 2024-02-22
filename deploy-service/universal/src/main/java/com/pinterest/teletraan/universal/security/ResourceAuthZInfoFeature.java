package com.pinterest.teletraan.universal.security;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;

public class ResourceAuthZInfoFeature implements DynamicFeature {
    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        if (resourceInfo.getResourceMethod().getAnnotation(ResourceAuthZInfo.class) != null) {
            context.register(ResourceAuthZInfoFilter.class);
        }
    }
}
