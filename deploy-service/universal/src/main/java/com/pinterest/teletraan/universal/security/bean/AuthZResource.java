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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Data;

/** AuthZResource represents a resource for authorization purposes. */
@Data
@AllArgsConstructor
public class AuthZResource {
    private @Nonnull String name;
    private final Type type;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String accountId;

    public static final String ALL = "*";
    public static final AuthZResource SYSTEM_RESOURCE = new AuthZResource(ALL, Type.SYSTEM);

    public AuthZResource(@Nonnull String name, Type type) {
        this(name, type, null);
    }

    /**
     * Convenient constructor for creating an ENV_STAGE resource, the most common resource type.
     *
     * @param envName the name of the environment
     * @param stageName the name of the stage
     */
    public AuthZResource(String envName, String stageName) {
        if (envName == null) {
            throw new IllegalArgumentException("envName cannot be null");
        }
        this.name = String.format("%s/%s", envName, stageName);
        this.type = Type.ENV_STAGE;
    }

    /**
     * Convenient method for getting the environment name from the resource name.
     *
     * @return the environment name if the resource is of type ENV_STAGE or ENV, null otherwise
     */
    @JsonIgnore
    public String getEnvName() {
        if (type == Type.ENV_STAGE) {
            return name.split("/")[0];
        } else if (type == Type.ENV) {
            return name;
        }
        return null;
    }

    /** The type of the resource. */
    public enum Type {
        /** For resources that are not tied to a specific stage. */
        ENV,
        /** For groups. */
        GROUP,
        /** For resources related to global Teletraan system. */
        SYSTEM,
        /** For resources related to a specific environment-stage. The most common resource type. */
        ENV_STAGE,
        /** For placement related resources. */
        PLACEMENT,
        /** For base image related resources. */
        BASE_IMAGE,
        /** For security zone related resources. */
        SECURITY_ZONE,
        /** For IAM role related resources. */
        IAM_ROLE,
        /** For build related resources. */
        BUILD,
        /** For deploy related resources. */
        DEPLOY,
        /** For hotfix related resources. */
        HOTFIX,
        /** For Host related resources. */
        HOST,
    }

    /**
     * @deprecated Use getName() instead this. It is needed for converting DB records to
     *     AuthZResource objects.
     */
    @Deprecated
    public void setId(@Nonnull String id) {
        this.name = id;
    }
}
