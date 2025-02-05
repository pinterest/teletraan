/**
 * Copyright (c) 2016-2024 Pinterest, Inc.
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

/**
 * MaskBasedRole is a role that is based on a mask which is used for role comparison. The use of
 * mask is similar to Unix file permission.
 */
public class MaskBasedRole implements Role<MaskBasedRole> {
    private long mask;

    public MaskBasedRole(long mask) {
        this.mask = mask;
    }

    public long getMask() {
        return mask;
    }

    public String getName() {
        return String.valueOf(mask);
    }

    @Override
    public boolean isEqualOrSuperior(MaskBasedRole requiredRole) {
        return (mask & requiredRole.mask) == requiredRole.mask;
    }
}
