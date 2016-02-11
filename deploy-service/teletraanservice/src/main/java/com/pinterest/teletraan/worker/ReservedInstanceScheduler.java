package com.pinterest.teletraan.worker;


import com.amazonaws.services.ec2.model.InstanceType;
import com.pinterest.arcee.Qubole.QuboleClusterBean;
import com.pinterest.arcee.Qubole.QuboleLeaseDAOImpl;
import com.pinterest.arcee.bean.ManagingGroupsBean;
import com.pinterest.arcee.dao.ManagingGroupDAO;
import com.pinterest.arcee.dao.ReservedInstanceInfoDAO;
import com.pinterest.arcee.metrics.MetricSource;
import com.pinterest.deployservice.ServiceContext;

import java.util.*;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReservedInstanceScheduler implements Runnable {
    private static Logger LOG = LoggerFactory.getLogger(ReservedInstanceScheduler.class);
    private ReservedInstanceInfoDAO reservedInstanceInfoDAO;
    private MetricSource metricSource;
    private ManagingGroupDAO managingGroupDAO;
    private static String FREEINSTANCE_METRIC_NAME = "running_instance.%s.count";
    private static String LENDING_FREEINSTANCE_METRIC_NAME = "lending_instance.%s.count";
    private static String QUBOLE_RUNNING_INSTANCE = "qubole.running_instance.count";
    private static int THRESHOLD = 100;
    private QuboleLeaseDAOImpl quboleLeaseDAO;

    public ReservedInstanceScheduler(ServiceContext context) {
        reservedInstanceInfoDAO = context.getReservedInstanceInfoDAO();
        managingGroupDAO = context.getManagingGroupDAO();
        metricSource = context.getMetricSource();
        quboleLeaseDAO = new QuboleLeaseDAOImpl(context.getQuboleAuthentication());
    }

    private ManagingGroupsBean pickLendingCandidate(String instanceType) throws Exception {
        // prototyping
        List<ManagingGroupsBean> managingGroupsBeans = managingGroupDAO.getManagingGroupsByInstanceType(instanceType);
        if (managingGroupsBeans.isEmpty()) {
            return null;
        } else {
            return managingGroupsBeans.get(0);
        }
    }

    // hard coded for stats
    private void reportQuboleStatus() throws Exception {
        QuboleClusterBean quboleClusterBean = quboleLeaseDAO.getCluster("13709");
        Long currentTimestamp = System.currentTimeMillis();
        metricSource.export(QUBOLE_RUNNING_INSTANCE, new HashMap<>(), (double)quboleClusterBean.getRunningReservedInstanceCount(), currentTimestamp);
        metricSource.export("resource_managing.qubole.min_size", new HashMap<>(), (double)quboleClusterBean.getMinSize(), currentTimestamp);
        metricSource.export("resource_managing.qubole.max_size", new HashMap<>(), (double)quboleClusterBean.getMaxSize(), currentTimestamp);
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

        String clusterName = managingGroupsBean.getGroup_name();
        int batchSize = managingGroupsBean.getBatch_size();
        int coolDown = managingGroupsBean.getCool_down();
        int lentSize = managingGroupsBean.getLent_size();
        int currentLendingSize = 0;
        if (freeInstance < THRESHOLD) {
            // return instance to the pool
            if (lentSize == 0) {
                LOG.info("Already returned all instances to the pool");
                metricSource.export(String.format(LENDING_FREEINSTANCE_METRIC_NAME, clusterName), new HashMap<>(), (double)currentLendingSize, currentTime);
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
            int toLendSize =  Math.min(batchSize, freeInstance - THRESHOLD);
            if (lentSize + toLendSize > managingGroupsBean.getMax_lending_size()) {
                toLendSize = managingGroupsBean.getMax_lending_size() - lentSize;
            }

            currentLendingSize = lentSize + toLendSize;
            LOG.info(String.format("can lend %d instances to the service", toLendSize));
            ManagingGroupsBean newManagingGroupsBean = new ManagingGroupsBean();
            newManagingGroupsBean.setLast_activity_time(currentTime);

            newManagingGroupsBean.setLent_size(currentLendingSize);
            managingGroupDAO.updateManagingGroup(clusterName, newManagingGroupsBean);
        }

        // hard coded it for now
        reportQuboleStatus();
        metricSource.export(String.format(LENDING_FREEINSTANCE_METRIC_NAME, clusterName), new HashMap<>(), (double)currentLendingSize, currentTime);
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
