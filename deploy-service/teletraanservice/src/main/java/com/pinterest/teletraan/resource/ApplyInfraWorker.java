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
import com.pinterest.deployservice.bean.rodimus.RodimusAutoScalingAlarm;
import com.pinterest.deployservice.bean.rodimus.RodimusAutoScalingPolicies;
import com.pinterest.deployservice.bean.rodimus.RodimusAutoScalingPolicy;
import com.pinterest.deployservice.bean.rodimus.RodimusScheduledAction;
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
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
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
            LOG.info(
                    "Creating cluster and supporting teletraan records for cluster: {}",
                    clusterName);
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
            LOG.info("Updating cluster: {}", clusterName);
            rodimusManager.updateClusterWithPublicIds(clusterName, newClusterInfoPublicIdsBean);
        }

        LOG.info("Updating cluster capacity for cluster: {}", clusterName);
        rodimusManager.updateClusterCapacity(
                clusterName, infraConfigBean.getMinCapacity(), infraConfigBean.getMaxCapacity());

        // Manage Cluster AutoScaling Resources
        //        RodimusGetAutoScalingPolicies getAutoScalingPolicies
        RodimusAutoScalingPolicies rodimusAutoScalingPolicies =
                rodimusManager.getClusterScalingPolicies(clusterName);

        // Only handle simple policies. We don't yet manage step or target tracking in Cluster IaC
        List<RodimusAutoScalingPolicy> existingPolicies =
                rodimusAutoScalingPolicies.allSimplePolicies();
        List<RodimusAutoScalingPolicy> desiredRodimusAutoScalingPolicies =
                infraConfigBean.getScalingPolicies().stream()
                        .map(RodimusAutoScalingPolicy::fromScalingPolicyBean)
                        .collect(Collectors.toList());

        LOG.info("Updating autoscaling policies for cluster: {}", clusterName);
        updateAutoScalingPolicies(clusterName, existingPolicies, desiredRodimusAutoScalingPolicies);

        List<RodimusAutoScalingAlarm> autoScalingAlarms =
                rodimusManager.getClusterAlarms(clusterName);

        // Fetch the Policies again so we can attach policies to alarms by policy ARN.
        //        RodimusGetAutoScalingPolicies updatedAutoScalingPolicies =
        RodimusAutoScalingPolicies updatedAutoScalingPolicies =
                rodimusManager.getClusterScalingPolicies(clusterName);
        List<RodimusAutoScalingAlarm> desiredRodimusAutoScalingAlarms =
                infraConfigBean.getAutoScalingAlarm().stream()
                        .map(RodimusAutoScalingAlarm::fromAutoScalingAlarmBean)
                        .collect(Collectors.toList());

        LOG.info("Updating autoscaling alarms for cluster: {}", clusterName);
        updateAutoScalingAlarms(
                clusterName,
                autoScalingAlarms,
                desiredRodimusAutoScalingAlarms,
                updatedAutoScalingPolicies);

        List<RodimusScheduledAction> existingRodimusScheduledActions =
                rodimusManager.getClusterScheduledActions(clusterName);
        List<RodimusScheduledAction> desiredRodimusScheduledActions =
                infraConfigBean.getScheduledAction().stream()
                        .map(RodimusScheduledAction::fromScheduledActionBean)
                        .collect(Collectors.toList());

        LOG.info("Updating autoscaling scheduled actions for cluster: {}", clusterName);
        updateScheduledActions(
                clusterName, existingRodimusScheduledActions, desiredRodimusScheduledActions);
    }

    /*
     * Create or delete the scheduled actions if necessary.
     *
     * Scheduled actions are managed based on matching only the desired properties from the cluster spec.
     */
    public void updateScheduledActions(
            String rodimusClusterName,
            List<RodimusScheduledAction> existingActions,
            List<RodimusScheduledAction> desiredActions)
            throws Exception {

        Objects.requireNonNull(
                existingActions,
                "existingActions cannot be null, it could cause attempts to re-create all existing Scheduled Actions.");
        Objects.requireNonNull(
                desiredActions,
                "desiredActions cannot be null, this could cause deletion of all existing Scheduled Actions.");

        // Delete each existing action that does not match any desired action
        // Note: Delete must remain before create because duplicate schedules are not allowed.
        for (RodimusScheduledAction rodimusScheduledAction : existingActions) {
            if (desiredActions.stream()
                    .noneMatch(desiredAction -> desiredAction.matches(rodimusScheduledAction))) {
                rodimusManager.deleteClusterScheduledAction(
                        rodimusClusterName, rodimusScheduledAction.getActionId());
            }
        }

        // Create each desired action that does not match any existing action
        List<RodimusScheduledAction> newScheduledActions =
                desiredActions.stream()
                        .filter(
                                desiredAction ->
                                        existingActions.stream().noneMatch(desiredAction::matches))
                        .collect(Collectors.toList());

        int index = 0;
        for (RodimusScheduledAction action : newScheduledActions) {
            action.setActionId(generateScheduledActionName(rodimusClusterName, index++));
        }

        if (!newScheduledActions.isEmpty()) {
            rodimusManager.postClusterScheduledActions(rodimusClusterName, newScheduledActions);
        }
    }

    /* Generate a name for the new scheduled action.
     *
     * AWS restriction: max length 256.
     * Therefore, we use max of 147 to 149 characters for this id generation.
     * If there is an unexpected large clusterName string size, then
     * any actionId string over 256 characters will throw from Rodimus & fail the task.
     */
    private String generateScheduledActionName(String clusterName, Integer count) {
        String timestamp = new SimpleDateFormat("yyMMdd-HHmmssSSS").format(new Date());
        return String.format("%s-%s-%d", clusterName, timestamp, count);
    }

    /*
     * Create or delete the alarms if necessary.
     *
     * Alarms are managed based on matching only the desired properties from the cluster spec.
     * If the customer uses step or target tracking alarms, they must go in the cluster spec, even if they are not yet managed.
     * Note: Deploy-board leveraged form reload after policy set to populate the policies for alarm submission.
     *       This allowed the policy arn to be set on the alarm in the web interface. However, posting the
     *       policy list is unnecessary, each list is 1 policy and only the PolicyBean.arn is used.
     */
    public void updateAutoScalingAlarms(
            String clusterName,
            List<RodimusAutoScalingAlarm> existingAlarms,
            List<RodimusAutoScalingAlarm> desiredAlarms,
            RodimusAutoScalingPolicies updatedAutoScalingPolicies)
            throws Exception {

        Objects.requireNonNull(
                existingAlarms,
                "existingAlarms cannot be null, it could cause attempts to re-create all existing Scheduled Actions.");
        Objects.requireNonNull(
                desiredAlarms,
                "desiredAlarms cannot be null, this could cause deletion of all existing Scheduled Actions.");

        // Note: Although the whole PolicyBean is passed, only policy.arn is consumed in POST
        // alarms.
        List<RodimusAutoScalingPolicy> scaleUpPolicies =
                updatedAutoScalingPolicies.getScaleupPolicies();
        List<RodimusAutoScalingPolicy> scaleDownPolicies =
                updatedAutoScalingPolicies.getScaledownPolicies();

        // Attach the policies to the desired alarms first, so we can match them against existing
        // alarms.
        // Multiple alarms can have a scaleup or scaledown policy, but per rodimus design,
        // there can only be one scaleup and one scaledown policy per cluster.
        desiredAlarms.forEach(
                desiredAlarm -> {
                    if ("GROW".equalsIgnoreCase(desiredAlarm.getActionType())) {
                        // Note: This is always a list of 0 or 1 policy, per rodimus design
                        desiredAlarm.setScalingPolicies(scaleUpPolicies);
                    } else if ("SHRINK".equalsIgnoreCase(desiredAlarm.getActionType())
                            && !scaleDownPolicies.isEmpty()) {
                        // Note: This is always a list of 0 or 1 policy, per rodimus design
                        desiredAlarm.setScalingPolicies(scaleDownPolicies);
                    }
                });

        // Delete non-matching existing alarms
        List<RodimusAutoScalingAlarm> alarmsToDelete =
                existingAlarms.stream()
                        .filter(
                                existingAlarm ->
                                        desiredAlarms.stream()
                                                .noneMatch(
                                                        desiredAlarm ->
                                                                desiredAlarm.matches(
                                                                        existingAlarm)))
                        .collect(Collectors.toList());
        for (RodimusAutoScalingAlarm alarm : alarmsToDelete) {
            LOG.info("Delete undesired alarm for cluster {}: {}", clusterName, alarm);
            rodimusManager.deleteClusterAlarm(clusterName, alarm.getAlarmId());
        }
        // Remove the deleted existingAlarms to keep it consistent.
        existingAlarms.removeAll(alarmsToDelete);

        List<RodimusAutoScalingAlarm> alarmsToCreate =
                desiredAlarms.stream()
                        .filter(
                                desiredAlarm ->
                                        existingAlarms.stream()
                                                .noneMatch(
                                                        existingAlarm ->
                                                                desiredAlarm.matches(
                                                                        existingAlarm)))
                        .collect(Collectors.toList());

        if (!alarmsToCreate.isEmpty()) {
            LOG.info("Post new alarms for cluster {}: {}", clusterName, alarmsToCreate);
            rodimusManager.createClusterAlarms(clusterName, alarmsToCreate);
        }
    }

    /*
     * Create or delete the scaledown and scaleup policies if necessary.
     *
     * Note: Teletraan deploy-board only supports 1 scaleup policy and 1 scaledown policy.
     * Policies are managed based on matching only the desired properties from the cluster spec.
     * If the customer has more than 1 of each policy, any nonmatching policy will be deleted.
     */
    private void updateAutoScalingPolicies(
            String clusterName,
            List<RodimusAutoScalingPolicy> existingPolicies,
            List<RodimusAutoScalingPolicy> desiredPolicies)
            throws Exception {

        Objects.requireNonNull(
                existingPolicies,
                "existingPolicies cannot be null, it could cause attempts to re-create all existing Scheduled Actions.");
        Objects.requireNonNull(
                desiredPolicies,
                "desiredPolicies cannot be null, this could cause deletion of all existing Scheduled Actions.");

        // Collect and delete any existing policies that are not in desired policies
        List<RodimusAutoScalingPolicy> policiesToDelete =
                existingPolicies.stream()
                        .filter(
                                existingPolicy ->
                                        existingPolicy.getPolicyType().matches("SCALEUP|SCALEDOWN")
                                                && desiredPolicies.stream()
                                                        .noneMatch(
                                                                desiredPolicy ->
                                                                        desiredPolicy.matches(
                                                                                existingPolicy)))
                        .collect(Collectors.toList());
        for (RodimusAutoScalingPolicy policy : policiesToDelete) {
            LOG.info("Delete undesired existing policy for cluster {}: {}", clusterName, policy);
            rodimusManager.deleteClusterScalingPolicy(clusterName, policy.getPolicyName());
        }
        // Keep existingPolicies up to date, it is used next to check for presence of scaleup or
        //   scaledown simple policies
        existingPolicies.removeAll(policiesToDelete);

        // Add one scaleup and/or one scaledown policy if each respective type doesn't exist yet.
        // There can only be one of each, see deploy-board/rodimus implementation.
        RodimusAutoScalingPolicies newPolicies = new RodimusAutoScalingPolicies();
        getFirstAvailablePolicy("SCALEUP", existingPolicies, desiredPolicies)
                .ifPresent(newPolicies.getScaleupPolicies()::add);
        getFirstAvailablePolicy("SCALEDOWN", existingPolicies, desiredPolicies)
                .ifPresent(newPolicies.getScaledownPolicies()::add);

        LOG.info("Post new policies for cluster {}: {}", clusterName, newPolicies);
        rodimusManager.postClusterScalingPolicies(clusterName, newPolicies);
    }

    private Optional<RodimusAutoScalingPolicy> getFirstAvailablePolicy(
            String policyType,
            List<RodimusAutoScalingPolicy> existingPolicies,
            List<RodimusAutoScalingPolicy> desiredPolicies) {

        // Test if any simple policy of policyType exists already
        boolean hasPolicy =
                existingPolicies.stream()
                        .anyMatch(policy -> policyType.equalsIgnoreCase(policy.getPolicyType()));

        if (!hasPolicy) {
            return desiredPolicies.stream()
                    .filter(policy -> policyType.equalsIgnoreCase(policy.getPolicyType()))
                    .findFirst();
        }

        return Optional.empty();
    }
}
