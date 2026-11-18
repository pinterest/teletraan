/**
 * Copyright (c) 2026 Pinterest, Inc.
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pinterest.deployservice.bean.ClusterInfoPublicIdsBean;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.InfraBean;
import com.pinterest.deployservice.bean.InfraConfigBean;
import com.pinterest.deployservice.bean.WorkerJobBean;
import com.pinterest.deployservice.bean.rodimus.RodimusAutoScalingPolicies;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.GroupDAO;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.dao.UtilDAO;
import com.pinterest.deployservice.dao.WorkerJobDAO;
import com.pinterest.deployservice.rodimus.RodimusManager;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.handler.EnvironmentHandler;
import java.security.Principal;
import java.sql.Connection;
import java.util.Collections;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * BUG-319978: an infra apply is authorized against the env in the request path, but the cluster it
 * acts on comes from the request body. These tests use the reporter's topology: victim-svc/prod
 * owns victim-asg, the caller owns attacker-svc/prod (bound to attacker-asg), unclaimed-grp has
 * hosts but no owning environment, and orphan-asg exists in Rodimus without a Teletraan owner.
 */
class InfraClusterOwnershipTest {

    private static final String ATTACKER_ENV = "attacker-svc";
    private static final String STAGE = "prod";

    private EnvironDAO environDAO;
    private GroupDAO groupDAO;
    private HostDAO hostDAO;
    private RodimusManager rodimusManager;
    private WorkerJobDAO workerJobDAO;
    private UtilDAO utilDAO;
    private TeletraanServiceContext context;
    private SecurityContext securityContext;
    private EnvironBean attackerEnv;

    @BeforeEach
    void setUp() throws Exception {
        environDAO = mock(EnvironDAO.class);
        groupDAO = mock(GroupDAO.class);
        hostDAO = mock(HostDAO.class);
        rodimusManager = mock(RodimusManager.class);
        workerJobDAO = mock(WorkerJobDAO.class);
        utilDAO = mock(UtilDAO.class);

        context = new TeletraanServiceContext();
        context.setEnvironDAO(environDAO);
        context.setGroupDAO(groupDAO);
        context.setHostDAO(hostDAO);
        context.setRodimusManager(rodimusManager);
        context.setWorkerJobDAO(workerJobDAO);
        context.setUtilDAO(utilDAO);

        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("attacker");
        securityContext = mock(SecurityContext.class);
        when(securityContext.getUserPrincipal()).thenReturn(principal);

        EnvironBean victimEnv = env("env-victim", "victim-svc", "victim-asg");
        attackerEnv = env("env-attacker", ATTACKER_ENV, "attacker-asg");
        when(environDAO.getByStage("victim-svc", STAGE)).thenReturn(victimEnv);
        when(environDAO.getByStage(ATTACKER_ENV, STAGE)).thenReturn(attackerEnv);
        when(environDAO.getByCluster("victim-asg")).thenReturn(victimEnv);
        when(environDAO.getByCluster("attacker-asg")).thenReturn(attackerEnv);
        when(environDAO.getEnvsByGroups(Collections.singletonList("victim-asg")))
                .thenReturn(Collections.singletonList(victimEnv));
        when(hostDAO.getGroupSize("victim-asg")).thenReturn(5L);
        when(hostDAO.getGroupSize("unclaimed-grp")).thenReturn(3L);

        ClusterInfoPublicIdsBean existingCluster = mock(ClusterInfoPublicIdsBean.class);
        when(rodimusManager.getClusterInfoPublicIdsBean("victim-asg")).thenReturn(existingCluster);
        when(rodimusManager.getClusterInfoPublicIdsBean("attacker-asg"))
                .thenReturn(existingCluster);
        when(rodimusManager.getClusterInfoPublicIdsBean("orphan-asg")).thenReturn(existingCluster);
        when(rodimusManager.getClusterScalingPolicies(anyString()))
                .thenReturn(RodimusAutoScalingPolicies.builder().build());
    }

    @Test
    void worker_failsJobForAnotherEnvsCluster() throws Exception {
        WorkerJobBean job = queueJob("victim-asg");

        new ApplyInfraWorker(context).run();

        assertFailedWithoutChanges(job);
    }

    @Test
    void worker_failsJobForRodimusClusterWithoutTeletraanOwner() throws Exception {
        WorkerJobBean job = queueJob("orphan-asg");

        new ApplyInfraWorker(context).run();

        assertFailedWithoutChanges(job);
    }

