package com.pinterest.teletraan.universal.security;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ResourceAuthZInfos {
  ResourceAuthZInfo[] value();
}
