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

import com.pinterest.deployservice.bean.BeanUtils;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.HostAgentBean;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.HostAgentDAO;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.fixture.EnvironBeanFixture;
import java.time.Instant;
import org.apache.commons.dbcp.BasicDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DBEnvironDAOImplTest {
    private static final String HOST_ID = "host123";
    private static final String HOST_NAME = "host456";
    private static final String TEST_CLUSTER = "test-cluster";
    private static BasicDataSource dataSource;
    private static HostAgentDAO hostAgentDAO;
    private static HostDAO hostDAO;
    private EnvironDAO sut;

    @BeforeAll
    static void setUpAll() throws Exception {
        dataSource = DBUtils.createTestDataSource();
        hostAgentDAO = new DBHostAgentDAOImpl(dataSource);
        hostDAO = new DBHostDAOImpl(dataSource);
    }

    @BeforeEach
    void setUp() {
        sut = new DBEnvironDAOImpl(dataSource);
    }

    @AfterEach
    void tearDown() throws Exception {
        DBUtils.truncateAllTables(dataSource);
    }

    @Test
    void testGetMainEnvByHostId_happyPath() throws Exception {
        EnvironBean expectedEnvBean = setupCommonEntities();

        EnvironBean actualEnvironBean = sut.getMainEnvByHostId(HOST_ID);
        assertEnvironBean(expectedEnvBean, actualEnvironBean);

        EnvironBean nullEnvironBean = sut.getMainEnvByHostId("random-host-id");
        assertNull(nullEnvironBean);
    }

    @Test
    void testGetMainEnvByHostName_happyPath() throws Exception {
        EnvironBean expectedEnvBean = setupCommonEntities();

        EnvironBean actualEnvironBean = sut.getMainEnvByHostName(HOST_NAME);
        assertEnvironBean(expectedEnvBean, actualEnvironBean);

        EnvironBean nullEnvironBean = sut.getMainEnvByHostName("random-host-name");
        assertNull(nullEnvironBean);
    }

    @Test
    void testGetMainEnvByHostId_noHost() throws Exception {
        EnvironBean actualEnvironBean = sut.getMainEnvByHostId(HOST_ID);
        assertNull(actualEnvironBean);
    }

    @Test
    void testGetMainEnvByHostName_noHost() throws Exception {
        EnvironBean actualEnvironBean = sut.getMainEnvByHostName(HOST_NAME);
        assertNull(actualEnvironBean);
    }

    @Test
    void testGetMainEnvByHostId_noEnv() throws Exception {
        setUpHostAgentBean();

        EnvironBean actualEnvironBean = sut.getMainEnvByHostId(HOST_ID);
        assertNull(actualEnvironBean);
    }

    @Test
    void testGetMainEnvByHostName_noEnv() throws Exception {
        setUpHostAgentBean();

        EnvironBean actualEnvironBean = sut.getMainEnvByHostName(HOST_NAME);
        assertNull(actualEnvironBean);
    }

    @Test
    void testGetMainEnvByHostId_noHostAgent() throws Exception {
        EnvironBean expectedEnvBean = EnvironBeanFixture.createRandomEnvironBean();
        expectedEnvBean.setCluster_name(TEST_CLUSTER);
        sut.insert(expectedEnvBean);

        setUpHostBean(TEST_CLUSTER);
        setUpHostBean(TEST_CLUSTER + "2");

        EnvironBean actualEnvironBean = sut.getMainEnvByHostId(HOST_ID);
        assertEnvironBean(expectedEnvBean, actualEnvironBean);
    }

    @Test
    void testGetMainEnvByHostName_noHostAgent() throws Exception {
        EnvironBean expectedEnvBean = EnvironBeanFixture.createRandomEnvironBean();
        expectedEnvBean.setCluster_name(TEST_CLUSTER);
        sut.insert(expectedEnvBean);

        setUpHostBean(TEST_CLUSTER);
        setUpHostBean(TEST_CLUSTER + "2");

        EnvironBean actualEnvironBean = sut.getMainEnvByHostName(HOST_NAME);
        assertEnvironBean(expectedEnvBean, actualEnvironBean);
    }

    private void setUpHostAgentBean() throws Exception {
        HostAgentBean hostAgentBean = new HostAgentBean();
        hostAgentBean.setHost_id(HOST_ID);
        hostAgentBean.setHost_name(HOST_NAME);
        hostAgentBean.setAuto_scaling_group(TEST_CLUSTER);
        hostAgentDAO.insert(hostAgentBean);
    }

    private EnvironBean setupCommonEntities() throws Exception {
        EnvironBean expectedEnvBean = EnvironBeanFixture.createRandomEnvironBean();
        expectedEnvBean.setCluster_name(TEST_CLUSTER);
        sut.insert(expectedEnvBean);

        setUpHostAgentBean();
        setUpHostBean(TEST_CLUSTER + "sidecar");

        return expectedEnvBean;
    }

    private void setUpHostBean(String groupName) throws Exception {
        HostBean hostBean = BeanUtils.createHostBean(Instant.now());
        hostBean.setHost_id(HOST_ID);
        hostBean.setHost_name(HOST_NAME);
        hostBean.setGroup_name(groupName);
        hostDAO.insert(hostBean);
    }

    private void assertEnvironBean(EnvironBean expected, EnvironBean actual) {
        assertEquals(expected.getEnv_name(), actual.getEnv_name());
        assertEquals(expected.getStage_name(), actual.getStage_name());
        assertEquals(expected.getCluster_name(), actual.getCluster_name());
    }
}
