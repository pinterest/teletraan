/**
 * Copyright (c) 2024, Pinterest Inc. All rights reserved.
 */
package com.pinterest.teletraan.universal.security;

import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** This annotation is used to specify the resource information useful for authorization. */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ResourceAuthZInfo {
  AuthZResource.Type type();

  /** The location of the resource identifier in the request. */
  Location idLocation() default Location.NA;

  Class<?> beanClass() default Object.class;

  public enum Location {
    PATH,
    QUERY,
    BODY,
    NA
  }
}
