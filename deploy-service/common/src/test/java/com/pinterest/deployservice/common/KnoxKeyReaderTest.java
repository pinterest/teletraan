/**
 * Copyright (c) 2023-2024 Pinterest, Inc.
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
package com.pinterest.deployservice.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

public class KnoxKeyReaderTest {
    private KnoxKeyReader sut;
    private KnoxKeyManager mockKnoxKeyManager;

    @Before
    public void setUp() {
        sut = new KnoxKeyReader();
        mockKnoxKeyManager = mock(KnoxKeyManager.class);
    }

    @Test
    public void testGetKey_noInit() {
        assertEquals("defaultKeyContent", sut.getKey());
    }

    @Test
    public void testGetKey_initialized() {
        sut.init("someRandomKey");
        assertNull(sut.getKey());
    }

    @Test
    public void testGetKey_cacheIsWorking() {
        sut.init("someRandomKey");
        sut.setKnoxManager(mockKnoxKeyManager);
        sut.getKey();
        sut.getKey();

        verify(mockKnoxKeyManager, only()).getKey();
    }

    @Test
    public void testGetKey_trimmed() {
        String expected = "keyWith234!~@";
        when(mockKnoxKeyManager.getKey()).thenReturn(String.format(" \n\r\t %s \n\r\t", expected));

        sut.init("someRandomKey");
        sut.setKnoxManager(mockKnoxKeyManager);
        String trimmed = sut.getKey();

        assertEquals(expected, trimmed);
    }
}
