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
package com.pinterest.deployservice.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.pinterest.deployservice.bean.AgentBean;
import com.pinterest.deployservice.bean.AgentState;
import com.pinterest.deployservice.bean.AgentStatus;
import com.pinterest.deployservice.bean.DeployBean;
import com.pinterest.deployservice.bean.DeployPriority;
import com.pinterest.deployservice.bean.DeployStage;
import com.pinterest.deployservice.bean.DeployType;
import com.pinterest.deployservice.bean.EnvState;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.PingReportBean;
import com.pinterest.deployservice.dao.DeployDAO;
import com.pinterest.deployservice.dao.EnvironDAO;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoalAnalystTest {

    private Map<String, EnvironBean> envs;
    private Map<String, PingReportBean> reports;
    private Map<String, AgentBean> agents;

    @BeforeClass
    public static void setUpClass() throws Exception {

    }

    AgentBean genDefaultAgent() {
        AgentBean agentBean = new AgentBean();
        agentBean.setEnv_id("foo");
        agentBean.setDeploy_id("foo");
        agentBean.setHost_name("foo");
        agentBean.setDeploy_stage(DeployStage.SERVING_BUILD);
        agentBean.setState(AgentState.NORMAL);
        agentBean.setFail_count(0);
        agentBean.setFirst_deploy(false);
        return agentBean;
    }

    PingReportBean genDefaultReport() {
        PingReportBean report = new PingReportBean();
        report.setDeployId("foo");
        report.setEnvId("foo");
        report.setDeployStage(DeployStage.SERVING_BUILD);
        report.setAgentStatus(AgentStatus.SUCCEEDED);

        return report;
    }

    EnvironBean genDefaultEnvBean() {
        EnvironBean envBean = new EnvironBean();
        envBean.setEnv_id("foo");
        envBean.setEnv_name("foo");
        envBean.setStage_name("foo");
        envBean.setEnv_state(EnvState.NORMAL);
        envBean.setPriority(DeployPriority.NORMAL);
        envBean.setDeploy_id("foo");
        envBean.setDeploy_type(DeployType.REGULAR);
        return envBean;
    }

    @Before
    public void setUpTest() throws Exception {
        envs = new HashMap<>();
        reports = new HashMap<>();
        agents = new HashMap<>();
    }

    // Case 0.1: env has no deploy yet
    @Test
    public void testNoDeployYet() throws Exception {
        EnvironBean envBean = genDefaultEnvBean();
        envBean.setDeploy_id(null);
        envs.put(envBean.getEnv_id(), envBean);

        GoalAnalyst analyst = new GoalAnalyst(null, null, "foo", "id-1", envs, reports, agents);
        analyst.analysis();

        assertEquals(analyst.getNeedUpdateAgents().size(), 0);
        assertEquals(analyst.getNeedDeleteAgentEnvIds().size(), 0);
        assertEquals(analyst.getInstallCandidates().size(), 0);
    }

    // Case 0.2: env is onhold
    @Test
    public void test1Env1ReportEnvOnhold() throws Exception {
        EnvironBean envBean = genDefaultEnvBean();
        envBean.setEnv_state(EnvState.PAUSED);
        envs.put(envBean.getEnv_id(), envBean);

        PingReportBean report = genDefaultReport();
        report.setDeployStage(DeployStage.PRE_RESTART);
        reports.put(report.getEnvId(), report);

        GoalAnalyst analyst = new GoalAnalyst(null, null, "foo", "id-1", envs, reports, agents);
        analyst.analysis();

        assertEquals(analyst.getNeedUpdateAgents().size(), 1);
        assertEquals(analyst.getNeedDeleteAgentEnvIds().size(), 0);
        assertEquals(analyst.getInstallCandidates().size(), 1);
    }

    // Case 0.3: agent is onhold by user
    @Test
    public void test1Env1ReportAgentOnhold() throws Exception {
        EnvironBean envBean = genDefaultEnvBean();
        envs.put(envBean.getEnv_id(), envBean);

        PingReportBean report = genDefaultReport();
        report.setDeployStage(DeployStage.PRE_RESTART);
        reports.put(report.getEnvId(), report);

        AgentBean agent = genDefaultAgent();
        agent.setState(AgentState.PAUSED_BY_USER);
        agents.put(envBean.getEnv_id(), agent);

        GoalAnalyst analyst = new GoalAnalyst(null, null, "foo", "id-1", envs, reports, agents);
        analyst.analysis();

        assertEquals(analyst.getNeedUpdateAgents().size(), 1);
        assertEquals(analyst.getNeedDeleteAgentEnvIds().size(), 0);
        assertEquals(analyst.getInstallCandidates().size(), 0);
    }

    // Case 1.1: serving the right deploy
    @Test
    public void test1Env1ReportServing() throws Exception {
        EnvironBean envBean = genDefaultEnvBean();
        envs.put(envBean.getEnv_id(), envBean);

        PingReportBean report = genDefaultReport();
        reports.put(report.getEnvId(), report);

        GoalAnalyst analyst = new GoalAnalyst(null, null, "foo", "id-1", envs, reports, agents);
        analyst.analysis();

        assertEquals(analyst.getNeedUpdateAgents().size(), 1);
        assertEquals(analyst.getNeedDeleteAgentEnvIds().size(), 0);
        assertEquals(analyst.getInstallCandidates().size(), 0);
    }

    // Case 1.2: succeeded on current step, proceed on next step
    @Test
    public void test1Env1ReportStageSuc() throws Exception {
        EnvironBean envBean = genDefaultEnvBean();
        envs.put(envBean.getEnv_id(), envBean);

        PingReportBean report = genDefaultReport();
        report.setDeployStage(DeployStage.PRE_RESTART);
        reports.put(report.getEnvId(), report);

        GoalAnalyst analyst = new GoalAnalyst(null, null, "foo", "id-1", envs, reports, agents);
        analyst.analysis();

        assertEquals(analyst.getNeedUpdateAgents().size(), 1);
        assertEquals(analyst.getNeedDeleteAgentEnvIds().size(), 0);
        assertEquals(analyst.getInstallCandidates().size(), 1);
        GoalAnalyst.InstallCandidate candidate = analyst.getInstallCandidates().get(0);
        assertEquals(candidate.needWait, false);
        assertEquals(candidate.updateBean.getDeploy_stage(), DeployStage.RESTARTING);
        assertEquals(candidate.updateBean.getStatus(), AgentStatus.UNKNOWN);
        assertEquals(candidate.updateBean.getLast_err_no(), new Integer(0));
    }

    // Case 1.3: succeeded on current step, proceed on next step, though agent is currently PAUSED
    @Test
    public void test1Env1ReportStageSucThoughAgentPaused() throws Exception {
        EnvironBean envBean = genDefaultEnvBean();
        envs.put(envBean.getEnv_id(), envBean);

        PingReportBean report = genDefaultReport();
        report.setDeployStage(DeployStage.PRE_RESTART);
        reports.put(report.getEnvId(), report);

        AgentBean agent = genDefaultAgent();
        agent.setState(AgentState.PAUSED_BY_SYSTEM);
        agents.put(agent.getEnv_id(), agent);

        GoalAnalyst analyst = new GoalAnalyst(null, null, "foo", "id-1", envs, reports, agents);
        analyst.analysis();

        assertEquals(analyst.getNeedUpdateAgents().size(), 1);
        assertEquals(analyst.getNeedDeleteAgentEnvIds().size(), 0);
        assertEquals(analyst.getInstallCandidates().size(), 1);
        GoalAnalyst.InstallCandidate candidate = analyst.getInstallCandidates().get(0);
        assertEquals(candidate.needWait, false);
        assertEquals(candidate.updateBean.getDeploy_stage(), DeployStage.RESTARTING);
        assertEquals(candidate.updateBean.getStatus(), AgentStatus.UNKNOWN);
        assertEquals(candidate.updateBean.getLast_err_no(), new Integer(0));
    }

    @Test
    public void testFirstTimeDeploy() throws Exception {
        EnvironBean envBean = genDefaultEnvBean();
        envs.put(envBean.getEnv_id(), envBean);
        // no report, no agents
        GoalAnalyst analyst = new GoalAnalyst(null, null, "foo", "id-1", envs, reports, agents);
        analyst.analysis();

        assertEquals(analyst.getNeedUpdateAgents().size(), 0);
        assertEquals(analyst.getNeedDeleteAgentEnvIds().size(), 0);
        assertEquals(analyst.getInstallCandidates().size(), 1);
        GoalAnalyst.InstallCandidate candidate = analyst.getInstallCandidates().get(0);
        assertEquals(candidate.needWait, true);
        assertTrue(candidate.updateBean.getFirst_deploy());
        assertNull(candidate.updateBean.getFirst_deploy_time());
    }

    @Test
    public void testFirstTimeDeploy2() throws Exception {
        EnvironBean envBean = genDefaultEnvBean();
        envs.put(envBean.getEnv_id(), envBean);
        AgentBean agentBean = genDefaultAgent();

        agentBean.setEnv_id("foo2");
        agents.put("foo2", agentBean);
        EnvironDAO environDAO = Mockito.mock(EnvironDAO.class);
        Mockito.when(environDAO.getById("foo2")).thenReturn(envBean);
        GoalAnalyst analyst = new GoalAnalyst(null, environDAO, "foo", "id-1", envs, reports, agents);
        analyst.analysis();

        assertEquals(analyst.getNeedUpdateAgents().size(), 0);
        assertEquals(analyst.getNeedDeleteAgentEnvIds().size(), 1);
        assertEquals(analyst.getInstallCandidates().size(), 1);
        GoalAnalyst.InstallCandidate candidate = analyst.getInstallCandidates().get(0);
        assertEquals(candidate.needWait, true);
        assertFalse(candidate.updateBean.getFirst_deploy());
        assertNull(candidate.updateBean.getFirst_deploy_time());
    }

    @Test
    public void testFirstTimeDeploy3() throws Exception {
        EnvironBean envBean = genDefaultEnvBean();
        envs.put(envBean.getEnv_id(), envBean);
        AgentBean agentBean = genDefaultAgent();
        agentBean.setEnv_id("foo2");
        agents.put("foo2", agentBean);
        EnvironBean envBean2 = genDefaultEnvBean();
        envBean2.setEnv_name("foo2");
        EnvironDAO environDAO = Mockito.mock(EnvironDAO.class);
        Mockito.when(environDAO.getById("foo2")).thenReturn(envBean2);
        GoalAnalyst analyst = new GoalAnalyst(null, environDAO, "foo", "id-1", envs, reports, agents);
        analyst.analysis();

        assertEquals(analyst.getNeedUpdateAgents().size(), 0);
        assertEquals(analyst.getNeedDeleteAgentEnvIds().size(), 1);
        assertEquals(analyst.getInstallCandidates().size(), 1);
        GoalAnalyst.InstallCandidate candidate = analyst.getInstallCandidates().get(0);
        assertEquals(candidate.needWait, true);
        assertTrue(candidate.updateBean.getFirst_deploy());
        assertNull(candidate.updateBean.getFirst_deploy_time());
    }

    @Test
    public void testFirstTimeDeployMiddle() throws Exception {
        EnvironBean envBean = genDefaultEnvBean();
        envs.put(envBean.getEnv_id(), envBean);

        PingReportBean report = genDefaultReport();
        report.setDeployStage(DeployStage.PRE_RESTART);
        reports.put(report.getEnvId(), report);

        AgentBean agent = genDefaultAgent();
        agent.setFirst_deploy(true);
        agents.put(agent.getEnv_id(), agent);
        GoalAnalyst analyst = new GoalAnalyst(null, null, "foo", "id-1", envs, reports, agents);
        analyst.analysis();

        assertEquals(analyst.getNeedUpdateAgents().size(), 1);
        assertEquals(analyst.getNeedDeleteAgentEnvIds().size(), 0);
        assertEquals(analyst.getInstallCandidates().size(), 1);
        GoalAnalyst.InstallCandidate candidate = analyst.getInstallCandidates().get(0);
        assertEquals(candidate.needWait, false);
        assertTrue(candidate.updateBean.getFirst_deploy());
        assertNull(candidate.updateBean.getFirst_deploy_time());
    }

    @Test
    public void testFirstTimeDeployPostRestart() throws Exception {
        EnvironBean envBean = genDefaultEnvBean();
        envs.put(envBean.getEnv_id(), envBean);

        PingReportBean report = genDefaultReport();
        report.setDeployStage(DeployStage.POST_RESTART);
        reports.put(report.getEnvId(), report);

        AgentBean agent = genDefaultAgent();
        agent.setFirst_deploy(true);
        agents.put(agent.getEnv_id(), agent);
        GoalAnalyst analyst = new GoalAnalyst(null, null, "foo", "id-1", envs, reports, agents);
        analyst.analysis();

        assertEquals(analyst.getNeedUpdateAgents().size(), 1);
        assertEquals(analyst.getNeedDeleteAgentEnvIds().size(), 0);
        assertEquals(analyst.getInstallCandidates().size(), 1);
        GoalAnalyst.InstallCandidate candidate = analyst.getInstallCandidates().get(0);
        assertEquals(candidate.needWait, false);
        assertTrue(candidate.updateBean.getFirst_deploy());
        assertNull(candidate.updateBean.getFirst_deploy_time());

        reports.get(report.getEnvId()).setDeployStage(DeployStage.SERVING_BUILD);
        GoalAnalyst analyst1 = new GoalAnalyst(null, null, "foo", "id-1", envs, reports, agents);
        analyst1.analysis();
        assertEquals(analyst1.getNeedUpdateAgents().size(), 1);
        assertEquals(analyst1.getNeedDeleteAgentEnvIds().size(), 0);
        assertEquals(analyst1.getInstallCandidates().size(), 0);
        AgentBean agentBean = analyst1.getNeedUpdateAgents().get(envBean.getEnv_id());
        assertNotNull(agentBean.getFirst_deploy_time());
    }

    @Test
    public void testFirstTimeDeployEnd() throws Exception {
        EnvironBean envBean = genDefaultEnvBean();
        envs.put(envBean.getEnv_id(), envBean);

        PingReportBean report = genDefaultReport();
        report.setDeployStage(DeployStage.SERVING_BUILD);
        reports.put(report.getEnvId(), report);

        AgentBean agent = genDefaultAgent();
        agent.setFirst_deploy(true);
        agents.put(agent.getEnv_id(), agent);
        GoalAnalyst analyst = new GoalAnalyst(null, null, "foo", "id-1", envs, reports, agents);
        analyst.analysis();

        assertEquals(analyst.getNeedUpdateAgents().size(), 1);
        assertEquals(analyst.getNeedDeleteAgentEnvIds().size(), 0);
        assertEquals(analyst.getInstallCandidates().size(), 0);
        AgentBean agentBean = analyst.getNeedUpdateAgents().get(envBean.getEnv_id());
        assertFalse(agentBean.getFirst_deploy());
    }

    // Case 1.3: failed on current step, agent paused, expecting none
    @Test
    public void test1Env1ReportAgentPaused() throws Exception {
        EnvironBean envBean = genDefaultEnvBean();
        envs.put(envBean.getEnv_id(), envBean);

        PingReportBean report = genDefaultReport();
        report.setDeployStage(DeployStage.PRE_RESTART);
        report.setAgentStatus(AgentStatus.SCRIPT_FAILED);
        report.setErrorCode(100);
        reports.put(report.getEnvId(), report);

        AgentBean agent = genDefaultAgent();
        agent.setState(AgentState.PAUSED_BY_SYSTEM);
        agents.put(agent.getEnv_id(), agent);

        GoalAnalyst analyst = new GoalAnalyst(null, null, "foo", "id-1", envs, reports, agents);
        analyst.analysis();

        assertEquals(analyst.getNeedUpdateAgents().size(), 1);
        assertEquals(analyst.getNeedDeleteAgentEnvIds().size(), 0);
        assertEquals(analyst.getInstallCandidates().size(), 0);
        AgentBean agentBean = analyst.getNeedUpdateAgents().get(envBean.getEnv_id());
        assertEquals(agentBean.getState(), AgentState.PAUSED_BY_SYSTEM);
        assertEquals(agentBean.getStatus(), AgentStatus.SCRIPT_FAILED);
        assertEquals(agentBean.getLast_err_no(), new Integer(100));
    }

    // Case 1.3: agent failed with nonretryable error, expecting none
    @Test
    public void test1Env1ReportPauseAgentOnNoneRetryableError() throws Exception {
        EnvironBean envBean = genDefaultEnvBean();
        envs.put(envBean.getEnv_id(), envBean);

        PingReportBean report = genDefaultReport();
        report.setDeployStage(DeployStage.PRE_RESTART);
        report.setAgentStatus(AgentStatus.SCRIPT_TIMEOUT);
        report.setErrorCode(100);
        reports.put(report.getEnvId(), report);

        GoalAnalyst analyst = new GoalAnalyst(null, null, "foo", "id-1", envs, reports, agents);
        analyst.analysis();

        assertEquals(analyst.getNeedUpdateAgents().size(), 1);
        assertEquals(analyst.getNeedDeleteAgentEnvIds().size(), 0);
        assertEquals(analyst.getInstallCandidates().size(), 0);
        AgentBean agentBean = analyst.getNeedUpdateAgents().get(envBean.getEnv_id());
        assertEquals(agentBean.getState(), AgentState.PAUSED_BY_SYSTEM);
        assertEquals(agentBean.getStatus(), AgentStatus.SCRIPT_TIMEOUT);
        assertEquals(agentBean.getLast_err_no(), new Integer(100));
    }

    // Case 1.4: agent failed with retryable error, repeat!
    @Test
    public void test1Env1ReportRepeatOnRetryableError() throws Exception {
        EnvironBean envBean = genDefaultEnvBean();
        envs.put(envBean.getEnv_id(), envBean);

        PingReportBean report = genDefaultReport();
        report.setDeployStage(DeployStage.PRE_RESTART);
        report.setAgentStatus(AgentStatus.SCRIPT_FAILED);
        report.setErrorCode(100);
        reports.put(report.getEnvId(), report);

        GoalAnalyst analyst = new GoalAnalyst(null, null, "foo", "id-1", envs, reports, agents);
        analyst.analysis();

        assertEquals(analyst.getNeedUpdateAgents().size(), 1);
        assertEquals(analyst.getNeedDeleteAgentEnvIds().size(), 0);
        assertEquals(analyst.getInstallCandidates().size(), 1);
        GoalAnalyst.InstallCandidate candidate = analyst.getInstallCandidates().get(0);
        assertEquals(candidate.needWait, false);
        assertEquals(candidate.updateBean.getDeploy_stage(), DeployStage.PRE_RESTART);
        assertEquals(candidate.updateBean.getLast_err_no(), new Integer(100));
    }

    // Case 1.4: failed on current step, noneretryable, but agent RESET,
    // give it one more chance, start from beginning
    @Test
    public void test1Env1ReportStageFailButReset() throws Exception {
        EnvironBean envBean = genDefaultEnvBean();
        envs.put(envBean.getEnv_id(), envBean);

        PingReportBean report = genDefaultReport();
        report.setDeployStage(DeployStage.PRE_RESTART);
        report.setAgentStatus(AgentStatus.SCRIPT_TIMEOUT);
        report.setErrorCode(100);
        reports.put(report.getEnvId(), report);

        AgentBean agent = genDefaultAgent();
        agent.setState(AgentState.RESET);
        agents.put(agent.getEnv_id(), agent);

        GoalAnalyst analyst = new GoalAnalyst(null, null, "foo", "id-1", envs, reports, agents);
        analyst.analysis();

        assertEquals(analyst.getNeedUpdateAgents().size(), 1);
        assertEquals(analyst.getNeedDeleteAgentEnvIds().size(), 0);
        assertEquals(analyst.getInstallCandidates().size(), 1);
        GoalAnalyst.InstallCandidate candidate = analyst.getInstallCandidates().get(0);
        assertEquals(candidate.needWait, false);
        assertEquals(candidate.updateBean.getDeploy_stage(), DeployStage.PRE_DOWNLOAD);
        assertEquals(candidate.updateBean.getLast_err_no(), new Integer(0));
        assertEquals(candidate.updateBean.getState(), AgentState.NORMAL);
        assertEquals(candidate.updateBean.getStatus(), AgentStatus.UNKNOWN);
    }

    // Case 1.5: Different deploy, install the first step
    @Test
    public void test1Env1ReportNewDeploy() throws Exception {
        EnvironBean envBean = genDefaultEnvBean();
        envBean.setDeploy_id("bar");
        envs.put(envBean.getEnv_id(), envBean);

        PingReportBean report = genDefaultReport();
        report.setDeployStage(DeployStage.SERVING_BUILD);
        reports.put(report.getEnvId(), report);

        GoalAnalyst analyst = new GoalAnalyst(null, null, "foo", "id-1", envs, reports, agents);
        analyst.analysis();

        assertEquals(analyst.getNeedUpdateAgents().size(), 1);
        assertEquals(analyst.getNeedDeleteAgentEnvIds().size(), 0);
        assertEquals(analyst.getInstallCandidates().size(), 1);
        AgentBean agentBean = analyst.getInstallCandidates().get(0).updateBean;
        assertEquals(agentBean.getDeploy_id(), "bar");
        assertEquals(agentBean.getState(), AgentState.NORMAL);
        assertEquals(agentBean.getStatus(), AgentStatus.UNKNOWN);
        assertEquals(agentBean.getDeploy_stage(), DeployStage.PRE_DOWNLOAD);
    }

    // Case 1.5: Different deploy, install the first step, though currently failing or PAUSED
    @Test
    public void test1Env1ReportNewDeployThoughAgentPaused() throws Exception {
        EnvironBean envBean = genDefaultEnvBean();
        envBean.setDeploy_id("bar");
        envs.put(envBean.getEnv_id(), envBean);

        PingReportBean report = genDefaultReport();
        report.setDeployStage(DeployStage.PRE_RESTART);
        report.setAgentStatus(AgentStatus.TOO_MANY_RETRY);
        report.setErrorCode(100);
        reports.put(report.getEnvId(), report);

        AgentBean agent = genDefaultAgent();
        agent.setState(AgentState.PAUSED_BY_SYSTEM);
        agents.put(agent.getEnv_id(), agent);

        GoalAnalyst analyst = new GoalAnalyst(null, null, "foo", "id-1", envs, reports, agents);
        analyst.analysis();

        assertEquals(analyst.getNeedUpdateAgents().size(), 1);
        assertEquals(analyst.getNeedDeleteAgentEnvIds().size(), 0);
        assertEquals(analyst.getInstallCandidates().size(), 1);
        AgentBean agentBean = analyst.getInstallCandidates().get(0).updateBean;
        assertEquals(agentBean.getDeploy_id(), "bar");
        assertEquals(agentBean.getState(), AgentState.NORMAL);
        assertEquals(agentBean.getStatus(), AgentStatus.UNKNOWN);
        assertEquals(agentBean.getDeploy_stage(), DeployStage.PRE_DOWNLOAD);
    }

    // Case 2.1: no report, install the first step
    @Test
    public void test1Env0Report() throws Exception {
        EnvironBean envBean = genDefaultEnvBean();
        envs.put(envBean.getEnv_id(), envBean);

        GoalAnalyst analyst = new GoalAnalyst(null, null, "foo", "id-1", envs, reports, agents);
        analyst.analysis();

        assertEquals(analyst.getNeedUpdateAgents().size(), 0);
        assertEquals(analyst.getNeedDeleteAgentEnvIds().size(), 0);
        assertEquals(analyst.getInstallCandidates().size(), 1);
        AgentBean agentBean = analyst.getInstallCandidates().get(0).updateBean;
        assertEquals(agentBean.getStatus(), AgentStatus.UNKNOWN);
        assertEquals(agentBean.getDeploy_stage(), DeployStage.PRE_DOWNLOAD);
    }

    // Case 3.1: no env but has report, and agent need to delete
    @Test
    public void test1Report0EnvDelete() throws Exception {
        PingReportBean report = genDefaultReport();
        report.setDeployStage(DeployStage.SERVING_BUILD);
        reports.put(report.getEnvId(), report);

        AgentBean agent = genDefaultAgent();
        agents.put(report.getEnvId(), agent);

        GoalAnalyst analyst = new GoalAnalyst(null, null, "foo", "id-1", envs, reports, agents);
        analyst.analysis();

        assertEquals(analyst.getNeedUpdateAgents().size(), 1);
        assertEquals(analyst.getNeedDeleteAgentEnvIds().size(), 0);
        assertEquals(analyst.getInstallCandidates().size(), 0);
        assertEquals(analyst.getUninstallCandidates().size(), 1);
    }

    // Case 4.1: no report, no env but has agent, delete it
    @Test
    public void test0Report0Env() throws Exception {
        AgentBean agent = genDefaultAgent();
        agents.put(agent.getEnv_id(), agent);

        GoalAnalyst analyst = new GoalAnalyst(null, null, "foo", "id-1", envs, reports, agents);
        analyst.analysis();

        assertEquals(analyst.getNeedUpdateAgents().size(), 0);
        assertEquals(analyst.getNeedDeleteAgentEnvIds().size(), 1);
        assertEquals(analyst.getInstallCandidates().size(), 0);
    }

    // Rollback but report indicate not necessary since it is running on the rollback version
    @Test
    public void test1Report1EnvRollBack() throws Exception {
        EnvironBean envBean = genDefaultEnvBean();
        envBean.setDeploy_id("bar");
        envBean.setDeploy_type(DeployType.ROLLBACK);
        envs.put(envBean.getEnv_id(), envBean);

        PingReportBean report = genDefaultReport();
        reports.put(report.getEnvId(), report);

        DeployBean deployBean = new DeployBean();
        deployBean.setAlias("foo");
        DeployDAO deployDAO = Mockito.mock(DeployDAO.class);
        Mockito.when(deployDAO.getById("bar")).thenReturn(deployBean);
        GoalAnalyst analyst = new GoalAnalyst(deployDAO, null, "foo", "id-1", envs, reports, agents);
        analyst.analysis();
        Mockito.verify(deployDAO).getById("bar");

        assertEquals(analyst.getNeedUpdateAgents().size(), 1);
        assertEquals(analyst.getNeedDeleteAgentEnvIds().size(), 0);
        assertEquals(analyst.getInstallCandidates().size(), 0);
        AgentBean agentBean = analyst.getNeedUpdateAgents().get(envBean.getEnv_id());
        assertEquals(agentBean.getStatus(), AgentStatus.SUCCEEDED);
        assertEquals(agentBean.getDeploy_id(), "bar");
    }

    // Rollback, report indicate need new install
    @Test
    public void test1Report1EnvRollBack2() throws Exception {
        EnvironBean envBean = genDefaultEnvBean();
        envBean.setDeploy_id("bar");
        envBean.setDeploy_type(DeployType.ROLLBACK);
        envs.put(envBean.getEnv_id(), envBean);

        PingReportBean report = genDefaultReport();
        report.setDeployId("whatever");
        reports.put(report.getEnvId(), report);

        DeployBean deployBean = new DeployBean();
        deployBean.setAlias("foo");
        DeployDAO deployDAO = Mockito.mock(DeployDAO.class);
        Mockito.when(deployDAO.getById("bar")).thenReturn(deployBean);
        GoalAnalyst analyst = new GoalAnalyst(deployDAO, null, "foo", "id-1", envs, reports, agents);
        analyst.analysis();

        Mockito.verify(deployDAO).getById("bar");

        assertEquals(analyst.getNeedUpdateAgents().size(), 1);
        assertEquals(analyst.getNeedDeleteAgentEnvIds().size(), 0);
        assertEquals(analyst.getInstallCandidates().size(), 1);
        AgentBean agentBean = analyst.getNeedUpdateAgents().get(envBean.getEnv_id());
        assertEquals(agentBean.getStatus(), AgentStatus.SUCCEEDED);
        agentBean = analyst.getInstallCandidates().get(0).updateBean;
        assertEquals(agentBean.getStatus(), AgentStatus.UNKNOWN);
        assertEquals(agentBean.getDeploy_stage(), DeployStage.PRE_DOWNLOAD);
    }

    // test multiple envs/reports/agents
    @Test
    public void testNEnvsNReportsNAgents() throws Exception {

        // Case 0.1: e1 has no deploy yet
        EnvironBean envBean1 = genDefaultEnvBean();
        envBean1.setEnv_id("e1");
        envBean1.setDeploy_id(null);
        envs.put(envBean1.getEnv_id(), envBean1);
        PingReportBean report1 = genDefaultReport();
        report1.setEnvId("e1");
        report1.setDeployStage(DeployStage.PRE_RESTART);
        reports.put(report1.getEnvId(), report1);

        // Case 0.2: e2 is onhold
        EnvironBean envBean2 = genDefaultEnvBean();
        envBean2.setEnv_id("e2");
        envBean2.setEnv_state(EnvState.PAUSED);
        envs.put(envBean2.getEnv_id(), envBean2);
        PingReportBean report2 = genDefaultReport();
        report2.setEnvId("e2");
        report2.setDeployStage(DeployStage.PRE_RESTART);
        reports.put(report2.getEnvId(), report2);

        // Case 0.3: Agent is onhold (e3)
        EnvironBean envBean3 = genDefaultEnvBean();
        envBean3.setEnv_id("e3");
        envs.put(envBean3.getEnv_id(), envBean3);
        PingReportBean report3 = genDefaultReport();
        report3.setEnvId("e3");
        report3.setDeployStage(DeployStage.PRE_RESTART);
        reports.put(report3.getEnvId(), report3);
        AgentBean agent3 = genDefaultAgent();
        agent3.setEnv_id("e3");
        agent3.setState(AgentState.PAUSED_BY_USER);
        agents.put(envBean3.getEnv_id(), agent3);

        // Case 1.1: report4 is serving the build (e4)
        EnvironBean envBean4 = genDefaultEnvBean();
        envBean4.setEnv_id("e4");
        envs.put(envBean4.getEnv_id(), envBean4);
        PingReportBean report4 = genDefaultReport();
        report4.setEnvId("e4");
        reports.put(report4.getEnvId(), report4);

        // Case 1.2: agent5 proceed on next stage, candidate, hotfix
        EnvironBean envBean5 = genDefaultEnvBean();
        envBean5.setEnv_id("e5");
        envBean5.setDeploy_type(DeployType.HOTFIX);
        envs.put(envBean5.getEnv_id(), envBean5);
        PingReportBean report5 = genDefaultReport();
        report5.setEnvId("e5");
        report5.setDeployStage(DeployStage.PRE_RESTART);
        reports.put(report5.getEnvId(), report5);

        // Case 1.2: agent6 expects next stage, though agent is PAUSED, candidate
        EnvironBean envBean6 = genDefaultEnvBean();
        envBean6.setEnv_id("e6");
        envBean6.setDeploy_type(DeployType.ROLLBACK);
        envs.put(envBean6.getEnv_id(), envBean6);
        PingReportBean report6 = genDefaultReport();
        report6.setEnvId("e6");
        report6.setDeployStage(DeployStage.PRE_RESTART);
        reports.put(report6.getEnvId(), report6);
        AgentBean agent6 = genDefaultAgent();
        agent6.setEnv_id("e6");
        agent6.setState(AgentState.PAUSED_BY_SYSTEM);
        agents.put(agent6.getEnv_id(), agent6);

        // Case 1.3: failed on current step, agent paused, expecting none
        EnvironBean envBean7 = genDefaultEnvBean();
        envBean7.setEnv_id("e7");
        envs.put(envBean7.getEnv_id(), envBean7);
        PingReportBean report7 = genDefaultReport();
        report7.setEnvId("e7");
        report7.setDeployStage(DeployStage.PRE_RESTART);
        report7.setAgentStatus(AgentStatus.SCRIPT_FAILED);
        report7.setErrorCode(100);
        reports.put(report7.getEnvId(), report7);
        AgentBean agent7 = genDefaultAgent();
        agent7.setEnv_id("e7");
        agent7.setState(AgentState.PAUSED_BY_SYSTEM);
        agents.put(agent7.getEnv_id(), agent7);

        // Case 1.3: agent failed with nonretryable error, expecting none
        EnvironBean envBean8 = genDefaultEnvBean();
        envBean8.setEnv_id("e8");
        envs.put(envBean8.getEnv_id(), envBean8);
        PingReportBean report8 = genDefaultReport();
        report8.setEnvId("e8");
        report8.setDeployStage(DeployStage.PRE_RESTART);
        report8.setAgentStatus(AgentStatus.SCRIPT_TIMEOUT);
        report8.setErrorCode(100);
        reports.put(report8.getEnvId(), report8);

        // Case 1.4: agent failed with retryable error, repeat!, candidate
        EnvironBean envBean9 = genDefaultEnvBean();
        envBean9.setEnv_id("e9");
        envBean9.setPriority(DeployPriority.HIGHER);
        envs.put(envBean9.getEnv_id(), envBean9);
        PingReportBean report9 = genDefaultReport();
        report9.setEnvId("e9");
        report9.setDeployStage(DeployStage.PRE_RESTART);
        report9.setAgentStatus(AgentStatus.SCRIPT_FAILED);
        report9.setErrorCode(100);
        reports.put(report9.getEnvId(), report9);

        // Case 1.4: failed on current step, noneretryable, but agent RESET,
        // give it one more chance, candidate
        EnvironBean envBean11 = genDefaultEnvBean();
        envBean11.setEnv_id("e11");
        envBean11.setPriority(DeployPriority.NORMAL);
        envs.put(envBean11.getEnv_id(), envBean11);
        PingReportBean report11 = genDefaultReport();
        report11.setEnvId("e11");
        report11.setDeployStage(DeployStage.PRE_RESTART);
        report11.setAgentStatus(AgentStatus.SCRIPT_TIMEOUT);
        report11.setErrorCode(100);
        reports.put(report11.getEnvId(), report11);
        AgentBean agent11 = genDefaultAgent();
        agent11.setEnv_id("e11");
        agent11.setState(AgentState.RESET);
        agents.put(agent11.getEnv_id(), agent11);

        // Case 1.5: Different deploy, install the first step, candidate
        EnvironBean envBean22 = genDefaultEnvBean();
        envBean22.setEnv_id("e22");
        envBean22.setDeploy_id("bar");
        envBean22.setPriority(DeployPriority.LOWER);
        envs.put(envBean22.getEnv_id(), envBean22);
        PingReportBean report22 = genDefaultReport();
        report22.setEnvId("e22");
        report22.setDeployStage(DeployStage.SERVING_BUILD);
        reports.put(report22.getEnvId(), report22);

        // Case 1.5: Different deploy, install the first step, though currently
        // failing or PAUSED, candidate
        EnvironBean envBean33 = genDefaultEnvBean();
        envBean33.setEnv_id("e33");
        envBean33.setDeploy_id("bar");
        envBean33.setPriority(DeployPriority.HIGH);
        envs.put(envBean33.getEnv_id(), envBean33);
        PingReportBean report33 = genDefaultReport();
        report33.setEnvId("e33");
        report33.setDeployStage(DeployStage.PRE_RESTART);
        report33.setAgentStatus(AgentStatus.TOO_MANY_RETRY);
        report33.setErrorCode(100);
        reports.put(report33.getEnvId(), report33);
        AgentBean agent33 = genDefaultAgent();
        agent33.setEnv_id("e33");
        agent33.setState(AgentState.PAUSED_BY_SYSTEM);
        agents.put(agent33.getEnv_id(), agent33);

        // Case 2.1: no report, install the first step, candidate
        EnvironBean envBean44 = genDefaultEnvBean();
        envBean44.setEnv_id("e44");
        envBean44.setPriority(DeployPriority.LOW);
        envs.put(envBean44.getEnv_id(), envBean44);

        // Case 3.1: no env but has report, and agent is confirmed on uninstall, expect uninstall
        PingReportBean report55 = genDefaultReport();
        report55.setEnvId("e55");
        report55.setDeployStage(DeployStage.SERVING_BUILD);
        reports.put(report55.getEnvId(), report55);
        AgentBean agent55 = genDefaultAgent();
        agent55.setEnv_id("e55");
        agents.put(agent55.getEnv_id(), agent55);

        // Case 4.1: no report, no env but has agent, delete it
        AgentBean agent66 = genDefaultAgent();
        agent66.setEnv_id("e66");
        agents.put(agent66.getEnv_id(), agent66);

        GoalAnalyst analyst = new GoalAnalyst(null, null, "foo", "id-1", envs, reports, agents);
        analyst.analysis();

        assertEquals(analyst.getNeedUpdateAgents().size(), 13);
        assertEquals(analyst.getNeedDeleteAgentEnvIds().size(), 1);
        assertEquals(analyst.getInstallCandidates().size(), 7);

        // Making sure the candidates are sorted as expected
        List<GoalAnalyst.InstallCandidate> candidates = analyst.getInstallCandidates();
        assertEquals(candidates.get(0).env.getEnv_id(), "e5");
        assertEquals(candidates.get(1).env.getEnv_id(), "e6");
        assertEquals(candidates.get(2).env.getEnv_id(), "e9");
        assertEquals(candidates.get(3).env.getEnv_id(), "e33");
        assertEquals(candidates.get(4).env.getEnv_id(), "e11");
        assertEquals(candidates.get(5).env.getEnv_id(), "e44");
        assertEquals(candidates.get(6).env.getEnv_id(), "e22");
    }

    @Test
    public void testDeployPriority() throws Exception {

        EnvironBean envBean1 = genDefaultEnvBean();
        envBean1.setEnv_id("e1");
        envBean1.setDeploy_id("DeployA");
        envBean1.setDeploy_type(DeployType.REGULAR);
        envBean1.setPriority(DeployPriority.HIGHER);
        envs.put(envBean1.getEnv_id(), envBean1);

        EnvironBean envBean2 = genDefaultEnvBean();
        envBean2.setEnv_id("e2");
        envBean2.setDeploy_id("DeployB");
        envBean2.setDeploy_type(DeployType.REGULAR);
        envBean2.setPriority(DeployPriority.NORMAL);
        envs.put(envBean2.getEnv_id(), envBean2);

        EnvironBean envBean3 = genDefaultEnvBean();
        envBean3.setEnv_id("e3");
        envBean3.setDeploy_type(DeployType.REGULAR);
        envBean3.setPriority(DeployPriority.LOWER);
        envs.put(envBean3.getEnv_id(), envBean3);

        AgentBean agent1 = genDefaultAgent();
        agent1.setEnv_id("e3");
        agents.put(agent1.getEnv_id(), agent1);
        agent1.setFirst_deploy(true);
        GoalAnalyst analyst = new GoalAnalyst(null, null, "foo", "id-1", envs, reports, agents);
        analyst.analysis();

        // Making sure the candidates are sorted as expected
        List<GoalAnalyst.InstallCandidate> candidates = analyst.getInstallCandidates();
        assertEquals(candidates.get(0).env.getEnv_id(), "e1");
        assertEquals(candidates.get(1).env.getEnv_id(), "e2");
        assertEquals(candidates.get(2).env.getEnv_id(), "e3");

        envBean3.setDeploy_type(DeployType.ROLLBACK);
        analyst = new GoalAnalyst(null, null, "foo", "id-1", envs, reports, agents);
        analyst.analysis();

        //First deploy
        candidates = analyst.getInstallCandidates();
        assertEquals(candidates.get(0).env.getEnv_id(), "e1");
        assertEquals(candidates.get(1).env.getEnv_id(), "e2");
        assertEquals(candidates.get(2).env.getEnv_id(), "e3");

        agent1.setFirst_deploy(false);

        analyst = new GoalAnalyst(null, null, "foo", "id-1", envs, reports, agents);
        analyst.analysis();
        candidates = analyst.getInstallCandidates();
        assertEquals(candidates.get(0).env.getEnv_id(), "e3");
        assertEquals(candidates.get(1).env.getEnv_id(), "e1");
        assertEquals(candidates.get(2).env.getEnv_id(), "e2");
    }

    @Test
    public void testNEnvsNReportsNStoppingAgents() throws Exception {
        EnvironBean envBean1 = genDefaultEnvBean();
        envBean1.setEnv_id("e1");
        envBean1.setPriority(DeployPriority.HIGH);
        envs.put(envBean1.getEnv_id(), envBean1);

        PingReportBean report1 = genDefaultReport();
        report1.setEnvId("e1");
        report1.setDeployStage(DeployStage.PRE_RESTART);
        reports.put(report1.getEnvId(), report1);

        AgentBean agent1 = genDefaultAgent();
        agent1.setEnv_id("e1");
        agent1.setState(AgentState.STOP);
        agents.put(envBean1.getEnv_id(), agent1);

        EnvironBean envBean2 = genDefaultEnvBean();
        envBean2.setEnv_id("e2");
        envBean2.setPriority(DeployPriority.LOW);
        envs.put(envBean2.getEnv_id(), envBean2);

        PingReportBean report2 = genDefaultReport();
        report2.setEnvId("e2");
        report2.setDeployStage(DeployStage.SERVING_BUILD);
        reports.put(report2.getEnvId(), report2);

        AgentBean agent2 = genDefaultAgent();
        agent2.setEnv_id("e2");
        agent2.setState(AgentState.STOP);
        agents.put(envBean2.getEnv_id(), agent2);

        EnvironBean envBean3 = genDefaultEnvBean();
        envBean3.setEnv_id("e3");
        envBean3.setPriority(DeployPriority.HIGHER);
        envs.put(envBean3.getEnv_id(), envBean3);

        PingReportBean report3 = genDefaultReport();
        report3.setEnvId("e3");
        report3.setDeployStage(DeployStage.STOPPING);
        reports.put(report3.getEnvId(), report3);

        AgentBean agent3 = genDefaultAgent();
        agent3.setEnv_id("e3");
        agent3.setState(AgentState.STOP);
        agents.put(agent3.getEnv_id(), agent3);

        EnvironBean envBean22 = genDefaultEnvBean();
        envBean22.setEnv_id("e22");
        envBean22.setPriority(DeployPriority.NORMAL);
        envs.put(envBean22.getEnv_id(), envBean22);

        PingReportBean report22 = genDefaultReport();
        report22.setEnvId("e22");
        report22.setDeployStage(DeployStage.POST_RESTART);
        reports.put(report22.getEnvId(), report22);

        AgentBean agent22 = genDefaultAgent();
        agent22.setEnv_id("e22");
        agents.put(agent22.getEnv_id(), agent22);

        EnvironBean envBean23 = genDefaultEnvBean();
        envBean23.setEnv_id("e23");
        envBean23.setPriority(DeployPriority.LOWER);
        envs.put(envBean23.getEnv_id(), envBean23);

        PingReportBean report23 = genDefaultReport();
        report23.setEnvId("e23");
        report23.setDeployStage(DeployStage.RESTARTING);
        reports.put(report23.getEnvId(), report23);

        AgentBean agent23 = genDefaultAgent();
        agent23.setEnv_id("e23");
        agents.put(agent23.getEnv_id(), agent23);

        EnvironBean envBean24 = genDefaultEnvBean();
        envBean24.setEnv_id("e24");
        envBean24.setPriority(DeployPriority.HIGHER);
        envs.put(envBean24.getEnv_id(), envBean24);

        PingReportBean report24 = genDefaultReport();
        report24.setEnvId("e24");
        report24.setDeployStage(DeployStage.POST_DOWNLOAD);
        reports.put(report24.getEnvId(), report24);

        AgentBean agent24 = genDefaultAgent();
        agent24.setEnv_id("e24");
        agents.put(agent24.getEnv_id(), agent24);

        GoalAnalyst analyst = new GoalAnalyst(null, null, "foo", "id-1", envs, reports, agents);
        analyst.analysis();
        assertEquals(analyst.getInstallCandidates().size(), 6);

        List<GoalAnalyst.InstallCandidate> candidates = analyst.getInstallCandidates();
        assertEquals(candidates.get(0).env.getEnv_id(), "e2");
        assertEquals(candidates.get(1).env.getEnv_id(), "e1");
        assertEquals(candidates.get(2).env.getEnv_id(), "e3");
        assertEquals(candidates.get(3).env.getEnv_id(), "e24");
        assertEquals(candidates.get(4).env.getEnv_id(), "e22");
        assertEquals(candidates.get(5).env.getEnv_id(), "e23");
    }

    @Test
    public void testStoppingStage() throws Exception {
        EnvironBean environBean = genDefaultEnvBean();
        envs.put(environBean.getEnv_id(), environBean);
        AgentBean agentBean = genDefaultAgent();
        agentBean.setState(AgentState.STOP);
        agents.put(environBean.getEnv_id(), agentBean);
        PingReportBean report = genDefaultReport();
        reports.put(report.getEnvId(), report);

        GoalAnalyst analyst = new GoalAnalyst(null, null, "foo", "id-1", envs, reports, agents);
        analyst.analysis();

        assertEquals(analyst.getNeedUpdateAgents().size(), 1);
        assertEquals(analyst.getNeedDeleteAgentEnvIds().size(), 0);
        assertEquals(analyst.getInstallCandidates().size(), 1);
        GoalAnalyst.InstallCandidate candidate = analyst.getInstallCandidates().get(0);
        assertEquals(candidate.needWait, false);
        assertEquals(candidate.updateBean.getDeploy_stage(), DeployStage.STOPPING);
        assertEquals(candidate.updateBean.getState(), AgentState.STOP);
        assertEquals(candidate.updateBean.getStatus(), AgentStatus.UNKNOWN);
        assertEquals(candidate.updateBean.getLast_err_no(), new Integer(0));
    }

    @Test
    public void testStoppedStage() throws Exception {
        EnvironBean environBean = genDefaultEnvBean();
        envs.put(environBean.getEnv_id(), environBean);
        AgentBean agentBean = genDefaultAgent();
        agentBean.setState(AgentState.STOP);
        agentBean.setDeploy_stage(DeployStage.STOPPING);
        agents.put(environBean.getEnv_id(), agentBean);
        PingReportBean report = genDefaultReport();
        reports.put(report.getEnvId(), report);

        GoalAnalyst analyst = new GoalAnalyst(null, null, "foo", "id-1", envs, reports, agents);
        analyst.analysis();

        assertEquals(analyst.getNeedUpdateAgents().size(), 1);
        assertEquals(analyst.getNeedDeleteAgentEnvIds().size(), 0);
        assertEquals(analyst.getInstallCandidates().size(), 1);
        GoalAnalyst.InstallCandidate candidate = analyst.getInstallCandidates().get(0);
        assertEquals(candidate.needWait, false);
        assertEquals(candidate.updateBean.getDeploy_stage(), DeployStage.STOPPED);
        assertEquals(candidate.updateBean.getState(), AgentState.STOP);
        assertEquals(candidate.updateBean.getStatus(), AgentStatus.UNKNOWN);
        assertEquals(candidate.updateBean.getLast_err_no(), new Integer(0));
    }

    @Test
    public void testFianlStopStage() throws Exception {
        EnvironBean environBean = genDefaultEnvBean();
        envs.put(environBean.getEnv_id(), environBean);
        AgentBean agentBean = genDefaultAgent();
        agentBean.setState(AgentState.STOP);
        agentBean.setDeploy_stage(DeployStage.STOPPED);
        agents.put(environBean.getEnv_id(), agentBean);
        PingReportBean report = genDefaultReport();
        reports.put(report.getEnvId(), report);

        GoalAnalyst analyst = new GoalAnalyst(null, null, "foo", "id-1", envs, reports, agents);
        analyst.analysis();

        assertEquals(analyst.getNeedUpdateAgents().size(), 1);
        AgentBean needUpdateAgents = analyst.getNeedUpdateAgents().get(environBean.getEnv_id());
        assertEquals(needUpdateAgents.getState(), AgentState.STOP);
        assertEquals(analyst.getNeedDeleteAgentEnvIds().size(), 0);
        assertEquals(analyst.getInstallCandidates().size(), 0);
    }
}