    @Test
    void worker_failsJobClaimingUnclaimedGroupThatHasHosts() throws Exception {
        WorkerJobBean job = queueJob("unclaimed-grp");

        new ApplyInfraWorker(context).run();

        assertFailedWithoutChanges(job);
        verify(rodimusManager, never()).createClusterWithEnvPublicIds(any(), any(), any(), any());
    }

    @Test
    void worker_updatesOwnCluster() throws Exception {
        WorkerJobBean job = queueJob("attacker-asg");

        new ApplyInfraWorker(context).run();

        verify(rodimusManager).updateClusterWithPublicIds(eq("attacker-asg"), any());
        verify(rodimusManager).updateClusterCapacity(eq("attacker-asg"), any(), any());
        verify(workerJobDAO).updateStatus(eq(job), eq(WorkerJobBean.Status.COMPLETED), anyLong());
    }

    @Test
    void envUpdate_rejectsBindingAnotherEnvsCluster() throws Exception {
        EnvironBean update = new EnvironBean();
        update.setCluster_name("victim-asg");

        assertThrows(
                ForbiddenException.class,
                () ->
                        new EnvironmentHandler(context)
                                .updateEnvironment(null, "attacker", ATTACKER_ENV, STAGE, update));

        verify(environDAO, never()).update(any(), any(), any());
    }

    @Test
    void envUpdate_rejectsBindingUnclaimedGroupThatHasHosts() throws Exception {
        EnvironBean update = new EnvironBean();
        update.setCluster_name("unclaimed-grp");

        assertThrows(
                ForbiddenException.class,
                () ->
                        new EnvironmentHandler(context)
                                .updateEnvironment(null, "attacker", ATTACKER_ENV, STAGE, update));

        verify(environDAO, never()).update(any(), any(), any());
    }

    @Test
    void envCreate_rejectsAnotherEnvsClusterEvenWithSpoofedId() throws Exception {
        EnvironBean create = new EnvironBean();
        create.setEnv_id("env-victim");
        create.setEnv_name(ATTACKER_ENV);
        create.setStage_name("new");
        create.setCluster_name("victim-asg");

        assertThrows(
                ForbiddenException.class,
                () -> new Environs(context).create(securityContext, null, create));

        verify(environDAO, never()).insert(any());
    }

    private void assertFailedWithoutChanges(WorkerJobBean job) throws Exception {
        verify(workerJobDAO).updateStatus(eq(job), eq(WorkerJobBean.Status.FAILED), anyLong());
        verify(rodimusManager, never()).updateClusterWithPublicIds(any(), any());
        verify(rodimusManager, never()).updateClusterCapacity(any(), any(), any());
        verify(rodimusManager, never()).deleteClusterScalingPolicy(any(), any());
        verify(rodimusManager, never()).deleteClusterAlarm(any(), any());
        verify(rodimusManager, never()).deleteClusterScheduledAction(any(), any());
        verify(environDAO, never()).update(any(), any(), any());
        verify(groupDAO, never()).addGroupCapacity(any(), any());
    }

    private WorkerJobBean queueJob(String clusterName) throws Exception {
        InfraConfigBean config =
                InfraConfigBean.fromInfraBean("attacker", ATTACKER_ENV, STAGE, infra(clusterName));
        WorkerJobBean job =
                WorkerJobBean.builder()
                        .id("job-" + clusterName)
                        .job_type(WorkerJobBean.JobType.INFRA_APPLY)
                        .config(new ObjectMapper().writeValueAsString(config))
                        .status(WorkerJobBean.Status.INITIALIZED)
                        .build();
        when(workerJobDAO.getOldestByJobTypeStatus(any(), any(), anyInt()))
                .thenReturn(new java.util.ArrayList<>(Collections.singletonList(job)));
        when(workerJobDAO.getById(job.getId())).thenReturn(job);
        when(utilDAO.getLock(any())).thenReturn(mock(Connection.class));
        return job;
    }

    private static InfraBean infra(String clusterName) {
        return InfraBean.builder().clusterName(clusterName).minCapacity(0).maxCapacity(0).build();
    }

    private static EnvironBean env(String id, String name, String clusterName) {
        EnvironBean bean = new EnvironBean();
        bean.setEnv_id(id);
        bean.setEnv_name(name);
        bean.setStage_name(STAGE);
        bean.setCluster_name(clusterName);
        return bean;
    }
}
