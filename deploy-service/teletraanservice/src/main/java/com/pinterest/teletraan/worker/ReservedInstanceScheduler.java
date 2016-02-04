package com.pinterest.teletraan.worker;


import com.amazonaws.services.ec2.model.InstanceType;
import com.pinterest.arcee.dao.ReservedInstanceInfoDAO;
import com.pinterest.arcee.metrics.MetricSource;
import com.pinterest.deployservice.ServiceContext;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReservedInstanceScheduler implements Runnable {
    private static Logger LOG = LoggerFactory.getLogger(ReservedInstanceScheduler.class);
    private ReservedInstanceInfoDAO reservedInstanceInfoDAO;
    private MetricSource metricSource;
    private static String FREEINSTANCE_METRIC_NAME = "running_instance.%s.count";
    private static int ALERT_THRESHOLD = 50;
    private static int THRESHOLD = 100;

    public ReservedInstanceScheduler(ServiceContext context) {
        reservedInstanceInfoDAO = context.getReservedInstanceInfoDAO();
        metricSource = context.getMetricSource();
    }


    public void scheduleReserveInstance(String instanceType) throws Exception {
        int reservedInstanceCount = reservedInstanceInfoDAO.getReservedInstanceCount(instanceType);
        int reservedRunningInstance = reservedInstanceInfoDAO.getRunningReservedInstanceCount(instanceType);
        int freeInstance = reservedInstanceCount - reservedRunningInstance;
        LOG.info(String.format("Reserved instance type: %s, reserved count: %d, running: %d, free %d", instanceType, reservedInstanceCount, reservedRunningInstance, freeInstance));
        metricSource.export(String.format(FREEINSTANCE_METRIC_NAME, instanceType), new HashMap<>(), (double)freeInstance, System.currentTimeMillis());
        if (freeInstance < ALERT_THRESHOLD) {
            //
        } else if (freeInstance < THRESHOLD) {
            // return instance
        } else {
            // TODO lend instance to the service
        }
    }

    @Override
    public void run() {
        try {
            List<InstanceType> instanceTypes = Arrays.asList(InstanceType.values());
            Collections.shuffle(instanceTypes);
            for (InstanceType type : instanceTypes) {
                LOG.info(String.format("Process instance type: %s", type.toString()));
                scheduleReserveInstance(type.toString());
            }

        } catch (Throwable t) {
            LOG.error("Failed to schedule instance.", t);
        }
    }
}
