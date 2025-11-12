/**
 * Copyright (c) 2025 Pinterest, Inc.
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
package com.pinterest.teletraan.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pinterest.deployservice.bean.ClusterInfoPublicIdsBean;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.InfraConfigBean;
import com.pinterest.deployservice.bean.WorkerJobBean;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.UtilDAO;
import com.pinterest.deployservice.dao.WorkerJobDAO;
import com.pinterest.deployservice.rodimus.RodimusManager;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.handler.EnvironmentHandler;
import com.pinterest.teletraan.universal.metrics.ErrorBudgetCounterFactory;
import com.pinterest.teletraan.worker.HostTerminator;
import com.pinterest.teletraan.worker.WorkerTimerFactory;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplyInfraWorker implements Runnable {

    private static final int WORKER_JOB_BATCH_COUNT = 10;
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Logger LOG = LoggerFactory.getLogger(ApplyInfraWorker.class);
    private static final Timer WORKER_TIMER =
            WorkerTimerFactory.createWorkerTimer(HostTerminator.class);

    private final EnvironDAO environDAO;
    private final EnvironmentHandler environmentHandler;
    private final RodimusManager rodimusManager;
    private final UtilDAO utilDAO;
    private final WorkerJobDAO workerJobDAO;
    private Counter errorBudgetSuccess;
    private Counter errorBudgetFailure;

    public ApplyInfraWorker(TeletraanServiceContext serviceContext) {
        environDAO = serviceContext.getEnvironDAO();
        environmentHandler = new EnvironmentHandler(serviceContext);
        rodimusManager = serviceContext.getRodimusManager();
        utilDAO = serviceContext.getUtilDAO();
        workerJobDAO = serviceContext.getWorkerJobDAO();
        errorBudgetSuccess =
                ErrorBudgetCounterFactory.createSuccessCounter(this.getClass().getSimpleName());
        errorBudgetFailure =
                ErrorBudgetCounterFactory.createFailureCounter(this.getClass().getSimpleName());
    }

    @Override
    public void run() {
        WORKER_TIMER.record(() -> runInternal());
    }

    private void runInternal() {
        try {
            LOG.info("Start to run ApplyInfraWorker");
            processBatch();

            errorBudgetSuccess.increment();
        } catch (Throwable t) {
            LOG.error("ApplyInfraWorker failed", t);
            errorBudgetFailure.increment();
        }
    }

    private void processBatch() throws Exception {
        List<WorkerJobBean> workerJobBeans =
                workerJobDAO.getOldestByJobTypeStatus(
                        WorkerJobBean.JobType.INFRA_APPLY,
                        WorkerJobBean.Status.INITIALIZED,
                        WORKER_JOB_BATCH_COUNT);
        Collections.shuffle(workerJobBeans);

        for (WorkerJobBean workerJobBean : workerJobBeans) {
            String id = workerJobBean.getId();
            String lockName = String.format("APPLY_INFRA-%s", id);
            Connection connection = utilDAO.getLock(lockName);

            if (connection != null) {
                LOG.info(String.format("DB lock operation is successful: get lock %s", lockName));
                try {
                    WorkerJobBean latestWorkerJobBean = workerJobDAO.getById(id);
                    if (latestWorkerJobBean.getStatus() == WorkerJobBean.Status.INITIALIZED) {
                        workerJobDAO.updateStatus(
                                workerJobBean,
                                WorkerJobBean.Status.RUNNING,
                                System.currentTimeMillis());
                        applyInfra(workerJobBean);
                        workerJobDAO.updateStatus(
                                workerJobBean,
                                WorkerJobBean.Status.COMPLETED,
                                System.currentTimeMillis());
                    }
                } catch (Exception e) {
                    workerJobDAO.updateStatus(
                            workerJobBean, WorkerJobBean.Status.FAILED, System.currentTimeMillis());
                    LOG.error("Failed to process worker job id {}", workerJobBean.getId(), e);
                } finally {
                    utilDAO.releaseLock(lockName, connection);
                    LOG.info(
                            String.format(
                                    "DB lock operation is successful: release lock %s", lockName));
                }
            } else {
                LOG.warn(String.format("DB lock operation fails: failed to get lock %s", lockName));
                LOG.warn(String.format("DB lock operation fails: failed to get lock %s", lockName));
            }
        }
    }

    private void applyInfra(WorkerJobBean workerJobBean) throws Exception {
        InfraConfigBean infraConfigBean =
                mapper.readValue(workerJobBean.getConfig(), InfraConfigBean.class);
        String operator = infraConfigBean.getOperator();
        String envName = infraConfigBean.getEnvName();
        String stageName = infraConfigBean.getStageName();
        String clusterName = infraConfigBean.getClusterName();

        ClusterInfoPublicIdsBean newClusterInfoPublicIdsBean =
                ClusterInfoPublicIdsBean.fromInfraConfigBean(infraConfigBean);
        ClusterInfoPublicIdsBean existingClusterInfoPublicIdsBean =
                rodimusManager.getCluster(clusterName);
        if (existingClusterInfoPublicIdsBean == null) {
            EnvironBean originEnvironBean = Utils.getEnvStage(environDAO, envName, stageName);
            try {
                EnvironBean updateEnvironBean = originEnvironBean.withCluster_name(clusterName);
                environmentHandler.updateEnvironment(
                        operator, envName, stageName, updateEnvironBean);
                environmentHandler.createCapacityForHostOrGroup(
                        operator,
                        envName,
                        stageName,
                        Optional.of(EnvCapacities.CapacityType.GROUP),
                        clusterName,
                        originEnvironBean);
                rodimusManager.createClusterWithEnvPublicIds(
                        clusterName, envName, stageName, newClusterInfoPublicIdsBean);
            } catch (Exception e) {
                environmentHandler.updateEnvironment(
                        operator, envName, stageName, originEnvironBean);
                environmentHandler.deleteCapacityForHostOrGroup(
                        operator,
                        envName,
                        stageName,
                        Optional.of(EnvCapacities.CapacityType.GROUP),
                        clusterName);
                throw e;
            }
        } else {
            rodimusManager.updateClusterWithPublicIds(clusterName, newClusterInfoPublicIdsBean);
        }
    }
}
