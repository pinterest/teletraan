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
package com.pinterest.deployservice.handler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.EnvType;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.bean.HostState;
import com.pinterest.deployservice.dao.AgentDAO;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.GroupDAO;
import com.pinterest.deployservice.dao.HostDAO;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class EnvironHandlerTest {
    private static final String TEST_OPERATOR = "operator";
    private static final String TEST_CLUSTER_NAME = "clusterName";
    private static final String DEFAULT_HOST_ID = "hostId";

    private EnvironHandler environHandler;

    @Mock private HostDAO mockHostDAO;
    @Mock private AgentDAO mockAgentDAO;
    @Mock private EnvironDAO environDAO;
    @Mock private GroupDAO groupDAO;
    EnvironBean testEnvBean;
    private List<String> hostIds = Arrays.asList("hostId1", "hostId2");

    private ServiceContext createMockServiceContext() {
        ServiceContext serviceContext = new ServiceContext();
        serviceContext.setHostDAO(mockHostDAO);
        serviceContext.setAgentDAO(mockAgentDAO);
        serviceContext.setEnvironDAO(environDAO);
        serviceContext.setGroupDAO(groupDAO);
        return serviceContext;
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testEnvBean = new EnvironBean();
        testEnvBean.setEnv_id("envId");
        testEnvBean.setCluster_name(TEST_CLUSTER_NAME);
        environHandler = new EnvironHandler(createMockServiceContext());
    }

    @Test
    void stopServiceOnHost_withReplaceHost_hostBeanStateIsPendingTerminate() throws Exception {
        ArgumentCaptor<HostBean> argument = ArgumentCaptor.forClass(HostBean.class);

        environHandler.stopServiceOnHost(DEFAULT_HOST_ID, true);
        verify(mockHostDAO).updateHostById(eq(DEFAULT_HOST_ID), argument.capture());
        assertEquals(HostState.PENDING_TERMINATE, argument.getValue().getState());
    }

    @Test
    void stopServiceOnHost_withoutReplaceHost_hostBeanStateIsPendingTerminateNoReplace()
            throws Exception {
        ArgumentCaptor<HostBean> argument = ArgumentCaptor.forClass(HostBean.class);

        environHandler.stopServiceOnHost(DEFAULT_HOST_ID, false);
        verify(mockHostDAO).updateHostById(eq(DEFAULT_HOST_ID), argument.capture());
        assertEquals(HostState.PENDING_TERMINATE_NO_REPLACE, argument.getValue().getState());
    }

    @Test
    void updateStage_type_enables_private_build() throws Exception {
        ArgumentCaptor<EnvironBean> argument = ArgumentCaptor.forClass(EnvironBean.class);
        testEnvBean.setStage_type(EnvType.DEV);
        environHandler.createEnvStage(testEnvBean, "Anonymous");
        verify(environDAO).insert(argument.capture());
        assertEquals(true, argument.getValue().getAllow_private_build());
    }

    @Test
    void ensureHostsOwnedByEnv_noMainEnv() {
        assertThrows(
                NotFoundException.class,
                () -> environHandler.ensureHostsOwnedByEnv(new EnvironBean(), hostIds));
    }

    @Test
    void ensureHostsOwnedByEnv_differentMainEnv() throws Exception {
        when(environDAO.getMainEnvByHostId(anyString())).thenReturn(testEnvBean);
        assertThrows(
                ForbiddenException.class,
                () -> environHandler.ensureHostsOwnedByEnv(new EnvironBean(), hostIds));
    }

    @Test
    void ensureHostsOwnedByEnv_sameMainEnv() throws Exception {
        when(environDAO.getMainEnvByHostId(anyString())).thenReturn(testEnvBean);
        assertDoesNotThrow(() -> environHandler.ensureHostsOwnedByEnv(testEnvBean, hostIds));
    }

    @Test
    void ensureHostsOwnedByEnv_sqlException() throws Exception {
        when(environDAO.getMainEnvByHostId(anyString())).thenThrow(SQLException.class);
        assertThrows(
                WebApplicationException.class,
                () -> environHandler.ensureHostsOwnedByEnv(new EnvironBean(), hostIds));
    }

    @Test
    void updateGroups_addNewGroups() throws Exception {
        List<String> groups = ImmutableList.of("group1", "group2");
        when(groupDAO.getCapacityGroups(testEnvBean.getEnv_id()))
                .thenReturn(ImmutableList.of(TEST_CLUSTER_NAME));

        environHandler.updateGroups(testEnvBean, groups, TEST_OPERATOR);

        ArgumentCaptor<String> groupCaptor = ArgumentCaptor.forClass(String.class);

        verify(groupDAO, times(2)).addGroupCapacity(anyString(), groupCaptor.capture());
        verify(groupDAO, never()).removeGroupCapacity(anyString(), anyString());

        List<String> capturedGroups = groupCaptor.getAllValues();

        for (int i = 0; i < groups.size(); i++) {
            assertEquals(groups.get(i), capturedGroups.get(i));
        }
    }

    @Test
    void updateGroups_addEmptyGroups() throws Exception {
        List<String> groups = ImmutableList.of();
        when(groupDAO.getCapacityGroups(testEnvBean.getEnv_id()))
                .thenReturn(ImmutableList.of(new String(TEST_CLUSTER_NAME)));

        environHandler.updateGroups(testEnvBean, groups, TEST_OPERATOR);

        verify(groupDAO, never()).addGroupCapacity(anyString(), anyString());
        verify(groupDAO, never()).removeGroupCapacity(anyString(), anyString());
    }

    @Test
    void updateGroups_replaceGroups() throws Exception {
        ImmutableList<String> oldGroups = ImmutableList.of(TEST_CLUSTER_NAME, "group1");
        when(groupDAO.getCapacityGroups(testEnvBean.getEnv_id())).thenReturn(oldGroups);

        environHandler.updateGroups(testEnvBean, ImmutableList.of("group2"), TEST_OPERATOR);

        ArgumentCaptor<String> removedGroupCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> addedGroupCaptor = ArgumentCaptor.forClass(String.class);

        verify(groupDAO, times(1)).addGroupCapacity(anyString(), addedGroupCaptor.capture());
        verify(groupDAO, times(1)).removeGroupCapacity(anyString(), removedGroupCaptor.capture());

        List<String> addedGroups = addedGroupCaptor.getAllValues();
        assertEquals("group2", addedGroups.get(0));

        List<String> removedGroups = removedGroupCaptor.getAllValues();
        assertEquals("group1", removedGroups.get(0));
    }
}
