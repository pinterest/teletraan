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
package com.pinterest.teletraan.worker;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.AgentState;
import com.pinterest.deployservice.bean.HostAgentBean;
import com.pinterest.deployservice.dao.AgentDAO;
import com.pinterest.deployservice.dao.HostAgentDAO;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.dao.HostTagDAO;
import com.pinterest.deployservice.rodimus.RodimusManager;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AgentJanitorTest {
    private static String TEST_HOST_ID = "i-testHostId";

    private AgentDAO agentDAO;
    private HostDAO hostDAO;
    private HostAgentDAO hostAgentDAO;
    private RodimusManager rodimusManager;
    private HostTagDAO hostTagDAO;
    private ServiceContext serviceContext;
    private AgentJanitor agentJanitor;
    private final int maxStaleHostThresholdSeconds = 150;
    private final int minStaleHostThresholdSeconds = 600;
    private final int maxLaunchLatencyThresholdSeconds = 600;

    @BeforeAll
    public static void setUpClass() {
        Metrics.addRegistry(new SimpleMeterRegistry());
    }

    @BeforeEach
    public void setUp() {
        agentDAO = mock(AgentDAO.class);
        hostDAO = mock(HostDAO.class);
        hostAgentDAO = mock(HostAgentDAO.class);
        rodimusManager = mock(RodimusManager.class);
        hostTagDAO = mock(HostTagDAO.class);

        serviceContext = new ServiceContext();
        serviceContext.setAgentDAO(agentDAO);
        serviceContext.setHostDAO(hostDAO);
        serviceContext.setHostAgentDAO(hostAgentDAO);
        serviceContext.setRodimusManager(rodimusManager);
        serviceContext.setHostTagDAO(hostTagDAO);

        agentJanitor =
                new AgentJanitor(
                        serviceContext,
                        minStaleHostThresholdSeconds,
                        maxStaleHostThresholdSeconds,
                        maxLaunchLatencyThresholdSeconds);
    }

    @AfterEach
    public void tearDown() {
        Metrics.globalRegistry.clear();
    }

    @Test
    public void testProcessStaleHost_hostRemoved() throws Exception {
        // Set up mock data
        HostAgentBean hostAgentBean = createHostAgentBean();
        String hostId = hostAgentBean.getHost_id();
        when(hostAgentDAO.getStaleHosts(anyLong())).thenReturn(ImmutableList.of(hostAgentBean));
        when(rodimusManager.getTerminatedHosts(eq(ImmutableList.of(hostId))))
                .thenReturn(ImmutableSet.of(hostId));

        // Run the worker
        agentJanitor.run();

        // Verify the result
        verify(agentDAO, times(1)).deleteAllById(eq(hostId));
        verify(hostTagDAO, times(1)).deleteByHostId(eq(hostId));
        verify(hostAgentDAO, times(1)).delete(eq(hostId));
        verify(hostDAO, times(1)).deleteAllById(eq(hostId));
        verify(agentDAO, times(0)).updateAgentById(any(), any());
    }

    @Test
    public void testProcessStaleHost_hostUnreachable() throws Exception {
        // Set up mock data
        HostAgentBean hostAgentBean = createHostAgentBean();
        String hostId = hostAgentBean.getHost_id();
        when(hostAgentDAO.getStaleHosts(anyLong())).thenReturn(ImmutableList.of(hostAgentBean));
        when(rodimusManager.getTerminatedHosts(eq(ImmutableList.of(hostId))))
                .thenReturn(ImmutableSet.of());

        // Run the worker
        agentJanitor.run();

        // Verify the result
        verify(agentDAO, times(1))
                .updateAgentById(eq(hostId), argThat(a -> a.getState() == AgentState.UNREACHABLE));
        verify(agentDAO, times(0)).deleteAllById(eq(hostId));
        verify(hostTagDAO, times(0)).deleteByHostId(eq(hostId));
        verify(hostAgentDAO, times(0)).delete(eq(hostId));
        verify(hostDAO, times(0)).deleteAllById(eq(hostId));
    }

    @Test
    public void testDetermineStaleHostCandidates_hostRemoved() throws Exception {
        // Set up mock data
        HostAgentBean hostAgentBean = createHostAgentBean();
        String hostId = hostAgentBean.getHost_id();
        when(hostAgentDAO.getStaleHosts(anyLong(), anyLong()))
                .thenReturn(ImmutableList.of(hostAgentBean));
        when(rodimusManager.getTerminatedHosts(eq(ImmutableList.of(hostId))))
                .thenReturn(ImmutableSet.of(hostId));

        // Run the worker
        agentJanitor.run();

        // Verify the result
        verify(agentDAO, times(1)).deleteAllById(eq(hostId));
        verify(hostTagDAO, times(1)).deleteByHostId(eq(hostId));
        verify(hostAgentDAO, times(1)).delete(eq(hostId));
        verify(hostDAO, times(1)).deleteAllById(eq(hostId));
        verify(agentDAO, times(0)).updateAgentById(any(), any());
    }

    @Test
    public void testDetermineStaleHostCandidates_hostUnreachable() throws Exception {
        // Set up mock data
        HostAgentBean hostAgentBean = createHostAgentBean();
        String hostId = hostAgentBean.getHost_id();
        when(hostAgentDAO.getStaleHosts(anyLong(), anyLong()))
                .thenReturn(ImmutableList.of(hostAgentBean));
        when(rodimusManager.getTerminatedHosts(eq(ImmutableList.of(hostId))))
                .thenReturn(ImmutableSet.of());

        // Run the worker
        agentJanitor.run();

        // Verify the result
        verify(agentDAO, times(1))
                .updateAgentById(eq(hostId), argThat(a -> a.getState() == AgentState.UNREACHABLE));
        verify(agentDAO, times(0)).deleteAllById(any());
        verify(hostTagDAO, times(0)).deleteByHostId(any());
        verify(hostAgentDAO, times(0)).delete(any());
        verify(hostDAO, times(0)).deleteAllById(any());
    }

    @Test
    public void testCleanUpAgentlessHosts_hostRemoved() throws Exception {
        // Set up mock data
        HostAgentBean hostAgentBean = createHostAgentBean();
        String hostId = hostAgentBean.getHost_id();
        when(hostDAO.getStaleAgentlessHostIds(anyLong(), eq(300)))
                .thenReturn(ImmutableList.of(hostId));
        when(rodimusManager.getTerminatedHosts(eq(ImmutableList.of(hostId))))
                .thenReturn(ImmutableSet.of(hostId));

        // Run the worker
        agentJanitor.run();

        // Verify the result
        verify(agentDAO, times(1)).deleteAllById(eq(hostId));
        verify(hostTagDAO, times(1)).deleteByHostId(eq(hostId));
        verify(hostAgentDAO, times(1)).delete(eq(hostId));
        verify(hostDAO, times(1)).deleteAllById(eq(hostId));
        verify(agentDAO, times(0)).updateAgentById(any(), any());
    }

    private HostAgentBean createHostAgentBean() {
        HostAgentBean bean = new HostAgentBean();
        bean.setHost_id(TEST_HOST_ID);
        bean.setLast_update(0l);
        return bean;
    }
}
