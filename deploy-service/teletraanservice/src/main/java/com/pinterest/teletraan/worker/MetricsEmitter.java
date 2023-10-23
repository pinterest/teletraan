package com.pinterest.teletraan.worker;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.dao.DeployDAO;
import com.pinterest.deployservice.dao.HostAgentDAO;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;

public class MetricsEmitter implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(MetricsEmitter.class);

  static final String HOSTS_TOTAL = "hosts.total";
  static final String DEPLOYS_TODAY_TOTAL = "deploys.today.total";
  static final String DEPLOYS_RUNNING_TOTAL = "deploys.running.total";

  public MetricsEmitter(ServiceContext serviceContext) {
    // HostAgentDAO is more efficient than HostDAO to get total hosts
    Gauge.builder(HOSTS_TOTAL, serviceContext.getHostAgentDAO(), MetricsEmitter::reportHostsCount)
        .strongReference(true)
        .register(Metrics.globalRegistry);

    Gauge.builder(DEPLOYS_TODAY_TOTAL, serviceContext.getDeployDAO(), MetricsEmitter::reportDailyDeployCount)
        .strongReference(true)
        .register(Metrics.globalRegistry);
    Gauge.builder(DEPLOYS_RUNNING_TOTAL, serviceContext.getDeployDAO(), MetricsEmitter::reportRunningDeployCount)
        .strongReference(true)
        .register(Metrics.globalRegistry);
  }

  @Override
  public void run() {
    // noop
  }

  static int reportHostsCount(HostAgentDAO hostAgentDAO) {
    try {
      return hostAgentDAO.getDistinctHostsCount();
    } catch (SQLException e) {
      LOG.error("Failed to get host count", e);
    }
    return 0;
  }

  static int reportDailyDeployCount(DeployDAO deployDAO) {
    try {
      return (int) deployDAO.getDailyDeployCount();
    } catch (SQLException e) {
      LOG.error("Failed to get daily deploy count", e);
    }
    return 0;
  }

  static int reportRunningDeployCount(DeployDAO deployDAO) {
    try {
      return (int) deployDAO.getRunningDeployCount();
    } catch (SQLException e) {
      LOG.error("Failed to get running deploy count", e);
    }
    return 0;
  }
}
