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

import static com.pinterest.deployservice.bean.BeanUtils.createScheduleBean;
import static com.pinterest.deployservice.fixture.EnvironBeanFixture.createRandomEnvironBean;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.HostAgentBean;
import com.pinterest.deployservice.bean.KnoxStatus;
import com.pinterest.deployservice.bean.NormandieStatus;
import com.pinterest.deployservice.bean.ScheduleBean;
import com.pinterest.deployservice.bean.ScheduleState;
import com.pinterest.deployservice.dao.AgentCountDAO;
import com.pinterest.deployservice.dao.AgentDAO;
import com.pinterest.deployservice.dao.AgentErrorDAO;
import com.pinterest.deployservice.dao.BuildDAO;
import com.pinterest.deployservice.dao.DeployConstraintDAO;
import com.pinterest.deployservice.dao.DeployDAO;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.GroupDAO;
import com.pinterest.deployservice.dao.HostAgentDAO;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.dao.HostTagDAO;
import com.pinterest.deployservice.dao.ScheduleDAO;
import com.pinterest.deployservice.dao.UtilDAO;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.dbcp.BasicDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class PingHandlerTest {

    private AgentDAO agentDAO;
    private AgentCountDAO agentCountDAO;
    private AgentErrorDAO agentErrorDAO;
    private BasicDataSource dataSource;
    private DeployDAO deployDAO;
    private BuildDAO buildDAO;
    private EnvironDAO environDAO;
    private HostDAO hostDAO;
    private HostAgentDAO hostAgentDAO;
    private UtilDAO utilDAO;
    private ScheduleDAO scheduleDAO;
    private HostTagDAO hostTagDAO;
    private GroupDAO groupDAO;
    private DeployConstraintDAO deployConstraintDAO;
    private ServiceContext serviceContext;
    private PingHandler pingHandler;

    @BeforeEach
    public void setUp() {
        agentDAO = mock(AgentDAO.class);
        agentCountDAO = mock(AgentCountDAO.class);
        agentErrorDAO = mock(AgentErrorDAO.class);
        dataSource = mock(BasicDataSource.class);
        deployDAO = mock(DeployDAO.class);
        buildDAO = mock(BuildDAO.class);
        environDAO = mock(EnvironDAO.class);
        hostDAO = mock(HostDAO.class);
        hostAgentDAO = mock(HostAgentDAO.class);
        utilDAO = mock(UtilDAO.class);
        scheduleDAO = mock(ScheduleDAO.class);
        hostTagDAO = mock(HostTagDAO.class);
        groupDAO = mock(GroupDAO.class);
        deployConstraintDAO = mock(DeployConstraintDAO.class);

        serviceContext = new ServiceContext();
        serviceContext.setAgentDAO(agentDAO);
        serviceContext.setAgentCountDAO(agentCountDAO);
        serviceContext.setAgentErrorDAO(agentErrorDAO);
        serviceContext.setDataSource(dataSource);
        serviceContext.setDeployDAO(deployDAO);
        serviceContext.setBuildDAO(buildDAO);
        serviceContext.setEnvironDAO(environDAO);
        serviceContext.setHostDAO(hostDAO);
        serviceContext.setHostAgentDAO(hostAgentDAO);
        serviceContext.setUtilDAO(utilDAO);
        serviceContext.setScheduleDAO(scheduleDAO);
        serviceContext.setHostTagDAO(hostTagDAO);
        serviceContext.setGroupDAO(groupDAO);
        serviceContext.setDeployConstraintDAO(deployConstraintDAO);

        pingHandler = new PingHandler(serviceContext);
    }

    @Test
    public void testCanDeployWithSchedule_NoSchedule() throws Exception {
        EnvironBean environ = createRandomEnvironBean();
        boolean canDeploy = pingHandler.canDeploywithSchedule(environ);
        assertTrue(canDeploy);
        verify(scheduleDAO, times(0)).getById(any());
    }

    @Test
    public void testCanDeployWithSchedule_CoolingDown() throws Exception {
        // Set up mock data
        ScheduleBean schedule = createScheduleBean(ScheduleState.COOLING_DOWN);
        EnvironBean environ = createRandomEnvironBean();
        environ.setSchedule_id(schedule.getId());
        when(scheduleDAO.getById(eq(schedule.getId()))).thenReturn(schedule);

        // Run the schedule
        boolean canDeploy = pingHandler.canDeploywithSchedule(environ);

        // Verify the result
        assertFalse(canDeploy);
        verify(scheduleDAO, times(1)).getById(eq(schedule.getId()));
    }

    @ParameterizedTest
    @MethodSource("runningSchedules")
    public void testCanDeployWithSchedule_Running(
            long finishedAgents, long totalAgents, String hostNumbers, boolean expectedCanDeploy)
            throws Exception {
        // Set up mock data
        ScheduleBean schedule = createScheduleBean(ScheduleState.RUNNING);
        schedule.setHost_numbers(hostNumbers);
        EnvironBean environ = createRandomEnvironBean();
        environ.setSchedule_id(schedule.getId());
        when(scheduleDAO.getById(eq(schedule.getId()))).thenReturn(schedule);
        when(agentDAO.countAgentsByDeploy(eq(environ.getDeploy_id()))).thenReturn(finishedAgents);
        when(agentDAO.countAgentByEnv(eq(environ.getEnv_id()))).thenReturn(totalAgents);

        // Run the schedule
        boolean actualCanDeploy = pingHandler.canDeploywithSchedule(environ);

        // Verify the result
        assertEquals(expectedCanDeploy, actualCanDeploy);
        verify(scheduleDAO, times(1)).getById(eq(schedule.getId()));
    }

    static Stream<Arguments> runningSchedules() {
        return Stream.of(
                Arguments.of(0l, 6l, "1,2,3", true),
                Arguments.of(1l, 6l, "1,2,3", false),
                Arguments.of(1l, 3l, "3%,20%,60%", false),
                Arguments.of(0l, 3l, "3%,20%,60%", true),
                Arguments.of(2l, 100l, "3%,20%,60%", true),
                Arguments.of(3l, 100l, "3%,20%,60%", false));
    }

    @Test
    public void testUpdateHostStatus_newHost_insertsRow() throws Exception {
        when(hostAgentDAO.getHostById("host-1")).thenReturn(null);

        pingHandler.updateHostStatus(
                "host-1",
                "hostname",
                "10.0.0.1",
                "2.0",
                "asg-1",
                NormandieStatus.OK,
                KnoxStatus.OK);

        verify(hostAgentDAO).insert(any(HostAgentBean.class));
        verify(hostAgentDAO, never()).update(anyString(), any(HostAgentBean.class));
        verify(hostAgentDAO, never()).touchLastUpdate(anyString(), anyLong());
    }

    @Test
    public void testUpdateHostStatus_fieldsChanged_fullUpdate() throws Exception {
        HostAgentBean existing =
                HostAgentBean.builder()
                        .host_id("host-1")
                        .host_name("hostname")
                        .ip("10.0.0.1")
                        .agent_version("1.0")
                        .auto_scaling_group("asg-1")
                        .normandie_status(NormandieStatus.OK)
                        .knox_status(KnoxStatus.OK)
                        .last_update(1000L)
                        .create_date(500L)
                        .build();
        when(hostAgentDAO.getHostById("host-1")).thenReturn(existing);

        // Change agent_version from 1.0 -> 2.0
        pingHandler.updateHostStatus(
                "host-1",
                "hostname",
                "10.0.0.1",
                "2.0",
                "asg-1",
                NormandieStatus.OK,
                KnoxStatus.OK);

        verify(hostAgentDAO).update(eq("host-1"), any(HostAgentBean.class));
        verify(hostAgentDAO, never()).touchLastUpdate(anyString(), anyLong());
        verify(hostAgentDAO, never()).insert(any(HostAgentBean.class));
    }

    @Test
    public void testUpdateHostStatus_noFieldsChanged_touchLastUpdateOnly() throws Exception {
        HostAgentBean existing =
                HostAgentBean.builder()
                        .host_id("host-1")
                        .host_name("hostname")
                        .ip("10.0.0.1")
                        .agent_version("2.0")
                        .auto_scaling_group("asg-1")
                        .normandie_status(NormandieStatus.OK)
                        .knox_status(KnoxStatus.OK)
                        .last_update(1000L)
                        .create_date(500L)
                        .build();
        when(hostAgentDAO.getHostById("host-1")).thenReturn(existing);

        // Same fields as existing
        pingHandler.updateHostStatus(
                "host-1",
                "hostname",
                "10.0.0.1",
                "2.0",
                "asg-1",
                NormandieStatus.OK,
                KnoxStatus.OK);

        verify(hostAgentDAO).touchLastUpdate(eq("host-1"), anyLong());
        verify(hostAgentDAO, never()).update(anyString(), any(HostAgentBean.class));
        verify(hostAgentDAO, never()).insert(any(HostAgentBean.class));
    }

    @Test
    public void testHasFieldChanged_allSame_returnsFalse() {
        HostAgentBean existing =
                HostAgentBean.builder()
                        .host_name("h")
                        .ip("1.2.3.4")
                        .agent_version("v1")
                        .auto_scaling_group("asg")
                        .normandie_status(NormandieStatus.OK)
                        .knox_status(KnoxStatus.OK)
                        .build();
        assertFalse(
                PingHandler.hasFieldChanged(
                        existing, "h", "1.2.3.4", "v1", "asg", NormandieStatus.OK, KnoxStatus.OK));
    }

    @Test
    public void testHasFieldChanged_ipDiffers_returnsTrue() {
        HostAgentBean existing =
                HostAgentBean.builder()
                        .host_name("h")
                        .ip("1.2.3.4")
                        .agent_version("v1")
                        .auto_scaling_group("asg")
                        .normandie_status(NormandieStatus.OK)
                        .knox_status(KnoxStatus.OK)
                        .build();
        assertTrue(
                PingHandler.hasFieldChanged(
                        existing, "h", "5.6.7.8", "v1", "asg", NormandieStatus.OK, KnoxStatus.OK));
    }

    @Test
    public void testHasFieldChanged_nullFields_handledCorrectly() {
        HostAgentBean existing =
                HostAgentBean.builder()
                        .host_name("h")
                        .ip(null)
                        .agent_version("v1")
                        .auto_scaling_group(null)
                        .normandie_status(NormandieStatus.OK)
                        .knox_status(KnoxStatus.OK)
                        .build();
        // Both null - no change
        assertFalse(
                PingHandler.hasFieldChanged(
                        existing, "h", null, "v1", null, NormandieStatus.OK, KnoxStatus.OK));
        // One null, one not - changed
        assertTrue(
                PingHandler.hasFieldChanged(
                        existing, "h", "1.2.3.4", "v1", null, NormandieStatus.OK, KnoxStatus.OK));
    }

    private static EnvironBean env(String envId, String envName, String stageName) {
        EnvironBean b = createRandomEnvironBean();
        b.setEnv_id(envId);
        b.setEnv_name(envName);
        b.setStage_name(stageName);
        return b;
    }

    @Test
    public void testConvergeEnvs_reportedGroupBeatsShardedGroup() {
        // Both envs share env_name "foo" but come from different match tiers.
        EnvironBean reported = env("env-reported", "foo", "prod");
        EnvironBean sharded = env("env-sharded", "foo", "prod-us-west-2b");

        Map<String, EnvironBean> converged =
                pingHandler.convergeEnvs(
                        "host-1",
                        Collections.emptyList(),
                        Collections.singletonList(reported),
                        Collections.singletonList(sharded));

        assertEquals(1, converged.size());
        assertTrue(converged.containsKey("env-reported"));
        assertFalse(converged.containsKey("env-sharded"));
    }

    @Test
    public void testConvergeEnvs_hostCapacityBeatsAllGroupTiers() {
        EnvironBean hostEnv = env("env-host", "foo", "canary");
        EnvironBean reported = env("env-reported", "foo", "prod");
        EnvironBean sharded = env("env-sharded", "foo", "prod-us-west-2b");

        Map<String, EnvironBean> converged =
                pingHandler.convergeEnvs(
                        "host-1",
                        Collections.singletonList(hostEnv),
                        Collections.singletonList(reported),
                        Collections.singletonList(sharded));

        assertEquals(1, converged.size());
        assertTrue(converged.containsKey("env-host"));
    }

    @Test
    public void testConvergeEnvs_noConflictKeepsAllTiers() {
        EnvironBean hostEnv = env("env-host", "alpha", "prod");
        EnvironBean reported = env("env-reported", "beta", "prod");
        EnvironBean sharded = env("env-sharded", "gamma", "prod");

        Map<String, EnvironBean> converged =
                pingHandler.convergeEnvs(
                        "host-1",
                        Collections.singletonList(hostEnv),
                        Collections.singletonList(reported),
                        Collections.singletonList(sharded));

        assertEquals(3, converged.size());
        assertTrue(converged.containsKey("env-host"));
        assertTrue(converged.containsKey("env-reported"));
        assertTrue(converged.containsKey("env-sharded"));
    }

    @Test
    public void testConvergeEnvs_onlyShardedFallsThrough() {
        EnvironBean sharded = env("env-sharded", "foo", "prod");

        Map<String, EnvironBean> converged =
                pingHandler.convergeEnvs(
                        "host-1",
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.singletonList(sharded));

        assertEquals(1, converged.size());
        assertTrue(converged.containsKey("env-sharded"));
    }

    @Test
    public void testConvergeEnvs_deprecatedTwoArgMatchesLegacyBehavior() {
        // Pre-change behavior: single group list, host beats group.
        EnvironBean hostEnv = env("env-host", "foo", "canary");
        EnvironBean groupEnv = env("env-group", "foo", "prod");
        EnvironBean otherGroupEnv = env("env-other", "bar", "prod");

        Map<String, EnvironBean> converged =
                pingHandler.convergeEnvs(
                        "host-1",
                        Collections.singletonList(hostEnv),
                        Arrays.asList(groupEnv, otherGroupEnv));

        assertEquals(2, converged.size());
        assertTrue(converged.containsKey("env-host"));
        assertTrue(converged.containsKey("env-other"));
        assertFalse(converged.containsKey("env-group"));
    }

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
