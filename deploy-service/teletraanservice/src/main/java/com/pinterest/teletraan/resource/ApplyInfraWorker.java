package com.pinterest.teletraan.resource;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.bean.HostState;
import com.pinterest.deployservice.bean.WorkerJobBean;
import com.pinterest.deployservice.dao.UtilDAO;
import com.pinterest.deployservice.dao.WorkerJobDAO;
import com.pinterest.deployservice.handler.HostHandler;
import com.pinterest.deployservice.rodimus.RodimusManager;
import com.pinterest.teletraan.universal.metrics.ErrorBudgetCounterFactory;
import com.pinterest.teletraan.worker.HostTerminator;
import com.pinterest.teletraan.worker.WorkerTimerFactory;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;

public class ApplyInfraWorker implements Runnable {

  private static final int WORKER_JOB_BATCH_COUNT = 10;
  private static final Logger LOG = LoggerFactory.getLogger(ApplyInfraWorker.class);
  private static final Timer WORKER_TIMER =
          WorkerTimerFactory.createWorkerTimer(HostTerminator.class);

  private final RodimusManager rodimusManager;
  private final UtilDAO utilDAO;
  private final WorkerJobDAO workerJobDAO;
  private Counter errorBudgetSuccess;
  private Counter errorBudgetFailure;

  public ApplyInfraWorker(ServiceContext serviceContext) {
    workerJobDAO = serviceContext.getWorkerJobDAO();
    utilDAO = serviceContext.getUtilDAO();
    rodimusManager = serviceContext.getRodimusManager();
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
      LOG.info("Start to run HostTerminator");
      processBatch();

      errorBudgetSuccess.increment();
    } catch (Throwable t) {
      LOG.error("HostTerminator failed", t);

      errorBudgetFailure.increment();
    }
  }

  void processBatch() throws Exception {
    List<WorkerJobBean> workerJobBeans = workerJobDAO.getOldestByJobTypeStatus(WorkerJobBean.JobType.INFRA_APPLY, WorkerJobBean.Status.INITIALIZED, WORKER_JOB_BATCH_COUNT);
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
            workerJobDAO.updateStatus(id, WorkerJobBean.Status.RUNNING);
            applyInfra(workerJobBean);
            workerJobDAO.updateStatus(id, WorkerJobBean.Status.COMPLETED);
            LOG.info(String.format("Completed worker job for id %s", id));
          } else {
            LOG.info(String.format("Worker job is no longer in INITIALIZED state for id %s", id));
          }
        } catch (Exception e) {
          workerJobDAO.updateStatus(id, WorkerJobBean.Status.FAILED);
          LOG.error(
                  "Failed to process worker job id {}", workerJobBean.getId(), e);
        } finally {
          utilDAO.releaseLock(lockName, connection);
          LOG.info(
                  String.format(
                          "DB lock operation is successful: release lock %s", lockName));
        }
      } else {
        LOG.warn(String.format("DB lock operation fails: failed to get lock %s", lockName));
      }
    }
  }

  private void applyInfra(WorkerJobBean workerJobBean) {
    LOG.info(String.format("Doing work on %s", workerJobBean.getId()));
  }
}
