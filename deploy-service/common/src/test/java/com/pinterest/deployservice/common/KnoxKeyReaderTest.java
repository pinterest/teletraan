package com.pinterest.deployservice.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;

import org.junit.Test;

public class KnoxKeyReaderTest {
    @Test
    public void testGetKey_noInit() {
        KnoxKeyReader sut = new KnoxKeyReader();
        assertEquals("defaultKeyContent", sut.getKey());
    }

    @Test
    public void testGetKey_initialized() {
        KnoxKeyReader sut = new KnoxKeyReader();
        sut.init("someRandomKey");
        assertNull(sut.getKey());
    }

    @Test
    public void testGetKey_cacheIsWorking() {
        KnoxKeyReader sut = new KnoxKeyReader();
        KnoxKeyManager mockKnoxKeyManager = mock(KnoxKeyManager.class);

        sut.init("someRandomKey");
        sut.setKnoxManager(mockKnoxKeyManager);
        sut.getKey();
        sut.getKey();

        verify(mockKnoxKeyManager, only()).getKey();
    }
}
