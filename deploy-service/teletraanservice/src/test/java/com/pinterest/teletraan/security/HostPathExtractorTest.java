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
package com.pinterest.teletraan.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.dao.HostAgentDAO;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor.ExtractionException;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import java.sql.SQLException;
import javax.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HostPathExtractorTest extends BasePathExtractorTest {
    private HostPathExtractor sut;
    private ServiceContext serviceContext;
    private HostAgentDAO hostDAO;

    @BeforeEach
    void setUp() {
        super.setUp();
        hostDAO = mock(HostAgentDAO.class);
        serviceContext = new ServiceContext();
        serviceContext.setHostAgentDAO(hostDAO);
        sut = new HostPathExtractor(serviceContext);
    }

    @Test
    void testExtractResource_noPathParams_exception() {
        assertThrows(ExtractionException.class, () -> sut.extractResource(context));
    }

    @Test
    void testExtractResource_0HostId() {
        pathParameters.add("param", "val");

        assertThrows(ExtractionException.class, () -> sut.extractResource(context));
    }

    @Test
    void testExtractResource() throws Exception {
        String hostId = "testHostId";
        pathParameters.add("hostId", hostId);

        when(hostDAO.getMainEnvByHostId(hostId)).thenReturn(null);
        assertThrows(NotFoundException.class, () -> sut.extractResource(context));

        EnvironBean envBean = new EnvironBean();
        envBean.setEnv_name("host_env");
        envBean.setStage_name("host_stage");
        when(hostDAO.getMainEnvByHostId(hostId)).thenReturn(envBean);

        AuthZResource result = sut.extractResource(context);

        assertNotNull(result);
        assertTrue(result.getName().contains(envBean.getEnv_name()));
        assertTrue(result.getName().contains(envBean.getStage_name()));
        assertEquals(AuthZResource.Type.ENV_STAGE, result.getType());
    }

    @Test
    void testExtractResource_sqlException() throws Exception {
        pathParameters.add("hostId", "someId");
        when(hostDAO.getMainEnvByHostId(any())).thenThrow(SQLException.class);

        assertThrows(ExtractionException.class, () -> sut.extractResource(context));
    }
}
