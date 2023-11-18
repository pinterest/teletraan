package com.pinterest.deployservice.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
}
