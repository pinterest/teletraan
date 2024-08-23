/**
 * Copyright (c) 2022-2024 Pinterest, Inc.
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.AgentBean;
import com.pinterest.deployservice.bean.DeployStage;
import com.pinterest.deployservice.bean.HostAgentBean;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.bean.HostState;
import com.pinterest.deployservice.dao.AgentDAO;
import com.pinterest.deployservice.dao.HostAgentDAO;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.dao.UtilDAO;
import com.pinterest.deployservice.rodimus.RodimusManager;
import java.sql.Connection;
import java.util.Collection;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HostTerminatorTest {
    private static String TEST_HOST_ID = "i-testHostId";
    private static String TEST_ASG_NAME = "test-asg-name";
    private static String TEST_GROUP_NAME = "test-group-name";

    private HostTerminator hostTerminator;

    private AgentDAO mockAgentDAO;
    private HostAgentDAO mockHostAgentDAO;
    private HostDAO mockHostDAO;
    private RodimusManager mockRodimusManager;
    private UtilDAO mockUtilDAO;

    private Collection<String> testHostIds = Collections.singletonList(TEST_HOST_ID);;
    private AgentBean testAgentBean;
    private HostAgentBean testHostAgentBean;
    private HostBean testHostBean;

    private ServiceContext createMockServiceContext() throws Exception {
        ServiceContext serviceContext = new ServiceContext();
        serviceContext.setAgentDAO(mockAgentDAO);
        serviceContext.setHostAgentDAO(mockHostAgentDAO);
        serviceContext.setHostDAO(mockHostDAO);
        serviceContext.setRodimusManager(mockRodimusManager);
        serviceContext.setUtilDAO(mockUtilDAO);

        return serviceContext;
    }

    private AgentBean createAgentBean() {
        AgentBean bean = new AgentBean();
        bean.setHost_id(TEST_HOST_ID);
        bean.setDeploy_stage(DeployStage.STOPPED);
        return bean;
    }

    private HostAgentBean createHostAgentBean() {
        HostAgentBean bean = new HostAgentBean();
        bean.setHost_id(TEST_HOST_ID);
        bean.setAuto_scaling_group(TEST_ASG_NAME);
        return bean;
    }

    private HostBean createHostBean() {
        HostBean bean = new HostBean();
        bean.setHost_id(TEST_HOST_ID);
        bean.setGroup_name(TEST_GROUP_NAME);
        bean.setState(HostState.PENDING_TERMINATE);
        return bean;
    }

    @BeforeEach
    public void setUp() throws Exception {
        mockAgentDAO = mock(AgentDAO.class);
        mockHostAgentDAO = mock(HostAgentDAO.class);
        mockHostDAO = mock(HostDAO.class);
        mockRodimusManager = mock(RodimusManager.class);
        mockUtilDAO = mock(UtilDAO.class);

        ServiceContext mockServiceContext = createMockServiceContext();
        hostTerminator = new HostTerminator(mockServiceContext);

        testAgentBean = createAgentBean();
        testHostAgentBean = createHostAgentBean();
        testHostBean = createHostBean();

        when(mockUtilDAO.getLock(any())).thenReturn(mock(Connection.class));
    }

    @Test
    public void hostAgentHasHost_ASGNameIsUsed() throws Exception {
        when(mockAgentDAO.getByHostId(TEST_HOST_ID))
                .thenReturn(Collections.singletonList(testAgentBean));
        when(mockHostAgentDAO.getHostById(TEST_HOST_ID)).thenReturn(testHostAgentBean);
        when(mockHostDAO.getTerminatingHosts()).thenReturn(Collections.singletonList(testHostBean));

        hostTerminator.run();

        verify(mockRodimusManager).terminateHostsByClusterName(TEST_ASG_NAME, testHostIds, true);
    }

    @Test
    public void hostAgentDoesNotHaveHost_ASGNameIsUsed() throws Exception {
        when(mockAgentDAO.getByHostId(TEST_HOST_ID))
                .thenReturn(Collections.singletonList(testAgentBean));
        when(mockHostAgentDAO.getHostById(TEST_HOST_ID)).thenReturn(null);
        when(mockHostDAO.getTerminatingHosts()).thenReturn(Collections.singletonList(testHostBean));

        hostTerminator.run();

        verify(mockRodimusManager).terminateHostsByClusterName(TEST_GROUP_NAME, testHostIds, true);
    }

    @Test
    public void hostBeanStateIsPendingNoReplace_terminateHostWithoutReplace() throws Exception {
        testHostBean.setState(HostState.PENDING_TERMINATE_NO_REPLACE);
        when(mockAgentDAO.getByHostId(TEST_HOST_ID))
                .thenReturn(Collections.singletonList(testAgentBean));
        when(mockHostAgentDAO.getHostById(TEST_HOST_ID)).thenReturn(null);
        when(mockHostDAO.getTerminatingHosts()).thenReturn(Collections.singletonList(testHostBean));

        hostTerminator.run();

        verify(mockRodimusManager).terminateHostsByClusterName(TEST_GROUP_NAME, testHostIds, false);
    }
}
