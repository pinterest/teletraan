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
import java.util.Map;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Value;

/** AuthZResource represents a resource for authorization purposes. */
@Value
@AllArgsConstructor
public class AuthZResource {
    @Nonnull String name;
    @Nonnull Type type;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    String accountId;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    Map<String, String> attributes;

    public static final String ALL = "*";
    public static final String NA = "NA";
    public static final AuthZResource SYSTEM_RESOURCE = new AuthZResource(ALL, Type.SYSTEM);
    public static final AuthZResource UNSPECIFIED_RESOURCE =
            new AuthZResource(NA, Type.UNSPECIFIED);

    public AuthZResource(@Nonnull String name, Type type) {
        this(name, type, null, null);
    }

    public AuthZResource(@Nonnull String name, Type type, Map<String, String> attributes) {
        this(name, type, null, attributes);
    }

    /**
     * Convenient constructor for creating an ENV_STAGE resource, the most common resource type.
     *
     * @param envName the name of the environment
     * @param stageName the name of the stage
     */
    public AuthZResource(String envName, String stageName) {
        this(envName, stageName, null);
    }

    /**
     * Convenient constructor for creating an ENV_STAGE resource, the most common resource type.
     *
     * @param envName the name of the environment
     * @param stageName the name of the stage
     * @param attributes the attributes of the resource
     */
    public AuthZResource(String envName, String stageName, Map<String, String> attributes) {
        if (envName == null) {
            throw new IllegalArgumentException("envName cannot be null");
        }
        this.name = String.format("%s/%s", envName, stageName);
        this.type = Type.ENV_STAGE;
        this.attributes = attributes;
        this.accountId = null;
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
        /** For host related resources. */
        HOST,
        /** For cluster provision prefix resource. */
        PROVISION_PREFIX,
        /** For instance type mapping url */
        INSTANCE_TYPE_MAPPING,
        /* For anything else */
        UNSPECIFIED,
    }

    public enum AttributeKeys {
        ENV_STAGE_IS_SOX,
        BUILD_ARTIFACT_URL,
    }
}
