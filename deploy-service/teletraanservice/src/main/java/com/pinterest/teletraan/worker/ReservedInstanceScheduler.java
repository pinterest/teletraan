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

    private int lendInstances(ManagingGroupsBean managingGroupsBean, int freeInstances) throws Exception {
        String clusterName = managingGroupsBean.getGroup_name();
        int lentSize = managingGroupsBean.getLent_size();
        int maxLendingSize = managingGroupsBean.getMax_lending_size();
        int toLendSize;
        if (lentSize + freeInstances > maxLendingSize) {
            toLendSize = maxLendingSize - lentSize;
        } else {
            toLendSize = freeInstances;
        }
        LOG.info(String.format("Cluster: %s, current lending: %d, free instances: %d, to lend: %d", clusterName, lentSize, freeInstances, toLendSize));
        int currentLendingSize = toLendSize + lentSize;
        ManagingGroupsBean newManagingGroupsBean = new ManagingGroupsBean();
        Long currentTime = System.currentTimeMillis();
        newManagingGroupsBean.setLast_activity_time(currentTime);
        newManagingGroupsBean.setLent_size(currentLendingSize);
        managingGroupDAO.updateManagingGroup(clusterName, newManagingGroupsBean);

        // lend instance to qubole
        // quboleLeaseDAO.lendInstances(managingGroupsBean.getGroup_name(), toLendSize);
        // reportQuboleStatus(managingGroupsBean.getGroup_name());

        metricSource.export(String.format(LENDING_FREEINSTANCE_METRIC_NAME, clusterName), new HashMap<>(), (double)currentLendingSize, currentTime);
        // schedule work for
        return toLendSize;
    }

    private int returnInstances(ManagingGroupsBean managingGroupsBean) throws Exception {
        String clusterName = managingGroupsBean.getGroup_name();
        Long currTime = System.currentTimeMillis();
        int lentSize = managingGroupsBean.getLent_size();
        if (lentSize == 0) {
            LOG.info(String.format("Cluster: %s, current lending: %d. Nothing to return.", clusterName, lentSize));
            metricSource.export(String.format(LENDING_FREEINSTANCE_METRIC_NAME, clusterName), new HashMap<>(), (double)lentSize, currTime);
            return 0;
        }

        Long lastUpdateTime = managingGroupsBean.getLast_activity_time();
        int coolDown = managingGroupsBean.getCool_down();
        if (currTime - lastUpdateTime <  coolDown * 1000 * 60) {
            LOG.info(String.format("Last activity: %d, Now: %d Still in the cooldown period. Skip", lastUpdateTime, currTime));
            metricSource.export(String.format(LENDING_FREEINSTANCE_METRIC_NAME, clusterName), new HashMap<>(), (double)lentSize, currTime);
            return 0;
        }
        int batchSize = managingGroupsBean.getBatch_size();
        int returnSize = Math.min(batchSize, lentSize);
        int currentLendingSize = lentSize - returnSize;
        LOG.info(String.format("Cluster: %s, current lending: %d, returning: %d", clusterName, lentSize,returnSize));
        ManagingGroupsBean newManageGroupsBean = new ManagingGroupsBean();
        newManageGroupsBean.setLast_activity_time(currTime);
        newManageGroupsBean.setLent_size(currentLendingSize);
        managingGroupDAO.updateManagingGroup(clusterName,  newManageGroupsBean);

        metricSource.export(String.format(LENDING_FREEINSTANCE_METRIC_NAME, clusterName), new HashMap<>(), (double)currentLendingSize, currTime);
        //quboleLeaseDAO.returnInstances(clusterName, returnSize);
        //reportQuboleStatus(clusterName);
        return returnSize;
    }

    // hard coded for stats
    private void reportQuboleStatus(String clusterId) throws Exception {
        QuboleClusterBean quboleClusterBean = quboleLeaseDAO.getCluster(clusterId);
        Long currentTimestamp = System.currentTimeMillis();
        metricSource.export(QUBOLE_RUNNING_INSTANCE, new HashMap<>(), (double)quboleClusterBean.getRunningReservedInstanceCount(), currentTimestamp);
        metricSource.export("resource_managing.qubole.min_size", new HashMap<>(), (double)quboleClusterBean.getMinSize(), currentTimestamp);
        metricSource.export("resource_managing.qubole.max_size", new HashMap<>(), (double)quboleClusterBean.getMaxSize(), currentTimestamp);
    }

    public void scheduleReserveInstances(String instanceType) throws Exception {
        int reservedInstanceCount = reservedInstanceInfoDAO.getReservedInstanceCount(instanceType);
        int reservedRunningInstance = reservedInstanceInfoDAO.getRunningReservedInstanceCount(instanceType);
        int freeInstance = reservedInstanceCount - reservedRunningInstance;
        LOG.info(String.format("Reserved instance type: %s, reserved count: %d, running: %d, free %d", instanceType,
                reservedInstanceCount, reservedRunningInstance, freeInstance));
        long currentTime = System.currentTimeMillis();
        metricSource.export(String.format(FREEINSTANCE_METRIC_NAME, instanceType), new HashMap<>(), (double)freeInstance, currentTime);

        if (freeInstance > THRESHOLD) {
            freeInstance -= THRESHOLD;
            Collection<ManagingGroupsBean> managingGroupsBeans = managingGroupDAO.getLendManagingGroupsByInstanceType(instanceType);
            for (ManagingGroupsBean managingGroupsBean : managingGroupsBeans) {
                LOG.info(String.format("Free instance: %d, next service to lend: %s", freeInstance, managingGroupsBean.getGroup_name()));
                int toLendSize = lendInstances(managingGroupsBean, freeInstance);
                freeInstance -= toLendSize;
                if (freeInstance <= 0) {
                    break;
                }
            }
        } else {
            Collection<ManagingGroupsBean> managingGroupsBeans = managingGroupDAO.getReturnManagingGroupsByInstanceType(instanceType);
            for (ManagingGroupsBean managingGroupsBean : managingGroupsBeans) {
                LOG.info(String.format("Free instance: %d, next service to return: %s", freeInstance, managingGroupsBean.getGroup_name()));
                int returnSize = returnInstances(managingGroupsBean);
                freeInstance += returnSize;
                if (freeInstance > THRESHOLD) {
                    break;
                }
            }
        }
    }

    @Override
    public void run() {
        try {
            List<InstanceType> instanceTypes = Arrays.asList(InstanceType.values());
            Collections.shuffle(instanceTypes);
            for (InstanceType type : instanceTypes) {
                LOG.info(String.format("Process instance type: %s", type.toString()));
                scheduleReserveInstances(type.toString());
            }

        } catch (Throwable t) {
            LOG.error("Failed to schedule instance.", t);
        }
    }
}
