package com.pinterest.teletraan.worker;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

  private MetricsEmitter sut;
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

    ServiceContext serviceContext = new ServiceContext();
    serviceContext.setHostAgentDAO(hostAgentDAO);
    serviceContext.setDeployDAO(deployDAO);

    sut = new MetricsEmitter(serviceContext);
  }

  @After
  public void tearDown() {
    Metrics.globalRegistry.clear();
  }

  @Test
  public void testReportDailyDeployCount() throws SQLException {
    when(deployDAO.getDailyDeployCount()).thenReturn(1L);
    sut.reportDailyDeployCount();
    assertEquals(1, Metrics.globalRegistry.get(MetricsEmitter.DEPLOYS_TODAY_TOTAL).gauge().value(), 0.01);

    when(deployDAO.getDailyDeployCount()).thenReturn(5L);
    sut.reportDailyDeployCount();
    assertEquals(5, Metrics.globalRegistry.get(MetricsEmitter.DEPLOYS_TODAY_TOTAL).gauge().value(), 0.01);
  }

  @Test
  public void testReportHostsCount() throws SQLException {
    when(hostAgentDAO.getDistinctHostsCount()).thenReturn(2);
    sut.reportHostsCount();
    assertEquals(2, Metrics.globalRegistry.get(MetricsEmitter.HOSTS_TOTAL).gauge().value(), 0.01);
  }

  @Test
  public void testReportRunningDeployCount() throws SQLException {
    when(deployDAO.getRunningDeployCount()).thenReturn(3L);
    sut.reportRunningDeployCount();
    assertEquals(3, Metrics.globalRegistry.get(MetricsEmitter.DEPLOYS_RUNNING_TOTAL).gauge().value(), 0.01);

    when(deployDAO.getRunningDeployCount()).thenReturn(2L);
    sut.reportRunningDeployCount();
    assertEquals(2, Metrics.globalRegistry.get(MetricsEmitter.DEPLOYS_RUNNING_TOTAL).gauge().value(), 0.01);
  }

  @Test
  public void testRun() throws SQLException{
    sut.run();
    verify(hostAgentDAO).getDistinctHostsCount();
    verify(deployDAO).getRunningDeployCount();
    verify(deployDAO).getDailyDeployCount();
  }
}
