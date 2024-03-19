package com.pinterest.teletraan.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.security.Principal;

import javax.ws.rs.core.SecurityContext;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.pinterest.deployservice.bean.EnvType;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.resource.EnvStages;
import com.pinterest.teletraan.security.Authorizer;

public class EnvStagesTest {
    private EnvStages envStages;
    private EnvironDAO environDAO;

    @Before
    public void setup() throws Exception {
        environDAO = mock(EnvironDAO.class);
        Authorizer auth = mock(Authorizer.class);
        TeletraanServiceContext tsc = new TeletraanServiceContext();
        tsc.setAuthorizer(auth);
        tsc.setEnvironDAO(environDAO);
        envStages = new EnvStages(tsc);
    }

    @Test
    public void createEnvStage() throws Exception {
        ArgumentCaptor<EnvironBean> argument = ArgumentCaptor.forClass(EnvironBean.class);
        EnvironBean envBean = new EnvironBean();
        envBean.setStage_type(EnvType.DEV);
        Mockito.when(environDAO.getByStage(Mockito.anyString(), Mockito.anyString())).thenReturn(envBean);
        SecurityContext mockSC = mock(SecurityContext.class);
        Principal mockPrincipal = mock(Principal.class);
        Mockito.when(mockSC.getUserPrincipal()).thenReturn(mockPrincipal);
        envStages.update(mockSC, "test-env", "test-stage", envBean);
        verify(environDAO).update(Mockito.any(), Mockito.any(), argument.capture());
        assertEquals(true, argument.getValue().getAllow_private_build());
    }
}
