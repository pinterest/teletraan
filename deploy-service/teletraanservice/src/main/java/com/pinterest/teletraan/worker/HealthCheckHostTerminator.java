package com.pinterest.teletraan.worker;


import com.pinterest.arcee.bean.HealthCheckBean;
import com.pinterest.arcee.dao.HealthCheckDAO;
import com.pinterest.arcee.dao.HostInfoDAO;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.bean.HostState;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.dao.UtilDAO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class HealthCheckHostTerminator implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(HealthCheckHostTerminator.class);
    private static final long timeToRetain = 2 * 60 * 60 * 1000; // 2 hour
    private final HealthCheckDAO healthCheckDAO;
    private final HostDAO hostDAO;
    private final HostInfoDAO hostInfoDAO;
    private final UtilDAO utilDAO;

    public HealthCheckHostTerminator(ServiceContext serviceContext) {
        healthCheckDAO = serviceContext.getHealthCheckDAO();
        hostDAO = serviceContext.getHostDAO();
        hostInfoDAO = serviceContext.getHostInfoDAO();
        utilDAO = serviceContext.getUtilDAO();
    }

    public void processBatch() throws Exception {
        List<HealthCheckBean> healthCheckBeans = healthCheckDAO.getHealthChecksByUnterminatedHosts();
        if (healthCheckBeans.isEmpty()) {
            return;
        }

        Collections.shuffle(healthCheckBeans);
        for (HealthCheckBean bean : healthCheckBeans) {
            String hostId = bean.getHost_id();
            String lockName = String.format("HEALTHCHECKHOSTTERMINATOR-%s", hostId);
            Connection connection = utilDAO.getLock(lockName);
            if (connection != null) {
                try {
                    if (System.currentTimeMillis() - bean.getHost_launch_time() > timeToRetain) {
                        LOG.info(String.format("This health check failed host %s has been retained for more than 3 hours since "
                            + "it's launched.", hostId));

                        List<String> runningIds = hostInfoDAO.getRunningInstances(Arrays.asList(hostId));
                        if (!runningIds.isEmpty()) {
                            LOG.info(String.format("This host %s is still running. Terminate it", hostId));
                            hostInfoDAO.terminateHost(hostId);

                            HostBean hostBean = new HostBean();
                            hostBean.setState(HostState.TERMINATING);
                            hostBean.setLast_update(System.currentTimeMillis());
                            hostDAO.updateHostById(hostId, hostBean);
                        }

                        HealthCheckBean newBean = new HealthCheckBean();
                        newBean.setHost_terminated(true);
                        healthCheckDAO.updateHealthCheckById(bean.getId(), newBean);
                    }
                } catch (Exception e) {
                    LOG.error("Failed to process failed health check host {}", hostId, e);
                } finally {
                    utilDAO.releaseLock(lockName, connection);
                }
            } else {
                LOG.warn(String.format("Failed to get lock: %s", lockName));
            }
        }
    }

    @Override
    public void run() {
        try {
            LOG.info("Start to run HealthCheckHostTerminator");
            processBatch();
        } catch (Throwable t) {
            LOG.error("Faile to run HealthCheckHostTerminator", t);
        }
    }
}
