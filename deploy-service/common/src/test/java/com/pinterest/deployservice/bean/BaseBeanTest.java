package com.pinterest.deployservice.bean;


import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class BaseBeanTest {
    @Test
    void testGetStringWithinSizeLimitInputNull() {
        BaseBean baseBean = new BaseBean();
        String result = baseBean.getStringWithinSizeLimit(null, 10);
        assertNull(result);
    }

    @Test
    void testGetStringWithinSizeLimitInputWithinLimit() {
        BaseBean baseBean = new BaseBean();
        String input = "test";
        String result = baseBean.getStringWithinSizeLimit(input, 10);
        assertSame(input, result);
    }
    @Test
    void testGetStringWithinSizeLimitInputExceedsLimit() {
        BaseBean baseBean = new BaseBean();
        String result = baseBean.getStringWithinSizeLimit("0123456789", 5);
        assertEquals("56789", result);
    }
}
