package com.pinterest.teletraan.universal.security.bean;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthZResource {
    private String name;
    private final Type type;
    private String accountId;

    public final static String ALL = "*";
    public final static AuthZResource SYSTEM_RESOURCE = new AuthZResource(ALL, Type.SYSTEM);

    public AuthZResource(String name, Type type) {
        this(name, type, null);
    }

    public enum Type {
        ENV,
        GROUP,
        SYSTEM,
        ENV_STAGE,
        PLACEMENT,
        BASE_IMAGE,
        SECURITY_ZONE,
        IAM_ROLE,
        BUILD
    }

    @Deprecated
    public void setId(String id) {
        this.name = id;
    }
}
