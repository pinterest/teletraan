package com.pinterest.teletraan.worker;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.dao.DeployDAO;
import com.pinterest.deployservice.dao.HostAgentDAO;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

public class MetricsEmitterTest {

  private HostAgentDAO hostAgentDAO;
  private DeployDAO deployDAO;

  @BeforeClass
  public static void setUpClass() {
    Metrics.addRegistry(new SimpleMeterRegistry());
  }

  @Before
  public void setUp() {
    hostAgentDAO = mock(HostAgentDAO.class);
    deployDAO = mock(DeployDAO.class);
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
    ServiceContext serviceContext = new ServiceContext();
    serviceContext.setHostAgentDAO(hostAgentDAO);
    serviceContext.setDeployDAO(deployDAO);

    MetricsEmitter sut = new MetricsEmitter(serviceContext);

    when(hostAgentDAO.getDistinctHostsCount()).thenReturn(2L);
    assertEquals(2, Metrics.globalRegistry.get(MetricsEmitter.HOSTS_TOTAL).gauge().value(), 0.01);

    when(deployDAO.getDailyDeployCount()).thenReturn(1L);
    assertEquals(1, Metrics.globalRegistry.get(MetricsEmitter.DEPLOYS_TODAY_TOTAL).gauge().value(), 0.01);

    when(deployDAO.getDailyDeployCount()).thenReturn(5L);
    assertEquals(5, Metrics.globalRegistry.get(MetricsEmitter.DEPLOYS_TODAY_TOTAL).gauge().value(), 0.01);

    when(deployDAO.getRunningDeployCount()).thenReturn(3L);
    assertEquals(3, Metrics.globalRegistry.get(MetricsEmitter.DEPLOYS_RUNNING_TOTAL).gauge().value(), 0.01);

    when(deployDAO.getRunningDeployCount()).thenReturn(2L);
    assertEquals(2, Metrics.globalRegistry.get(MetricsEmitter.DEPLOYS_RUNNING_TOTAL).gauge().value(), 0.01);
  }
}
