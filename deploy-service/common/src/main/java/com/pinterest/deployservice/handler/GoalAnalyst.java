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

import com.pinterest.deployservice.bean.*;
import com.pinterest.deployservice.common.Constants;
import com.pinterest.deployservice.common.StateMachines;
import com.pinterest.deployservice.dao.DeployDAO;
import com.pinterest.deployservice.dao.EnvironDAO;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class GoalAnalyst {
    private static final Logger LOG = LoggerFactory.getLogger(GoalAnalyst.class);
    // Define lower value for hotfix and rollback priority to make sure they deploy first
    // Notice hotfix and rollback priority should still lower than system service priority
    private static final int HOT_FIX_PRIORITY = DeployPriority.HIGHER.getValue() - 20;
    private static final int ROLL_BACK_PRIORITY = DeployPriority.HIGHER.getValue() - 10;

    private String host;
    private String host_id;
    private DeployDAO deployDAO;

    // input maps, all keyed by envId
    private Map<String, EnvironBean> envs;
    private Map<String, PingReportBean> reports;
    private Map<String, AgentBean> agents;

    // env for all existing agent
    private Map<String, EnvironBean> existingAgentEnv = new HashMap<>();

    // env name set for all existing agent environment name
    private Set<String> existingAgentEnvNames = new HashSet<>();

    // Agents need to be updated, largely based on reports
    private Map<String, AgentBean> needUpdateAgents = new HashMap<>();

    // Agent error message
    private Map<String, String> errorMessages = new HashMap<>();

    // Agents need to be deleted, due to the missing reports
    private List<String> needDeleteAgentEnvIds = new ArrayList<>();

    // Install candidates, if chosen, need to removed it from needUpdateAgents
    private List<InstallCandidate> installCandidates = new ArrayList<>();

    // Uninstall candidates
    private List<UninstallCandidate> uninstallCandidates = new ArrayList<>();

    private static final Set<AgentStatus> FATAL_AGENT_STATUSES = new HashSet<>();

    static {
        FATAL_AGENT_STATUSES.add(AgentStatus.SCRIPT_TIMEOUT);
        FATAL_AGENT_STATUSES.add(AgentStatus.TOO_MANY_RETRY);
        FATAL_AGENT_STATUSES.add(AgentStatus.AGENT_FAILED);
        FATAL_AGENT_STATUSES.add(AgentStatus.RUNTIME_MISMATCH);
    }

    public class InstallCandidate implements Comparable<InstallCandidate> {
        // The candidate env used to generate new deploy goal
        EnvironBean env;
        // If need to wait for deploy
        boolean needWait;
        // If this candidate chosen, then use this update agent bean instead to update agent record
        AgentBean updateBean;
        // Original report
        PingReportBean report;

        InstallCandidate(EnvironBean env, boolean needWait, AgentBean updateBean, PingReportBean report) {
            this.env = env;
            this.needWait = needWait;
            this.updateBean = updateBean;
            this.report = report;
        }

        int getDeployPriority(EnvironBean env, Boolean firstDeploy) {
            if (env.getDeploy_type() == null) {
              return DeployPriority.NORMAL.getValue();
            } else {
                // System level deploy, or sidecar service deploy mostly, will use dedicated system
                // priority. Notice for system level deploy, we disregard the priorities of hotfix
                // or rollback.
                Integer systemPriority = env.getSystem_priority();
                if (systemPriority != null) {
                    return systemPriority;
                }

                if (firstDeploy != null && firstDeploy) {
                    //First deploy should always use the setting priority.
                    return env.getPriority().getValue();
                } else {
                    DeployType deployType = env.getDeploy_type();
                    if (deployType == DeployType.HOTFIX) {
                        return HOT_FIX_PRIORITY;
                    } else if (deployType == DeployType.ROLLBACK) {
                        return ROLL_BACK_PRIORITY;
                    } else {
                        return env.getPriority().getValue();
                    }
                }
            }
        }

        @Override
        public int compareTo(InstallCandidate installCandidate) {
            int priority1 = getDeployPriority(env, updateBean.getFirst_deploy() );
            int priority2 = getDeployPriority(installCandidate.env, updateBean.getFirst_deploy());
            // STOP has higher priority. If the agent state is STOP, reverse the priority
            if (updateBean.getState() == AgentState.STOP && installCandidate.updateBean.getState() == AgentState.STOP) {
                return priority2 - priority1;
            } else if (updateBean.getState() == AgentState.STOP) {
                return -1;
            } else if (installCandidate.updateBean.getState() == AgentState.STOP) {
                return 1;
            }

            if (priority1 == priority2) {
                // If same priority, choose the one does not need to wait, or in the middle of deploying already
                if (needWait && !installCandidate.needWait) {
                    return 1;
                } else if (!needWait && installCandidate.needWait) {
                    return -1;
                } else {
                    return 0;
                }
            }
            return priority1 - priority2;
        }

        @Override
        public String toString() {
            return "InstallCandidate{" +
                "env=" + env +
                ", checkNeedWait=" + needWait +
                ", updateBean=" + updateBean +
                '}';
        }
    }

    public class UninstallCandidate implements Comparable<UninstallCandidate> {
        AgentBean updateBean;
        PingReportBean report;
        EnvironBean environ;

        UninstallCandidate(AgentBean updateBean, PingReportBean report) {
            this.report = report;
            this.updateBean = updateBean;
            environ = existingAgentEnv.getOrDefault(report.getEnvId(), new EnvironBean());
        }

        @Override
        public int compareTo(UninstallCandidate uninstallCandidate) {
            return 0;
        }

        @Override
        public String toString() {
            return "UninstallCandidate{" +
                "report=" + report +
                ", env=" + environ +
                '}';
        }
    }

    GoalAnalyst(DeployDAO deployDAO, EnvironDAO environDAO, String host, String host_id, Map<String, EnvironBean> envs, Map<String, PingReportBean> reports, Map<String, AgentBean> agents) {
        this.deployDAO = deployDAO;
        this.host = host;
        this.host_id = host_id;
        this.envs = envs;
        this.reports = reports;
        this.agents = agents;

        for (Map.Entry<String, AgentBean> entry : agents.entrySet()) {
            try {
                String envId = entry.getKey();
                EnvironBean environBean = envs.get(envId);
                if (environBean == null) {
                    environBean = environDAO.getById(envId);
                }
                if (environBean == null) {
                    continue;
                }

                existingAgentEnv.put(envId, environBean);
                existingAgentEnvNames.add(environBean.getEnv_name());
            } catch (Exception ex) {
                LOG.error("Failed to obtain environment info:", ex);
            }
        }
    }

    public Map<String, AgentBean> getNeedUpdateAgents() {
        return needUpdateAgents;
    }

    public List<String> getNeedDeleteAgentEnvIds() {
        return needDeleteAgentEnvIds;
    }

    public List<InstallCandidate> getInstallCandidates() {
        return installCandidates;
    }

    public List<UninstallCandidate> getUninstallCandidates() {
        return uninstallCandidates;
    }

    public Map<String, String> getErrorMessages() {
        return errorMessages;
    }

    private DeployStage getNextStage(DeployType deployType, DeployStage currStage) {
        return StateMachines.DEPLOY_TYPE_TRANSITION_MAP.get(deployType).get(currStage);
    }

    // What the new agent state should be, if this agent is not chosen to be deploy goal
    AgentState proposeNewAgentState(PingReportBean report, AgentBean agent) {
        AgentStatus status = report.getAgentStatus();
        if (agent != null && agent.getState() == AgentState.STOP) {
            // agent has been explicitly STOP, do not override
            if (agent.getDeploy_stage() == DeployStage.STOPPING && FATAL_AGENT_STATUSES.contains(status)) {
                LOG.debug("Agent DeployStage is STOPPING, but report status is {}, propose new agent state as {} ", status, AgentState.PAUSED_BY_SYSTEM);
                return AgentState.PAUSED_BY_SYSTEM;
            }
            return AgentState.STOP;
        }

        if (agent != null && agent.getState() == AgentState.PAUSED_BY_USER) {
            // agent has been explicitly paused by user, do not override
            return AgentState.PAUSED_BY_USER;
        }

        if (agent != null && agent.getState() == AgentState.RESET) {
            // agent has been explicitly reset by user, do not override
            // subsequent code would have to decide if need to override RESET state
            return AgentState.RESET;
        }

        if (status == AgentStatus.SUCCEEDED) {
            // Override current agent status record
            LOG.debug("Report status is {}, propose new agent state as {} ",
                status, AgentState.NORMAL);
            return AgentState.NORMAL;
        }

        if (FATAL_AGENT_STATUSES.contains(status)) {
            // Fatal error, pause agent deploy
            LOG.debug("Report status is {}, propose new agent state as {} ", status, AgentState.PAUSED_BY_SYSTEM);
            return AgentState.PAUSED_BY_SYSTEM;
        }

        // Otherwise, return NORMAL
        if (agent != null) {
            LOG.debug("Report status is {}, reuse the old agent state {} ", status, agent.getState());
            return agent.getState();
        } else {
            LOG.debug("Report status is {}, and we do not have agent record, use state {} ", status, AgentState.NORMAL);
            return AgentState.NORMAL;
        }
    }

    boolean shouldUpdateAgentRecord(AgentBean origBean, AgentBean updateBean) {
        
        if (origBean == null || updateBean == null) {
            return true;
        }
        if (origBean.getHost_id() != null && origBean.getHost_id().equals(host_id) && 
            origBean.getDeploy_id() != null && origBean.getDeploy_id().equals(updateBean.getDeploy_id()) &&
            origBean.getEnv_id() != null && origBean.getEnv_id().equals(updateBean.getEnv_id()) && 
            origBean.getFail_count() != null && origBean.getFail_count().equals(updateBean.getFail_count()) &&
            origBean.getStatus() != null && origBean.getStatus().equals(updateBean.getStatus()) && 
            origBean.getLast_err_no() != null && origBean.getLast_err_no().equals(updateBean.getLast_err_no()) &&
            origBean.getState() != null && origBean.getState().equals(updateBean.getState()) && 
            origBean.getDeploy_stage() != null && origBean.getDeploy_stage().equals(updateBean.getDeploy_stage())) {
            LOG.debug("Skip updating agent record for env_id {}, deploy_id {} on host {}",
                    origBean.getEnv_id(), origBean.getDeploy_id(), origBean.getHost_id());
            return false;
        }
        LOG.info("Agent record for env_id {}, deploy_id {} on host {} needs update",
                origBean.getEnv_id(), origBean.getDeploy_id(), origBean.getHost_id());
        return true;
    }

    // Generate new agent bean based on the report & current agent record,
    // We populate all the fields, since this could be used for insertOrUpdate as well
    AgentBean genUpdateBeanByReport(PingReportBean report, AgentBean agent) {
        // We generate complete bean in case we need to insertOrUpdate it into agents table
        AgentBean updateBean = new AgentBean();
        updateBean.setHost_name(host);
        updateBean.setHost_id(host_id);
        updateBean.setDeploy_id(report.getDeployId());
        updateBean.setEnv_id(report.getEnvId());
        updateBean.setLast_update(System.currentTimeMillis());
        updateBean.setLast_operator(Constants.SYSTEM_OPERATOR);
        updateBean.setFail_count(report.getFailCount());
        updateBean.setStatus(report.getAgentStatus());
        updateBean.setLast_err_no(report.getErrorCode());
        updateBean.setState(proposeNewAgentState(report, agent));
        updateBean.setStage_start_date(System.currentTimeMillis());
        updateBean.setDeploy_stage(report.getDeployStage());

        if (agent == null) {
            // if agent is missing in agent table, treat it as not first_deploy.
            updateBean.setFirst_deploy(false);
            updateBean.setStart_date(System.currentTimeMillis());
        } else {
            updateBean.setFirst_deploy(agent.getFirst_deploy());
            updateBean.setStart_date(agent.getStart_date());
        }

        if (report.getDeployStage() == DeployStage.SERVING_BUILD) {
            // turn off first deploy flag
            updateBean.setFirst_deploy(false);
            updateBean.setFirst_deploy_time(System.currentTimeMillis());
        }

        // TODO record error message as well if errorCode != 0
        return updateBean;
    }

    // Generate new agent bean based on the report & current agent record,
    // This is intended to be used for deploy goal to install next stage
    AgentBean genNextStageUpdateBean(EnvironBean env, PingReportBean report, AgentBean agent) {
        AgentBean agentBean = genUpdateBeanByReport(report, agent);
        // TODO optimize this, next stage could be skipped sometime
        agentBean.setDeploy_stage(getNextStage(env.getDeploy_type(), report.getDeployStage()));
        agentBean.setStatus(AgentStatus.UNKNOWN);
        agentBean.setState(AgentState.NORMAL);
        agentBean.setLast_err_no(0);
        agentBean.setFail_count(0);
        agentBean.setLast_operator(Constants.SYSTEM_OPERATOR);
        long now = System.currentTimeMillis();
        agentBean.setStage_start_date(now);
        agentBean.setLast_update(now);
        return agentBean;
    }

    // Generate new agent bean based on the env only,
    // This is intended to be used for deploy goal to install from first stage
    AgentBean genNewUpdateBean(EnvironBean env, AgentBean agent) {
        AgentBean agentBean = new AgentBean();
        agentBean.setHost_name(host);
        agentBean.setHost_id(host_id);
        agentBean.setDeploy_id(env.getDeploy_id());
        agentBean.setEnv_id(env.getEnv_id());
        // TODO optimize this, next stage could be skipped sometime
        agentBean.setDeploy_stage(getNextStage(env.getDeploy_type(), DeployStage.UNKNOWN));
        agentBean.setStatus(AgentStatus.UNKNOWN);
        agentBean.setState(AgentState.NORMAL);
        agentBean.setLast_operator(Constants.SYSTEM_OPERATOR);
        long now = System.currentTimeMillis();
        agentBean.setStart_date(now);
        agentBean.setLast_update(now);
        agentBean.setLast_err_no(0);
        agentBean.setFail_count(0);
        agentBean.setStage_start_date(now);
        // agent does not exist, and the existing agent does not have the same env name as current environment
        if (agent == null && (!existingAgentEnvNames.contains(env.getEnv_name()))) {
            // both agent and report are null, this should be the first deploy
            agentBean.setFirst_deploy(true);
        } else {
            boolean firstDeploy = (agent == null ? false : agent.getFirst_deploy());
            // agent is not null. the first_deploy flag should be same as the record in the database
            agentBean.setFirst_deploy(firstDeploy);
        }
        return agentBean;
    }

    // Generate default stop agent bean
    AgentBean genAgentStopBean(EnvironBean env, AgentBean agent) {
        AgentBean agentBean = new AgentBean();
        agentBean.setHost_name(host);
        agentBean.setHost_id(host_id);
        agentBean.setDeploy_id(env.getDeploy_id());
        agentBean.setEnv_id(env.getEnv_id());
        agentBean.setDeploy_stage(DeployStage.STOPPING);
        agentBean.setStatus(AgentStatus.UNKNOWN);
        agentBean.setState(AgentState.STOP);
        agentBean.setLast_operator(Constants.SYSTEM_OPERATOR);
        long now = System.currentTimeMillis();
        agentBean.setStart_date(now);
        agentBean.setLast_update(now);
        agentBean.setLast_err_no(0);
        agentBean.setFail_count(0);
        agentBean.setStage_start_date(now);
        agentBean.setFirst_deploy(agent.getFirst_deploy());
        return agentBean;
    }

    // Generate next stop stage agent bean
    AgentBean genNextStopStageBean(PingReportBean report, AgentBean agent) {
        AgentBean agentBean = genUpdateBeanByReport(report, agent);
        agentBean.setState(AgentState.STOP);
        agentBean.setDeploy_stage(getNextStage(DeployType.STOP, agent.getDeploy_stage()));
        agentBean.setStatus(AgentStatus.UNKNOWN);
        agentBean.setLast_err_no(0);
        agentBean.setFail_count(0);
        agentBean.setLast_operator(Constants.SYSTEM_OPERATOR);
        long now = System.currentTimeMillis();
        agentBean.setStage_start_date(now);
        agentBean.setLast_update(now);
        return agentBean;
    }

    // This is a special case when the deploy in report is actually the deploy we are
    // currently rolling back to. In this case, we should continue to have the agent
    // work on its current step. We achieve this by replacing the report deployId with
    // the deployId we are rolling back to. So the rest of the code on serverside will
    // just treat this report as if it was working the new deployId. Keep in mind that
    // when we generate return deploy goal, we would have to flip the alias and deployid
    // again - we want this process to be transparent to the agent
    void transformRollbackDeployId(EnvironBean env, PingReportBean report, AgentBean updateBean) throws Exception {
        if (env.getDeploy_id().equals(report.getDeployId())) {
            // We have a matched deploy, nothing to change
            return;
        }

        if (env.getDeploy_type() != DeployType.ROLLBACK) {
            // Not a rollback, nothing to change
            return;
        }

        DeployBean deployBean = deployDAO.getById(env.getDeploy_id());
        String alias = deployBean.getAlias();
        if (report.getDeployId().equals(alias)) {
            LOG.debug("Reported deploy {} is the alias of deploy {}, use {} instead.",
                report.getDeployId(), env.getDeploy_id(), env.getDeploy_id());
            report.setDeployId(env.getDeploy_id());
            report.setDeployAlias(alias);
            updateBean.setDeploy_id(env.getDeploy_id());
        }
    }

    void installNewUpdateBean(EnvironBean env, PingReportBean report, AgentBean agent) {
        AgentBean newUpdateBean = genNewUpdateBean(env, agent);
        boolean needWait = (report.getDeployStage() == DeployStage.SERVING_BUILD);
        if (needWait) {
            // A special case when even report suggests wait, for agent record disagree
            if (agent != null && agent.getDeploy_stage() != DeployStage.SERVING_BUILD) {
                needWait = false;
            }
        }
        installCandidates.add(new InstallCandidate(env, needWait, newUpdateBean, report));
        return;
    }

    boolean isFirstDeploy(AgentBean agent, EnvironBean env) {
        // agent does not exist, and the existing agent does not have the same env name as current environment
        if (agent == null && (!existingAgentEnvNames.contains(env.getEnv_name()))) {
            // both agent and report are null, this should be the first deploy
            return true;
        } else {
            // agent is not null. the first_deploy flag should be same as the record in the database
            return (agent == null ? false : agent.getFirst_deploy());
        }
    }

    /**
     * Compute suggested next step based on current env deploy, report deploy and agent status
     */
    void process(String envId, EnvironBean env, PingReportBean report, AgentBean agent) throws Exception {
        LOG.debug("GoalAnalyst process env {}, report = {}, env = {} and agent = {}.", envId, report, env, agent);

        /**
         * Generate updateBean based on report, we need to update the agent based on report in the end,
         * regardless if this agent report is chosen as goal or not
         */
        AgentBean updateBean = null;
        if (report != null) {
            updateBean = genUpdateBeanByReport(report, agent);
            if (!StringUtils.isEmpty(report.getEnvId()) && updateBean != null &&
                shouldUpdateAgentRecord(agent, updateBean) == true) {
                // Only record this in agent table when there is env, otherwise,
                // we do not know which env it belongs to
                needUpdateAgents.put(envId, updateBean);
                if (report.getErrorMessage() != null) {
                    errorMessages.put(envId, report.getErrorMessage());
                }
            }
        }

        /**
         * Case 0.1: Env has no deploy yet, do not update agent record and return immediately
         */
        if (env != null && env.getDeploy_id() == null) {
            if (report == null) {
                LOG.debug("GoalAnalyst case 0.1.1 - env {} had no deploy yet, not a goal candidate.", env.getEnv_id());
            } else {
                LOG.error("GoalAnalyst case 0.1.2 - env {} had no deploy yet, but has report = {}! not a goal candidate.", env, report);
            }
            return;
        }

        /**
         * Case 0.2: Env is onhold, update agent record and return immediately
         */
        if (env != null && !StateMachines.ENV_DEPLOY_STATES.contains(env.getEnv_state()) &&
            (isFirstDeploy(agent, env) == false)) {
            LOG.debug("GoalAnalyst case 0.2 - env {} is onhold, not a goal candidate.", envId);
            return;
        }

        /**
         * Case 0.3: Agent is onhold by users for this env explicitly, return immediately
         */
        if (agent != null && agent.getState() == AgentState.PAUSED_BY_USER) {
            LOG.debug("GoalAnalyst case 0.3 - agent {} for env {} is onhold by users, not a goal candidate.",
                agent.getHost_name(), envId);
            return;
        }

        /**
         * Case 0.4: Agent is STOP
         */
        if (agent != null && agent.getState() == AgentState.STOP) {
            /**
             * Case 0.4.1: STOP step completed or status is fatal, send NOOP
             */
            if (agent.getDeploy_stage() == DeployStage.STOPPED) {
                LOG.debug("GoalAnalyst - AgentState is STOP, and host {} already in STOPPED stage, not a goal candidate", host);
                return;
            }

            if (env != null) {
                /**
                 * Case 0.4.2: If a certain agent is in STOP state and not in STOP deploy stage,
                 * Send OpCode STOP to agent immediately
                 */
                if (agent.getDeploy_stage() != DeployStage.STOPPING) {
                    AgentBean newStopBean = genAgentStopBean(env, agent);
                    installCandidates.add(new InstallCandidate(env, false, newStopBean, report));
                    LOG.debug("GoalAnalyst - AgentState is STOP and DeployStage is {}, host {} start to gracefully shut down the service for env {}", agent.getDeploy_stage(), host, envId);
                    return;
                } else {
                    if (report != null) {
                        if (FATAL_AGENT_STATUSES.contains(report.getAgentStatus())) {
                            LOG.debug("GoalAnalyst - AgentState is STOP and DeployStage is STOPPING, but host {} is in {} status, not a goal candidate", host, report.getAgentStatus());
                            return;
                        }

                        /**
                         * Case 0.4.3: If agent status SUCCEEDED, move stage to STOPPED, and send NOOP
                         * If not succeeded, retry
                         */
                        if (report.getAgentStatus() == AgentStatus.SUCCEEDED) {
                            LOG.debug("GoalAnalyst - host {} STOP successfully, move agent deploy stage to STOPPED", host);
                            AgentBean nextStopBean = genNextStopStageBean(report, agent);
                            installCandidates.add(new InstallCandidate(env, false, nextStopBean, report));
                        } else {
                            LOG.debug("GoalAnalyst - host {} did not complete gracefully shut down yet", host);
                            installCandidates.add(new InstallCandidate(env, false, agent, report));
                        }
                    }
                    return;
                }
            }
        }

        /**
         * Case 1: Both report & env are valid
         */
        if (env != null && report != null) {
            // In case the current deploy is a rollback, & reported deploy happens to be the
            // deploy we are rollbacking to, need to user deploy alias instead
            transformRollbackDeployId(env, report, updateBean);
            AgentBean newUpdateBean;

            if (env.getDeploy_id().equals(report.getDeployId())) {
                // Special case when agent state is RESET, start from beginning
                if (updateBean.getState() == AgentState.RESET) {
                    installNewUpdateBean(env, report, agent);
                    LOG.debug("GoalAnalyst case 1.0 - host {} work on the same deploy {}, but agent state is RESET, set env {} as a goal candidate and start from beginning.",
                        host, env.getDeploy_id(), envId);
                    return;
                }

                if (report.getDeployStage() == DeployStage.SERVING_BUILD) {
                    /**
                     * Case 1.1: Report matches env, and deployStage is SERVING_BUILD, e.g agent serves the correct
                     * deploy. This is the most common case for ping, not an install candidate, return immediately
                     */
                    LOG.debug("GoalAnalyst case 1.1 - host {} is serving correct deploy {} for env {}, not a goal candidate.",
                        host, env.getDeploy_id(), env.getEnv_id());
                    return;
                }

                if (report.getAgentStatus() == AgentStatus.SUCCEEDED) {
                    /**
                     * Case 1.2: Report matches env, but deployStage is NOT SERVING_BUILD. A potential deploy candidate.
                     * This is the typical agent/server conversation to install a new deploy in different stages.
                     * There are 2 cases, agent proceed to next stage when succeeded, or, retry the same stage
                     */
                    newUpdateBean = genNextStageUpdateBean(env, report, agent);
                    LOG.debug("GoalAnalyst case 1.2 - host {} successfully finished stage {} for same deploy {}, set env {} as a goal candidate for next stage {}.",
                        host, report.getDeployStage(), env.getDeploy_id(), env.getEnv_id(), newUpdateBean.getDeploy_stage());
                    // Since the report already claim agent is restarting, no need to wait
                    installCandidates.add(new InstallCandidate(env, false, newUpdateBean, report));
                } else {
                    if (updateBean.getState() == AgentState.PAUSED_BY_SYSTEM) {
                        /**
                         * Case 1.3: This is the case where agent has a fatal failure, and should not be given
                         * any more change to try, set the state as PAUSED_BY_SYSTEM, not a deploy candidate.
                         */
                        LOG.debug("GoalAnalyst case 1.3 - host {} failed on stage {} for same deploy {} " +
                            "will be set to PAUSED_BY_SYSTEM, not a goal candidate.",
                            host, report.getDeployStage(), env.getDeploy_id(), env.getEnv_id());
                        return;
                    } else {
                        /**
                         * TODO how do we make sure to NOT interupt this deploy
                         * Case 1.4: This is the case we retry deploy step, or just simply record
                         * the statue while agent is still working on the same stage ( status=UNKNOWN ).
                         * We allow agent to retry either because user explicitly set AgentState to RESET, or the
                         * reported error is not fail.
                         */
                        LOG.debug("GoalAnalyst case 1.4 - host {} failed stage {} for same deploy {}, set env {} as a goal candidate and repeat the same stage {}.",
                            host, report.getDeployStage(), env.getDeploy_id(), env.getEnv_id(), updateBean.getDeploy_stage());
                        // Since the report already claim agent is restarting, no need to wait
                        installCandidates.add(new InstallCandidate(env, false, updateBean, report));
                    }
                }

            } else {
                /**
                 * Case 1.5: Env has a different deploy than report, this is typically the start of a new deploy.
                 *           This is an install candidate. Need to also check if agent is already in restarting mode,
                 *           so that we know if we need to wait in the line or not
                 */
                installNewUpdateBean(env, report, agent);
                LOG.debug("GoalAnalyst case 1.5 - add env {} as a candidate for host {} to install the new deploy {}, agent is reported as in stage {}",
                    env.getEnv_id(), host, env.getDeploy_id(), report.getDeployStage());
            }
            return;
        }

        /**
         * Case 2: No report, could be new env, new agent or agent status file being deleted/corrupted.
         *         Create install candidate and start from the very first step, event though agent PAUSED.
         *         Need also to check if agent is already in restarting mode, so that we know if we
         *         need to wait in the line or not
         */
        if (env != null) {
            AgentBean tempBean = genNewUpdateBean(env, agent);
            boolean needWait = agent == null || agent.getDeploy_stage() == DeployStage.SERVING_BUILD;
            LOG.debug("GoalAnalyst case 2.1 - add env {} as a candidate for host {} to install new deploy {} from scratch since we do not have a report, needWait={}",
                env.getEnv_id(), host, env.getDeploy_id(), needWait);
            installCandidates.add(new InstallCandidate(env, needWait, tempBean, null));
            return;
        }

        /**
         * Case 3: This is when the agent is removed out of the env by system or user. We need to add
         *         this to uninstallCandidates. We also set the agent state as DELETE
         */
        if (report != null && !StringUtils.isEmpty(report.getEnvId())) {
            LOG.debug("GoalAnalyst case 3.1 - add an uninstall candidate to instruct host {} to remove the retired env {}", host, report.getEnvId());
            uninstallCandidates.add(new UninstallCandidate(updateBean, report));
            updateBean.setState(AgentState.DELETE);
            return;
        }

        /**
         * Case 4: Both report & env are null, remove the unexpected obsoleted agent record!
         */
        if (agent != null) {
            LOG.warn("GoalAnalyst case 4 - agent record for host={}/env={} is obsoleted, delete it!", host, agent.getEnv_id());
            needDeleteAgentEnvIds.add(agent.getEnv_id());
        } else {
            LOG.error("This should NOT happen!");
        }
    }

    Set<String> gatherAllEnvIds() {
        Set<String> envIds = new HashSet<>();
        envIds.addAll(agents.keySet());
        envIds.addAll(reports.keySet());
        envIds.addAll(envs.keySet());
        return envIds;
    }

    void analysis() throws Exception {
        Set<String> envIds = gatherAllEnvIds();

        // Handle all the cases, for all possible envs
        for (String envId : envIds) {
            process(envId, envs.get(envId), reports.get(envId), agents.get(envId));
        }

        // Sort the install candidates
        if (installCandidates.size() > 1) {
            // TODO the current thinking is not to shuffle the candidates so we
            // can get consistent results, we will see if this is a good idea
            // Collections.shuffle(installCandidates);
            Collections.sort(installCandidates);
        }

        // Sort the uninstall candidates
        if (uninstallCandidates.size() > 1) {
            // Collections.shuffle(uninstallCandidates);
            Collections.sort(uninstallCandidates);
        }
    }
}
