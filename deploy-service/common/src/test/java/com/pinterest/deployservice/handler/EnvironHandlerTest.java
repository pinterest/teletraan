/**
 * Copyright (c) 2024 Pinterest, Inc.
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
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.EnvType;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.bean.HostState;
import com.pinterest.deployservice.dao.AgentDAO;
import com.pinterest.deployservice.dao.EnvironDAO;
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

class EnvironHandlerTest {
    private static final String DEFAULT_HOST_ID = "hostId";

    private EnvironHandler environHandler;

    private HostDAO mockHostDAO;
    private AgentDAO mockAgentDAO;
    private EnvironDAO environDAO;
    private List<String> hostIds = Arrays.asList("hostId1", "hostId2");

    private ServiceContext createMockServiceContext() throws Exception {
        mockHostDAO = mock(HostDAO.class);
        mockAgentDAO = mock(AgentDAO.class);
        environDAO = mock(EnvironDAO.class);

        ServiceContext serviceContext = new ServiceContext();
        serviceContext.setHostDAO(mockHostDAO);
        serviceContext.setAgentDAO(mockAgentDAO);
        serviceContext.setEnvironDAO(environDAO);
        return serviceContext;
    }

    @BeforeEach
    void setUp() throws Exception {
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
        EnvironBean envBean = new EnvironBean();
        envBean.setStage_type(EnvType.DEV);
        environHandler.createEnvStage(envBean, "Anonymous");
        verify(environDAO).insert(argument.capture());
        assertEquals(true, argument.getValue().getAllow_private_build());
    }

    @Test
    void ensureHostsOwnedByEnv_noMainEnv() throws Exception {
        assertThrows(
                NotFoundException.class,
                () -> environHandler.ensureHostsOwnedByEnv(new EnvironBean(), hostIds));
    }

    @Test
    void ensureHostsOwnedByEnv_differentMainEnv() throws Exception {
        EnvironBean envBean = new EnvironBean();
        envBean.setEnv_id("envId");
        when(environDAO.getMainEnvByHostId(anyString())).thenReturn(envBean);
        assertThrows(
                ForbiddenException.class,
                () -> environHandler.ensureHostsOwnedByEnv(new EnvironBean(), hostIds));
    }

    @Test
    void ensureHostsOwnedByEnv_sameMainEnv() throws Exception {
        EnvironBean envBean = new EnvironBean();
        envBean.setEnv_id("envId");
        when(environDAO.getMainEnvByHostId(anyString())).thenReturn(envBean);
        assertDoesNotThrow(() -> environHandler.ensureHostsOwnedByEnv(envBean, hostIds));
    }

    @Test
    void ensureHostsOwnedByEnv_sqlException() throws Exception {
        when(environDAO.getMainEnvByHostId(anyString())).thenThrow(SQLException.class);
        assertThrows(
                WebApplicationException.class,
                () -> environHandler.ensureHostsOwnedByEnv(new EnvironBean(), hostIds));
    }
}
