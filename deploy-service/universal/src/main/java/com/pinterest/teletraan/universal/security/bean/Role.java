package com.pinterest.teletraan.universal.security.bean;

public interface Role<T extends Role<T>> {
    boolean isEqualOrSuperior(T requiredRole);
}