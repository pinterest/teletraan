/**
 * Copyright (c) 2016-2021 Pinterest, Inc.
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
package com.pinterest.deployservice.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.pinterest.deployservice.bean.EnvironBean;
import org.junit.jupiter.api.Test;

public class PingHandlerTest {
    @Test
    public void getGetFinalMaxParallelCount() throws Exception {
        EnvironBean bean = new EnvironBean();
        // Always return 1 when nothing set
        assertEquals(1, PingHandler.getFinalMaxParallelCount(bean, 1));
        assertEquals(1, PingHandler.getFinalMaxParallelCount(bean, 10));
        assertEquals(1, PingHandler.getFinalMaxParallelCount(bean, 100));

        // Only hosts set
        bean.setMax_parallel(10);
        assertEquals(1, PingHandler.getFinalMaxParallelCount(bean, 1));
        assertEquals(10, PingHandler.getFinalMaxParallelCount(bean, 10));
        assertEquals(10, PingHandler.getFinalMaxParallelCount(bean, 100));

        // Only percentage set
        bean.setMax_parallel(null);
        bean.setMax_parallel_pct(20);
        assertEquals(1, PingHandler.getFinalMaxParallelCount(bean, 1));
        assertEquals(2, PingHandler.getFinalMaxParallelCount(bean, 10));
        assertEquals(20, PingHandler.getFinalMaxParallelCount(bean, 100));

        // Both set, pick the smaller one
        bean.setMax_parallel(10);
        assertEquals(1, PingHandler.getFinalMaxParallelCount(bean, 1));
        assertEquals(2, PingHandler.getFinalMaxParallelCount(bean, 10));
        assertEquals(10, PingHandler.getFinalMaxParallelCount(bean, 100));

        // Context maxParallelThershold set
        assertEquals(1, PingHandler.calculateParallelThreshold(bean, 2, 1), 1);
        assertEquals(10, PingHandler.calculateParallelThreshold(bean, 2, 1), 10);
        assertEquals(10, PingHandler.calculateParallelThreshold(bean, 2, 1), 100);
    }
}
