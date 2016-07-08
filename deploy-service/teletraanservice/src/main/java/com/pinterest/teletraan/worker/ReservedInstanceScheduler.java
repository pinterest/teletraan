/**
 * Copyright 2016 Pinterest, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pinterest.teletraan.worker;


import com.amazonaws.services.ec2.model.InstanceType;
import com.pinterest.arcee.lease.InstanceLeaseManager;
import com.pinterest.arcee.lease.QuboleLeaseManager;
import com.pinterest.arcee.bean.ManagingGroupsBean;
import com.pinterest.arcee.bean.ResourceType;
import com.pinterest.arcee.dao.LeaseDAO;
import com.pinterest.arcee.dao.ManagingGroupDAO;
import com.pinterest.arcee.dao.ReservedInstanceInfoDAO;
import com.pinterest.arcee.metrics.MetricSource;
import com.pinterest.deployservice.ServiceContext;

import java.sql.Connection;
import java.util.*;

import com.pinterest.deployservice.dao.UtilDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReservedInstanceScheduler implements Runnable {
    private static Logger LOG = LoggerFactory.getLogger(ReservedInstanceScheduler.class);
    private ReservedInstanceInfoDAO reservedInstanceInfoDAO;
    private MetricSource metricSource;
    private ManagingGroupDAO managingGroupDAO;
    private UtilDAO utilDAO;
    private static String FREEINSTANCE_METRIC_NAME = "free_reserved_instance.%s.count";
    private static String LENDING_FREEINSTANCE_METRIC_NAME = "lending_instance.%s.count";
    private static int THRESHOLD = 100;
    private static InstanceType[] MANAGING_INSTANCE_TYPE = {InstanceType.C38xlarge, InstanceType.C32xlarge};
    private HashSet<InstanceType> managingInstanceType;
    private ServiceContext serviceContext;

    public ReservedInstanceScheduler(ServiceContext context) {
        reservedInstanceInfoDAO = context.getReservedInstanceInfoDAO();
        managingGroupDAO = context.getManagingGroupDAO();
        utilDAO = context.getUtilDAO();
        metricSource = context.getMetricSource();
        serviceContext = context;
        managingInstanceType = new HashSet<>(Arrays.asList(MANAGING_INSTANCE_TYPE));
    }

    private LeaseDAO getLeaseDAO(ResourceType resourceType) {
        if (resourceType == ResourceType.QUBOLE) {
            return new QuboleLeaseManager(serviceContext);
        } else {
            return new InstanceLeaseManager(serviceContext);
        }
    }

    private int lendInstances(ManagingGroupsBean managingGroupsBean, int freeInstances) throws Exception {
        String clusterName = managingGroupsBean.getGroup_name();
        Long currentTime = System.currentTimeMillis();
        Long lastUpdateTime = managingGroupsBean.getLast_activity_time();
        int coolDown = managingGroupsBean.getCool_down();
        if (currentTime - lastUpdateTime <  coolDown * 1000 * 60) {
            LOG.info(String.format("Last activity: %d, Now: %d Still in the cooldown period. Skip", lastUpdateTime, currentTime));
            return 0;
        }

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
        // lend instance to lease
        getLeaseDAO(managingGroupsBean.getResource_type()).lendInstances(managingGroupsBean.getGroup_name(), toLendSize);
        metricSource.export(String.format(LENDING_FREEINSTANCE_METRIC_NAME, clusterName), new HashMap<>(), (double)currentLendingSize, currentTime);

        ManagingGroupsBean newManagingGroupsBean = new ManagingGroupsBean();
        newManagingGroupsBean.setLast_activity_time(currentTime);
        newManagingGroupsBean.setLent_size(currentLendingSize);
        managingGroupDAO.updateManagingGroup(clusterName, newManagingGroupsBean);
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


        int batchSize = managingGroupsBean.getBatch_size();
        int returnSize = Math.min(batchSize, lentSize);
        int currentLendingSize = lentSize - returnSize;
        LOG.info(String.format("Cluster: %s, current lending: %d, returning: %d", clusterName, lentSize,returnSize));


        metricSource.export(String.format(LENDING_FREEINSTANCE_METRIC_NAME, clusterName), new HashMap<>(), (double)currentLendingSize, currTime);
        getLeaseDAO(managingGroupsBean.getResource_type()).returnInstances(clusterName, returnSize);

        ManagingGroupsBean newManageGroupsBean = new ManagingGroupsBean();
        newManageGroupsBean.setLast_activity_time(currTime);
        newManageGroupsBean.setLent_size(currentLendingSize);
        managingGroupDAO.updateManagingGroup(clusterName,  newManageGroupsBean);
        return returnSize;
    }

    public void scheduleReserveInstances(String instanceType) throws Exception {
        String lockId = String.format("RESOURCE_MANAGE_%s", instanceType);
        Connection connection = utilDAO.getLock(lockId);
        if (connection == null) {
            LOG.info("Failed to obtain lock, skip");
            return;
        }

        try {
            int reservedInstanceCount = reservedInstanceInfoDAO.getReservedInstanceCount(instanceType);
            int reservedRunningInstance = reservedInstanceInfoDAO.getRunningReservedInstanceCount(instanceType);
            int freeInstance = reservedInstanceCount - reservedRunningInstance;
            LOG.info(String.format("Reserved instance type: %s, reserved count: %d, running: %d, free %d", instanceType,
                    reservedInstanceCount, reservedRunningInstance, freeInstance));
            long currentTime = System.currentTimeMillis();
            metricSource.export(String.format(FREEINSTANCE_METRIC_NAME, instanceType), new HashMap<>(), (double) freeInstance, currentTime);

            if (freeInstance > THRESHOLD) {
                freeInstance -= THRESHOLD;
                Collection<ManagingGroupsBean> managingGroupsBeans = managingGroupDAO.getLendManagingGroupsByInstanceType(instanceType);
                for (ManagingGroupsBean managingGroupsBean : managingGroupsBeans) {
                    try {
                        LOG.info(String.format("Free instance: %d, next service to lend: %s",
                                               freeInstance, managingGroupsBean.getGroup_name()));
                        int toLendSize = lendInstances(managingGroupsBean, freeInstance);
                        freeInstance -= toLendSize;
                        if (freeInstance <= 0) {
                            break;
                        }
                    } catch (Exception ex) {
                        LOG.error(String.format("Failed to lend instances to group: %s", managingGroupsBean.getGroup_name()), ex);
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
        } catch (Exception ex) {
            LOG.error(String.format("Failed to schedule free reserved instance type: %s", instanceType), ex);
        } finally {
            utilDAO.releaseLock(lockId, connection);
        }
    }

    @Override
    public void run() {
        try {
            List<InstanceType> instanceTypes = Arrays.asList(InstanceType.values());
            Collections.shuffle(instanceTypes);
            for (InstanceType type : instanceTypes) {
                if (!managingInstanceType.contains(type)) {
                    continue;
                }

                LOG.info(String.format("Process instance type: %s", type.toString()));
                scheduleReserveInstances(type.toString());
                Thread.sleep(500);
            }

        } catch (Throwable t) {
            LOG.error("Failed to schedule instance.", t);
        }
    }
}
