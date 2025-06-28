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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class HostAgentBeanTest {
    @Test
    void testGenChangedSetClause() {
        HostAgentBean oldBean = BeanUtils.createHostAgentBean();
        HostAgentBean newBean =
                oldBean.toBuilder().agent_version("new agent version").ip("new ip").build();

        SetClause setClause = newBean.genChangedSetClause(oldBean);

        assertTrue(setClause.getClause().contains("ip"));
        assertTrue(setClause.getClause().contains("agent_version"));

        assertFalse(setClause.getClause().contains("host_id"));
        assertFalse(setClause.getClause().contains("create_date"));
        assertFalse(setClause.getClause().contains("auto_scaling_group"));
    }
}
