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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MaskBasedRoleTest {
    private static final MaskBasedRole ADMIN = new MaskBasedRole(0xFFFFFFFFL);
    private static final MaskBasedRole READ = new MaskBasedRole(0b100L);
    private static final MaskBasedRole WRITE = new MaskBasedRole(0b010L);
    private static final MaskBasedRole READ_WRITE = new MaskBasedRole(0b110L);
    private static final MaskBasedRole READ_WRITE_EXECUTE = new MaskBasedRole(0b111L);

    @Test
    void testGetMask() {
        assertEquals(0b100L, READ.getMask());
    }

    @Test
    void testGetName() {
        assertEquals("4294967295", ADMIN.getName());
        assertEquals("4", READ.getName());
        assertEquals("2", WRITE.getName());
        assertEquals("6", READ_WRITE.getName());
        assertEquals("7", READ_WRITE_EXECUTE.getName());
    }

    @Test
    void testIsEqualOrSuperior_ADMIN() {
        assertTrue(ADMIN.isEqualOrSuperior(ADMIN));
        assertTrue(ADMIN.isEqualOrSuperior(READ));
        assertTrue(ADMIN.isEqualOrSuperior(WRITE));
        assertTrue(ADMIN.isEqualOrSuperior(READ_WRITE));
        assertTrue(ADMIN.isEqualOrSuperior(READ_WRITE_EXECUTE));
    }

    @Test
    void testIsEqualOrSuperior_READ() {
        assertFalse(READ.isEqualOrSuperior(ADMIN));
        assertTrue(READ.isEqualOrSuperior(READ));
        assertFalse(READ.isEqualOrSuperior(WRITE));
        assertFalse(READ.isEqualOrSuperior(READ_WRITE));
        assertFalse(READ.isEqualOrSuperior(READ_WRITE_EXECUTE));
    }

    @Test
    void testIsEqualOrSuperior_WRITE() {
        assertFalse(WRITE.isEqualOrSuperior(ADMIN));
        assertFalse(WRITE.isEqualOrSuperior(READ));
        assertTrue(WRITE.isEqualOrSuperior(WRITE));
        assertFalse(WRITE.isEqualOrSuperior(READ_WRITE));
        assertFalse(WRITE.isEqualOrSuperior(READ_WRITE_EXECUTE));
    }

    @Test
    void testIsEqualOrSuperior_READ_WRITE() {
        assertFalse(READ_WRITE.isEqualOrSuperior(ADMIN));
        assertTrue(READ_WRITE.isEqualOrSuperior(READ));
        assertTrue(READ_WRITE.isEqualOrSuperior(WRITE));
        assertTrue(READ_WRITE.isEqualOrSuperior(READ_WRITE));
        assertFalse(READ_WRITE.isEqualOrSuperior(READ_WRITE_EXECUTE));
    }

    @Test
    void testIsEqualOrSuperiorREAD_WRITE_EXECUTE() {
        assertFalse(READ_WRITE_EXECUTE.isEqualOrSuperior(ADMIN));
        assertTrue(READ_WRITE_EXECUTE.isEqualOrSuperior(READ));
        assertTrue(READ_WRITE_EXECUTE.isEqualOrSuperior(WRITE));
        assertTrue(READ_WRITE_EXECUTE.isEqualOrSuperior(READ_WRITE));
        assertTrue(READ_WRITE_EXECUTE.isEqualOrSuperior(READ_WRITE_EXECUTE));
    }
}
