/**
 * Copyright 2016 Pinterest, Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.pinterest.deployservice.bean.AcceptanceStatus;
import com.pinterest.deployservice.bean.AcceptanceType;
import com.pinterest.deployservice.bean.AgentBean;
import com.pinterest.deployservice.bean.AgentErrorBean;
import com.pinterest.deployservice.bean.AgentState;
import com.pinterest.deployservice.bean.AgentStatus;
import com.pinterest.deployservice.bean.BuildBean;
import com.pinterest.deployservice.bean.ConfigHistoryBean;
import com.pinterest.deployservice.bean.DataBean;
import com.pinterest.deployservice.bean.DeployBean;
import com.pinterest.deployservice.bean.DeployFilterBean;
import com.pinterest.deployservice.bean.DeployPriority;
import com.pinterest.deployservice.bean.DeployQueryResultBean;
import com.pinterest.deployservice.bean.DeployStage;
import com.pinterest.deployservice.bean.DeployState;
import com.pinterest.deployservice.bean.DeployType;
import com.pinterest.deployservice.bean.EnvState;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.EnvironState;
import com.pinterest.deployservice.bean.GroupRolesBean;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.bean.HostState;
import com.pinterest.deployservice.bean.OverridePolicy;
import com.pinterest.deployservice.bean.PromoteBean;
import com.pinterest.deployservice.bean.PromoteType;
import com.pinterest.deployservice.bean.RatingBean;
import com.pinterest.deployservice.bean.Resource;
import com.pinterest.deployservice.bean.Role;
import com.pinterest.deployservice.bean.ScheduleBean;
import com.pinterest.deployservice.bean.ScheduleState;
import com.pinterest.deployservice.bean.TagBean;
import com.pinterest.deployservice.bean.TagTargetType;
import com.pinterest.deployservice.bean.TagValue;
import com.pinterest.deployservice.bean.TokenRolesBean;
import com.pinterest.deployservice.bean.UserRolesBean;
import com.pinterest.deployservice.common.CommonUtils;
import com.pinterest.deployservice.common.Constants;
import com.pinterest.deployservice.dao.AgentDAO;
import com.pinterest.deployservice.dao.AgentErrorDAO;
import com.pinterest.deployservice.dao.BuildDAO;
import com.pinterest.deployservice.dao.ConfigHistoryDAO;
import com.pinterest.deployservice.dao.DataDAO;
import com.pinterest.deployservice.dao.DeployDAO;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.GroupDAO;
import com.pinterest.deployservice.dao.GroupRolesDAO;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.dao.PromoteDAO;
import com.pinterest.deployservice.dao.RatingDAO;
import com.pinterest.deployservice.dao.ScheduleDAO;
import com.pinterest.deployservice.dao.TagDAO;
import com.pinterest.deployservice.dao.TokenRolesDAO;
import com.pinterest.deployservice.dao.UserRolesDAO;

import com.ibatis.common.jdbc.ScriptRunner;
import com.mysql.management.driverlaunched.ServerLauncherSocketFactory;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.joda.time.Interval;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class DBDAOTest {

    private final static String DEFAULT_BASE_DIR = "/tmp/deploy-unit-test";
    private final static String DEFAULT_DB_NAME = "deploy";
    private final static int DEFAULT_PORT = 3303;

    private static BuildDAO buildDAO;
    private static AgentDAO agentDAO;
    private static AgentErrorDAO agentErrorDAO;
    private static DataDAO dataDAO;
    private static DeployDAO deployDAO;
    private static EnvironDAO environDAO;
    private static PromoteDAO promoteDAO;
    private static HostDAO hostDAO;
    private static GroupDAO groupDAO;
    private static RatingDAO ratingDAO;
    private static UserRolesDAO userRolesDAO;
    private static TokenRolesDAO tokenRolesDAO;
    private static GroupRolesDAO groupRolesDAO;
    private static ConfigHistoryDAO configHistoryDAO;
    private static TagDAO tagDAO;
    private static ScheduleDAO scheduleDAO;

    @BeforeClass
    public static void setUpClass() throws Exception {
        try {
            // making sure we do not have anything running
            ServerLauncherSocketFactory.shutdown(new File(DEFAULT_BASE_DIR), null);
        } catch (Exception e) {
            // ignore
        }
        BasicDataSource DATASOURCE = DatabaseUtil.createMXJDataSource(DEFAULT_DB_NAME,
            DEFAULT_BASE_DIR, DEFAULT_PORT);
        Connection conn = DATASOURCE.getConnection();
        ScriptRunner runner = new ScriptRunner(conn, false, true);
        runner.runScript(new BufferedReader(new InputStreamReader(
            DBDAOTest.class.getResourceAsStream("/sql/cleanup.sql"))));
        runner.runScript(new BufferedReader(new InputStreamReader(
            DBDAOTest.class.getResourceAsStream("/sql/deploy.sql"))));
        buildDAO = new DBBuildDAOImpl(DATASOURCE);
        agentDAO = new DBAgentDAOImpl(DATASOURCE);
        agentErrorDAO = new DBAgentErrorDAOImpl(DATASOURCE);
        dataDAO = new DBDataDAOImpl(DATASOURCE);
        deployDAO = new DBDeployDAOImpl(DATASOURCE);
        environDAO = new DBEnvironDAOImpl(DATASOURCE);
        promoteDAO = new DBPromoteDAOImpl(DATASOURCE);
        hostDAO = new DBHostDAOImpl(DATASOURCE);
        groupDAO = new DBGroupDAOImpl(DATASOURCE);
        ratingDAO = new DBRatingsDAOImpl(DATASOURCE);
        userRolesDAO = new DBUserRolesDAOImpl(DATASOURCE);
        groupRolesDAO = new DBGroupRolesDAOImpl(DATASOURCE);
        tokenRolesDAO = new DBTokenRolesDAOImpl(DATASOURCE);
        configHistoryDAO = new DBConfigHistoryDAOImpl(DATASOURCE);
        tagDAO = new DBTagDAOImpl(DATASOURCE);
        scheduleDAO = new DBScheduleDAOImpl(DATASOURCE);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        ServerLauncherSocketFactory.shutdown(new File(DEFAULT_BASE_DIR), null);
    }

    @Test
    public void testDeploymentQueries() throws Exception {
        long now = System.currentTimeMillis();

        BuildBean buildBean1 =
            genDefaultBuildInfoBean("bbb-1", "s-1", "ccc-1", "r-1", now);
        BuildBean buildBean2 =
            genDefaultBuildInfoBean("bbb-2", "s-1", "ccc-1", "r-1", now + 1000);
        BuildBean buildBean3 =
            genDefaultBuildInfoBean("bbb-3", "s-1", "ccc-1", "r-1", now + 2000);
        BuildBean buildBean4 =
            genDefaultBuildInfoBean("bbb-4", "s-2", "ccc-2", "r-1", now + 3000);

        buildDAO.insert(buildBean1);
        buildDAO.insert(buildBean2);
        buildDAO.insert(buildBean3);
        buildDAO.insert(buildBean4);

        DeployBean deployBean1 =
            genDefaultDeployBean("d-1", "env-1", "bbb-1", now, DeployState.SUCCEEDED);
        DeployBean deployBean2 =
            genDefaultDeployBean("d-2", "env-1", "bbb-1", now + 1000, DeployState.SUCCEEDED);
        DeployBean deployBean3 =
            genDefaultDeployBean("d-3", "env-1", "bbb-1", now + 2000, DeployState.RUNNING);
        DeployBean deployBean4 =
            genDefaultDeployBean("d-4", "env-2", "bbb-2", now, DeployState.FAILING);
        // just so we have the build
        BuildBean buildBeanx =
            genDefaultBuildInfoBean("d-x", "s-1", "ccc-x", "r-1", now);
        buildDAO.insert(buildBeanx);
        DeployBean deployBean5 =
            genDefaultDeployBean("d-5", "env-3", "bcc-x", now, DeployState.SUCCEEDING);
        DeployBean deployBean6 =
            genDefaultDeployBean("d-6", "env-2", "bbb-4", now, DeployState.SUCCEEDED);

        deployDAO.insert(deployBean1);
        deployDAO.insert(deployBean2);
        deployDAO.insert(deployBean3);
        deployDAO.insert(deployBean4);
        deployDAO.insert(deployBean5);
        deployDAO.insert(deployBean6);

        deployBean1.setOperator("bar");
        DeployBean updateBean = new DeployBean();
        updateBean.setOperator("bar");
        deployDAO.update("d-1", updateBean);
        assertTrue(EqualsBuilder.reflectionEquals(deployBean1, deployDAO.getById("d-1")));

        DeployQueryResultBean queryResult;
        DeployFilterBean buildFilter = new DeployFilterBean();
        buildFilter.setCommit("ccc-1");
        buildFilter.setRepo("repo-1");
        buildFilter.setBranch("branch-1");
        buildFilter.setCommitDate(now);
        buildFilter.setOldestFirst(true);
        buildFilter.setPageIndex(1);
        buildFilter.setPageSize(10);
        DeployQueryFilter buildFilterBean = new DeployQueryFilter(buildFilter);
        queryResult = deployDAO.getAllDeploys(buildFilterBean);
        assertEquals(queryResult.getTotal().longValue(), 5L);
        assertFalse(queryResult.isTruncated());
        assertEquals(queryResult.getDeploys().size(), 5);

        buildFilter.setCommit("ccc-2");
        buildFilter.setCommitDate(now + 3000);
        buildFilterBean = new DeployQueryFilter(buildFilter);
        queryResult = deployDAO.getAllDeploys(buildFilterBean);
        assertEquals(queryResult.getTotal().longValue(), 1);
        assertFalse(queryResult.isTruncated());
        assertEquals(queryResult.getDeploys().size(), 1);
        DeployBean deployBean = queryResult.getDeploys().get(0);
        assertEquals("d-6", deployBean.getDeploy_id());
        assertEquals("bbb-4", deployBean.getBuild_id());

        DeployFilterBean envFilter = new DeployFilterBean();
        envFilter.setEnvIds(Arrays.asList("env-1"));
        buildFilter.setCommitDate(now);
        envFilter.setBefore(now + 2000);
        envFilter.setAfter(now);
        envFilter.setPageIndex(1);
        envFilter.setPageSize(2);
        DeployQueryFilter envFilterBean1 = new DeployQueryFilter(envFilter);
        queryResult = deployDAO.getAllDeploys(envFilterBean1);
        assertEquals(queryResult.getTotal().longValue(), 3);
        assertTrue(queryResult.isTruncated());
        assertEquals(queryResult.getDeploys().size(), 2);

        envFilter.setPageIndex(2);
        envFilter.setPageSize(2);
        DeployQueryFilter envFilterBean2 = new DeployQueryFilter(envFilter);
        queryResult = deployDAO.getAllDeploys(envFilterBean2);
        assertEquals(queryResult.getTotal().longValue(), 3);
        assertFalse(queryResult.isTruncated());
        assertEquals(queryResult.getDeploys().size(), 1);
        assertTrue(EqualsBuilder.reflectionEquals(queryResult.getDeploys().get(0), deployBean1));

        envFilter.setPageIndex(3);
        envFilter.setPageSize(2);
        DeployQueryFilter envFilterBean3 = new DeployQueryFilter(envFilter);
        queryResult = deployDAO.getAllDeploys(envFilterBean3);
        assertEquals(queryResult.getTotal().longValue(), 3);
        assertFalse(queryResult.isTruncated());
        assertEquals(queryResult.getDeploys().size(), 0);

        DeployFilterBean envFilter2 = new DeployFilterBean();
        envFilter2.setEnvIds(Arrays.asList("env-1"));
        envFilter2.setBefore(now + 1000);
        envFilter2.setAfter(now);
        envFilter2.setPageIndex(1);
        envFilter2.setPageSize(10);
        DeployQueryFilter envFilterBean4 = new DeployQueryFilter(envFilter2);
        queryResult = deployDAO.getAllDeploys(envFilterBean4);
        assertEquals(queryResult.getTotal().longValue(), 2);
        assertFalse(queryResult.isTruncated());
        assertEquals(queryResult.getDeploys().size(), 2);

        DeployFilterBean filter = new DeployFilterBean();
        filter.setAfter(now - 1000);
        filter.setPageIndex(2);
        filter.setPageSize(2);
        DeployQueryFilter filterBean = new DeployQueryFilter(filter);
        DeployQueryResultBean resultBean = deployDAO.getAllDeploys(filterBean);
        assertEquals(resultBean.getDeploys().size(), 2);
        assertEquals(resultBean.getTotal().longValue(), 6);

        EnvironBean envBean1 = genDefaultEnvBean("env-1", "s-1", "prod", "d-3");
        EnvironBean envBean2 = genDefaultEnvBean("env-2", "s-2", "prod", "d-4");
        EnvironBean envBean3 = genDefaultEnvBean("env-3", "s-3", "prod", "d-5");
        environDAO.insert(envBean1);
        environDAO.insert(envBean2);
        environDAO.insert(envBean3);

        DeployFilterBean ongoingFilter = new DeployFilterBean();
        ongoingFilter.setDeployStates(Arrays.asList(DeployState.RUNNING, DeployState.FAILING));
        DeployQueryFilter ongoingFilterBean = new DeployQueryFilter(ongoingFilter);
        DeployQueryResultBean ongoingResultBean = deployDAO.getAllDeploys(ongoingFilterBean);
        assertEquals(ongoingResultBean.getDeploys().size(), 2);

        deployBean5.setAcc_status(AcceptanceStatus.ACCEPTED);
        deployDAO.update("d-5", deployBean5);
        List<DeployBean>
            beans =
            deployDAO.getAcceptedDeploys("env-3", new Interval(0, Long.MAX_VALUE), 100);
        assertEquals(beans.size(), 1);
        assertEquals(beans.get(0).getDeploy_id(), "d-5");

        buildDAO.delete("bbb-1");
        buildDAO.delete("bbb-2");
        buildDAO.delete("bbb-3");

        environDAO.delete("env-1");
        environDAO.delete("env-2");
        environDAO.delete("env-3");

        deployDAO.delete("d-1");
        deployDAO.delete("d-2");
        deployDAO.delete("d-3");
        deployDAO.delete("d-4");
        deployDAO.delete("d-5");
        assertEquals(deployDAO.getById("d-1"), null);
    }

    @Test
    public void testBuildDAO() throws Exception {
        long now = System.currentTimeMillis();
        BuildBean buildBean1 =
            genDefaultBuildInfoBean("b-1", "sss-1", "c-1", "r-1", now);
        BuildBean buildBean2 =
            genDefaultBuildInfoBean("b-2", "sss-1", "c-1", "r-1", now + 1000);
        BuildBean buildBean22 =
            genDefaultBuildInfoBean("b-22", "sss-2", "c-1", "r-1", now + 1000);
        BuildBean buildBean3 =
            genDefaultBuildInfoBean("b-3", "sss-1", "c-1", "r-1", now + 2000);

        buildDAO.insert(buildBean1);
        buildDAO.insert(buildBean2);
        buildDAO.insert(buildBean22);
        buildDAO.insert(buildBean3);

        assertTrue(EqualsBuilder.reflectionEquals(buildBean1, buildDAO.getById("b-1")));
        assertEquals(buildDAO.getByCommit7("c-1", "", 1, 10).size(), 4);
        assertEquals(buildDAO.getByCommit7("c-1", "sss-1", 1, 10).size(), 3);
        assertEquals(buildDAO.getByCommit7("c-1", "sss-2", 1, 10).size(), 1);
        assertEquals(buildDAO.getBuildNames("sss-", 1, 100).size(), 2);

        List<BuildBean>
            buildBeans =
            buildDAO.getAcceptedBuilds("sss-1", null, new Interval(now, Long.MAX_VALUE), 100);
        assertEquals(buildBeans.size(), 2);
        BuildBean bean1 = buildBeans.get(0);
        assertEquals(bean1.getBuild_id(), "b-3");

        BuildBean latestBuildBean = buildDAO.getLatest("sss-1", "branch-1");
        assertTrue(EqualsBuilder.reflectionEquals(buildBean3, latestBuildBean));

        List<BuildBean> buildBeans1 = buildDAO.getByNameDate("sss-1", null, now + 2000, now);
        assertEquals(buildBeans1.size(), 2);
        List<BuildBean> buildBeans2 = buildDAO.getByNameDate("sss-1", "branch-1", now + 2000, now);
        assertEquals(buildBeans2.size(), 2);
        assertTrue(EqualsBuilder.reflectionEquals(buildBeans2.get(0), buildBean3));
        List<BuildBean> buildBeans3 = buildDAO.getByName("sss-1", null, 3, 2);
        assertEquals(buildBeans3.size(), 0);

        List<BuildBean> buildBeans4 = buildDAO.getByName("sss-1", "branch-1", 1, 2);
        assertEquals(buildBeans4.size(), 2);

        List<BuildBean>
            allBuildBeans =
            buildDAO.getBuildsFromIds(Arrays.asList("b-1", "b-2", "b-22"));
        assertEquals(3, allBuildBeans.size());

        allBuildBeans = buildDAO.getBuildsFromIds(Arrays.asList("b-1", "b-2", "Not There"));
        assertEquals(2, allBuildBeans.size());

        allBuildBeans = buildDAO.getBuildsFromIds(Arrays.asList("Not There"));
        assertEquals(0, allBuildBeans.size());

        allBuildBeans = buildDAO.getBuildsFromIds(new ArrayList<>());
        assertEquals(0, allBuildBeans.size());

        buildDAO.delete("b-1");
        buildDAO.delete("b-2");
        buildDAO.delete("b-22");
        buildDAO.delete("b-3");
        assertEquals(buildDAO.getById("b-1"), null);
    }

    @Test
    public void testAgentUpdate() throws Exception {
        AgentBean
            agentBean1 =
            genDefaultAgentBean("h1", "id-1", "e-1", "d-1", DeployStage.PRE_DOWNLOAD);
        agentDAO.insertOrUpdate(agentBean1);

        AgentBean
            updateBean1 =
            genDefaultAgentBean("h1", "id-1", "e-1", "d-1", DeployStage.POST_DOWNLOAD);
        updateBean1.setFirst_deploy_time(10L);
        agentDAO.insertOrUpdate(updateBean1);

        List<AgentBean> agentBeans = agentDAO.getByHost("h1");
        assertEquals(agentBeans.size(), 1);
        assertEquals(agentBeans.get(0).getFirst_deploy_time(), new Long(10));

        updateBean1.setFirst_deploy_time(1000L);
        agentDAO.insertOrUpdate(updateBean1);

        agentBeans = agentDAO.getByHost("h1");
        assertEquals(agentBeans.size(), 1);
        assertEquals(agentBeans.get(0).getFirst_deploy_time(), new Long(10));
    }

    @Test
    public void testAgentUpdateMultiple() throws Exception {
        AgentBean
            agentBean1 =
            genDefaultAgentBean("h5", "id-5", "e-2", "d-1", DeployStage.PRE_DOWNLOAD);
        AgentBean
            agentBean2 =
            genDefaultAgentBean("h6", "id-6", "e-2", "d-1", DeployStage.PRE_DOWNLOAD);
        AgentBean
            agentBean3 =
            genDefaultAgentBean("h7", "id-7", "e-2", "d-1", DeployStage.PRE_DOWNLOAD);

        agentDAO.insertOrUpdate(agentBean1);
        agentDAO.insertOrUpdate(agentBean2);
        agentDAO.insertOrUpdate(agentBean3);

        List<String> hostIds = Arrays.asList("id-5", "id-6", "id-7");

        AgentBean updateBean = new AgentBean();
        updateBean.setState(AgentState.RESET);
        updateBean.setDeploy_id("d-2");

        agentDAO.updateMultiple(hostIds, "e-2", updateBean);

        List<AgentBean> beans = agentDAO.getAllByEnv("e-2");

        for (AgentBean bean : beans) {
            assertEquals(bean.getState(), AgentState.RESET);
            assertEquals(bean.getDeploy_id(), "d-2");
        }
    }

    @Test
    public void testFirstDeployCount() throws Exception {
        AgentBean
            agentBean1 =
            genDefaultAgentBean("h12", "id-123", "e-12", "d-12", DeployStage.POST_RESTART);
        agentBean1.setFirst_deploy(true);
        agentBean1.setStatus(AgentStatus.ABORTED_BY_SERVICE);

        AgentBean
            agentBean2 =
            genDefaultAgentBean("h22", "id-124", "e-12", "d-12", DeployStage.POST_RESTART);
        agentBean2.setFirst_deploy(true);

        agentDAO.insertOrUpdate(agentBean1);
        agentDAO.insertOrUpdate(agentBean2);

        long total = agentDAO.countFirstDeployingAgent("e-12");
        assertEquals(total, 2);
        long total_failed = agentDAO.countFailedFirstDeployingAgent("e-12");
        assertEquals(total_failed, 1);
    }

    @Test
    public void testAgentQueries() throws Exception {
        AgentBean agentBean1 = genDefaultAgentBean(
            "h-1", "id-1", "e-1", "d-1", DeployStage.POST_RESTART);
        AgentBean agentBean11 = genDefaultAgentBean(
            "h-1", "id-1", "e-2", "d-1", DeployStage.SERVING_BUILD);
        AgentBean agentBean2 = genDefaultAgentBean(
            "h-2", "id-3", "e-1", "d-2", DeployStage.RESTARTING);
        AgentBean agentBean3 = genDefaultAgentBean(
            "h-3", "id-4", "e-1", "d-1", DeployStage.SERVING_BUILD);
        agentBean3.setFirst_deploy_time(System.currentTimeMillis());

        agentDAO.insertOrUpdate(agentBean1);
        agentDAO.insertOrUpdate(agentBean11);
        agentDAO.insertOrUpdate(agentBean2);
        agentDAO.insertOrUpdate(agentBean3);

        AgentBean agentBean22 = new AgentBean();
        agentBean22.setLast_err_no(22);
        agentBean2.setLast_err_no(22);
        agentDAO.update("id-3", "e-1", agentBean22);

        List<AgentBean> hostStatusList = agentDAO.getByHost("h-1");
        assertEquals(hostStatusList.size(), 2);

        List<AgentBean> agentBeans = agentDAO.getAllByEnv("e-1");
        assertEquals(agentBeans.size(), 3);
        int nServing = 0;
        int nRestarting = 0;
        int nPostRestart = 0;
        int nPrestaging = 0;
        for (AgentBean agentBean : agentBeans) {
            if (agentBean.getDeploy_stage() == DeployStage.SERVING_BUILD) {
                nServing++;
            }
            if (agentBean.getDeploy_stage() == DeployStage.POST_RESTART) {
                nPostRestart++;
            }
            if (agentBean.getDeploy_stage() == DeployStage.RESTARTING) {
                nRestarting++;
            }
            if (agentBean.getDeploy_stage() == DeployStage.PRE_DOWNLOAD) {
                nPrestaging++;
            }
        }
        assertEquals(nServing, 1);
        assertEquals(nRestarting, 1);
        assertEquals(nPostRestart, 1);
        assertEquals(nPrestaging, 0);

        assertEquals(agentDAO.countDeployingAgent("e-1"), 2);
        assertEquals(agentDAO.countServingTotal("e-1"), 1);
        assertEquals(agentDAO.countSucceededAgent("e-1", "d-1"), 1);
        assertEquals(agentDAO.countStuckAgent("e-1", "d-1"), 0);
        assertEquals(agentDAO.getByEnvAndFirstDeployTime("e-1", 0).size(), 1);
        assertEquals(agentDAO.getByEnvAndFirstDeployTime("e-2", 0).size(), 0);

        agentDAO.deleteAllById("id-1");
        assertEquals(agentDAO.countAgentByEnv("e-1"), 2);
        agentDAO.delete("id-2", "e-1");
        agentDAO.delete("id-3", "e-2");
        agentDAO.delete("id-4", "e-1");
        assertEquals(agentDAO.countAgentByEnv("e-2"), 0);
    }

    @Test
    public void testEnvDAO() throws Exception {

        // Test insert
        EnvironBean envBean = genDefaultEnvBean("env-1", "s-1", "prod", "deploy-1");
        environDAO.insert(envBean);

        // Test getById
        EnvironBean envBean2 = environDAO.getById(envBean.getEnv_id());
        assertTrue(EqualsBuilder.reflectionEquals(envBean, envBean2));

        // Test getByStage
        EnvironBean envBean22 = environDAO.getByStage("s-1", "prod");
        assertTrue(EqualsBuilder.reflectionEquals(envBean, envBean22));

        // Test Watcher Column
        assertTrue(envBean2.getWatch_recipients().equals("watcher"));

        // Test update
        EnvironBean envBean3 = new EnvironBean();
        envBean3.setAdv_config_id("config_id_2");
        envBean2.setAdv_config_id("config_id_2");
        environDAO.update("env-1", envBean3);
        EnvironBean envBean4 = environDAO.getById("env-1");
        assertTrue(EqualsBuilder.reflectionEquals(envBean2, envBean4));

        // Test getByName return 2 envs after add another env
        envBean = genDefaultEnvBean("env-2", "s-1", "whatever", "deploy-1");
        environDAO.insert(envBean);
        List<EnvironBean> envBeans = environDAO.getByName("s-1");
        assertEquals(envBeans.size(), 2);

        assertEquals(groupDAO.getCapacityHosts("env-1").size(), 0);
        assertEquals(groupDAO.getCapacityGroups("env-1").size(), 0);
        assertEquals(environDAO.getOverrideHosts("env-1", "s-1", "prod").size(), 0);
        assertEquals(environDAO.countTotalCapacity("env-1", "s-1", "prod"), 0);

        // Add 2 hosts capacity to env-1, host1 & host2
        groupDAO.addHostCapacity("env-1", "host1");
        groupDAO.addHostCapacity("env-1", "host2");

        // Add 2 groups capacity to env-1, group1 & group2
        groupDAO.addGroupCapacity("env-1", "group1");
        groupDAO.addGroupCapacity("env-1", "group2");

        // env-1 : host1, host2, group1, group2, empty groups though
        assertEquals(groupDAO.getCapacityHosts("env-1").size(), 2);
        assertEquals(groupDAO.getCapacityGroups("env-1").size(), 2);
        assertEquals(environDAO.getOverrideHosts("env-1", "s-1", "prod").size(), 0);
        assertEquals(environDAO.countTotalCapacity("env-1", "s-1", "prod"), 0);
        assertEquals(environDAO.getMissingHosts("env-1").size(), 2);
        ArrayList<String> groupNames = new ArrayList<>();
        groupNames.add("group1");
        assertEquals(environDAO.getEnvsByGroups(groupNames).size(), 1);
        groupNames.add("group-lo");
        assertEquals(environDAO.getEnvsByGroups(groupNames).size(), 1);

        // Test remove Host capacity and remove host capacity
        groupDAO.removeHostCapacity("env-1", "host2");
        groupDAO.removeGroupCapacity("env-1", "group2");
        // now env-1 associate with only : host1, group1
        assertEquals(groupDAO.getCapacityHosts("env-1").size(), 1);
        assertEquals(groupDAO.getCapacityGroups("env-1").size(), 1);
        assertEquals(environDAO.getOverrideHosts("env-1", "s-1", "prod").size(), 0);
        assertEquals(environDAO.getMissingHosts("env-1").size(), 1);

        // Added 2 hosts to group1 and group2
        Set<String> groups = new HashSet<>(Arrays.asList("group1", "group2"));
        hostDAO
            .insertOrUpdate("host-1", "1.1.1.1", "id-123434", HostState.ACTIVE.toString(), groups);
        hostDAO
            .insertOrUpdate("host-2", "1.1.1.2", "id-123435", HostState.TERMINATING.toString(),
                groups);
        hostDAO
            .insertOrUpdate("host-2", "1.1.1.2", "id-123435", HostState.ACTIVE.toString(), groups);
        List<HostBean> hostBeans = hostDAO.getHostsByHostId("id-123435");
        assertEquals(hostBeans.get(0).getState(), HostState.TERMINATING);

        // Total capacity for env-1 should be 2, host-1(group1), host-2(group2) and one missing
      // host1
        assertEquals(environDAO.getOverrideHosts("env-1", "s-1", "prod").size(), 0);
        assertEquals(environDAO.countTotalCapacity("env-1", "s-1", "prod"), 2);
        assertEquals(environDAO.getMissingHosts("env-1").size(), 1);
        List<String> totalHosts = environDAO.getTotalCapacityHosts("env-1", "s-1", "prod");
        assertEquals(totalHosts.size(), 2);
        assertTrue(totalHosts.containsAll(Arrays.asList("host-1", "host-2")));

        // Now, override host-1 with env2
        groupDAO.addHostCapacity("env-2", "host-1");

        // override hosts should be 1, host-1
        // Total capacity for env1 should be 1, host-2
        assertEquals(environDAO.getOverrideHosts("env-1", "s-1", "prod").size(), 1);
        assertEquals(environDAO.countTotalCapacity("env-1", "s-1", "prod"), 1);
        List<String> totalHosts2 = environDAO.getTotalCapacityHosts("env-1", "s-1", "prod");
        assertEquals(totalHosts2.size(), 1);
        assertFalse(totalHosts2.contains("hosts-1"));

        // ineffective override (noise), add host-2 override on env-1
        // override hosts should be 1, host-1
        // Total capacity for env1 still is 1, host-2
        groupDAO.addHostCapacity("env-1", "host-2"); // noise
        assertEquals(environDAO.getOverrideHosts("env-1", "s-1", "prod").size(), 1);
        assertEquals(environDAO.countTotalCapacity("env-1", "s-1", "prod"), 1);
        List<String> totalHosts3 = environDAO.getTotalCapacityHosts("env-1", "s-1", "prod");
        assertEquals(totalHosts3.size(), 1);
        assertTrue(totalHosts3.containsAll(Arrays.asList("host-2")));

        // env-1 : group1
        // env-2 : host-1,
        List<EnvironBean> envs = environDAO.getEnvsByHost("host-1");
        assertEquals(envs.size(), 1);
        assertEquals(envs.get(0).getEnv_name(), "s-1");
        assertEquals(envs.get(0).getStage_name(), "whatever");

        envs = environDAO.getEnvsByGroups(groups);
        assertEquals(envs.size(), 1);

        environDAO.delete("env-1");
        environDAO.delete("env-2");
        envBean = environDAO.getById("env-1");
        assertEquals(envBean, null);
    }

    @Test
    public void testHostDAO() throws Exception {
        Set<String> groups = new HashSet<>(Arrays.asList("group1", "group2"));
        hostDAO.insertOrUpdate("host-1", "1.1.1.1", "id-1", HostState.ACTIVE.toString(), groups);
        groups = new HashSet<>(Arrays.asList("group1"));
        hostDAO.insertOrUpdate("host-2", "1.1.1.2", "id-2", HostState.ACTIVE.toString(), groups);
        hostDAO.insertOrUpdate("host-3", "1.1.1.3", "id-3", HostState.ACTIVE.toString(), groups);
        /*
        host-1 : group1, group2
        host-2 : group1
        host-3 : group1
         */
        assertEquals(hostDAO.getHostNamesByGroup("group1").size(), 3);
        hostDAO.removeHostFromGroup("id-3", "group1");
        /*
        host-1 : group1, group2
        host-2 : group1
         */
        assertEquals(hostDAO.getHostNamesByGroup("group1").size(), 2);
        assertEquals(hostDAO.getGroupSize("group1").intValue(), 2);
        assertEquals(hostDAO.getGroupNamesByHost("host-1").size(), 2);
        // test on non-existing group size
        assertEquals(hostDAO.getGroupSize("group10").intValue(), 0);

        hostDAO.deleteById("id-1");
        /*
        host-2 : group1
         */
        assertEquals(hostDAO.getHostNamesByGroup("group1").size(), 1);
        assertEquals(hostDAO.getHostNamesByGroup("group2").size(), 0);

        hostDAO.deleteById("id-2");

        // test host transactional delete
        hostDAO.insertOrUpdate("host-1", "1.1.1.1", "id-1", HostState.ACTIVE.toString(), groups);
        AgentBean agentBean = genDefaultAgentBean(
            "host-1", "id-1", "e-1", "d-1", DeployStage.SERVING_BUILD);
        agentDAO.insertOrUpdate(agentBean);
        AgentErrorBean agentErrorBean = new AgentErrorBean();
        agentErrorBean.setHost_name("host-1");
        agentErrorBean.setHost_id("id-1");
        agentErrorBean.setEnv_id("env-test-host");
        agentErrorBean.setError_msg("Yeah, it is wrong!");
        agentErrorDAO.insert(agentErrorBean);

        hostDAO.deleteAllById("id-1");
        List<HostBean> hostBeans1 = hostDAO.getHosts("host-1");
        assertTrue(hostBeans1.isEmpty());
        List<AgentBean> agentBeans = agentDAO.getByHost("host-1");
        assertTrue(agentBeans.isEmpty());
        AgentErrorBean agentErrorBeans = agentErrorDAO.get("host-1", "env-test-host");
        assertNull(agentErrorBeans);

        // test hosts_and_envs
        groupDAO.addHostCapacity("e-3", "host-3");
        assertEquals(environDAO.getMissingHosts("e-3").size(), 1);

        Set<String> groups2 = new HashSet<>(Arrays.asList("new_group"));
        hostDAO
            .insertOrUpdate("host-3", "3.3.3.3", "id-3", HostState.TERMINATING.toString(), groups2);
        assertEquals(environDAO.getMissingHosts("e-3").size(), 0);

        Collection<HostBean> hostBean3 = hostDAO.getByEnvIdAndHostName("e-3", "host-3");
        assertEquals(hostBean3.iterator().next().getHost_name(), "host-3");

        groupDAO.addGroupCapacity("e-3", "new_group");
        hostBean3 = hostDAO.getByEnvIdAndHostName("e-3", "host-3");
        assertEquals(hostBean3.iterator().next().getHost_name(), "host-3");
        groupDAO.removeGroupCapacity("e-3", "new_group");

        // test host insertOrUpdate
        HostBean hostBean1 = new HostBean();
        hostBean1.setHost_name("i-9");
        hostBean1.setHost_id("i-9");
        hostBean1.setGroup_name("test_dup");
        Long currentTime = System.currentTimeMillis();
        hostBean1.setCreate_date(currentTime);
        hostBean1.setLast_update(currentTime);
        hostBean1.setState(HostState.PROVISIONED);
        hostDAO.insert(hostBean1);
        hostDAO.insert(hostBean1);
        List<HostBean> hostBeans3 = hostDAO.getHosts("i-9");
        assertEquals(hostBeans3.size(), 1);
        assertEquals(hostBeans3.get(0).getHost_name(), "i-9");

        HashSet<String> groups9 = new HashSet<>(Arrays.asList("test_dup"));
        hostDAO
            .insertOrUpdate("h-9", "9.9.9.9", "i-9", HostState.PENDING_TERMINATE.toString(),
                groups9);
        List<HostBean> hostBeans4 = hostDAO.getHosts("h-9");
        assertEquals(hostBeans4.size(), 1);
        assertEquals(hostBeans4.get(0).getHost_name(), "h-9");
        assertEquals(hostBeans4.get(0).getHost_id(), "i-9");

        List<HostBean> hostBeans5 = hostDAO.getTerminatingHosts();
        assertEquals(hostBeans5.size(), 2);

        // Test can retire hosts
        HostBean hostBean6 = new HostBean();
        hostBean6.setHost_name("i-11");
        hostBean6.setHost_id("i-11");
        hostBean6.setGroup_name("retire-group");
        hostBean6.setCreate_date(currentTime);
        hostBean6.setLast_update(currentTime);
        hostBean6.setState(HostState.ACTIVE);
        hostBean6.setCan_retire(1);
        hostDAO.insert(hostBean6);

        hostBean6.setHost_name("i-12");
        hostBean6.setHost_id("i-12");
        hostDAO.insert(hostBean6);

        HostBean hostBean7 = new HostBean();
        hostBean7.setHost_name("i-13");
        hostBean7.setHost_id("i-13");
        hostBean7.setGroup_name("retire-group");
        hostBean7.setCreate_date(currentTime);
        hostBean7.setLast_update(currentTime);
        hostBean7.setState(HostState.TERMINATING);
        hostBean7.setCan_retire(1);
        hostDAO.insert(hostBean7);

        Collection<String>
            retiredHostBeanIds =
            hostDAO.getToBeRetiredHostIdsByGroup("retire-group");
        assertEquals(retiredHostBeanIds.size(), 2);

        AgentBean
            agentBean1 =
            genDefaultAgentBean("i-11", "i-11", "e-1", "d-1", DeployStage.RESTARTING);
        agentBean1.setStatus(AgentStatus.AGENT_FAILED);
        agentDAO.insertOrUpdate(agentBean1);
        Collection<String>
            retiredAndFailedHostIds =
            hostDAO.getToBeRetiredAndFailedHostIdsByGroup("retire-group");
        assertEquals(retiredAndFailedHostIds.size(), 1);

        Collection<String> failedHostIds = hostDAO.getFailedHostIdsByGroup("retire-group");
        assertEquals(failedHostIds.size(), 1);
    }

    @Test
    public void testDataDAO() throws Exception {
        DataBean dataBean = genDefaultDataBean("foo1", "name1=value1,name2=value2");
        dataDAO.insert(dataBean);
        DataBean dataBean2 = dataDAO.getById("foo1");
        assertTrue(EqualsBuilder.reflectionEquals(dataBean, dataBean2));
        dataDAO.delete("foo1");
        DataBean dataBean3 = dataDAO.getById("foo1");
        assertEquals(dataBean3, null);
    }

    @Test
    public void testRatingsDAO() throws Exception {
        RatingBean ratingBean = genDefaultRatingsBean("1", "foo", System.currentTimeMillis());
        ratingDAO.insert(ratingBean);
        List<RatingBean> ratingBeans = ratingDAO.getRatingsByAuthor("foo");
        assertTrue(ratingBeans.size() == 1);
        assertTrue(EqualsBuilder.reflectionEquals(ratingBean, ratingBeans.get(0)));
        ratingDAO.delete("1");
        List<RatingBean> ratingBeans2 = ratingDAO.getRatingsByAuthor("foo");
        assertTrue(ratingBeans2.size() == 0);
    }

    @Test
    public void testAgentErrorDAO() throws Exception {
        AgentErrorBean agentErrorBean = new AgentErrorBean();
        agentErrorBean.setHost_name("host-1");
        agentErrorBean.setHost_id("id-1");
        agentErrorBean.setEnv_id("env-1");
        agentErrorBean.setError_msg("Yeah, it is wrong!");
        agentErrorDAO.insert(agentErrorBean);

        AgentErrorBean agentErrorBean2 = new AgentErrorBean();
        agentErrorBean2.setHost_name("host-1");
        agentErrorBean2.setHost_id("id-1");
        agentErrorBean2.setEnv_id("env-2");
        agentErrorBean2.setError_msg("never mind!");
        agentErrorDAO.insert(agentErrorBean2);

        agentErrorBean.setError_msg("what, again?");
        agentErrorDAO.update("host-1", "env-1", agentErrorBean);
        AgentErrorBean agentErrorBean3 = agentErrorDAO.get("host-1", "env-1");
        assertTrue(EqualsBuilder.reflectionEquals(agentErrorBean, agentErrorBean3));
    }

    @Test
    public void testPromoteDAO() throws Exception {
        long now = System.currentTimeMillis();
        PromoteBean bean1 = new PromoteBean();
        bean1.setEnv_id("env1");
        bean1.setType(PromoteType.MANUAL);
        bean1.setQueue_size(Constants.DEFAULT_PROMOTE_QUEUE_SIZE);
        bean1.setDisable_policy(Constants.DEFAULT_PROMOTE_DISABLE_POLICY);
        bean1.setFail_policy(Constants.DEFAULT_PROMOTE_FAIL_POLICY);
        bean1.setLast_operator("system");
        bean1.setLast_update(now);
        bean1.setDelay(0);
        promoteDAO.insert(bean1);
        PromoteBean bean11 = promoteDAO.getById("env1");
        assertTrue(EqualsBuilder.reflectionEquals(bean1, bean11));

        PromoteBean bean2 = new PromoteBean();
        bean2.setEnv_id("env1");
        bean2.setType(PromoteType.AUTO);
        bean2.setPred_stage("xxx");
        bean2.setQueue_size(Constants.DEFAULT_PROMOTE_QUEUE_SIZE);
        bean2.setDisable_policy(Constants.DEFAULT_PROMOTE_DISABLE_POLICY);
        bean2.setFail_policy(Constants.DEFAULT_PROMOTE_FAIL_POLICY);
        bean2.setLast_operator("system");
        bean2.setLast_update(now);
        bean2.setDelay(0);
        promoteDAO.update("env1", bean2);

        PromoteBean bean22 = promoteDAO.getById("env1");
        assertTrue(EqualsBuilder.reflectionEquals(bean2, bean22));

        List<String> ids = promoteDAO.getAutoPromoteEnvIds();
        assertEquals(ids.size(), 1);

        promoteDAO.delete("env1");
        assertEquals(promoteDAO.getById("env1"), null);
    }

    @Test
    public void testGroupDAO() throws Exception {
        groupDAO.addGroupCapacity("env-id3", "group3");
        groupDAO.addGroupCapacity("env-id4", "group4");
        groupDAO.addGroupCapacity("env-id5", "group3");

        List<String> envids = groupDAO.getEnvsByGroupName("group3");
        assertEquals(envids.size(), 2);
        HashSet<String> target_ids = new HashSet<>();
        target_ids.addAll(envids);
        assertTrue(target_ids.contains("env-id3"));
        assertTrue(target_ids.contains("env-id5"));

        List<String> groups = groupDAO.getAllEnvGroups();
        assertEquals(groups.size(), 2);
    }

    @Test
    public void testUserRolesDAO() throws Exception {
        UserRolesBean bean = new UserRolesBean();
        bean.setUser_name("test");
        bean.setResource_id("envTest");
        bean.setResource_type(Resource.Type.ENV);
        bean.setRole(Role.ADMIN);
        userRolesDAO.insert(bean);
        UserRolesBean
            bean2 =
            userRolesDAO.getByNameAndResource("test", "envTest", Resource.Type.ENV);
        assertEquals(bean2.getRole(), Role.ADMIN);
    }

    @Test
    public void testGroupRolesDAO() throws Exception {
        GroupRolesBean bean = new GroupRolesBean();
        bean.setGroup_name("group");
        bean.setResource_id("123");
        bean.setResource_type(Resource.Type.ENV);
        bean.setRole(Role.ADMIN);
        groupRolesDAO.insert(bean);
        GroupRolesBean
            bean2 =
            groupRolesDAO.getByNameAndResource("group", "123", Resource.Type.ENV);
        assertEquals(bean2.getRole(), Role.ADMIN);
    }

    @Test
    public void testTokenRolesDAO() throws Exception {
        TokenRolesBean bean = new TokenRolesBean();
        bean.setScript_name("test");
        bean.setToken("token");
        bean.setResource_id("envTest");
        bean.setResource_type(Resource.Type.ENV);
        bean.setRole(Role.ADMIN);
        bean.setExpire_date(System.currentTimeMillis());
        tokenRolesDAO.insert(bean);
        TokenRolesBean
            bean2 =
            tokenRolesDAO.getByNameAndResource("test", "envTest", Resource.Type.ENV);
        assertEquals(bean2.getRole(), Role.ADMIN);
    }


    @Test
    public void testConfigHistoryDAO() throws Exception {
        ConfigHistoryBean bean = new ConfigHistoryBean();
        bean.setChange_id("id-1");
        bean.setConfig_id("group-1");
        bean.setConfig_change("Test config change");
        bean.setCreation_time(System.currentTimeMillis());
        bean.setOperator("lo");
        bean.setType("Host Terminate");
        configHistoryDAO.insert(bean);

        ConfigHistoryBean bean1 = configHistoryDAO.getByChangeId("id-1");
        assertEquals(bean1.getType(), "Host Terminate");

        bean.setChange_id("id-2");
        bean.setConfig_id("group-1");
        bean.setConfig_change("Test config change 2");
        bean.setCreation_time(System.currentTimeMillis());
        bean.setOperator("lo");
        bean.setType("Host Launch");
        configHistoryDAO.insert(bean);

        List<ConfigHistoryBean> beanList = configHistoryDAO.getByConfigId("group-1", 1, 10);
        assertEquals(beanList.size(), 2);
    }

    @Test
    public void testTagDAO() throws Exception {
        TagBean tag = genTagBean(TagValue.BAD_BUILD, "TestEnv", "BUILD",
            genDefaultBuildInfoBean("b-3", "sss-1", "c-1", "r-1", System.currentTimeMillis()));
        tagDAO.insert(tag);
        TagBean tag2 = tagDAO.getById(tag.getId());
        assertNotNull(tag2);
        assertEquals(tag.getTarget_id(), tag2.getTarget_id());
        BuildBean embededBean = tag2.deserializeTagMetaInfo(BuildBean.class);
        assertEquals("b-3", embededBean.getBuild_id());

        List<TagBean> targetList = tagDAO.getByTargetId(tag.getTarget_id());
        assertEquals(1, targetList.size());
        targetList = tagDAO.getByTargetIdAndType(tag.getTarget_id(), TagTargetType.BUILD);
        assertEquals(1, targetList.size());

        tagDAO.delete(tag.getId());
        tag2 = tagDAO.getById(tag.getId());
        assertNull(tag2);
        targetList = tagDAO.getByTargetId(tag.getTarget_id());
        assertEquals(0, targetList.size());
        targetList = tagDAO.getByTargetIdAndType(tag.getTarget_id(), TagTargetType.BUILD);
        assertEquals(0, targetList.size());

        tagDAO
            .insert(genTagBean(TagValue.BAD_BUILD, "env1", "BUILD", new HashMap<String, String>()));
        tagDAO
            .insert(genTagBean(TagValue.BAD_BUILD, "env1", "BUILD", new HashMap<String, String>()));
        tagDAO
            .insert(genTagBean(TagValue.BAD_BUILD, "env1", "BUILD", new HashMap<String, String>()));
        tagDAO
            .insert(genTagBean(TagValue.BAD_BUILD, "env1", "BUILD", new HashMap<String, String>()));

        assertEquals(4, tagDAO.getByValue(TagValue.BAD_BUILD).size());
        assertEquals(0, tagDAO.getByValue(TagValue.GOOD_BUILD).size());
    }


    @Test
    public void testScheduleDAO() throws Exception {
        Long time = System.currentTimeMillis();
        String id = CommonUtils.getBase64UUID();
        ScheduleBean scheduleBean = new ScheduleBean();
        scheduleBean.setId(id);
        scheduleBean.setTotal_sessions(3);
        scheduleBean.setCooldown_times("40,50,60");
        scheduleBean.setCurrent_session(2);
        scheduleBean.setHost_numbers("50,60,500");
        scheduleBean.setState(ScheduleState.COOLING_DOWN);
        scheduleBean.setState_start_time(time);
        scheduleDAO.insert(scheduleBean);
        ScheduleBean bean = scheduleDAO.getById(id);

        assertEquals(bean.getTotal_sessions(), (Integer) 3);
        assertEquals(bean.getCooldown_times(), "40,50,60");
        assertEquals(bean.getCurrent_session(), (Integer) 2);
        assertEquals(bean.getHost_numbers(), "50,60,500");
        assertEquals(bean.getState(), ScheduleState.COOLING_DOWN);
        assertEquals(bean.getState_start_time(), time);

        ScheduleBean updateBean = new ScheduleBean();
        updateBean.setTotal_sessions(3);
        updateBean.setCurrent_session(1);
        updateBean.setState(ScheduleState.RUNNING);
        scheduleDAO.update(updateBean, id);
        ScheduleBean updatedBean = scheduleDAO.getById(id);

        assertEquals(updatedBean.getCurrent_session(), (Integer) 1);
        assertEquals(updatedBean.getState(), ScheduleState.RUNNING);
        assertEquals(updatedBean.getHost_numbers(), "50,60,500");

    }


    private EnvironBean genDefaultEnvBean(String envId, String envName, String envStage,
                                          String deployId) {
        EnvironBean envBean = new EnvironBean();
        envBean.setEnv_id(envId);
        envBean.setEnv_name(envName);
        envBean.setStage_name(envStage);
        envBean.setEnv_state(EnvState.NORMAL);
        envBean.setMax_parallel(1);
        envBean.setPriority(DeployPriority.NORMAL);
        envBean.setStuck_th(100);

        //To keep the precision, the default success_th value should be 10000 in DB.
        envBean.setSuccess_th(10000);
        envBean.setDescription("foo");
        envBean.setDeploy_id(deployId);
        envBean.setAdv_config_id("config_id_1");
        envBean.setSc_config_id("envvar_id_1");
        envBean.setLast_operator("bar");
        envBean.setLast_update(System.currentTimeMillis());
        envBean.setAccept_type(AcceptanceType.AUTO);
        envBean.setNotify_authors(false);
        envBean.setWatch_recipients("watcher");
        envBean.setMax_deploy_num(5100);
        envBean.setMax_deploy_day(366);
        envBean.setIs_docker(false);
        envBean.setMax_parallel_pct(0);
        envBean.setState(EnvironState.NORMAL);
        envBean.setMax_parallel_rp(1);
        envBean.setOverride_policy(OverridePolicy.OVERRIDE);
        envBean.setAllow_private_build(false);
        envBean.setEnsure_trusted_build(false);
        return envBean;
    }

    private DeployBean genDefaultDeployBean(String id, String envId, String buildId,
                                            long startDate, DeployState state) {
        DeployBean deployBean = new DeployBean();
        deployBean.setDeploy_id(id);
        deployBean.setEnv_id(envId);
        deployBean.setBuild_id(buildId);
        deployBean.setDeploy_type(DeployType.REGULAR);
        deployBean.setState(state);
        deployBean.setStart_date(startDate);
        deployBean.setOperator("foo");
        deployBean.setLast_update(startDate);
        deployBean.setDescription("foo");
        deployBean.setSuc_total(0);
        deployBean.setFail_total(0);
        deployBean.setTotal(0);
        deployBean.setAcc_status(Constants.DEFAULT_ACCEPTANCE_STATUS);
        return deployBean;
    }

    private RatingBean genDefaultRatingsBean(String id, String author, long timestamp) {
        RatingBean ratingBean = new RatingBean();
        ratingBean.setRating_id(id);
        ratingBean.setRating("5");
        ratingBean.setTimestamp(timestamp);
        ratingBean.setFeedback("good feedback");
        ratingBean.setAuthor(author);
        return ratingBean;
    }

    private BuildBean genDefaultBuildInfoBean(String id, String buildName,
                                              String commitId, String repoUrl, long buildDate) {
        BuildBean buildBean = new BuildBean();
        buildBean.setBuild_id(id);
        buildBean.setBuild_name(buildName);
        buildBean.setScm_repo("repo-1");
        buildBean.setScm_branch("branch-1");
        buildBean.setScm_commit(commitId);
        buildBean.setScm_commit_7(commitId);
        buildBean.setCommit_date(buildDate);
        buildBean.setArtifact_url(repoUrl);
        buildBean.setPublish_info("jenkins12345");
        buildBean.setPublish_date(buildDate);
        return buildBean;
    }

    private AgentBean genDefaultAgentBean(String hostName, String hostId, String envId,
                                          String deployId, DeployStage deployStage) {
        AgentBean agentBean = new AgentBean();
        agentBean.setHost_name(hostName);
        agentBean.setHost_id(hostId);
        agentBean.setEnv_id(envId);
        agentBean.setDeploy_id(deployId);
        agentBean.setDeploy_stage(deployStage);
        agentBean.setStart_date(System.currentTimeMillis());
        agentBean.setStatus(AgentStatus.SUCCEEDED);
        agentBean.setState(AgentState.NORMAL);
        agentBean.setLast_update(System.currentTimeMillis());
        agentBean.setLast_operator("me");
        agentBean.setFail_count(0);
        agentBean.setFirst_deploy(false);
        agentBean.setStage_start_date(System.currentTimeMillis());
        return agentBean;
    }

    private DataBean genDefaultDataBean(String id, String data) {
        DataBean dataBean = new DataBean();
        dataBean.setData_id(id);
        dataBean.setData_kind("script");
        dataBean.setOperator("foo");
        dataBean.setTimestamp(System.currentTimeMillis());
        dataBean.setData(data);
        return dataBean;
    }

    private TagBean genTagBean(TagValue val, String target_id, String target_type, Object meta_info)
        throws Exception {
        TagBean bean = new TagBean();
        bean.setId(CommonUtils.getBase64UUID());
        bean.setCreated_date(System.currentTimeMillis());
        bean.setOperator("johndoe");
        bean.setValue(TagValue.BAD_BUILD);
        bean.setTarget_id(target_id);
        bean.setTarget_type(TagTargetType.BUILD);
        bean.serializeTagMetaInfo(meta_info);
        return bean;
    }
}
