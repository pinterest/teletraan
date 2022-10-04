package com.pinterest.deployservice.handler;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.bean.HostState;
import com.pinterest.deployservice.dao.AgentDAO;
import com.pinterest.deployservice.dao.HostDAO;

public class EnvironHandlerTest {
    private final static String DEFAULT_HOST_ID = "hostId";

    private EnvironHandler environHandler;

    private HostDAO mockHostDAO;
    private AgentDAO mockAgentDAO;

    private ServiceContext createMockServiceContext() throws Exception {
        mockHostDAO = mock(HostDAO.class);
        mockAgentDAO = mock(AgentDAO.class);

        ServiceContext serviceContext = new ServiceContext();
        serviceContext.setHostDAO(mockHostDAO);
        serviceContext.setAgentDAO(mockAgentDAO);
        return serviceContext;
    }

    @Before
    public void setUp() throws Exception {
        environHandler = new EnvironHandler(createMockServiceContext());
    }

    @Test
    public void stopServiceOnHost_withReplaceHost_hostBeanStateIsPendingTerminate() throws Exception {
        ArgumentCaptor<HostBean> argument = ArgumentCaptor.forClass(HostBean.class);

        environHandler.stopServiceOnHost(DEFAULT_HOST_ID, true);
        verify(mockHostDAO).updateHostById(eq(DEFAULT_HOST_ID), argument.capture());
        assertEquals(HostState.PENDING_TERMINATE, argument.getValue().getState());
    }

    @Test
    public void stopServiceOnHost_withoutReplaceHost_hostBeanStateIsPendingTerminateNoReplace() throws Exception {
        ArgumentCaptor<HostBean> argument = ArgumentCaptor.forClass(HostBean.class);

        environHandler.stopServiceOnHost(DEFAULT_HOST_ID, false);
        verify(mockHostDAO).updateHostById(eq(DEFAULT_HOST_ID), argument.capture());
        assertEquals(HostState.PENDING_TERMINATE_NO_REPLACE, argument.getValue().getState());
    }
}
