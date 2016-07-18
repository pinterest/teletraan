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

import com.ibatis.common.jdbc.ScriptRunner;
import com.mysql.management.driverlaunched.ServerLauncherSocketFactory;
import com.pinterest.arcee.bean.*;
import com.pinterest.arcee.common.AutoScalingConstants;
import com.pinterest.arcee.dao.*;
import com.pinterest.arcee.db.*;
import com.pinterest.clusterservice.bean.*;
import com.pinterest.clusterservice.dao.*;
import com.pinterest.clusterservice.db.*;
import com.pinterest.deployservice.bean.*;
import com.pinterest.deployservice.common.CommonUtils;
import com.pinterest.deployservice.common.Constants;
import com.pinterest.deployservice.dao.*;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.util.*;

import static org.junit.Assert.*;


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
    private static GroupInfoDAO groupInfoDAO;
    private static RatingDAO ratingDAO;
    private static AlarmDAO alarmDAO;
    private static UserRolesDAO userRolesDAO;
    private static TokenRolesDAO tokenRolesDAO;
    private static GroupRolesDAO groupRolesDAO;
    private static ImageDAO imageDAO;
    private static HealthCheckDAO healthCheckDAO;
    private static HealthCheckErrorDAO healthCheckErrorDAO;
    private static ConfigHistoryDAO configHistoryDAO;
    private static NewInstanceReportDAO newInstanceReportDAO;
    private static AsgLifecycleEventDAO asgLifecycleEventDAO;
    private static ManagingGroupDAO managingGroupDAO;
    private static ClusterDAO clusterDAO;
    private static BaseImageDAO baseImageDAO;
    private static HostTypeDAO hostTypeDAO;
    private static SecurityZoneDAO securityZoneDAO;
    private static PlacementDAO placementDAO;
    private static ClusterUpgradeEventDAO clusterUpgradeEventDAO;
    private static SpotAutoScalingDAO spotAutoScalingDAO;
    private static PasConfigDAO pasConfigDAO;
    private static TagDAO tagDAO;

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
        groupInfoDAO = new DBGroupInfoDAOImpl(DATASOURCE);
        alarmDAO = new DBAlarmDAOImpl(DATASOURCE);
        userRolesDAO = new DBUserRolesDAOImpl(DATASOURCE);
        groupRolesDAO = new DBGroupRolesDAOImpl(DATASOURCE);
        tokenRolesDAO = new DBTokenRolesDAOImpl(DATASOURCE);
        imageDAO = new DBImageDAOImpl(DATASOURCE);
        healthCheckDAO = new DBHealthCheckDAOImpl(DATASOURCE);
        healthCheckErrorDAO = new DBHealthCheckErrorDAOImpl(DATASOURCE);
        configHistoryDAO = new DBConfigHistoryDAOImpl(DATASOURCE);
        newInstanceReportDAO = new DBNewInstanceReportDAOImpl(DATASOURCE);
        asgLifecycleEventDAO = new DBAsgLifecycleEventDAOImpl(DATASOURCE);
        managingGroupDAO = new DBManaginGroupDAOImpl(DATASOURCE);
        clusterDAO = new DBClusterDAOImpl(DATASOURCE);
        baseImageDAO = new DBBaseImageDAOImpl(DATASOURCE);
        hostTypeDAO = new DBHostTypeDAOImpl(DATASOURCE);
        securityZoneDAO = new DBSecurityZoneDAOImpl(DATASOURCE);
        placementDAO = new DBPlacementDAOImpl(DATASOURCE);
        clusterUpgradeEventDAO = new DBClusterUpgradeEventDAOImpl(DATASOURCE);
        spotAutoScalingDAO = new DBSpotAutoScalingDAOImpl(DATASOURCE);
        tagDAO = new DBTagDAOImpl(DATASOURCE);
        pasConfigDAO = new DBPasConfigDAOImpl(DATASOURCE);
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
        List<DeployBean> beans = deployDAO.getAcceptedDeploys("env-3", 0, 100);
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
        assertEquals(buildDAO.getByCommit7("c-1", 1, 10).size(), 4);
        assertEquals(buildDAO.getBuildNames("sss-", 1, 100).size(), 2);

        List<BuildBean> buildBeans = buildDAO.getAcceptedBuilds("sss-1", null, now, 100);
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

        List<BuildBean> allBuildBeans = buildDAO.getBuildsFromIds(Arrays.asList("b-1","b-2","b-22"));
        assertEquals(3, allBuildBeans.size());

        allBuildBeans = buildDAO.getBuildsFromIds(Arrays.asList("b-1","b-2","Not There"));
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
        AgentBean agentBean1 = genDefaultAgentBean("h1", "id-1", "e-1", "d-1", DeployStage.PRE_DOWNLOAD);
        agentDAO.insertOrUpdate(agentBean1);


        AgentBean updateBean1 = genDefaultAgentBean("h1", "id-1", "e-1", "d-1", DeployStage.POST_DOWNLOAD);
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
    public void testFirstDeployCount() throws Exception {
        AgentBean agentBean1 = genDefaultAgentBean("h12", "id-123", "e-12", "d-12", DeployStage.POST_RESTART);
        agentBean1.setFirst_deploy(true);
        agentBean1.setStatus(AgentStatus.ABORTED_BY_SERVICE);

        AgentBean agentBean2 = genDefaultAgentBean("h22", "id-124", "e-12", "d-12", DeployStage.POST_RESTART);
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
        hostDAO.insertOrUpdate("host-1", "1.1.1.1", "id-123434", HostState.ACTIVE.toString(), groups);
        hostDAO.insertOrUpdate("host-2", "1.1.1.2", "id-123435", HostState.TERMINATING.toString(), groups);
        hostDAO.insertOrUpdate("host-2", "1.1.1.2", "id-123435", HostState.ACTIVE.toString(), groups);
        List<HostBean> hostBeans = hostDAO.getHostsByHostId("id-123435");
        assertEquals(hostBeans.get(0).getState(), HostState.TERMINATING);

        // Total capacity for env-1 should be 2, host-1(group1), host-2(group2) and one missing host1
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
        hostDAO.insertOrUpdate("host-3", "3.3.3.3", "id-3", HostState.TERMINATING.toString(), groups2);
        assertEquals(environDAO.getMissingHosts("e-3").size(), 0);
        List<HostBean> hostBeans2 = hostDAO.getStaleEnvHosts(System.currentTimeMillis() + 100);
        assertEquals(hostBeans2.size(), 1);
        HostBean hostBean2 = hostBeans2.get(0);
        assertEquals(hostBean2.getGroup_name(), "new_group");
        assertEquals(hostBean2.getHost_name(), "host-3");
        assertEquals(hostBean2.getIp(), "3.3.3.3");

        Collection<HostBean> hostBean3 = hostDAO.getByEnvIdAndHostName("e-3", "host-3");
        assertEquals(hostBean3.iterator().next().getHost_name(), "host-3");

        groupDAO.addGroupCapacity("e-3", "new_group");
        hostBean3 = hostDAO.getByEnvIdAndHostName("e-3", "host-3");
        assertEquals(hostBean3.iterator().next().getHost_name(), "host-3");
        groupDAO.removeGroupCapacity("e-3", "new_group");

        // test host insert
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
        hostDAO.insertOrUpdate("h-9", "9.9.9.9", "i-9", HostState.PENDING_TERMINATE.toString(), groups9);
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
        hostBean6.setCan_retire(true);
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
        hostBean7.setCan_retire(true);
        hostDAO.insert(hostBean7);

        Collection<String> retiredHostBeanIds = hostDAO.getRetiredHostIdsByGroup("retire-group");
        assertEquals(retiredHostBeanIds.size(), 2);

        AgentBean agentBean1 = genDefaultAgentBean("i-11", "i-11", "e-1", "d-1", DeployStage.RESTARTING);
        agentBean1.setStatus(AgentStatus.AGENT_FAILED);
        agentDAO.insertOrUpdate(agentBean1);
        Collection<String> retiredAndFailedHostIds = hostDAO.getRetiredAndFailedHostIdsByGroup("retire-group");
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
    public void testGroupInfoDAO() throws Exception {
        // Insert test
        GroupBean groupBean = new GroupBean();
        groupBean.setGroup_name("deploy-test");
        groupBean.setLast_update(System.currentTimeMillis());
        groupBean.setChatroom("#lo");
        groupBean.setEmail_recipients("lo@pinterest.com");
        groupBean.setPager_recipients("lo@pinterest.com");
        groupBean.setHealthcheck_state(true);
        groupBean.setHealthcheck_period(10L);
        groupInfoDAO.insertOrUpdateGroupInfo("deploy-test", groupBean);
        GroupBean gBean = groupInfoDAO.getGroupInfo("deploy-test");
        assertEquals(gBean.getGroup_name(), "deploy-test");
        assertEquals(gBean.getChatroom(), "#lo");
        assertEquals(gBean.getPager_recipients(), "lo@pinterest.com");
        assertEquals(gBean.getLaunch_latency_th(), Integer.valueOf(AutoScalingConstants.DEFAULT_LAUNCH_LATENCY_THRESHOLD));
        assertNull(gBean.getWatch_recipients());
        assertTrue(gBean.getHealthcheck_state());
        List<String> groups = groupDAO.getExistingGroups(1, 10);
        assertEquals(groups.size(), 1);

        List<String> enabledHealthCheckGroup = groupInfoDAO.getEnabledHealthCheckGroupNames();
        assertEquals(enabledHealthCheckGroup.size(), 1);

        // Update test
        groupBean.setWatch_recipients("lo");
        groupBean.setLaunch_latency_th(300);
        groupBean.setLifecycle_state(true);
        groupInfoDAO.updateGroupInfo("deploy-test", groupBean);
        gBean = groupInfoDAO.getGroupInfo("deploy-test");
        assertEquals(gBean.getWatch_recipients(), "lo");
        assertEquals(gBean.getLaunch_latency_th(), Integer.valueOf(300));
        assertTrue(gBean.getLifecycle_state());
    }

    @Test
    public void testUserRolesDAO() throws Exception {
        UserRolesBean bean = new UserRolesBean();
        bean.setUser_name("test");
        bean.setResource_id("envTest");
        bean.setResource_type(Resource.Type.ENV);
        bean.setRole(Role.ADMIN);
        userRolesDAO.insert(bean);
        UserRolesBean bean2 = userRolesDAO.getByNameAndResource("test", "envTest", Resource.Type.ENV);
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
        GroupRolesBean bean2 = groupRolesDAO.getByNameAndResource("group", "123", Resource.Type.ENV);
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
        TokenRolesBean bean2 = tokenRolesDAO.getByNameAndResource("test", "envTest", Resource.Type.ENV);
        assertEquals(bean2.getRole(), Role.ADMIN);
    }

    @Test
    public void testImageDAO() throws Exception {
        ImageBean bean1 = new ImageBean();
        bean1.setId("ami-1");
        bean1.setApp_name("app-1");
        bean1.setPublish_date(1L);
        bean1.setQualified(true);

        ImageBean bean2 = new ImageBean();
        bean2.setId("ami-2");
        bean2.setApp_name("app-2");
        bean2.setPublish_date(2L);

        ImageBean bean3 = new ImageBean();
        bean3.setId("ami-3");
        bean3.setApp_name("app-2");
        bean3.setPublish_date(3L);

        imageDAO.insertOrUpdate(bean1);
        imageDAO.insertOrUpdate(bean2);
        imageDAO.insertOrUpdate(bean3);

        ImageBean resultBean1 = imageDAO.getById("ami-1");
        assertEquals(bean1.getApp_name(), resultBean1.getApp_name());
        assertEquals(bean1.getId(), resultBean1.getId());
        assertEquals(bean1.getPublish_date(), resultBean1.getPublish_date());
        assertEquals(bean1.getQualified(), resultBean1.getQualified());

        List<ImageBean> imageBeans = imageDAO.getImages("app-2", 1, 10);
        assertEquals(imageBeans.size(), 2);
        assertEquals(imageBeans.get(0).getId(), "ami-3");

        List<ImageBean> imageBeans1 = imageDAO.getImages("app-3", 1, 10);
        assertTrue(imageBeans1.isEmpty());

        imageDAO.delete("ami-2");
        ImageBean resultBean2 = imageDAO.getById("ami-2");
        assertNull(resultBean2);

        List<String> appNames = imageDAO.getAppNames();
        assertEquals(appNames.size(), 2);
        assertEquals(appNames, Arrays.asList("app-1", "app-2"));
    }

    @Test
    public void testAlarmInfoDAO() throws Exception {
        // test insert
        AsgAlarmBean bean1 = new AsgAlarmBean();
        bean1.setAlarm_id("ABCDEF1");
        bean1.setGroup_name("deploy-agent-test");
        bean1.setAction_type("GROW");
        bean1.setComparator("GreaterThanThreshold");
        bean1.setEvaluation_time(1000);
        bean1.setMetric_name("test-metric");
        bean1.setMetric_source("https://pinadmin.com");
        bean1.setThreshold(20.1);
        bean1.setFrom_aws_metric(true);


        AsgAlarmBean bean2 = new AsgAlarmBean();
        bean2.setAlarm_id("ABCDEF2");
        bean2.setGroup_name("deploy-agent-test");
        bean2.setAction_type("SHRINK");
        bean2.setComparator("LessThanThreshold");
        bean2.setEvaluation_time(1000);
        bean2.setMetric_name("test-metric");
        bean2.setMetric_source("https://pinadmin.com");
        bean2.setThreshold(10.1);
        bean2.setFrom_aws_metric(true);

        AsgAlarmBean bean3 = new AsgAlarmBean();
        bean3.setAlarm_id("ABCDEF3");
        bean3.setGroup_name("deploy-agent-test");
        bean3.setAction_type("SHRINK");
        bean3.setComparator("LessThanThreshold");
        bean3.setEvaluation_time(1000);
        bean3.setMetric_name("test-metric2");
        bean3.setMetric_source("https://pinadmin.com2");
        bean3.setThreshold(10.1);
        bean3.setFrom_aws_metric(false);

        alarmDAO.insertOrUpdateAlarmInfo(bean1);
        alarmDAO.insertOrUpdateAlarmInfo(bean2);
        alarmDAO.insertOrUpdateAlarmInfo(bean3);

        // test get
        AsgAlarmBean resultBean = alarmDAO.getAlarmInfoById("ABCDEF1");
        assertTrue(EqualsBuilder.reflectionEquals(resultBean, bean1));

        List<AsgAlarmBean> resultList = alarmDAO.getAlarmInfosByGroup("deploy-agent-test");
        assertEquals(resultList.size(), 3);

        List<MetricBean> resultMetricList = alarmDAO.getMetrics();
        assertEquals(resultMetricList.size(), 2);


        // test delete
        alarmDAO.deleteAlarmInfoById("ABCDEF3");
        assertNull(alarmDAO.getAlarmInfoById("ABCDEF3"));
        resultMetricList = alarmDAO.getMetrics();
        assertEquals(resultMetricList.size(), 1);
        MetricBean expectedBean = new MetricBean();
        expectedBean.setMetric_name("test-metric");
        expectedBean.setMetric_source("https://pinadmin.com");
        expectedBean.setGroup_name("deploy-agent-test");
        expectedBean.setFrom_aws_metric(true);
        assertTrue(EqualsBuilder.reflectionEquals(resultMetricList.get(0), expectedBean));

        // test update
        bean2.setComparator("LessThanOrEqualToThreshold");
        bean2.setThreshold(30.1);

        alarmDAO.updateAlarmInfoById
            (bean2.getAlarm_id(), bean2);
        AsgAlarmBean resultBean2 = alarmDAO.getAlarmInfoById(bean2.getAlarm_id());
        assertTrue(EqualsBuilder.reflectionEquals(resultBean2, bean2));
    }

    private HealthCheckBean genDefaultHealthCheck() {
        HealthCheckBean bean = new HealthCheckBean();
        bean.setGroup_name("group-1");
        bean.setEnv_id("env-1");
        bean.setDeploy_id("deploy-1");
        bean.setAmi_id("ami-1");
        bean.setState(HealthCheckState.COMPLETED);
        bean.setStatus(HealthCheckStatus.FAILED);
        bean.setType(HealthCheckType.AMI_TRIGGERED);
        bean.setLast_worked_on(System.currentTimeMillis());
        bean.setStart_time(System.currentTimeMillis());
        bean.setState_start_time(System.currentTimeMillis());
        return bean;
    }

    @Test
    public void testGetRecentHistory() throws Exception {
        HealthCheckBean bean1 = genDefaultHealthCheck();
        bean1.setId("hid-1");
        bean1.setState(HealthCheckState.COMPLETED);
        bean1.setStatus(HealthCheckStatus.FAILED);
        healthCheckDAO.insertHealthCheck(bean1);

        HealthCheckBean bean2 = genDefaultHealthCheck();
        bean2.setId("hid-2");
        bean2.setState(HealthCheckState.INIT);
        bean2.setStatus(HealthCheckStatus.TIMEOUT);
        healthCheckDAO.insertHealthCheck(bean2);

        HealthCheckBean bean3 = genDefaultHealthCheck();
        bean3.setId("hid-3");
        bean3.setState(HealthCheckState.COMPLETED);
        bean3.setStatus(HealthCheckStatus.TIMEOUT);
        healthCheckDAO.insertHealthCheck(bean3);

        Collection<String> states =  healthCheckDAO.getRecentHealthCheckStatus("group-1", 2);
        assertEquals(states.size(), 2);
        assertTrue(states.contains(HealthCheckStatus.FAILED.toString()));
        assertTrue(states.contains(HealthCheckStatus.TIMEOUT.toString()));
    }

    @Test
    public void testHealthCheckDAO() throws Exception {
        HealthCheckBean bean1 = new HealthCheckBean();
        bean1.setId("id-1");
        bean1.setGroup_name("group-1");
        bean1.setEnv_id("env-1");
        bean1.setDeploy_id("deploy-1");
        bean1.setAmi_id("ami-1");
        bean1.setState(HealthCheckState.INIT);
        bean1.setStatus(HealthCheckStatus.UNKNOWN);
        bean1.setType(HealthCheckType.AMI_TRIGGERED);
        bean1.setLast_worked_on(System.currentTimeMillis());
        bean1.setStart_time(System.currentTimeMillis());
        bean1.setState_start_time(System.currentTimeMillis());
        healthCheckDAO.insertHealthCheck(bean1);

        HealthCheckBean bean2 = new HealthCheckBean();
        bean2.setId("id-2");
        bean2.setGroup_name("group-1");
        bean2.setEnv_id("env-1");
        bean2.setDeploy_id("deploy-1");
        bean2.setAmi_id("ami-1");
        bean2.setState(HealthCheckState.COMPLETED);
        bean2.setStatus(HealthCheckStatus.UNKNOWN);
        bean2.setType(HealthCheckType.AMI_TRIGGERED);
        bean2.setLast_worked_on(System.currentTimeMillis());
        bean2.setStart_time(System.currentTimeMillis());
        bean2.setState_start_time(System.currentTimeMillis());
        bean2.setHost_terminated(false);
        healthCheckDAO.insertHealthCheck(bean2);

        List<HealthCheckBean> beans = healthCheckDAO.getHealthChecksByGroup("group-1", 1, 3);
        assertEquals(beans.size(), 2);

        beans = healthCheckDAO.getHealthChecksByUnterminatedHosts();
        assertEquals(beans.size(), 1);

        HealthCheckBean bean3 = healthCheckDAO.getHealthCheckById("id-1");
        assertEquals(bean3.getId(), bean1.getId());
        assertEquals(bean3.getGroup_name(), bean1.getGroup_name());
        assertEquals(bean3.getEnv_id(), bean1.getEnv_id());
        assertEquals(bean3.getDeploy_id(), bean1.getDeploy_id());
        assertEquals(bean3.getAmi_id(), bean1.getAmi_id());
        assertEquals(bean3.getState(), bean1.getState());
        assertEquals(bean3.getStatus(), bean1.getStatus());
        assertEquals(bean3.getType(), bean1.getType());
        assertNull(bean3.getHost_id());
        assertNull(bean3.getHost_launch_time());
        assertNull(bean3.getDeploy_start_time());
        assertNull(bean3.getDeploy_complete_time());

        // Update
        Long time = System.currentTimeMillis();
        bean1.setHost_id("host-1");
        bean1.setHost_launch_time(time);
        bean1.setDeploy_start_time(time);
        bean1.setState(HealthCheckState.LAUNCHING);
        healthCheckDAO.updateHealthCheckById("id-1", bean1);

        HealthCheckBean bean4 = healthCheckDAO.getHealthCheckById("id-1");
        assertEquals(bean4.getHost_id(), bean1.getHost_id());
        assertEquals(bean4.getState(), bean1.getState());
        assertEquals(bean4.getHost_launch_time(), time);
        assertEquals(bean4.getDeploy_start_time(), time);
        assertNull(bean4.getDeploy_complete_time());

        beans = healthCheckDAO.getOngoingHealthChecks();
        assertEquals(beans.size(), 1);

        healthCheckDAO.removeHealthCheckById("id-1");
        beans = healthCheckDAO.getOngoingHealthChecks();
        assertEquals(beans.size(), 0);
    }

    @Test
    public void testHealthCheckErrorDAO() throws Exception {
        HealthCheckErrorBean bean = new HealthCheckErrorBean();
        bean.setId("id-1");
        bean.setEnv_id("env-1");
        bean.setDeploy_stage(DeployStage.DOWNLOADING);
        bean.setAgent_state(AgentState.PAUSED_BY_SYSTEM);
        bean.setAgent_status(AgentStatus.TOO_MANY_RETRY);
        bean.setLast_err_no(22);
        bean.setFail_count(3);
        bean.setError_msg("Health check failed");
        bean.setAgent_start_date(System.currentTimeMillis());
        bean.setAgent_last_update(System.currentTimeMillis());
        healthCheckErrorDAO.insertHealthCheckError(bean);

        HealthCheckErrorBean bean1 = healthCheckErrorDAO.getHealthCheckErrorById("id-1");
        assertEquals(bean.getDeploy_stage(), bean1.getDeploy_stage());
        assertEquals(bean.getAgent_state(), bean1.getAgent_state());
        assertEquals(bean.getAgent_status(), bean1.getAgent_status());

        healthCheckErrorDAO.removeHealthCheckErrorById("id-1");
        bean1 = healthCheckErrorDAO.getHealthCheckErrorById("id-1");
        assertNull(bean1);
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
    public void testNewInstanceDAO() throws Exception {
        newInstanceReportDAO.addNewInstanceReport("h-123", 123L, Arrays.asList("e-123", "e-124"));
        newInstanceReportDAO.addNewInstanceReport("h-124", 123L, Arrays.asList("e-123", "e-124"));
        List<String> ids = newInstanceReportDAO.getNewInstanceIdsByEnv("e-123");
        assertEquals(ids.size(), 2);
        NewInstanceReportBean instanceReportBean = newInstanceReportDAO.getByIds("h-123", "e-123");
        assertFalse(instanceReportBean.getReported());
        newInstanceReportDAO.deleteNewInstanceReport("h-123", "e-123");
        instanceReportBean = newInstanceReportDAO.getByIds("h-123", "e-123");
        assertNull(instanceReportBean);
        newInstanceReportDAO.reportNewInstances("h-124", "e-123");
        assertTrue(newInstanceReportDAO.getByIds("h-124", "e-123").getReported());
    }

    @Test
    public void testAsgLifecycleEventDAO() throws Exception {
        AsgLifecycleEventBean bean1 = new AsgLifecycleEventBean();
        bean1.setToken_id("id1");
        bean1.setHook_id("hook-1");
        bean1.setGroup_name("group-1");
        bean1.setHost_id("host-1");
        bean1.setStart_date(System.currentTimeMillis());
        asgLifecycleEventDAO.insertAsgLifecycleEvent(bean1);

        AsgLifecycleEventBean bean2 = new AsgLifecycleEventBean();
        bean2.setToken_id("id2");
        bean2.setHook_id("hook-2");
        bean2.setGroup_name("group-1");
        bean2.setHost_id("host-2");
        bean2.setStart_date(System.currentTimeMillis());
        asgLifecycleEventDAO.insertAsgLifecycleEvent(bean2);

        AsgLifecycleEventBean bean3 = new AsgLifecycleEventBean();
        bean3.setToken_id("id3");
        bean3.setHook_id("hook-2");
        bean3.setGroup_name("group-1");
        bean3.setHost_id("host-3");
        bean3.setStart_date(System.currentTimeMillis());
        asgLifecycleEventDAO.insertAsgLifecycleEvent(bean3);

        List<String> hookIds = asgLifecycleEventDAO.getHookIdsFromAsgLifeCycleEvent();
        assertEquals(hookIds.size(), 2);

        List<AsgLifecycleEventBean> beans = asgLifecycleEventDAO.getAsgLifecycleEventByHook("hook-1");
        assertEquals(beans.size(), 1);
        assertEquals(beans.get(0).getToken_id(), "id1");
        assertEquals(beans.get(0).getGroup_name(), "group-1");
        assertEquals(beans.get(0).getHost_id(), "host-1");

        asgLifecycleEventDAO.deleteAsgLifecycleEventById("id2");
        beans = asgLifecycleEventDAO.getAsgLifecycleEventByHook("hook-2");
        assertEquals(beans.size(), 1);

        asgLifecycleEventDAO.deleteAsgLifeCycleEventByHookId("hook-2");
        beans = asgLifecycleEventDAO.getAsgLifecycleEventByHook("hook-2");
        assertEquals(beans.size(), 0);
    }

    @Test
    public void testManagingGroupDAO() throws Exception {
        ManagingGroupsBean managingGroupsBean = new ManagingGroupsBean();
        managingGroupsBean.setBatch_size(10);
        managingGroupsBean.setCool_down(100);
        managingGroupsBean.setGroup_name("test1");
        managingGroupsBean.setLast_activity_time(System.currentTimeMillis());
        managingGroupsBean.setLending_priority(0);
        managingGroupsBean.setInstance_type("c3.8xlarge");
        managingGroupsBean.setLent_size(0);
        managingGroupsBean.setMax_lending_size(100);
        managingGroupDAO.insertManagingGroup("test1", managingGroupsBean);

        ManagingGroupsBean bean = managingGroupDAO.getManagingGroupByGroupName("test1");
        assertEquals(bean.getBatch_size(), (Integer) 10);
        assertEquals(bean.getCool_down(), (Integer) 100);
        assertEquals(bean.getGroup_name(), "test1");
        assertEquals(bean.getLending_priority(), new Integer(0));
        assertEquals(bean.getLent_size(), (Integer) 0);
        assertEquals(bean.getInstance_type(), "c3.8xlarge");
        assertEquals(bean.getMax_lending_size(), (Integer) 100);

        bean.setLent_size(10);
        Long currentTime = System.currentTimeMillis();
        bean.setLast_activity_time(currentTime);
        managingGroupDAO.updateManagingGroup("test1", bean);
        ManagingGroupsBean bean2 = managingGroupDAO.getManagingGroupByGroupName("test1");
        assertEquals(bean2.getLent_size(), (Integer) 10);
        assertEquals(bean2.getLast_activity_time(), currentTime);
    }

    @Test
    public void testClusterDAO() throws Exception {
        ClusterBean bean1 = new ClusterBean();
        bean1.setCluster_name("sample1-prod");
        bean1.setCapacity(10);
        bean1.setBase_image_id("base-image");
        bean1.setHost_type_id("ComputeHi");
        bean1.setSecurity_zone_id("prod-public");
        bean1.setPlacement_id("us-east");
        bean1.setProvider(CloudProvider.AWS);
        bean1.setLast_update(System.currentTimeMillis());
        clusterDAO.insert(bean1);

        ClusterBean bean2 = clusterDAO.getByClusterName("sample1-prod");
        assertEquals(bean2.getBase_image_id(), "base-image");
        assertEquals(bean2.getHost_type_id(), "ComputeHi");
        assertEquals(bean2.getSecurity_zone_id(), "prod-public");
        assertEquals(bean2.getPlacement_id(), "us-east");
        assertEquals(bean2.getProvider(), CloudProvider.AWS);

        ClusterBean bean3 = new ClusterBean();
        bean3.setHost_type_id("ComputeLo");
        bean3.setPlacement_id("us-north");
        clusterDAO.update("sample1-prod", bean3);

        bean2 = clusterDAO.getByClusterName("sample1-prod");
        assertEquals(bean2.getHost_type_id(), "ComputeLo");
        assertEquals(bean2.getPlacement_id(), "us-north");

        clusterDAO.delete("sample1-prod");
        bean2 = clusterDAO.getByClusterName("sample1-prod");
        assertNull(bean2);
    }

    @Test
    public void testBaseImageDAO() throws Exception {
        BaseImageBean bean1 = new BaseImageBean();
        String id = CommonUtils.getBase64UUID();
        bean1.setId(id);
        bean1.setAbstract_name("base-vm");
        bean1.setProvider_name("pinterest-image-a");
        bean1.setProvider(CloudProvider.AWS);
        bean1.setBasic(true);
        bean1.setQualified(true);
        bean1.setDescription("This is a basic vm image");
        bean1.setPublish_date(System.currentTimeMillis());
        baseImageDAO.insert(bean1);

        BaseImageBean bean2 = baseImageDAO.getById(id);
        assertEquals(bean2.getProvider_name(), "pinterest-image-a");
        assertEquals(bean2.getProvider(), CloudProvider.AWS);
        assertTrue(bean2.getBasic());
        assertTrue(bean2.getQualified());
        assertEquals(bean2.getDescription(), "This is a basic vm image");
    }

    @Test
    public void testHostTypeDAO() throws Exception {
        HostTypeBean bean1 = new HostTypeBean();
        String id = CommonUtils.getBase64UUID();
        bean1.setId(id);
        bean1.setAbstract_name("ComputeHi");
        bean1.setProvider_name("c10");
        bean1.setProvider(CloudProvider.AWS);
        bean1.setBasic(true);
        bean1.setCore(16);
        bean1.setMem(32000);
        bean1.setStorage("512G HDD");
        bean1.setDescription("This is a high computing capability machine. $8/hour");
        hostTypeDAO.insert(bean1);

        HostTypeBean bean2 = hostTypeDAO.getById(id);
        assertEquals(bean2.getProvider_name(), "c10");
        assertEquals(bean2.getProvider(), CloudProvider.AWS);
        assertTrue(bean2.getBasic());
        assertEquals(bean2.getCore().intValue(), 16);
        assertEquals(bean2.getMem().intValue(), 32000);
        assertEquals(bean2.getStorage(), "512G HDD");
        assertEquals(bean2.getDescription(), "This is a high computing capability machine. $8/hour");

        Collection<HostTypeBean> beans = hostTypeDAO.getByProvider(CloudProvider.AWS.toString());
        assertEquals(beans.size(), 1);

        HostTypeBean bean3 = hostTypeDAO.getByProviderAndAbstractName(CloudProvider.AWS.toString(), "ComputeHi");
        assertEquals(bean3.getProvider_name(), "c10");
        assertEquals(bean3.getId(), id);
    }

    @Test
    public void testSecurityZoneDAO() throws Exception {
        SecurityZoneBean bean1 = new SecurityZoneBean();
        String id = CommonUtils.getBase64UUID();
        bean1.setId(id);
        bean1.setAbstract_name("prod-public");
        bean1.setProvider_name("prod-public-123");
        bean1.setProvider(CloudProvider.AWS);
        bean1.setBasic(true);
        bean1.setDescription("This network zone is used for web facing service.");
        securityZoneDAO.insert(bean1);

        SecurityZoneBean bean2 = securityZoneDAO.getById(id);
        assertEquals(bean2.getProvider_name(), "prod-public-123");
        assertEquals(bean2.getProvider(), CloudProvider.AWS);
        assertTrue(bean2.getBasic());
        assertEquals(bean2.getDescription(), "This network zone is used for web facing service.");

        Collection<SecurityZoneBean> beans = securityZoneDAO.getByProvider(CloudProvider.AWS.toString());
        assertEquals(beans.size(), 1);

        SecurityZoneBean bean3 = securityZoneDAO.getByProviderAndAbstractName(CloudProvider.AWS.toString(), "prod-public");
        assertEquals(bean3.getProvider_name(), "prod-public-123");
        assertEquals(bean3.getId(), id);
    }

    @Test
    public void testPlacementDAO() throws Exception {
        PlacementBean bean1 = new PlacementBean();
        String id = CommonUtils.getBase64UUID();
        bean1.setId(id);
        bean1.setAbstract_name("us-east");
        bean1.setProvider_name("us-east-1");
        bean1.setProvider(CloudProvider.AWS);
        bean1.setBasic(true);
        bean1.setCapacity(100);
        bean1.setDescription("This is east region datacenter.");
        placementDAO.insert(bean1);

        PlacementBean bean2 = placementDAO.getById(id);
        assertEquals(bean2.getProvider_name(), "us-east-1");
        assertEquals(bean2.getProvider(), CloudProvider.AWS);
        assertTrue(bean2.getBasic());
        assertEquals(bean2.getCapacity().intValue(), 100);
        assertEquals(bean2.getDescription(), "This is east region datacenter.");

        PlacementBean updateBean = new PlacementBean();
        updateBean.setCapacity(200);
        placementDAO.updateById(id, updateBean);
        bean2 = placementDAO.getById(id);
        assertEquals(bean2.getCapacity().intValue(), 200);

        Collection<PlacementBean> beans = placementDAO.getByProvider(CloudProvider.AWS.toString());
        assertEquals(beans.size(), 1);

        PlacementBean bean3 = placementDAO.getByProviderAndAbstractName(CloudProvider.AWS.toString(), "us-east");
        assertEquals(bean3.getProvider_name(), "us-east-1");
        assertEquals(bean3.getId(), id);
    }

    @Test
    public void testSpotAutoScalingGroupDAO() throws Exception {
        SpotAutoScalingBean bean1 = new SpotAutoScalingBean();
        bean1.setCluster_name("asg-1");
        bean1.setBid_price("0.5");
        bean1.setSpot_ratio(0.5);
        spotAutoScalingDAO.insertAutoScalingGroupToCluster("asg-1-spot", bean1);
        SpotAutoScalingBean resultBean = spotAutoScalingDAO.getAutoScalingGroupsByCluster("asg-1");
        assertNotNull(resultBean);

        assertEquals(resultBean.getCluster_name(), "asg-1");
        assertEquals(resultBean.getBid_price(), "0.5");

        SpotAutoScalingBean updatedBean = new SpotAutoScalingBean();
        updatedBean.setBid_price("0.6");
        spotAutoScalingDAO.updateSpotAutoScalingGroup("asg-1", updatedBean);
        SpotAutoScalingBean resultBean2 = spotAutoScalingDAO.getAutoScalingGroupsByCluster("asg-1");
        assertNotNull(resultBean2);

        assertEquals(resultBean2.getCluster_name(), "asg-1");
        assertEquals(resultBean2.getBid_price(), "0.6");

        SpotAutoScalingBean bean3 = new SpotAutoScalingBean();
        bean3.setCluster_name("asg-1");
        bean3.setBid_price("0.7");
        bean3.setSpot_ratio(0.6);


        spotAutoScalingDAO.deleteAllAutoScalingGroupByCluster("asg-1");
        resultBean = spotAutoScalingDAO.getAutoScalingGroupsByCluster("asg-1");
        assertNull(resultBean);

    }

    @Test
    public void testTagDAO() throws Exception{
        TagBean tag = genTagBean(TagValue.BAD_BUILD,"TestEnv","BUILD",
                genDefaultBuildInfoBean("b-3", "sss-1", "c-1", "r-1", System.currentTimeMillis()));
        tagDAO.insert(tag);
        TagBean tag2 = tagDAO.getById(tag.getId());
        assertNotNull(tag2);
        assertEquals(tag.getTarget_id(),tag2.getTarget_id());
        BuildBean embededBean = tag2.deserializeTagMetaInfo(BuildBean.class);
        assertEquals("b-3", embededBean.getBuild_id());

        List<TagBean> targetList = tagDAO.getByTargetId(tag.getTarget_id());
        assertEquals(1,targetList.size());
        targetList = tagDAO.getByTargetIdAndType(tag.getTarget_id(), TagTargetType.BUILD);
        assertEquals(1,targetList.size());

        tagDAO.delete(tag.getId());
        tag2 = tagDAO.getById(tag.getId());
        assertNull(tag2);
        targetList = tagDAO.getByTargetId(tag.getTarget_id());
        assertEquals(0,targetList.size());
        targetList = tagDAO.getByTargetIdAndType(tag.getTarget_id(), TagTargetType.BUILD);
        assertEquals(0,targetList.size());

        tagDAO.insert(genTagBean(TagValue.BAD_BUILD,"env1","BUILD",new HashMap<String,String>()));
        tagDAO.insert(genTagBean(TagValue.BAD_BUILD,"env1", "BUILD",new HashMap<String,String>()));
        tagDAO.insert(genTagBean(TagValue.BAD_BUILD,"env1","BUILD",new HashMap<String,String>()));
        tagDAO.insert(genTagBean(TagValue.BAD_BUILD,"env1","BUILD",new HashMap<String,String>()));

        assertEquals(4, tagDAO.getByValue(TagValue.BAD_BUILD).size());
        assertEquals(0, tagDAO.getByValue(TagValue.GOOD_BUILD).size());
    }

    @Test
    public void testClusterUpgradeEventDAO() throws Exception {
        ClusterUpgradeEventBean bean = new ClusterUpgradeEventBean();
        bean.setId("id-1");
        bean.setCluster_name("sample-env-canary");
        bean.setEnv_id("env-1");
        bean.setState(ClusterUpgradeEventState.INIT);
        bean.setStatus(ClusterUpgradeEventStatus.UNKNOWN);
        bean.setStart_time(System.currentTimeMillis());
        bean.setState_start_time(System.currentTimeMillis());
        bean.setLast_worked_on(System.currentTimeMillis());
        clusterUpgradeEventDAO.insertClusterUpgradeEvent(bean);

        ClusterUpgradeEventBean bean1 = clusterUpgradeEventDAO.getById("id-1");
        assertEquals("sample-env-canary", bean1.getCluster_name());
        assertEquals("env-1", bean1.getEnv_id());
        assertEquals(ClusterUpgradeEventState.INIT, bean1.getState());
        assertEquals(ClusterUpgradeEventStatus.UNKNOWN, bean1.getStatus());
        assertNull(bean1.getHost_ids());
        assertNull(bean1.getError_message());

        ClusterUpgradeEventBean updateBean = new ClusterUpgradeEventBean();
        updateBean.setState(ClusterUpgradeEventState.REPLACING);
        updateBean.setHost_ids("host-1,host-2,host-3");
        updateBean.setLast_worked_on(System.currentTimeMillis());
        updateBean.setError_message("This is error msg");
        clusterUpgradeEventDAO.updateById("id-1", updateBean);

        bean1 = clusterUpgradeEventDAO.getById("id-1");
        assertEquals(ClusterUpgradeEventState.REPLACING, bean1.getState());
        assertEquals("host-1,host-2,host-3", bean1.getHost_ids());
        assertEquals("This is error msg", bean1.getError_message());

        Collection<ClusterUpgradeEventBean> beans = clusterUpgradeEventDAO.getOngoingEvents();
        assertEquals(beans.size(), 1);
    }

    @Test
    public void testPasConfigDAO() throws Exception {
        PasConfigBean pasConfigBean = new PasConfigBean();
        pasConfigBean.setGroup_name("test1");
        pasConfigBean.setThroughput(100);
        pasConfigBean.setLast_updated(10L);
        pasConfigBean.setMetric("");
        pasConfigBean.setPas_state(PasState.DISABLED);

        pasConfigDAO.insertPasConfig(pasConfigBean);

        List<String> configNames = pasConfigDAO.getAllPasGroups();
        assertEquals(configNames.size(), 1);
        assertEquals(configNames.get(0), "test1");
    }

    private EnvironBean genDefaultEnvBean(String envId, String envName, String envStage, String deployId) {
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

    private AgentBean genDefaultAgentBean(String hostName, String hostId, String envId, String deployId, DeployStage deployStage) {
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
