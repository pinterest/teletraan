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
            LOG.info("navi Start to run ApplyInfraWorker");
            processBatch();

            errorBudgetSuccess.increment();
        } catch (Throwable t) {
            LOG.error("navi ApplyInfraWorker failed", t);

            errorBudgetFailure.increment();
        }
    }

    private void processBatch() throws Exception {
        //        LOG.info("navid DB lock operation getting data: get lock");
        List<WorkerJobBean> workerJobBeans =
                workerJobDAO.getOldestByJobTypeStatus(
                        WorkerJobBean.JobType.INFRA_APPLY,
                        WorkerJobBean.Status.INITIALIZED,
                        WORKER_JOB_BATCH_COUNT);
        LOG.info(String.format("navid DB lock operation got data %s", workerJobBeans));
        Collections.shuffle(workerJobBeans);

        for (WorkerJobBean workerJobBean : workerJobBeans) {
            String id = workerJobBean.getId();
            String lockName = String.format("APPLY_INFRA-%s", id);
            Connection connection = utilDAO.getLock(lockName);

            if (connection != null) {
                LOG.info(
                        String.format(
                                "navid DB lock operation is successful: get lock %s", lockName));
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
                        LOG.info(String.format("navid Completed worker job for id %s", id));
                    } else {
                        LOG.info(
                                String.format(
                                        "navid Worker job is no longer in INITIALIZED state for id %s",
                                        id));
                    }
                } catch (Exception e) {
                    workerJobDAO.updateStatus(
                            workerJobBean, WorkerJobBean.Status.FAILED, System.currentTimeMillis());
                    LOG.error("navid Failed to process worker job id {}", workerJobBean.getId(), e);
                } finally {
                    utilDAO.releaseLock(lockName, connection);
                    LOG.info(
                            String.format(
                                    "navid DB lock operation is successful: release lock %s",
                                    lockName));
                }
            } else {
                LOG.warn(
                        String.format(
                                "navid DB lock operation fails: failed to get lock %s", lockName));
            }
        }
    }

    private void applyInfra(WorkerJobBean workerJobBean) throws Exception {
        LOG.info(String.format("Doing work on %s", workerJobBean.getId()));

        InfraConfigBean infraConfigBean =
                mapper.readValue(workerJobBean.getConfig(), InfraConfigBean.class);
        LOG.info(
                "navid Endpoint for getting status of applying infra configurations found job 2: {}",
                infraConfigBean);
        ClusterInfoPublicIdsBean newClusterInfoPublicIdsBean =
                ClusterInfoPublicIdsBean.fromInfraConfigBean(infraConfigBean);
        LOG.info(
                "navid Endpoint for getting status of applying infra configurations found job 3: {}, {}, {}, {}",
                infraConfigBean.getClusterName(),
                infraConfigBean.getEnvName(),
                infraConfigBean.getStageName(),
                newClusterInfoPublicIdsBean);
        LOG.info("navid 1");
        ClusterInfoPublicIdsBean existingClusterInfoPublicIdsBean =
                rodimusManager.getCluster(infraConfigBean.getClusterName());
        LOG.info("navid 2");
        if (existingClusterInfoPublicIdsBean == null) {
            LOG.info("navid 3");

            EnvironBean originEnvironBean =
                    Utils.getEnvStage(
                            environDAO,
                            infraConfigBean.getEnvName(),
                            infraConfigBean.getStageName());
            LOG.info("navid 3_1");
            try {
                EnvironBean updateEnvironBean =
                        originEnvironBean.withCluster_name(infraConfigBean.getClusterName());
                LOG.info("navid 3_2");
                environmentHandler.updateEnvironment(
                        infraConfigBean.getOperator(),
                        infraConfigBean.getEnvName(),
                        infraConfigBean.getStageName(),
                        updateEnvironBean);
                LOG.info("navid 3_3");
                environmentHandler.createCapacityForHostOrGroup(
                        infraConfigBean.getOperator(),
                        infraConfigBean.getEnvName(),
                        infraConfigBean.getStageName(),
                        Optional.of(EnvCapacities.CapacityType.GROUP),
                        infraConfigBean.getClusterName(),
                        originEnvironBean);
                LOG.info("navid 3_4");
                rodimusManager.createClusterWithEnvPublicIds(
                        infraConfigBean.getClusterName(),
                        infraConfigBean.getEnvName(),
                        infraConfigBean.getStageName(),
                        newClusterInfoPublicIdsBean);
                LOG.info("navid 3_5");
            } catch (Exception e) {
                LOG.info("navid 3_6");
                environmentHandler.updateEnvironment(
                        infraConfigBean.getOperator(),
                        infraConfigBean.getEnvName(),
                        infraConfigBean.getStageName(),
                        originEnvironBean);
                LOG.info("navid 3_7");
                environmentHandler.deleteCapacityForHostOrGroup(
                        infraConfigBean.getOperator(),
                        infraConfigBean.getEnvName(),
                        infraConfigBean.getStageName());
                LOG.info("navid 3_8");
                throw e;
            }

            LOG.info("navid 4");
        } else {
            LOG.info("navid 5");
            rodimusManager.updateClusterWithPublicIds(
                    infraConfigBean.getClusterName(), newClusterInfoPublicIdsBean);
            LOG.info("navid 6");
        }
    }
}
