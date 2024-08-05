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
package com.pinterest.deployservice.bean;

import com.pinterest.teletraan.universal.security.bean.RoleEnum;
import com.pinterest.teletraan.universal.security.bean.ValueBasedRole;

public enum TeletraanPrincipalRole implements RoleEnum<ValueBasedRole> {
    /** Default role, everyone who is able to use Teletraan has READER access. */
    READ(-1),
    READER(0), // legacy
    /** Role required to ping server. */
    PINGER(1), // legacy
    /** Role required to publish artifacts. */
    PUBLISHER(1), // legacy
    EXECUTE(9),
    WRITE(9),
    DELETE(9),
    /**
     * Role where user can modify a specific environment's config and perform deploy related
     * actions.
     */
    OPERATOR(10), // legacy
    /**
     * Role that has the same environment specific privileges as OPERATOR plus the ability specify
     * new OPERATORS and ADMINs for said environment. When a new environment is created the creating
     * user is the designated the first ADMIN.
     */
    ADMIN(20);

    public class Names {
        private Names() {}

        public static final String PINGER = "PINGER";
        public static final String PUBLISHER = "PUBLISHER";
        public static final String READER = "READER";
        public static final String OPERATOR = "OPERATOR";
        public static final String ADMIN = "ADMIN";

        public static final String READ = "READ";
        public static final String WRITE = "WRITE";
        public static final String EXECUTE = "EXECUTE";
        public static final String DELETE = "DELETE";
    }

    private final ValueBasedRole role;

    TeletraanPrincipalRole(int value) {
        this.role = new ValueBasedRole(value);
    }

    public ValueBasedRole getRole() {
        return role;
    }

    public boolean isEqualOrSuperior(TeletraanPrincipalRole otherRole) {
        return this.role.isEqualOrSuperior(otherRole.getRole());
    }
}
