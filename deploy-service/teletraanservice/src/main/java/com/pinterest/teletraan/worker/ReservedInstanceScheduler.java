package com.pinterest.teletraan.worker;


import com.amazonaws.services.ec2.model.InstanceType;
import com.pinterest.arcee.bean.ManagingGroupsBean;
import com.pinterest.arcee.dao.ManagingGroupDAO;
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
    private ManagingGroupDAO managingGroupDAO;
    private static String FREEINSTANCE_METRIC_NAME = "running_instance.%s.count";
    private static String LENDING_FREEINSTANCE_METRIC_NAME = "lending_instance.%s.count";
    private static int THRESHOLD = 100;

    public ReservedInstanceScheduler(ServiceContext context) {
        reservedInstanceInfoDAO = context.getReservedInstanceInfoDAO();
        managingGroupDAO = context.getManagingGroupDAO();
        metricSource = context.getMetricSource();
    }

    private ManagingGroupsBean pickLendingCandidate(String instanceType) throws Exception {
        // prototyping
        if (instanceType.equals("c3.8xlarge")) {
            return managingGroupDAO.getManagingGroupByGroupName("qubole");
        } else {
            return null;
        }
    }

    public void scheduleReserveInstance(String instanceType) throws Exception {
        int reservedInstanceCount = reservedInstanceInfoDAO.getReservedInstanceCount(instanceType);
        int reservedRunningInstance = reservedInstanceInfoDAO.getRunningReservedInstanceCount(instanceType);
        int freeInstance = reservedInstanceCount - reservedRunningInstance;
        LOG.info(String.format("Reserved instance type: %s, reserved count: %d, running: %d, free %d", instanceType,
                reservedInstanceCount, reservedRunningInstance, freeInstance));

        long currentTime = System.currentTimeMillis();
        metricSource.export(String.format(FREEINSTANCE_METRIC_NAME, instanceType), new HashMap<>(), (double)freeInstance, currentTime);
        ManagingGroupsBean managingGroupsBean = pickLendingCandidate(instanceType);
        if (managingGroupsBean == null) {
            return;
        }

        int batchSize = managingGroupsBean.getBatch_size();
        int coolDown = managingGroupsBean.getCool_down();
        int lentSize = managingGroupsBean.getLent_size();
        int currentLendingSize = 0;
        if (freeInstance < THRESHOLD) {
            // return instance to the pool
            if (lentSize == 0) {
                LOG.info("Already returned all instances to the pool");
                metricSource.export(String.format(LENDING_FREEINSTANCE_METRIC_NAME, "qubole"), new HashMap<>(), (double)currentLendingSize, currentTime);
                return;
            }
            if (currentTime - managingGroupsBean.getLast_activity_time() >= coolDown * 1000 * 60) {
                int returnSize = batchSize > lentSize ? lentSize : batchSize;
                currentLendingSize = lentSize - returnSize;
                ManagingGroupsBean newManagingGroupsBean = new ManagingGroupsBean();
                newManagingGroupsBean.setLast_activity_time(currentTime);
                newManagingGroupsBean.setLent_size(currentLendingSize);
                managingGroupDAO.updateManagingGroup("qubole", newManagingGroupsBean);
                LOG.info(String.format("need to return %d instances to the pool", returnSize));
            }
        } else {
            int lend_size =  freeInstance - THRESHOLD;
            LOG.info(String.format("can lend %d instances to the service", lend_size));
            ManagingGroupsBean newManagingGroupsBean = new ManagingGroupsBean();
            newManagingGroupsBean.setLast_activity_time(currentTime);
            currentLendingSize = lentSize + lend_size;
            newManagingGroupsBean.setLent_size(currentLendingSize);
            managingGroupDAO.updateManagingGroup("qubole", newManagingGroupsBean);
        }
        metricSource.export(String.format(LENDING_FREEINSTANCE_METRIC_NAME, "qubole"), new HashMap<>(), (double)currentLendingSize, currentTime);
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
