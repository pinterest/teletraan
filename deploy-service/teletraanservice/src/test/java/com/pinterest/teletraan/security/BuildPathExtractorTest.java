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
import com.pinterest.deployservice.bean.BuildBean;
import com.pinterest.deployservice.dao.BuildDAO;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor.ExtractionException;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import java.sql.SQLException;
import javax.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BuildPathExtractorTest extends BasePathExtractorTest {
    private BuildPathExtractor sut;
    private ServiceContext serviceContext;
    private BuildDAO buildDAO;

    @BeforeEach
    void setUp() {
        super.setUp();
        buildDAO = mock(BuildDAO.class);
        serviceContext = new ServiceContext();
        serviceContext.setBuildDAO(buildDAO);
        sut = new BuildPathExtractor(serviceContext);
    }

    @Test
    void testExtractResource_noPathParams_exception() {
        assertThrows(ExtractionException.class, () -> sut.extractResource(context));
    }

    @Test
    void testExtractResource_0BuildId() {
        pathParameters.add("param", "val");

        assertThrows(ExtractionException.class, () -> sut.extractResource(context));
    }

    @Test
    void testExtractResource() throws Exception {
        String buildId = "testBuildId";
        pathParameters.add("id", buildId);

        when(buildDAO.getById(buildId)).thenReturn(null);
        assertThrows(NotFoundException.class, () -> sut.extractResource(context));

        BuildBean buildBean = new BuildBean();
        buildBean.setBuild_name("Test Build");
        buildBean.setArtifact_url("testURL");
        when(buildDAO.getById(buildId)).thenReturn(buildBean);

        AuthZResource result = sut.extractResource(context);

        assertNotNull(result);
        assertEquals("Test Build", result.getName());
        assertEquals(AuthZResource.Type.BUILD, result.getType());
        assertEquals(
                "testURL",
                result.getAttributes().get(AuthZResource.AttributeKeys.BUILD_ARTIFACT_URL.name()));
    }

    @Test
    void testExtractResource_sqlException() throws Exception {
        pathParameters.add("id", "someId");
        when(buildDAO.getById(any())).thenThrow(SQLException.class);

        assertThrows(ExtractionException.class, () -> sut.extractResource(context));
    }
}
