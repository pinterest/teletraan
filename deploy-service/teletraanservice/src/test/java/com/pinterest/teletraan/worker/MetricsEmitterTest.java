/**
 * Copyright (c) 2023-2024 Pinterest, Inc.
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

import static com.pinterest.deployservice.bean.BeanUtils.createHostBean;
import static com.pinterest.teletraan.universal.metrics.ErrorBudgetCounterFactory.ERROR_BUDGET_METRIC_NAME;
import static com.pinterest.teletraan.universal.metrics.ErrorBudgetCounterFactory.ERROR_BUDGET_TAG_NAME_RESPONSE_TYPE;
import static com.pinterest.teletraan.universal.metrics.ErrorBudgetCounterFactory.ERROR_BUDGET_TAG_VALUE_RESPONSE_TYPE_FAILURE;
import static com.pinterest.teletraan.universal.metrics.ErrorBudgetCounterFactory.ERROR_BUDGET_TAG_VALUE_RESPONSE_TYPE_SUCCESS;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.dao.DeployDAO;
import com.pinterest.deployservice.dao.HostAgentDAO;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.teletraan.universal.metrics.micrometer.PinStatsConfig;
import com.pinterest.teletraan.universal.metrics.micrometer.PinStatsMeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class MetricsEmitterTest {

    private HostAgentDAO hostAgentDAO;
    private HostDAO hostDAO;
    private DeployDAO deployDAO;
    private ServiceContext serviceContext;

    @BeforeClass
    public static void setUpClass() {
        Metrics.addRegistry(new SimpleMeterRegistry());
    }

    @Before
    public void setUp() {
        hostDAO = mock(HostDAO.class);
        hostAgentDAO = mock(HostAgentDAO.class);
        deployDAO = mock(DeployDAO.class);

        serviceContext = new ServiceContext();
        serviceContext.setHostAgentDAO(hostAgentDAO);
        serviceContext.setDeployDAO(deployDAO);
        serviceContext.setHostDAO(hostDAO);
    }

    @After
    public void tearDown() {
        Metrics.globalRegistry.clear();
    }

    @Test
    public void testReportDailyDeployCount() throws SQLException {
        when(deployDAO.getDailyDeployCount()).thenReturn(1L);
        assertEquals(1, MetricsEmitter.reportDailyDeployCount(deployDAO));
    }

    @Test
    public void testReportDailyDeployCount_exceptionHandling() throws SQLException {
        when(deployDAO.getDailyDeployCount()).thenThrow(new SQLException());
        assertEquals(0, MetricsEmitter.reportDailyDeployCount(deployDAO));
    }

    @Test
    public void testReportHostsCount() throws SQLException {
        when(hostAgentDAO.getDistinctHostsCount()).thenReturn(2L);
        assertEquals(2, MetricsEmitter.reportHostsCount(hostAgentDAO));
    }

    @Test
    public void testReportRunningDeployCount() throws SQLException {
        when(deployDAO.getRunningDeployCount()).thenReturn(3L);
        assertEquals(3, MetricsEmitter.reportRunningDeployCount(deployDAO));
    }

    @Test
    public void testMetricsEmitter() throws SQLException {
        new MetricsEmitter(serviceContext);

        Gauge deploysTotal = Metrics.globalRegistry.get(MetricsEmitter.DEPLOYS_TODAY_TOTAL).gauge();
        Gauge deploysRunning =
                Metrics.globalRegistry.get(MetricsEmitter.DEPLOYS_RUNNING_TOTAL).gauge();

        when(hostAgentDAO.getDistinctHostsCount()).thenReturn(2L);
        assertEquals(
                2, Metrics.globalRegistry.get(MetricsEmitter.HOSTS_TOTAL).gauge().value(), 0.01);

        when(deployDAO.getDailyDeployCount()).thenReturn(1L);
        assertEquals(1, deploysTotal.value(), 0.01);

        when(deployDAO.getDailyDeployCount()).thenReturn(5L);
        assertEquals(5, deploysTotal.value(), 0.01);

        when(deployDAO.getRunningDeployCount()).thenReturn(3L);
        assertEquals(3, deploysRunning.value(), 0.01);

        when(deployDAO.getRunningDeployCount()).thenReturn(2L);
        assertEquals(2, deploysRunning.value(), 0.01);
    }

    @Test
    public void testEmitLaunchingMetrics() throws SQLException {
        MockClock clock = new MockClock();
        MeterRegistry registry = new PinStatsMeterRegistry(PinStatsConfig.DEFAULT, clock);
        Metrics.globalRegistry.add(registry);
        MetricsEmitter sut = new MetricsEmitter(serviceContext, clock);

        long t1 = clock.wallTime();
        clock.add(Duration.ofMinutes(MetricsEmitter.LAUNCH_TIMEOUT_MINUTE + 1));
        long t2 = clock.wallTime();
        HostBean timeoutHost = createHostBean(Instant.ofEpochMilli(t1));
        HostBean normalHost = createHostBean(Instant.ofEpochMilli(t2));
        HostBean carryOverHost = createHostBean(Instant.ofEpochMilli(t2));
        HostBean cleanedUpHost = createHostBean(Instant.ofEpochMilli(t2));
        HostBean cleanedUpHost2 = createHostBean(Instant.ofEpochMilli(t2));

        // T2
        when(hostDAO.getAgentlessHosts(anyLong(), anyInt()))
                .thenReturn(Arrays.asList(timeoutHost, normalHost, carryOverHost, cleanedUpHost));
        sut.emitLaunchingMetrics();

        LongTaskTimer timer =
                registry.get(
                                String.format(
                                        MetricsEmitter.HOSTS_LAUNCHING,
                                        timeoutHost.getGroup_name()))
                        .longTaskTimer();
        Counter successCounter =
                Metrics.globalRegistry
                        .get(ERROR_BUDGET_METRIC_NAME)
                        .tag(
                                ERROR_BUDGET_TAG_NAME_RESPONSE_TYPE,
                                ERROR_BUDGET_TAG_VALUE_RESPONSE_TYPE_SUCCESS)
                        .counter();
        Counter failureCounter =
                Metrics.globalRegistry
                        .get(ERROR_BUDGET_METRIC_NAME)
                        .tag(
                                ERROR_BUDGET_TAG_NAME_RESPONSE_TYPE,
                                ERROR_BUDGET_TAG_VALUE_RESPONSE_TYPE_FAILURE)
                        .counter();
        assertEquals(4, timer.activeTasks());
        assertEquals(0, successCounter.count(), 0.01);
        assertEquals(0, failureCounter.count(), 0.01);

        // T2 + 1, normalHost launched
        clock.add(Duration.ofMinutes(1));
        when(hostDAO.getAgentlessHosts(anyLong(), anyInt()))
                .thenReturn(Arrays.asList(timeoutHost, carryOverHost, cleanedUpHost));
        sut.emitLaunchingMetrics();

        assertEquals(3, timer.activeTasks());
        assertEquals(1, successCounter.count(), 0.01);
        assertEquals(0, failureCounter.count(), 0.01);

        // T2 + 1 + LAUNCH_TIMEOUT_MINUTE, carryOverHost launched
        clock.add(Duration.ofMinutes(MetricsEmitter.LAUNCH_TIMEOUT_MINUTE));
        when(hostDAO.getAgentlessHosts(anyLong(), anyInt()))
                .thenReturn(Arrays.asList(timeoutHost, cleanedUpHost));
        sut.emitLaunchingMetrics();

        assertEquals(2, timer.activeTasks());
        assertEquals(1, successCounter.count(), 0.01);
        assertEquals(1, failureCounter.count(), 0.01);

        // T1 + 3 + MAX_TRACK_DURATION_MINUTE, timeoutHost no longer in the list
        clock.add(
                Duration.ofMinutes(
                        MetricsEmitter.MAX_TRACK_DURATION_MINUTE
                                - 2 * MetricsEmitter.LAUNCH_TIMEOUT_MINUTE));
        when(hostDAO.getAgentlessHosts(anyLong(), anyInt()))
                .thenReturn(Arrays.asList(cleanedUpHost));
        sut.emitLaunchingMetrics();

        assertEquals(1, timer.activeTasks());
        assertEquals(1, successCounter.count(), 0.01);
        assertEquals(2, failureCounter.count(), 0.01);

        // T2 + 2 + MAX_TRACK_DURATION_MINUTE, cleanedUpHost is cleaned up even if it
        // appears in the list
        clock.add(Duration.ofMinutes(MetricsEmitter.LAUNCH_TIMEOUT_MINUTE));
        when(hostDAO.getAgentlessHosts(anyLong(), anyInt()))
                .thenReturn(Arrays.asList(cleanedUpHost, cleanedUpHost2));
        sut.emitLaunchingMetrics();

        assertEquals(0, timer.activeTasks());
        assertEquals(1, successCounter.count(), 0.01);
        assertEquals(4, failureCounter.count(), 0.01);

        // When cleanedUpHost is removed from the list, the metrics won't change again
        when(hostDAO.getAgentlessHosts(anyLong(), anyInt())).thenReturn(Arrays.asList());
        sut.emitLaunchingMetrics();

        assertEquals(0, timer.activeTasks());
        assertEquals(1, successCounter.count(), 0.01);
        assertEquals(4, failureCounter.count(), 0.01);
    }
}
