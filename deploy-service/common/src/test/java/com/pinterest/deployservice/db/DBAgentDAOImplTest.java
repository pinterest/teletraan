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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pinterest.deployservice.bean.AgentBean;
import com.pinterest.deployservice.bean.AgentState;
import com.pinterest.deployservice.bean.AgentStatus;
import com.pinterest.deployservice.bean.DeployStage;
import com.pinterest.deployservice.bean.HostAgentBean;
import com.pinterest.deployservice.bean.KnoxStatus;
import com.pinterest.deployservice.bean.NormandieStatus;
import com.pinterest.deployservice.dao.AgentDAO;
import com.pinterest.deployservice.dao.HostAgentDAO;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.apache.commons.dbcp.BasicDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class DBAgentDAOImplTest {

    private static BasicDataSource dataSource;
    private static HostAgentDAO hostAgentDAO;
    private static AgentDAO agentDAO;

    @BeforeAll
    public static void setUpClass() throws Exception {
        dataSource = DBUtils.createTestDataSource();

        hostAgentDAO = new DBHostAgentDAOImpl(dataSource);
        agentDAO = new DBAgentDAOImpl(dataSource);
    }

    @AfterEach
    void tearDown() throws Exception {
        DBUtils.truncateAllTables(dataSource);
    }

    @Test
    public void testBatchInsertOrUpdate_insertsMultipleAgents() throws Exception {
        AgentBean agent1 = genDefaultAgentBean("host-1", "env-1");
        AgentBean agent2 = genDefaultAgentBean("host-2", "env-1");

        agentDAO.batchInsertOrUpdate(Arrays.asList(agent1, agent2));

        AgentBean result1 = agentDAO.getByHostEnvIds("host-1", "env-1");
        assertNotNull(result1);
        assertEquals("host-1", result1.getHost_id());
        assertEquals("env-1", result1.getEnv_id());

        AgentBean result2 = agentDAO.getByHostEnvIds("host-2", "env-1");
        assertNotNull(result2);
        assertEquals("host-2", result2.getHost_id());
    }

    @Test
    public void testBatchInsertOrUpdate_updatesExistingAgents() throws Exception {
        AgentBean agent = genDefaultAgentBean("host-1", "env-1");
        agentDAO.insertOrUpdate(agent);

        agent.setDeploy_stage(DeployStage.SERVING_BUILD);
        agentDAO.batchInsertOrUpdate(Collections.singletonList(agent));

        AgentBean result = agentDAO.getByHostEnvIds("host-1", "env-1");
        assertNotNull(result);
        assertEquals(DeployStage.SERVING_BUILD, result.getDeploy_stage());
    }

    @Test
    public void testBatchInsertOrUpdate_emptyList() throws Exception {
        agentDAO.batchInsertOrUpdate(Collections.emptyList());
    }

    @Test
    public void testBatchInsertOrUpdate_mixedInsertAndUpdate() throws Exception {
        AgentBean existing = genDefaultAgentBean("host-1", "env-1");
        agentDAO.insertOrUpdate(existing);

        existing.setStatus(AgentStatus.UNKNOWN);
        AgentBean newAgent = genDefaultAgentBean("host-2", "env-1");

        agentDAO.batchInsertOrUpdate(Arrays.asList(existing, newAgent));

        AgentBean result1 = agentDAO.getByHostEnvIds("host-1", "env-1");
        assertNotNull(result1);
        assertEquals(AgentStatus.UNKNOWN, result1.getStatus());

        AgentBean result2 = agentDAO.getByHostEnvIds("host-2", "env-1");
        assertNotNull(result2);
        assertEquals(AgentStatus.SUCCEEDED, result2.getStatus());
    }

    private AgentBean genDefaultAgentBean(String hostId, String envId) {
        AgentBean bean = new AgentBean();
        bean.setHost_id(hostId);
        bean.setHost_name(hostId + "-name");
        bean.setEnv_id(envId);
        bean.setDeploy_id("deploy-1");
        bean.setDeploy_stage(DeployStage.PRE_DOWNLOAD);
        bean.setState(AgentState.NORMAL);
        bean.setStatus(AgentStatus.SUCCEEDED);
        bean.setLast_update(System.currentTimeMillis());
        bean.setStart_date(System.currentTimeMillis());
        bean.setLast_err_no(0);
        bean.setFail_count(0);
        bean.setFirst_deploy(false);
        bean.setStage_start_date(System.currentTimeMillis());
        return bean;
    }

    @Test
    public void testHostAgentDAO() throws Exception {
        final String hostId = "host-1";

        // Test Insert and getById
        HostAgentBean hostAgentBean = genDefaultHostAgentBean(hostId);
        hostAgentDAO.insert(hostAgentBean);
        HostAgentBean getByIdBean = hostAgentDAO.getHostById(hostId);
        assertEquals(hostAgentBean, getByIdBean);

        // Test Update and getById
        hostAgentBean.setIp("192.168.0.1");
        hostAgentDAO.update(hostId, hostAgentBean);
        HostAgentBean getByIdBean2 = hostAgentDAO.getHostById(hostId);
        assertEquals(hostAgentBean, getByIdBean2);

        // Test getHostByName
        HostAgentBean getByNameBean = hostAgentDAO.getHostByName(hostAgentBean.getHost_name());
        assertEquals(hostAgentBean, getByNameBean);

        // Test getDistinctHostsCount
        long hostCount = hostAgentDAO.getDistinctHostsCount();
        assertEquals(1, hostCount);

        // Test getStaleHosts
        List<HostAgentBean> staleHosts =
                hostAgentDAO.getStaleHosts(System.currentTimeMillis() - 100_000);
        assertTrue(staleHosts.isEmpty());

        List<HostAgentBean> staleHosts2 =
                hostAgentDAO.getStaleHosts(System.currentTimeMillis() + 100_000);
        assertEquals(1, staleHosts2.size());
        assertEquals(hostAgentBean, staleHosts2.get(0));

        List<HostAgentBean> staleHosts3 =
                hostAgentDAO.getStaleHosts(
                        System.currentTimeMillis() - 100_000, System.currentTimeMillis() + 100_000);
        assertEquals(1, staleHosts3.size());
        assertEquals(hostAgentBean, staleHosts3.get(0));

        // Test Delete
        hostAgentDAO.delete(hostId);
        HostAgentBean getByIdBean3 = hostAgentDAO.getHostById(hostId);
        assertNull(getByIdBean3);
        long hostCount2 = hostAgentDAO.getDistinctHostsCount();
        assertEquals(0, hostCount2);
    }

    @Test
    public void testTouchLastUpdate() throws Exception {
        final String hostId = "host-touch";

        HostAgentBean hostAgentBean = genDefaultHostAgentBean(hostId);
        hostAgentDAO.insert(hostAgentBean);

        long newLastUpdate = hostAgentBean.getLast_update() + 60_000;
        hostAgentDAO.touchLastUpdate(hostId, newLastUpdate);

        HostAgentBean result = hostAgentDAO.getHostById(hostId);
        assertEquals(newLastUpdate, result.getLast_update());
        // Other fields should remain unchanged
        assertEquals(hostAgentBean.getIp(), result.getIp());
        assertEquals(hostAgentBean.getAgent_version(), result.getAgent_version());
        assertEquals(hostAgentBean.getHost_name(), result.getHost_name());
        assertEquals(hostAgentBean.getAuto_scaling_group(), result.getAuto_scaling_group());
        assertEquals(hostAgentBean.getNormandie_status(), result.getNormandie_status());
        assertEquals(hostAgentBean.getKnox_status(), result.getKnox_status());
    }

    private HostAgentBean genDefaultHostAgentBean(String hostId) {
        return HostAgentBean.builder()
                .ip("127.0.0.1")
                .host_id(hostId)
                .host_name(UUID.randomUUID().toString())
                .create_date(System.currentTimeMillis())
                .last_update(System.currentTimeMillis())
                .agent_version("1.0")
                .auto_scaling_group("auto-scaling-group")
                .normandie_status(NormandieStatus.OK)
                .knox_status(KnoxStatus.OK)
                .build();
    }
}
