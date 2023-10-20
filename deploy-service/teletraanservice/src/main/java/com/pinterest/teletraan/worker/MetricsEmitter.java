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
  private final AtomicInteger deployCount;

  public MetricsEmitter(ServiceContext serviceContext) {
    // HostAgentDAO is more efficient than HostDAO to get total hosts
    hostAgentDAO = serviceContext.getHostAgentDAO();
    deployDAO = serviceContext.getDeployDAO();

    hostCount = Metrics.gauge("hosts.total", new AtomicInteger(0));
    deployCount = Metrics.gauge("deploys.today.total", new AtomicInteger(0));
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
      deployCount.set((int) deployDAO.getDailyDeployCount());
    } catch (SQLException e) {
      LOG.error("Failed to get group count", e);
    }
  }

  @Override
  public void run() {
    try {
      reportHostsCount();
      reportDailyDeployCount();
    } catch (Exception e) {
      LOG.error("Failed to emit metrics", e);
    }
  }

}
