/**
 * Copyright (c) 2016-2024, Pinterest Inc. All rights reserved.
 */
package com.pinterest.teletraan.universal.security.bean;

public interface Role<T extends Role<T>> {
    boolean isEqualOrSuperior(T requiredRole);
}
