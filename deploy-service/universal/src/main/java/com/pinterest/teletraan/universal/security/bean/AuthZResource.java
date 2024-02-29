/**
 * Copyright (c) 2024 Pinterest, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pinterest.teletraan.universal.security.bean;

import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthZResource {
    private @Nonnull String name;
    private final Type type;
    private String accountId;

    public static final String ALL = "*";
    public static final AuthZResource SYSTEM_RESOURCE = new AuthZResource(ALL, Type.SYSTEM);

    public AuthZResource(@Nonnull String name, Type type) {
        this(name, type, null);
    }

    public AuthZResource(String envName, String stageName) {
        if (envName == null) {
            throw new IllegalArgumentException("envName cannot be null");
        }
        this.name = String.format("%s/%s", envName, stageName);
        this.type = Type.ENV_STAGE;
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
        BUILD,
        RATINGS
    }

    @Deprecated
    public void setId(@Nonnull String id) {
        this.name = id;
    }
}
