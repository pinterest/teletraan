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
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.EnvironBean;
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
