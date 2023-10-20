package com.pinterest.teletraan.worker;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.dao.DeployDAO;
import com.pinterest.deployservice.dao.HostAgentDAO;

import io.micrometer.core.instrument.Metrics;

import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;

public class MetricsEmitter implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(MetricsEmitter.class);

  private final HostAgentDAO hostAgentDAO;
  private final DeployDAO deployDAO;

  private final AtomicInteger hostCount;
  private final AtomicInteger dailyDeployCount;
  private final AtomicInteger runningDeployCount;

  public MetricsEmitter(ServiceContext serviceContext) {
    // HostAgentDAO is more efficient than HostDAO to get total hosts
    hostAgentDAO = serviceContext.getHostAgentDAO();
    deployDAO = serviceContext.getDeployDAO();

    hostCount = Metrics.gauge("hosts.total", new AtomicInteger(0));
    dailyDeployCount = Metrics.gauge("deploys.today.total", new AtomicInteger(0));
    runningDeployCount = Metrics.gauge("deploys.running.total", new AtomicInteger(0));
  }

  void reportHostsCount() {
    try {
      hostCount.set(hostAgentDAO.getDistinctHostsCount());
    } catch (SQLException e) {
      LOG.error("Failed to get host count", e);
    }
  }

  void reportDailyDeployCount() {
    try {
      dailyDeployCount.set((int) deployDAO.getDailyDeployCount());
    } catch (SQLException e) {
      LOG.error("Failed to get daily deploy count", e);
    }
  }

  void reportRunningDeployCount() {
    try {
      runningDeployCount.set((int) deployDAO.getRunningDeployCount());
    } catch (SQLException e) {
      LOG.error("Failed to get running deploy count", e);
    }
  }

  @Override
  public void run() {
    try {
      reportHostsCount();
      reportDailyDeployCount();
      reportRunningDeployCount();
    } catch (Exception e) {
      LOG.error("Failed to emit metrics", e);
    }
  }

}
