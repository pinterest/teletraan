/**
 * Copyright (c) 2025 Pinterest, Inc.
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
package com.pinterest.teletraan.handler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.pinterest.deployservice.bean.EnvType;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.GroupDAO;
import com.pinterest.deployservice.handler.ConfigHistoryHandler;
import com.pinterest.deployservice.handler.EnvironHandler;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.resource.EnvCapacities.CapacityType;
import com.pinterest.teletraan.resource.Utils;
import java.util.Optional;
import javax.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

class EnvironmentHandlerTest {

    @Mock private ConfigHistoryHandler mockConfigHistoryHandler;
    @Mock private EnvironDAO mockEnvironDAO;
    @Mock private EnvironHandler mockEnvironHandler;
    @Mock private GroupDAO mockGroupDAO;
    @Mock private TeletraanServiceContext mockContext;

    private EnvironmentHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Set up context to return mocks
        when(mockContext.getEnvironDAO()).thenReturn(mockEnvironDAO);
        when(mockContext.getGroupDAO()).thenReturn(mockGroupDAO);

        // Use real constructor, but inject our mocks after (quick hack)
        handler = new EnvironmentHandler(mockContext);
        // Replace what was constructed internally!
        TestUtils.setField(handler, "configHistoryHandler", mockConfigHistoryHandler);
        TestUtils.setField(handler, "environHandler", mockEnvironHandler);
    }

    @Test
    void testCreateCapacityForGroup() throws Exception {
        EnvironBean bean = new EnvironBean();
        bean.setEnv_id("envId");
        handler.createCapacityForHostOrGroup(
                "op", "env", "stg", Optional.empty(), "groupname", bean);
        verify(mockGroupDAO).addGroupCapacity("envId", "groupname");
        verify(mockGroupDAO, never()).addHostCapacity(any(), any());
    }

    @Test
    void testCreateCapacityForHost() throws Exception {
        EnvironBean bean = new EnvironBean();
        bean.setEnv_id("envId");
        handler.createCapacityForHostOrGroup(
                "op", "env", "stg", Optional.of(CapacityType.HOST), "hostname", bean);
        verify(mockGroupDAO).addHostCapacity("envId", "hostname");
        verify(mockGroupDAO, never()).addGroupCapacity(any(), any());
    }

    @Test
    void testUpdateEnvironment_success() throws Exception {
        EnvironBean origin = new EnvironBean();
        origin.setEnv_name("env");
        origin.setStage_name("stage");
        origin.setEnv_id("id1");
        origin.setIs_sox(true);
        origin.setStage_type(EnvType.PRODUCTION);
        origin.setExternal_id("eid");
        // Returned from the static utility
        when(mockEnvironDAO.getByStage(eq("env"), eq("stage"))).thenReturn(origin);

        EnvironBean update = new EnvironBean();
        update.setIs_sox(true);
        update.setStage_type(EnvType.PRODUCTION);

        try (MockedStatic<Utils> mocked = mockStatic(Utils.class)) {
            when(Utils.getEnvStage(any(), any(), any())).thenReturn(origin);

            // Should not throw
            assertDoesNotThrow(() -> handler.updateEnvironment("op", "env", "stage", update));
            verify(mockEnvironHandler).updateStage(any(), eq("op"));
            verify(mockConfigHistoryHandler)
                    .updateConfigHistory(eq("id1"), any(), eq(update), eq("op"));
            verify(mockConfigHistoryHandler)
                    .updateChangeFeed(any(), eq("id1"), any(), eq("op"), eq("eid"));
        }
    }

    @Test
    void testUpdateEnvironment_disallowSoxFlagChange() throws Exception {
        EnvironBean origin = new EnvironBean();
        origin.setEnv_name("env");
        origin.setStage_name("stage");
        origin.setEnv_id("id1");
        origin.setIs_sox(true);
        origin.setStage_type(EnvType.PRODUCTION);

        EnvironBean update = new EnvironBean();
        update.setIs_sox(false);
        update.setStage_type(EnvType.PRODUCTION);

        try (MockedStatic<Utils> mocked = mockStatic(Utils.class)) {
            when(Utils.getEnvStage(any(), any(), any())).thenReturn(origin);

            WebApplicationException ex =
                    assertThrows(
                            WebApplicationException.class,
                            () -> handler.updateEnvironment("op", "env", "stage", update));
            assertEquals(403, ex.getResponse().getStatus());
        }
    }

    @Test
    void testUpdateEnvironment_invalidStageTypeChange() throws Exception {
        EnvironBean origin = new EnvironBean();
        origin.setEnv_name("env");
        origin.setStage_name("stage");
        origin.setEnv_id("id1");
        origin.setIs_sox(false);
        origin.setStage_type(EnvType.PRODUCTION);

        EnvironBean update = new EnvironBean();
        update.setIs_sox(false);
        update.setStage_type(EnvType.DEV); // Not allowed: prod->nonprod

        try (MockedStatic<Utils> mocked = mockStatic(Utils.class)) {
            when(Utils.getEnvStage(any(), any(), any())).thenReturn(origin);

            WebApplicationException ex =
                    assertThrows(
                            WebApplicationException.class,
                            () -> handler.updateEnvironment("op", "env", "stage", update));
            assertEquals(400, ex.getResponse().getStatus());
        }
    }

    @Test
    void testDeleteCapacityForHostOrGroup() throws Exception {
        handler.deleteCapacityForHostOrGroup("user", "env", "stage");
        verify(mockEnvironHandler).deleteEnvStage("env", "stage", "user");
    }
}

// Utility for injecting and mocking statics
class TestUtils {
    static void setField(Object obj, String field, Object value) {
        try {
            java.lang.reflect.Field f = obj.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
