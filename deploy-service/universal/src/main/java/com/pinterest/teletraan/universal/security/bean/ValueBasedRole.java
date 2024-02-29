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

/**
 * ValueBasedRole is a role that is based on a value. It is used to compare roles based on their
 * values. Higher value means more permissions.
 */
public class ValueBasedRole implements Role<ValueBasedRole> {
    private int value;

    public ValueBasedRole(int value) {
        this.value = value;
    }

    @Override
    public boolean isEqualOrSuperior(ValueBasedRole requiredRole) {
        return this.equals(requiredRole) || this.value > requiredRole.value;
    }
}
