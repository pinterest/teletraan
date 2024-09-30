package com.pinterest.deployservice.bean;


import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class BaseBeanTest {
    @Test
    void testGetStringWithSizeLimitInputNull() {
        BaseBean baseBean = new BaseBean();
        String result = baseBean.getStringWithSizeLimit(null, 10);
        assertNull(result);
    }

    @Test
    void testGetStringWithSizeLimitInputWithinLimit() {
        BaseBean baseBean = new BaseBean();
        String input = "test";
        String result = baseBean.getStringWithSizeLimit(input, 10);
        assertSame(input, result);
    }
    @Test
    void testGetStringWithSizeLimitInputExceedsLimit() {
        BaseBean baseBean = new BaseBean();
        String result = baseBean.getStringWithSizeLimit("0123456789", 5);
        assertEquals("56789", result);
    }
}
