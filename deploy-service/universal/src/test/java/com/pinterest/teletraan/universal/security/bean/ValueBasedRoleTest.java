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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ValueBasedRoleTest {
    @Test
    void testIsEqualOrSuperior() {
        ValueBasedRole role = new ValueBasedRole(10);
        assertTrue(role.isEqualOrSuperior(new ValueBasedRole(Integer.MIN_VALUE)));
        assertTrue(role.isEqualOrSuperior(new ValueBasedRole(5)));
        assertTrue(role.isEqualOrSuperior(role));

        assertFalse(role.isEqualOrSuperior(new ValueBasedRole(10)));
        assertFalse(role.isEqualOrSuperior(new ValueBasedRole(11)));
        assertFalse(role.isEqualOrSuperior(new ValueBasedRole(Integer.MAX_VALUE)));
    }
}
