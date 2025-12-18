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

import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pinterest.deployservice.bean.ClusterInfoPublicIdsBean;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.InfraConfigBean;
import com.pinterest.deployservice.bean.WorkerJobBean;
import com.pinterest.deployservice.bean.rodimus.RodimusAutoScalingPolicies;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.UtilDAO;
import com.pinterest.deployservice.dao.WorkerJobDAO;
import com.pinterest.deployservice.rodimus.RodimusManager;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.handler.EnvironmentHandler;
import io.micrometer.core.instrument.Counter;
import java.sql.Connection;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class ApplyInfraWorkerTest {

    private TeletraanServiceContext mockContext;
    private EnvironDAO mockEnvironDAO;
    private EnvironmentHandler mockEnvHandler;
    private RodimusManager mockRodimusManager;
    private UtilDAO mockUtilDAO;
    private WorkerJobDAO mockWorkerJobDAO;
    private Counter mockSuccessCounter;
    private Counter mockFailureCounter;
    private ApplyInfraWorker worker;

    @BeforeEach
    void setUp() {
        mockContext = mock(TeletraanServiceContext.class);
        mockEnvironDAO = mock(EnvironDAO.class);
        mockEnvHandler = mock(EnvironmentHandler.class);
        mockRodimusManager = mock(RodimusManager.class);
        mockUtilDAO = mock(UtilDAO.class);
        mockWorkerJobDAO = mock(WorkerJobDAO.class);

        when(mockContext.getEnvironDAO()).thenReturn(mockEnvironDAO);
        when(mockContext.getRodimusManager()).thenReturn(mockRodimusManager);
        when(mockContext.getUtilDAO()).thenReturn(mockUtilDAO);
        when(mockContext.getWorkerJobDAO()).thenReturn(mockWorkerJobDAO);

        // Patch the counter creation with dummy counters
        mockSuccessCounter = mock(Counter.class);
        mockFailureCounter = mock(Counter.class);

        // Patch ErrorBudgetCounterFactory statics
        worker = new ApplyInfraWorker(mockContext);

        try {
            java.lang.reflect.Field field =
                    ApplyInfraWorker.class.getDeclaredField("environmentHandler");
            field.setAccessible(true);
            field.set(worker, mockEnvHandler);
        } catch (Exception ignored) {
        }

        try {
            java.lang.reflect.Field field =
                    ApplyInfraWorker.class.getDeclaredField("errorBudgetSuccess");
            field.setAccessible(true);
            field.set(worker, mockSuccessCounter);
            field = ApplyInfraWorker.class.getDeclaredField("errorBudgetFailure");
            field.setAccessible(true);
            field.set(worker, mockFailureCounter);
        } catch (Exception ignored) {
        }
    }

    @Test
    void testRunInternal_clusterCreateBranch_success() throws Exception {
        WorkerJobBean job =
                makeJobBean(
                        "1",
                        WorkerJobBean.Status.INITIALIZED,
                        makeInfraConfigJson("CLUSTER-NEW", "env", "stage", "op"));
        when(mockWorkerJobDAO.getOldestByJobTypeStatus(any(), any(), anyInt()))
                .thenReturn(Collections.singletonList(job));

        Connection mockConn = mock(Connection.class);
        when(mockUtilDAO.getLock(any())).thenReturn(mockConn);

        WorkerJobBean runningJob =
                makeJobBean("1", WorkerJobBean.Status.INITIALIZED, job.getConfig());
        when(mockWorkerJobDAO.getById("1")).thenReturn(runningJob);

        // No pre-existing cluster triggers creation branch
        when(mockRodimusManager.getCluster("CLUSTER-NEW")).thenReturn(null);
        when(mockRodimusManager.getClusterScalingPolicies(any()))
                .thenReturn(RodimusAutoScalingPolicies.builder().build());

        // Env bean returned from static call
        EnvironBean origEnv = new EnvironBean();
        origEnv.setEnv_name("env");
        origEnv.setStage_name("stage");
        origEnv.setCluster_name(null);

        try (MockedStatic<Utils> utils = Mockito.mockStatic(Utils.class)) {
            utils.when(() -> Utils.getEnvStage(eq(mockEnvironDAO), eq("env"), eq("stage")))
                    .thenReturn(origEnv);

            worker.run();

            verify(mockWorkerJobDAO)
                    .updateStatus(eq(job), eq(WorkerJobBean.Status.RUNNING), anyLong());
            verify(mockEnvHandler).updateEnvironment(eq("op"), eq("env"), eq("stage"), any());
            verify(mockEnvHandler)
                    .createCapacityForHostOrGroup(
                            eq("op"),
                            eq("env"),
                            eq("stage"),
                            eq(Optional.of(EnvCapacities.CapacityType.GROUP)),
                            eq("CLUSTER-NEW"),
                            eq(origEnv));
            verify(mockRodimusManager).createClusterWithEnvPublicIds(any(), any(), any(), any());
            verify(mockWorkerJobDAO)
                    .updateStatus(eq(job), eq(WorkerJobBean.Status.COMPLETED), anyLong());
            verify(mockUtilDAO).releaseLock(startsWith("APPLY_INFRA-"), eq(mockConn));
        }
    }

    @Test
    void testRunInternal_clusterUpdateBranch_success() throws Exception {
        WorkerJobBean job =
                makeJobBean(
                        "2",
                        WorkerJobBean.Status.INITIALIZED,
                        makeInfraConfigJson("CLUSTER-EXIST", "env", "stage", "op"));
        when(mockWorkerJobDAO.getOldestByJobTypeStatus(any(), any(), anyInt()))
                .thenReturn(Collections.singletonList(job));

        Connection mockConn = mock(Connection.class);
        when(mockUtilDAO.getLock(any())).thenReturn(mockConn);

        WorkerJobBean runningJob =
                makeJobBean("2", WorkerJobBean.Status.INITIALIZED, job.getConfig());
        when(mockWorkerJobDAO.getById("2")).thenReturn(runningJob);

        // Cluster exists
        ClusterInfoPublicIdsBean existBean = mock(ClusterInfoPublicIdsBean.class);
        when(mockRodimusManager.getCluster("CLUSTER-EXIST")).thenReturn(existBean);
        when(mockRodimusManager.getClusterScalingPolicies(any()))
                .thenReturn(RodimusAutoScalingPolicies.builder().build());

        worker.run();

        verify(mockRodimusManager).updateClusterWithPublicIds(eq("CLUSTER-EXIST"), any());
        verify(mockWorkerJobDAO).updateStatus(eq(job), eq(WorkerJobBean.Status.RUNNING), anyLong());
        verify(mockWorkerJobDAO)
                .updateStatus(eq(job), eq(WorkerJobBean.Status.COMPLETED), anyLong());
        verify(mockUtilDAO).releaseLock(startsWith("APPLY_INFRA-"), eq(mockConn));
        verify(mockEnvHandler, never()).updateEnvironment(any(), any(), any(), any());
        verify(mockEnvHandler, never())
                .createCapacityForHostOrGroup(any(), any(), any(), any(), any(), any());
    }

    // Helper for WorkerJobBean
    private WorkerJobBean makeJobBean(String id, WorkerJobBean.Status status, String configJson) {
        WorkerJobBean job = new WorkerJobBean();
        job.setId(id);
        job.setStatus(status);
        job.setConfig(configJson);
        return job;
    }

    // Helper for simple InfraConfigBean json
    private String makeInfraConfigJson(String cluster, String env, String stage, String op)
            throws Exception {
        InfraConfigBean bean = new InfraConfigBean();
        bean.setClusterName(cluster);
        bean.setEnvName(env);
        bean.setStageName(stage);
        bean.setOperator(op);
        return new ObjectMapper().writeValueAsString(bean);
    }
}
