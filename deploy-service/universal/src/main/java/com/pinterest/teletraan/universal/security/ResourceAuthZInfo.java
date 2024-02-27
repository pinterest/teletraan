package com.pinterest.teletraan.universal.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.pinterest.teletraan.universal.security.bean.AuthZResource;

/**
 * This annotation is used to specify the resource information
 * useful for authorization.
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ResourceAuthZInfo {
    AuthZResource.Type type();

    /**
     * The location of the resource identifier in the request.
     */
    Location IdLocation() default Location.NA;

    Class<?> beanClass() default Object.class;

    public enum Location {
        PATH,
        QUERY,
        BODY,
        NA
    }
}