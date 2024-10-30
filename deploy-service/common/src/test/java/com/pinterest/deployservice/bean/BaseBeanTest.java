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
