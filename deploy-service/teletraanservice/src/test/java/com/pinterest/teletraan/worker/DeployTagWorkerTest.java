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
package com.pinterest.teletraan.worker;

import static com.pinterest.deployservice.bean.BeanUtils.createDeployConstraintBean;
import static com.pinterest.deployservice.bean.BeanUtils.createHostBean;
import static com.pinterest.deployservice.bean.BeanUtils.createHostTagBean;
import static com.pinterest.deployservice.fixture.EnvironBeanFixture.createRandomEnvironBean;
import static com.pinterest.teletraan.universal.metrics.ErrorBudgetCounterFactory.ERROR_BUDGET_METRIC_NAME;
import static com.pinterest.teletraan.universal.metrics.ErrorBudgetCounterFactory.ERROR_BUDGET_TAG_NAME_RESPONSE_TYPE;
import static com.pinterest.teletraan.universal.metrics.ErrorBudgetCounterFactory.ERROR_BUDGET_TAG_VALUE_RESPONSE_TYPE_FAILURE;
import static com.pinterest.teletraan.universal.metrics.ErrorBudgetCounterFactory.ERROR_BUDGET_TAG_VALUE_RESPONSE_TYPE_SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.DeployConstraintBean;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.bean.HostTagBean;
import com.pinterest.deployservice.bean.TagSyncState;
import com.pinterest.deployservice.dao.DeployConstraintDAO;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.dao.HostTagDAO;
import com.pinterest.deployservice.dao.UtilDAO;
import com.pinterest.deployservice.rodimus.RodimusManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.ConnectException;
import java.sql.Connection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.dbcp.BasicDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DeployTagWorkerTest {

    private static final String TEST_HOST_ID = "i-testHostId";

    private HostDAO hostDAO;
    private EnvironDAO environDAO;
    private DeployConstraintDAO deployConstraintDAO;
    private HostTagDAO hostTagDAO;
    private RodimusManager rodimusManager;
    private BasicDataSource dataSource;
    private UtilDAO utilDAO;
    private ServiceContext serviceContext;
    private DeployTagWorker deployTagWorker;

    @BeforeAll
    public static void setUpClass() {
        Metrics.addRegistry(new SimpleMeterRegistry());
    }

    @BeforeEach
    public void setUp() {
        hostDAO = mock(HostDAO.class);
        environDAO = mock(EnvironDAO.class);
        deployConstraintDAO = mock(DeployConstraintDAO.class);
        hostTagDAO = mock(HostTagDAO.class);
        rodimusManager = mock(RodimusManager.class);
        dataSource = mock(BasicDataSource.class);
        utilDAO = mock(UtilDAO.class);

        serviceContext = new ServiceContext();
        serviceContext.setHostDAO(hostDAO);
        serviceContext.setEnvironDAO(environDAO);
        serviceContext.setDeployConstraintDAO(deployConstraintDAO);
        serviceContext.setHostTagDAO(hostTagDAO);
        serviceContext.setRodimusManager(rodimusManager);
        serviceContext.setDataSource(dataSource);
        serviceContext.setUtilDAO(utilDAO);

        deployTagWorker = new DeployTagWorker(serviceContext);
    }

    @AfterEach
    public void tearDown() {
        Metrics.globalRegistry.clear();
    }

    @Test
    public void testMissingEnvironNoErrorBudgetAffected() throws Exception {
        // Set up mock data
        DeployConstraintBean deployConstraint = createDeployConstraintBean();
        List<DeployConstraintBean> deployConstraints = new ArrayList<>();
        deployConstraints.add(deployConstraint);

        when(deployConstraintDAO.getAllActiveDeployConstraint()).thenReturn(deployConstraints);

        when(utilDAO.getLock(eq(getDbLockId(deployConstraint.getConstraint_id()))))
                .thenReturn(mock(Connection.class));

        // Run the worker
        deployTagWorker.run();

        // Verify the result
        assertErrorBudget(0, 0);
    }

    @Test
    public void testTagsInSync() throws Exception {
        // Set up mock data
        DeployConstraintBean deployConstraint = createDeployConstraintBean();
        List<DeployConstraintBean> deployConstraints = new ArrayList<>();
        deployConstraints.add(deployConstraint);

        when(deployConstraintDAO.getAllActiveDeployConstraint()).thenReturn(deployConstraints);

        when(utilDAO.getLock(eq(getDbLockId(deployConstraint.getConstraint_id()))))
                .thenReturn(mock(Connection.class));

        EnvironBean environ = createRandomEnvironBean();
        when(environDAO.getEnvByDeployConstraintId(eq(deployConstraint.getConstraint_id())))
                .thenReturn(environ);

        Instant now = Instant.now();
        HostBean host = createHostBean(now);
        host.setHost_id(TEST_HOST_ID);
        List<HostBean> hosts = new ArrayList<>();
        hosts.add(host);
        when(hostDAO.getHostsByEnvId(eq(environ.getEnv_id()))).thenReturn(hosts);

        HostTagBean hostTag = createHostTagBean(now);
        hostTag.setHost_id(TEST_HOST_ID);
        List<HostTagBean> hostTags = new ArrayList<>();
        hostTags.add(hostTag);
        when(hostTagDAO.getAllByEnvIdAndTagName(
                        eq(environ.getEnv_id()), eq(deployConstraint.getConstraint_key())))
                .thenReturn(hostTags);

        // Run the worker
        deployTagWorker.run();

        // Verify the result
        assertErrorBudget(0, 0);

        verify(deployConstraintDAO, times(1))
                .updateById(
                        eq(deployConstraint.getConstraint_id()),
                        argThat(c -> TagSyncState.FINISHED == c.getState()));
    }

    @Test
    public void testNetworkExceptionHandled() throws Exception {
        // Set up mock data
        DeployConstraintBean deployConstraint = createDeployConstraintBean();
        List<DeployConstraintBean> deployConstraints = new ArrayList<>();
        deployConstraints.add(deployConstraint);

        when(deployConstraintDAO.getAllActiveDeployConstraint()).thenReturn(deployConstraints);

        when(utilDAO.getLock(eq(getDbLockId(deployConstraint.getConstraint_id()))))
                .thenReturn(mock(Connection.class));

        EnvironBean environ = createRandomEnvironBean();
        when(environDAO.getEnvByDeployConstraintId(eq(deployConstraint.getConstraint_id())))
                .thenReturn(environ);

        Instant now = Instant.now();
        HostBean host = createHostBean(now);
        List<HostBean> hosts = new ArrayList<>();
        hosts.add(host);
        when(hostDAO.getHostsByEnvId(eq(environ.getEnv_id()))).thenReturn(hosts);

        HostTagBean hostTag = createHostTagBean(now);
        List<HostTagBean> hostTags = new ArrayList<>();
        hostTags.add(hostTag);
        when(hostTagDAO.getAllByEnvIdAndTagName(
                        eq(environ.getEnv_id()), eq(deployConstraint.getConstraint_key())))
                .thenReturn(hostTags);

        List<String> hostTagsToFetch = new ArrayList<>();
        hostTagsToFetch.add(host.getHost_id());
        when(rodimusManager.getEc2Tags(eq(hostTagsToFetch)))
                .thenThrow(new ConnectException("testConnectException"));

        // Run the worker
        deployTagWorker.run();

        // Verify the result
        assertErrorBudget(0, 0);

        verify(deployConstraintDAO, times(1))
                .updateById(
                        eq(deployConstraint.getConstraint_id()),
                        argThat(c -> TagSyncState.PROCESSING == c.getState()));

        verify(deployConstraintDAO, never())
                .updateById(
                        eq(deployConstraint.getConstraint_id()),
                        argThat(c -> TagSyncState.ERROR == c.getState()));

        List<String> expectedExtraHostId = new ArrayList<>();
        expectedExtraHostId.add(hostTag.getHost_id());
        verify(hostTagDAO, times(1))
                .deleteAllByEnvIdAndHostIds(eq(environ.getEnv_id()), eq(expectedExtraHostId));
    }

    @Test
    public void testUnexpectedException() throws Exception {
        // Set up mock data
        DeployConstraintBean deployConstraint = createDeployConstraintBean();
        List<DeployConstraintBean> deployConstraints = new ArrayList<>();
        deployConstraints.add(deployConstraint);

        when(deployConstraintDAO.getAllActiveDeployConstraint()).thenReturn(deployConstraints);

        when(utilDAO.getLock(eq(getDbLockId(deployConstraint.getConstraint_id()))))
                .thenReturn(mock(Connection.class));

        EnvironBean environ = createRandomEnvironBean();
        when(environDAO.getEnvByDeployConstraintId(eq(deployConstraint.getConstraint_id())))
                .thenReturn(environ);

        Instant now = Instant.now();
        HostBean host = createHostBean(now);
        List<HostBean> hosts = new ArrayList<>();
        hosts.add(host);
        when(hostDAO.getHostsByEnvId(eq(environ.getEnv_id()))).thenReturn(hosts);

        HostTagBean hostTag = createHostTagBean(now);
        List<HostTagBean> hostTags = new ArrayList<>();
        hostTags.add(hostTag);
        when(hostTagDAO.getAllByEnvIdAndTagName(
                        eq(environ.getEnv_id()), eq(deployConstraint.getConstraint_key())))
                .thenReturn(hostTags);

        List<String> hostTagsToFetch = new ArrayList<>();
        hostTagsToFetch.add(host.getHost_id());
        when(rodimusManager.getEc2Tags(eq(hostTagsToFetch)))
                .thenThrow(new Exception("unexpectedException"));

        // Run the worker
        deployTagWorker.run();

        // Verify the result
        assertErrorBudget(0, 1);

        verify(deployConstraintDAO, atLeastOnce())
                .updateById(
                        eq(deployConstraint.getConstraint_id()),
                        argThat(c -> TagSyncState.ERROR == c.getState()));

        List<String> expectedExtraHostId = new ArrayList<>();
        expectedExtraHostId.add(hostTag.getHost_id());
        verify(hostTagDAO, times(1))
                .deleteAllByEnvIdAndHostIds(eq(environ.getEnv_id()), eq(expectedExtraHostId));
    }

    private void assertErrorBudget(int successCount, int failureCount) {
        Counter successCounter =
                Metrics.globalRegistry
                        .get(ERROR_BUDGET_METRIC_NAME)
                        .tag(
                                ERROR_BUDGET_TAG_NAME_RESPONSE_TYPE,
                                ERROR_BUDGET_TAG_VALUE_RESPONSE_TYPE_SUCCESS)
                        .counter();
        Counter failureCounter =
                Metrics.globalRegistry
                        .get(ERROR_BUDGET_METRIC_NAME)
                        .tag(
                                ERROR_BUDGET_TAG_NAME_RESPONSE_TYPE,
                                ERROR_BUDGET_TAG_VALUE_RESPONSE_TYPE_FAILURE)
                        .counter();
        assertEquals(successCount, successCounter.count(), 0.01);
        assertEquals(failureCount, failureCounter.count(), 0.01);
    }

    private String getDbLockId(String constraintId) {
        return String.format("DeployTagWorker-%s", constraintId);
    }
}
