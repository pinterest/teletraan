/**
 * Copyright 2016 Pinterest, Inc.
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

public class Role {
    private long accessLevel;

    public Role(long value) {
        this.accessLevel = value;
    }

    public boolean isAuthorized(Role requiredRole) {
        return this == requiredRole || this.accessLevel > requiredRole.accessLevel;
    }

    public long getAccessLevel() {
        return accessLevel;
    }

    public String getName() {
        return String.valueOf(accessLevel);
    }
}
