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
package com.pinterest.deployservice.db;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.HostAgentBean;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.HostAgentDAO;
import com.pinterest.deployservice.fixture.EnvironBeanFixture;
import org.apache.commons.dbcp.BasicDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DBHostAgentDAOImplTest {
    private static final String HOST_ID = "host123";
    private static final String TEST_CLUSTER = "test-cluster";
    private static BasicDataSource dataSource;
    private HostAgentDAO sut;

    @BeforeAll
    static void setUpAll() throws Exception {
        dataSource = DBUtils.createTestDataSource();
    }

    @BeforeEach
    void setUp() {
        sut = new DBHostAgentDAOImpl(dataSource);
    }

    @Test
    void testGetMainEnvbyHostId() throws Exception {
        EnvironDAO environDAO = new DBEnvironDAOImpl(dataSource);
        EnvironBean expectedEnvBean = EnvironBeanFixture.createRandomEnvironBean();
        expectedEnvBean.setCluster_name(TEST_CLUSTER);
        environDAO.insert(expectedEnvBean);

        HostAgentBean hostAgentBean = new HostAgentBean();
        hostAgentBean.setHost_id(HOST_ID);
        hostAgentBean.setAuto_scaling_group(TEST_CLUSTER);
        sut.insert(hostAgentBean);

        EnvironBean actualEnvironBean = sut.getMainEnvByHostId(HOST_ID);
        assertEquals(expectedEnvBean.getEnv_name(), actualEnvironBean.getEnv_name());
        assertEquals(expectedEnvBean.getStage_name(), actualEnvironBean.getStage_name());

        EnvironBean nullEnvironBean = sut.getMainEnvByHostId("random-host-id");
        assertNull(nullEnvironBean);
    }
}
