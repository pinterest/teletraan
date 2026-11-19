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
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.handler.ConfigHistoryHandler;
import com.pinterest.deployservice.handler.EnvironHandler;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.resource.EnvCapacities.CapacityType;
import com.pinterest.teletraan.resource.Utils;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

class EnvironmentHandlerTest {

    @Mock private ConfigHistoryHandler mockConfigHistoryHandler;
    @Mock private EnvironDAO mockEnvironDAO;
    @Mock private EnvironHandler mockEnvironHandler;
    @Mock private GroupDAO mockGroupDAO;
    @Mock private HostDAO mockHostDAO;
    @Mock private TeletraanServiceContext mockContext;

    private EnvironmentHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Set up context to return mocks
        when(mockContext.getEnvironDAO()).thenReturn(mockEnvironDAO);
        when(mockContext.getGroupDAO()).thenReturn(mockGroupDAO);
        when(mockContext.getHostDAO()).thenReturn(mockHostDAO);

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
            assertDoesNotThrow(() -> handler.updateEnvironment(null, "op", "env", "stage", update));
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
                            () -> handler.updateEnvironment(null, "op", "env", "stage", update));
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
                            () -> handler.updateEnvironment(null, "op", "env", "stage", update));
            assertEquals(400, ex.getResponse().getStatus());
        }
    }

    @Test
    void testDeleteCapacityForGroup_regularGroup() throws Exception {
        // Arrange
        EnvironBean bean = new EnvironBean();
        bean.setEnv_id("the_env_id");
        bean.setCluster_name("a-different-cluster"); // Ensure it's not the same as 'groupname'
        when(mockEnvironDAO.getByStage("env", "stage")).thenReturn(bean);

        try (MockedStatic<Utils> mocked = mockStatic(Utils.class)) {
            mocked.when(() -> Utils.getEnvStage(mockEnvironDAO, "env", "stage")).thenReturn(bean);

            // Act
            handler.deleteCapacityForHostOrGroup(
                    "theOp", "env", "stage", Optional.empty(), "\"groupname\"");

            // Assert
            verify(mockGroupDAO).removeGroupCapacity("the_env_id", "groupname");
            verify(mockEnvironDAO, never()).deleteCluster(any(), any());
            verify(mockGroupDAO, never()).removeHostCapacity(any(), any());
        }
    }

    @Test
    void testDeleteCapacityForGroup_whenGroupIsCluster() throws Exception {
        // Arrange
        EnvironBean bean = new EnvironBean();
        bean.setEnv_id("eid123");
        bean.setCluster_name("groupname"); // Name matches the to-be-removed group
        when(mockEnvironDAO.getByStage("env", "stage")).thenReturn(bean);

        try (MockedStatic<Utils> mocked = mockStatic(Utils.class)) {
            mocked.when(() -> Utils.getEnvStage(mockEnvironDAO, "env", "stage")).thenReturn(bean);

            // Act
            handler.deleteCapacityForHostOrGroup(
                    "who", "env", "stage", Optional.empty(), "\"groupname\"");

            // Assert
            verify(mockGroupDAO).removeGroupCapacity("eid123", "groupname");
            verify(mockEnvironDAO).deleteCluster("env", "stage");
            verify(mockGroupDAO, never()).removeHostCapacity(any(), any());
        }
    }

    @Test
    void testDeleteCapacityForHost() throws Exception {
        // Arrange
        EnvironBean bean = new EnvironBean();
        bean.setEnv_id("host_env_id");
        bean.setCluster_name("irrelevant");
        when(mockEnvironDAO.getByStage("env", "stage")).thenReturn(bean);

        try (MockedStatic<Utils> mocked = mockStatic(Utils.class)) {
            mocked.when(() -> Utils.getEnvStage(mockEnvironDAO, "env", "stage")).thenReturn(bean);

            // Act
            handler.deleteCapacityForHostOrGroup(
                    "opx", "env", "stage", Optional.of(CapacityType.HOST), "\"host.name\"");

            // Assert
            verify(mockGroupDAO).removeHostCapacity("host_env_id", "host.name");
            verify(mockGroupDAO, never()).removeGroupCapacity(any(), any());
            verify(mockEnvironDAO, never()).deleteCluster(any(), any());
        }
    }

    @Test
    void testValidateClusterName() {
        char[] maxLength = new char[128];
        Arrays.fill(maxLength, 'a');
        assertDoesNotThrow(() -> EnvironmentHandler.validateClusterName("svc-prod_1"));
        assertDoesNotThrow(() -> EnvironmentHandler.validateClusterName(new String(maxLength)));

        for (String invalid :
                new String[] {
                    null, "", new String(maxLength) + "a", "a/b", "../a", "a?b", "a#b", "a.b", "a b"
                }) {
            WebApplicationException e =
                    assertThrows(
                            WebApplicationException.class,
                            () -> EnvironmentHandler.validateClusterName(invalid));
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test
    void testValidateInfraCluster_existingClusterMustBeBoundToEnv() throws Exception {
        EnvironBean own = env("id1", "own-asg", null);
        EnvironBean unbound = env("id1", null, null);
        when(mockEnvironDAO.getByCluster("own-asg")).thenReturn(own);

        assertDoesNotThrow(() -> handler.validateInfraCluster(own, "own-asg", true));
        assertDoesNotThrow(() -> handler.validateInfraCluster(own, "OWN-ASG", true));
        assertThrows(
                ForbiddenException.class,
                () -> handler.validateInfraCluster(own, "other-asg", true));
        assertThrows(
                ForbiddenException.class,
                () -> handler.validateInfraCluster(unbound, "own-asg", true));
    }

    @Test
    void testValidateInfraCluster_rejectsWhenOwnerIsAnotherEnv() throws Exception {
        // Even if this env's cluster_name matches, the cluster's recorded owner must be this env.
        EnvironBean attacker = env("attacker", "victim-asg", null);
        when(mockEnvironDAO.getByCluster("victim-asg"))
                .thenReturn(env("victim", "victim-asg", null));

        assertThrows(
                ForbiddenException.class,
                () -> handler.validateInfraCluster(attacker, "victim-asg", true));
        assertThrows(
                ForbiddenException.class,
                () -> handler.validateInfraCluster(attacker, "victim-asg", false));
    }

    @Test
    void testValidateClusterClaim_newUnusedName() throws Exception {
        EnvironBean bean = env("id1", null, null);

        assertDoesNotThrow(() -> handler.validateClusterClaim(bean, "env-stage"));
        assertDoesNotThrow(() -> handler.validateInfraCluster(bean, "env-stage", false));
    }

    @Test
    void testValidateClusterClaim_groupOfAnotherEnv() throws Exception {
        EnvironBean bean = env("id1", null, null);
        when(mockEnvironDAO.getEnvsByGroups(Collections.singletonList("shared")))
                .thenReturn(Collections.singletonList(env("id2", null, null)));

        assertThrows(ForbiddenException.class, () -> handler.validateClusterClaim(bean, "shared"));
    }

    @Test
    void testValidateClusterClaim_ignoresSidecarEnvs() throws Exception {
        EnvironBean bean = env("id1", null, null);
        when(mockEnvironDAO.getEnvsByGroups(Collections.singletonList("grp")))
                .thenReturn(Collections.singletonList(env("sidecar", null, 10)));

        assertDoesNotThrow(() -> handler.validateClusterClaim(bean, "grp"));
    }

    @Test
    void testValidateClusterClaim_hostsInGroup() throws Exception {
        EnvironBean bean = env("id1", null, null);
        when(mockHostDAO.getGroupSize("grp")).thenReturn(2L);

        assertThrows(ForbiddenException.class, () -> handler.validateClusterClaim(bean, "grp"));

        // Hosts are fine if this environment already deploys to the group.
        when(mockEnvironDAO.getEnvsByGroups(Collections.singletonList("grp")))
                .thenReturn(Collections.singletonList(bean));
        assertDoesNotThrow(() -> handler.validateClusterClaim(bean, "grp"));
    }

    @Test
    void testUpdateEnvironment_rejectsClusterBoundToAnotherEnv() throws Exception {
        EnvironBean origin = env("id1", null, null);
        origin.setEnv_name("env");
        origin.setStage_name("stage");
        when(mockEnvironDAO.getByCluster("victim-asg")).thenReturn(env("id2", "victim-asg", null));
        EnvironBean update = new EnvironBean();
        update.setCluster_name("victim-asg");

        try (MockedStatic<Utils> mocked = mockStatic(Utils.class)) {
            when(Utils.getEnvStage(any(), any(), any())).thenReturn(origin);

            assertThrows(
                    ForbiddenException.class,
                    () -> handler.updateEnvironment(null, "op", "env", "stage", update));
            verify(mockEnvironHandler, never()).updateStage(any(), any());
        }
    }

    @Test
    void testUpdateEnvironment_skipsClusterCheckWhenUnchanged() throws Exception {
        EnvironBean origin = env("id1", "env-stage", null);
        origin.setEnv_name("env");
        origin.setStage_name("stage");
        origin.setStage_type(EnvType.PRODUCTION);
        EnvironBean update = new EnvironBean();
        update.setCluster_name("ENV-STAGE");
        update.setStage_type(EnvType.PRODUCTION);

        try (MockedStatic<Utils> mocked = mockStatic(Utils.class)) {
            when(Utils.getEnvStage(any(), any(), any())).thenReturn(origin);

            assertDoesNotThrow(() -> handler.updateEnvironment(null, "op", "env", "stage", update));
            verify(mockEnvironDAO, never()).getByCluster(any());
            verify(mockEnvironHandler).updateStage(any(), eq("op"));
        }
    }

    private static EnvironBean env(String id, String clusterName, Integer systemPriority) {
        EnvironBean bean = new EnvironBean();
        bean.setEnv_id(id);
        bean.setCluster_name(clusterName);
        bean.setSystem_priority(systemPriority);
        return bean;
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
