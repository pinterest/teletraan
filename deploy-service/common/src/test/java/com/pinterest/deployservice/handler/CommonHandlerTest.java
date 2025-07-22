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
package com.pinterest.deployservice.handler;

import static com.pinterest.deployservice.bean.BeanUtils.createScheduleBean;
import static com.pinterest.deployservice.fixture.EnvironBeanFixture.createRandomEnvironBean;
import static org.mockito.ArgumentMatchers.argThat;
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
import com.pinterest.deployservice.buildtags.BuildTagsManager;
import com.pinterest.deployservice.chat.ChatManager;
import com.pinterest.deployservice.dao.AgentDAO;
import com.pinterest.deployservice.dao.BuildDAO;
import com.pinterest.deployservice.dao.DeployDAO;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.ScheduleDAO;
import com.pinterest.deployservice.dao.UtilDAO;
import com.pinterest.deployservice.email.MailManager;
import com.pinterest.teletraan.universal.events.AppEventPublisher;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class CommonHandlerTest {

    private DeployDAO deployDAO;
    private EnvironDAO environDAO;
    private BuildDAO buildDAO;
    private AgentDAO agentDAO;
    private UtilDAO utilDAO;
    private ScheduleDAO scheduleDAO;
    private AppEventPublisher publisher;
    private ChatManager chatManager;
    private MailManager mailManager;
    private BuildTagsManager buildTagsManager;
    private ExecutorService jobPool;
    private ServiceContext serviceContext;
    private CommonHandler commonHandler;

    @BeforeEach
    public void setUp() {
        deployDAO = mock(DeployDAO.class);
        environDAO = mock(EnvironDAO.class);
        buildDAO = mock(BuildDAO.class);
        agentDAO = mock(AgentDAO.class);
        utilDAO = mock(UtilDAO.class);
        scheduleDAO = mock(ScheduleDAO.class);
        publisher = mock(AppEventPublisher.class);
        chatManager = mock(ChatManager.class);
        mailManager = mock(MailManager.class);
        buildTagsManager = mock(BuildTagsManager.class);
        jobPool = mock(ExecutorService.class);

        serviceContext = new ServiceContext();
        serviceContext.setDeployDAO(deployDAO);
        serviceContext.setEnvironDAO(environDAO);
        serviceContext.setBuildDAO(buildDAO);
        serviceContext.setAgentDAO(agentDAO);
        serviceContext.setUtilDAO(utilDAO);
        serviceContext.setScheduleDAO(scheduleDAO);
        serviceContext.setAppEventPublisher(publisher);
        serviceContext.setChatManager(chatManager);
        serviceContext.setMailManager(mailManager);
        serviceContext.setBuildTagsManager(buildTagsManager);
        serviceContext.setJobPool(jobPool);
        serviceContext.setDeployBoardUrlPrefix("testDeployBoardUrlPrefix");

        commonHandler = new CommonHandler(serviceContext);
    }

    @Test
    public void testTransitionSchedule_NoSchedule() throws Exception {
        EnvironBean environ = createRandomEnvironBean();
        commonHandler.transitionSchedule(environ);
        verify(scheduleDAO, times(0)).getById(any());
    }

    @ParameterizedTest
    @MethodSource("inactiveScheduleStates")
    public void testTransitonSchedule_ScheduleInactive(ScheduleState scheduleState)
            throws Exception {
        // Set up mock data
        ScheduleBean schedule = createScheduleBean(scheduleState);
        EnvironBean environ = createRandomEnvironBean();
        environ.setSchedule_id(schedule.getId());
        when(scheduleDAO.getById(eq(schedule.getId()))).thenReturn(schedule);

        // Run the schedule
        commonHandler.transitionSchedule(environ);

        // Verify the result
        verify(scheduleDAO, times(1)).getById(eq(schedule.getId()));
        verify(scheduleDAO, times(0)).update(any(), any());
    }

    static Stream<Arguments> inactiveScheduleStates() {
        return Stream.of(
                Arguments.of(ScheduleState.NOT_STARTED), Arguments.of(ScheduleState.FINAL));
    }

    @ParameterizedTest
    @MethodSource("runningSchedules")
    public void testTransitonSchedule_ScheduleRunning(
            long finishedAgents, long totalAgents, String hostNumbers, int numScheduleUpdates)
            throws Exception {
        // Set up mock data
        ScheduleBean schedule = createScheduleBean(ScheduleState.RUNNING);
        schedule.setHost_numbers(hostNumbers);
        EnvironBean environ = createRandomEnvironBean();
        environ.setSchedule_id(schedule.getId());
        when(scheduleDAO.getById(eq(schedule.getId()))).thenReturn(schedule);
        when(agentDAO.countFinishedAgentsByDeploy(eq(environ.getDeploy_id())))
                .thenReturn(finishedAgents);
        when(agentDAO.countAgentByEnv(eq(environ.getEnv_id()))).thenReturn(totalAgents);

        // Run the schedule
        commonHandler.transitionSchedule(environ);

        // Verify the result
        verify(scheduleDAO, times(1)).getById(eq(schedule.getId()));
        verify(scheduleDAO, times(numScheduleUpdates))
                .update(
                        argThat(s -> s.getState() == ScheduleState.COOLING_DOWN),
                        eq(schedule.getId()));
    }

    static Stream<Arguments> runningSchedules() {
        return Stream.of(
                Arguments.of(0l, 6l, "1,2,3", 0),
                Arguments.of(1l, 6l, "1,2,3", 1),
                Arguments.of(0l, 3l, "3%,20%,60%", 0),
                Arguments.of(1l, 3l, "3%,20%,60%", 1),
                Arguments.of(2l, 100l, "3%,20%,60%", 0),
                Arguments.of(3l, 100l, "3%,20%,60%", 1));
    }

    @Test
    public void testTransitonSchedule_ScheduleCoolingDown() throws Exception {
        // Set up mock data
        ScheduleBean schedule = createScheduleBean(ScheduleState.COOLING_DOWN);
        EnvironBean environ = createRandomEnvironBean();
        environ.setSchedule_id(schedule.getId());
        when(scheduleDAO.getById(eq(schedule.getId()))).thenReturn(schedule);

        // Run the schedule
        commonHandler.transitionSchedule(environ);

        // Verify the result
        verify(scheduleDAO, times(1)).getById(eq(schedule.getId()));
        verify(scheduleDAO, times(1))
                .update(
                        argThat(
                                s ->
                                        s.getCurrent_session() == 2
                                                && s.getState() == ScheduleState.RUNNING),
                        eq(schedule.getId()));
    }

    @Test
    public void testTransitonSchedule_ScheduleInfiniteCooldown() throws Exception {
        // Set up mock data
        ScheduleBean schedule = createScheduleBean(ScheduleState.COOLING_DOWN);
        EnvironBean environ = createRandomEnvironBean();
        environ.setSchedule_id(schedule.getId());
        schedule.setCooldown_times("-1,-1,-1");
        when(scheduleDAO.getById(eq(schedule.getId()))).thenReturn(schedule);

        // Run the schedule
        commonHandler.transitionSchedule(environ);

        // Verify the result
        verify(scheduleDAO, times(1)).getById(eq(schedule.getId()));
        verify(scheduleDAO, times(0)).update(any(), any());
    }
}
