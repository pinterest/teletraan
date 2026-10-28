/**
 * Copyright (c) 2026 Pinterest, Inc.
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.TagBean;
import com.pinterest.deployservice.bean.TagTargetType;
import com.pinterest.deployservice.dao.TagDAO;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor.ExtractionException;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import javax.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TagPathExtractorTest extends BasePathExtractorTest {
    private TagDAO tagDAO;
    private TagPathExtractor extractor;

    @BeforeEach
    @Override
    void setUp() {
        super.setUp();
        ServiceContext serviceContext = new ServiceContext();
        tagDAO = mock(TagDAO.class);
        serviceContext.setTagDAO(tagDAO);
        extractor = new TagPathExtractor(serviceContext);
    }

    @Test
    void resourceComesFromStoredTag() throws Exception {
        pathParameters.add("id", "tag-id");
        TagBean tag = new TagBean();
        tag.setTarget_type(TagTargetType.TELETRAAN);
        tag.setTarget_id("TELETRAAN");
        when(tagDAO.getById("tag-id")).thenReturn(tag);

        assertEquals(AuthZResource.SYSTEM_RESOURCE, extractor.extractResource(context));
        verify(tagDAO).getById("tag-id");
    }

    @Test
    void missingTagIsNotFound() {
        pathParameters.add("id", "missing");
        assertThrows(NotFoundException.class, () -> extractor.extractResource(context));
    }

    @Test
    void missingTagIdFailsClosed() {
        assertThrows(ExtractionException.class, () -> extractor.extractResource(context));
        verifyNoInteractions(tagDAO);
    }

    @Test
    void tagLookupFailureFailsClosed() throws Exception {
        pathParameters.add("id", "tag-id");
        when(tagDAO.getById("tag-id")).thenThrow(new Exception("database unavailable"));
        assertThrows(ExtractionException.class, () -> extractor.extractResource(context));
    }
}
