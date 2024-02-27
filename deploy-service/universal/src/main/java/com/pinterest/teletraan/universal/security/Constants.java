/**
 * Copyright (c) 2024, Pinterest Inc. All rights reserved.
 */
package com.pinterest.teletraan.universal.security;

public class Constants {
    private Constants() {}

    public static final String USER_HEADER = "x-forwarded-user";
    public static final String GROUPS_HEADER = "x-forwarded-groups";
    public static final String CLIENT_CERT_HEADER = "x-forwarded-client-cert";
}
