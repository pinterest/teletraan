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
package com.pinterest.teletraan.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.fixture.EnvironBeanFixture;
import com.pinterest.teletraan.TeletraanServiceContext;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DropwizardExtensionsSupport.class)
class EnvStagesTest {
    private static final String ENV1 = "env1";
    private static final String STAGE1 = "stage1";
    private static final String TARGET = "/v1/envs/";
    private static final ResourceExtension EXT;
    private static EnvironDAO environDAO = mock(EnvironDAO.class);

    static {
        TeletraanServiceContext context = new TeletraanServiceContext();
        context.setEnvironDAO(environDAO);
        EXT = ResourceExtension.builder()
                .addResource(new EnvStages(context))
                .build();
    }

    @Test
    void testUpdate_cannotChangeIsSox() throws Exception {
        EnvironBean originalBean = EnvironBeanFixture.createRandomEnvironBean();
        EnvironBean updatedBean = EnvironBeanFixture.createRandomEnvironBean();

        Boolean[] originalSox = {null, true, false};
        Boolean[] newSox = {true, false, true};
        for (int i = 0; i < originalSox.length; i++) {
            originalBean.setIs_sox(originalSox[i]);
            updatedBean.setIs_sox(newSox[i]);
            when(environDAO.getByStage(ENV1, STAGE1)).thenReturn(originalBean);

            Response response = EXT.target(TARGET + ENV1 + "/" + STAGE1)
                    .request()
                    .put(Entity.json(updatedBean));

            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }
    }
}
