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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.HotfixBean;
import com.pinterest.deployservice.dao.HotfixDAO;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor.ExtractionException;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import java.sql.SQLException;
import javax.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HotfixPathExtractorTest extends BasePathExtractorTest {
    private HotfixPathExtractor sut;
    private ServiceContext serviceContext;
    private HotfixDAO hotfixDAO;

    @BeforeEach
    void setUp() {
        super.setUp();
        hotfixDAO = mock(HotfixDAO.class);
        serviceContext = new ServiceContext();
        serviceContext.setHotfixDAO(hotfixDAO);
        sut = new HotfixPathExtractor(serviceContext);
    }

    @Test
    void testExtractResource_noPathParams_exception() {
        assertThrows(ExtractionException.class, () -> sut.extractResource(context));
    }

    @Test
    void testExtractResource_0HotfixId() {
        pathParameters.add("param", "val");

        assertThrows(ExtractionException.class, () -> sut.extractResource(context));
    }

    @Test
    void testExtractResource() throws Exception {
        String hotfixId = "testHotfixId";
        pathParameters.add("id", hotfixId);

        when(hotfixDAO.getByHotfixId(hotfixId)).thenReturn(null);
        assertThrows(NotFoundException.class, () -> sut.extractResource(context));

        HotfixBean hotfixBean = new HotfixBean();
        hotfixBean.setEnv_name("hotfix_env");
        when(hotfixDAO.getByHotfixId(hotfixId)).thenReturn(hotfixBean);

        AuthZResource result = sut.extractResource(context);

        assertNotNull(result);
        assertEquals(hotfixBean.getEnv_name() + "/", result.getName());
        assertEquals(AuthZResource.Type.ENV_STAGE, result.getType());
    }

    @Test
    void testExtractResource_sqlException() throws Exception {
        pathParameters.add("id", "someId");
        when(hotfixDAO.getByHotfixId(any())).thenThrow(SQLException.class);

        assertThrows(ExtractionException.class, () -> sut.extractResource(context));
    }
}
