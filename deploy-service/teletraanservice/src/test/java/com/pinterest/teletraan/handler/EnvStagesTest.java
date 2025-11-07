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
package com.pinterest.teletraan.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.pinterest.deployservice.bean.EnvType;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.dao.ConfigHistoryDAO;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.resource.EnvStages;
import java.security.Principal;
import javax.ws.rs.core.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class EnvStagesTest {
    private EnvStages envStages;
    private EnvironDAO environDAO;
    private ConfigHistoryDAO configHistoryDAO;

    @BeforeEach
    public void setup() throws Exception {
        environDAO = mock(EnvironDAO.class);
        configHistoryDAO = mock(ConfigHistoryDAO.class);
        TeletraanServiceContext tsc = new TeletraanServiceContext();
        tsc.setEnvironDAO(environDAO);
        tsc.setConfigHistoryDAO(configHistoryDAO);
        envStages = new EnvStages(tsc);
    }

    @Test
    public void updateEnvToDev_setsAllowPrivateBuildTrue() throws Exception {
        ArgumentCaptor<EnvironBean> argument = ArgumentCaptor.forClass(EnvironBean.class);
        EnvironBean envBean = new EnvironBean();
        envBean.setStage_type(EnvType.DEV);
        Mockito.when(environDAO.getByStage(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(envBean);
        SecurityContext mockSC = mock(SecurityContext.class);
        Principal mockPrincipal = mock(Principal.class);
        Mockito.when(mockSC.getUserPrincipal()).thenReturn(mockPrincipal);
        envStages.update(mockSC, "test-env", "test-stage", envBean);
        verify(environDAO).update(Mockito.any(), Mockito.any(), argument.capture());
        assertEquals(true, argument.getValue().getAllow_private_build());
    }

    @Test
    public void updateEnvStageOffDev_unsetsAllowPrivateBuild() throws Exception {
        ArgumentCaptor<EnvironBean> argument = ArgumentCaptor.forClass(EnvironBean.class);
        EnvironBean envBean = new EnvironBean();
        EnvironBean origBean = new EnvironBean();
        origBean.setAllow_private_build(true);
        origBean.setStage_type(EnvType.DEV);
        envBean.setStage_type(EnvType.STAGING);
        Mockito.when(environDAO.getByStage(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(origBean);
        SecurityContext mockSC = mock(SecurityContext.class);
        Principal mockPrincipal = mock(Principal.class);
        Mockito.when(mockSC.getUserPrincipal()).thenReturn(mockPrincipal);
        envStages.update(mockSC, "test-env", "test-stage", envBean); // ava
        verify(environDAO).update(Mockito.any(), Mockito.any(), argument.capture());
        assertEquals(false, argument.getValue().getAllow_private_build());
    }
}
