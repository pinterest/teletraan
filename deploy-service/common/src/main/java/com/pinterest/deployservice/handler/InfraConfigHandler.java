package com.pinterest.deployservice.handler;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.RodimusClusterBody;
import com.pinterest.deployservice.rodimus.RodimusManager;
import com.pinterest.teletraan.universal.metrics.ErrorBudgetCounterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InfraConfigHandler {
  private static final Logger LOG = LoggerFactory.getLogger(InfraConfigHandler.class);
  private final RodimusManager rodimusManager;

  public InfraConfigHandler(ServiceContext serviceContext) {
    rodimusManager = serviceContext.getRodimusManager();
  }

  public void test(String clusterName) throws Exception {
    RodimusClusterBody rodimusClusterBody = rodimusManager.getCluster(clusterName);
    LOG.error("Cluster body 123: " + rodimusClusterBody);
  }
}
