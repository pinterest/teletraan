/**
 * Copyright 2016 Pinterest, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pinterest.deployservice.handler;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.AgentBean;
import com.pinterest.deployservice.bean.AgentErrorBean;
import com.pinterest.deployservice.bean.AgentState;
import com.pinterest.deployservice.bean.ScheduleState;
import com.pinterest.deployservice.bean.BuildBean;
import com.pinterest.deployservice.bean.DeployBean;
import com.pinterest.deployservice.bean.DeployGoalBean;
import com.pinterest.deployservice.bean.DeployStage;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.ScheduleBean;
import com.pinterest.deployservice.bean.HostState;
import com.pinterest.deployservice.bean.OpCode;
import com.pinterest.deployservice.bean.PingReportBean;
import com.pinterest.deployservice.bean.PingRequestBean;
import com.pinterest.deployservice.bean.PingResponseBean;
import com.pinterest.deployservice.common.Constants;
import com.pinterest.deployservice.common.DeployInternalException;
import com.pinterest.deployservice.common.StateMachines;
import com.pinterest.deployservice.dao.AgentDAO;
import com.pinterest.deployservice.dao.AgentErrorDAO;
import com.pinterest.deployservice.dao.BuildDAO;
import com.pinterest.deployservice.dao.DeployDAO;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.dao.UtilDAO;
import com.pinterest.deployservice.dao.ScheduleDAO;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * This is where we handle agent ping and return deploy goal!
 */
public class PingHandler {
    private static final Logger LOG = LoggerFactory.getLogger(PingHandler.class);
    private static final PingResponseBean NOOP;
    private static final Set<String> EMPTY_GROUPS;

    static {
        NOOP = new PingResponseBean();
        NOOP.setOpCode(OpCode.NOOP);

        // TODO better to treat empty group as REAL NULL
        EMPTY_GROUPS = new HashSet<>(Arrays.asList(Constants.NULL_HOST_GROUP));
    }

    private AgentDAO agentDAO;
    private AgentErrorDAO agentErrorDAO;
    private DeployDAO deployDAO;
    private BuildDAO buildDAO;
    private EnvironDAO environDAO;
    private HostDAO hostDAO;
    private UtilDAO utilDAO;
    private ScheduleDAO scheduleDAO;
    private DataHandler dataHandler;
    private LoadingCache<String, BuildBean> buildCache;
    private LoadingCache<String, DeployBean> deployCache;

    public PingHandler(ServiceContext serviceContext) {
        agentDAO = serviceContext.getAgentDAO();
        agentErrorDAO = serviceContext.getAgentErrorDAO();
        deployDAO = serviceContext.getDeployDAO();
        buildDAO = serviceContext.getBuildDAO();
        environDAO = serviceContext.getEnvironDAO();
        hostDAO = serviceContext.getHostDAO();
        utilDAO = serviceContext.getUtilDAO();
        scheduleDAO = serviceContext.getScheduleDAO();
        dataHandler = new DataHandler(serviceContext);

        if (serviceContext.isBuildCacheEnabled()) {
            buildCache = CacheBuilder.from(serviceContext.getBuildCacheSpec().replace(";", ","))
                    .build(new CacheLoader<String, BuildBean>() {
                        @Override
                        public BuildBean load(String buildId) throws Exception {
                            return buildDAO.getById(buildId);
                        }
                    });
        }

        if (serviceContext.isDeployCacheEnabled()) {
            deployCache = CacheBuilder.from(serviceContext.getDeployCacheSpec().replace(";", ","))
                    .build(new CacheLoader<String, DeployBean>() {
                        @Override
                        public DeployBean load(String deployId) throws Exception {
                            return deployDAO.getById(deployId);
                        }
                    });
        }
    }

    private <K, V> V getFromCache(LoadingCache<K, V> cache, K key) throws ExecutionException {
        LOG.debug(key + " Miss Rate: " + cache.stats().missRate());
        return cache.get(key);
    }

    // Keep host and group membership in sync
    void updateHosts(PingRequestBean pingRequest) throws Exception {
        hostDAO.insertOrUpdate(pingRequest.getHostName(), pingRequest.getHostIp(),
                pingRequest.getHostId(), HostState.ACTIVE.toString(), pingRequest.getGroups());

        List<String> recordedGroups = hostDAO.getGroupNamesByHost(pingRequest.getHostName());
        Set<String> groups = pingRequest.getGroups();
        for (String recordedGroup : recordedGroups) {
            if (!groups.contains(recordedGroup)) {
                LOG.warn("Remove host {} from group {}", pingRequest.getHostName(), recordedGroup);
                this.hostDAO.removeHostFromGroup(pingRequest.getHostId(), recordedGroup);
            }
        }
    }

    void deleteAgentSafely(String hostId, String envId) {
        try {
            LOG.debug("Delete agent {}/{} record.", hostId, envId);
            agentDAO.delete(hostId, envId);
        } catch (Exception e) {
            LOG.error(String.format("Failed to delete agent %s/%s.", hostId, envId), e);
        }
    }

    void updateAgentsSafely(Collection<AgentBean> updateBeans, Map<String, String> errorMessages) {
        LOG.debug("Update agent beans with the following: {}", updateBeans);
        for (AgentBean bean : updateBeans) {
            try {
                Integer errorNo = bean.getLast_err_no();
                if (errorNo != null && errorNo != 0) {
                    String errorMessage = errorMessages.get(bean.getEnv_id());
                    if (errorMessage == null) {
                        errorMessage = "";
                    }
                    AgentErrorBean agentErrorBean = agentErrorDAO.get(bean.getHost_name(), bean.getEnv_id());
                    if (agentErrorBean == null) {
                        agentErrorBean = new AgentErrorBean();
                        agentErrorBean.setHost_id(bean.getHost_id());
                        agentErrorBean.setHost_name(bean.getHost_name());
                        agentErrorBean.setEnv_id(bean.getEnv_id());
                        agentErrorBean.setError_msg(errorMessage);
                        agentErrorDAO.insert(agentErrorBean);
                    } else {
                        if (!agentErrorBean.getError_msg().equals(errorMessage)) {
                            agentErrorBean.setError_msg(errorMessage);
                            agentErrorDAO.update(bean.getHost_name(), bean.getEnv_id(), agentErrorBean);
                        }
                    }
                }
                agentDAO.insertOrUpdate(bean);
            } catch (Exception e) {
                LOG.error("Failed to update agent {}.", bean, e);
            }
        }
    }

    /**
     * Check if we can start deploy on host for certain env. We should not allow
     * more than parallelThreshold hosts in install in the same time
     */
    boolean canDeploy(EnvironBean envBean, String host, AgentBean agentBean) throws Exception {
        // first deploy should always proceed
        
        // String scheduleId = envBean.getSchedule_id();
        // ScheduleBean schedule = null;
        // if (scheduleId != null) {
        //     schedule = scheduleDAO.getById(scheduleId);
        // }
        // if (scheduleId != null && schedule.getState() == ScheduleState.COOLING_DOWN) { 
        //     return false; 
        // } 

        if (agentBean.getFirst_deploy()) {
            agentDAO.insertOrUpdate(agentBean);
            LOG.debug("First deploy for env {}/{}, update and proceed on host {}", envBean.getEnv_name(), envBean.getStage_name(), host);
            return true;
        }

        // TODO use ecache to optimize
        // Make sure we do not exceed allowed number of concurrent active deploying agent
        String envId = envBean.getEnv_id();
        long totalNonFirstDeployAgents = agentDAO.countNonFirstDeployingAgent(envId);
        long parallelThreshold = this.getFinalMaxParallelCount(envBean, totalNonFirstDeployAgents);

        try {
            //Note: This count already excludes first deploy agents
            long totalActiveagents = agentDAO.countDeployingAgent(envId);
            if (totalActiveagents >= parallelThreshold) {
                LOG.debug("There are currently {} agent is actively deploying for env {}, host {} will have to wait for its turn.", totalActiveagents, envId, host);
                return false;
            }
        } catch (Exception e) {
            LOG.warn("Failed to check if can deploy or not for env = {}, host = {}, return false.", envId, host);
            return false;
        }

        // Looks like we can proceed with deploy, but let us double check with lock, and
        // Update table to occupy one seat if condition still hold
        String deployLockName = String.format("DEPLOY-%s", envId);
        Connection connection = utilDAO.getLock(deployLockName);
        if (connection != null) {
            LOG.info("Successfully get lock on {}", deployLockName);
            try {
                LOG.debug("Got lock on behavor of host {}, verify active agents", host);
                long totalActiveAgents = agentDAO.countDeployingAgent(envId);
                if (totalActiveAgents >= parallelThreshold) {
                    LOG.debug("Got lock, but there are currently {} agent is actively deploying for env {}, host {} will have to wait for its turn.", totalActiveAgents, envId, host);
                    return false;
                }
                agentDAO.insertOrUpdate(agentBean);
                LOG.debug("There are currently only {} agent is actively deploying for env {}, update and proceed on host {}.", totalActiveAgents, envId, host);
                return true;
            } catch (Exception e) {
                LOG.warn("Failed to check if can deploy or not for env = {}, host = {}, return false.", envId, host);
                return false;
            } finally {
                utilDAO.releaseLock(deployLockName, connection);
                LOG.info("Successfully released lock on {}", deployLockName);
            }
        } else {
            LOG.warn("Failed to grab PARALLEL_LOCK for env = {}, host = {}, return false.", envId, host);
            return false;
        }
    }

    // Host env will override group env, if there is conflicts, and convert to Map
    Map<String, EnvironBean> convergeEnvs(String host, List<EnvironBean> hostEnvs, List<EnvironBean> groupEnvs) {
        Map<String, EnvironBean> hostEnvMap = mergeEnvs(host, hostEnvs);
        Map<String, EnvironBean> groupEnvMap = mergeEnvs(host, groupEnvs);
        Map<String, EnvironBean> envs = new HashMap<>();
        for (Map.Entry<String, EnvironBean> entry : hostEnvMap.entrySet()) {
            EnvironBean envBean = entry.getValue();
            envs.put(envBean.getEnv_id(), envBean);
            String envName = entry.getKey();
            if (groupEnvMap.containsKey(envName)) {
                LOG.debug("Found conflict env for host {}: {}/{} and {}/{}, choose the former since it is associated with host directly.",
                        host, envName, envBean.getStage_name(), envName, groupEnvMap.get(envName).getStage_name());
                groupEnvMap.remove(envName);
            }
        }
        for (Map.Entry<String, EnvironBean> entry : groupEnvMap.entrySet()) {
            envs.put(entry.getValue().getEnv_id(), entry.getValue());
        }
        return envs;
    }

    // Host env will override group env, if there is conflicts, and convert to Map
    Map<String, EnvironBean> mergeEnvs(String host, List<EnvironBean> envList) {
        if (envList == null || envList.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, EnvironBean> envs = new HashMap<>(envList.size());
        for (EnvironBean envBean : envList) {
            String envName = envBean.getEnv_name();
            if (envs.containsKey(envName)) {
                // In theory, such conflict should've already been avoid by frontend/UI etc.
                LOG.error("Found conflict env for host {}: {}/{} and {}/{}, will ignore {}/{} for now. Please correct the wrong deploy configure.",
                        host, envName, envBean.getStage_name(), envName, envs.get(envName).getStage_name(), envName, envs.get(envName).getStage_name());
            } else {
                envs.put(envName, envBean);
            }
        }
        return envs;
    }

    Map<String, AgentBean> convertAgentBeans(List<AgentBean> agentBeans) {
        if (agentBeans == null || agentBeans.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, AgentBean> agents = new HashMap<>(agentBeans.size());
        for (AgentBean bean : agentBeans) {
            agents.put(bean.getEnv_id(), bean);
        }
        return agents;
    }

    Map<String, PingReportBean> convertReports(PingRequestBean pingRequest) {
        List<PingReportBean> pingReports = pingRequest.getReports();
        if (CollectionUtils.isEmpty(pingReports)) {
            return Collections.emptyMap();
        }
        Map<String, PingReportBean> reports = new HashMap<>(pingReports.size());
        for (PingReportBean report : pingReports) {
            reports.put(report.getEnvId(), report);
        }
        return reports;
    }

    PingRequestBean normalizePingRequest(PingRequestBean pingRequest) throws Exception {
        String hostId = pingRequest.getHostId();
        if (StringUtils.isEmpty(hostId)) {
            LOG.error("Missing host id in request: ", pingRequest);
            throw new DeployInternalException("Missing host id in PingReqest");
        }

        if (StringUtils.isEmpty(pingRequest.getHostName())) {
            LOG.warn("Host {} does not have host name, use {} for now", hostId, hostId);
            pingRequest.setHostName(hostId);
        }

        if (CollectionUtils.isEmpty(pingRequest.getGroups())) {
            LOG.info("Host {} does not belong to any group, use NULL for now", hostId);
            pingRequest.setGroups(EMPTY_GROUPS);
        }

        return pingRequest;
    }

    /**
     * This is the core function to update agent status and compute deploy goal
     */
    public PingResponseBean ping(PingRequestBean pingRequest) throws Exception {
        // handle empty or unexpected request fields
        pingRequest = normalizePingRequest(pingRequest);

        // always update the host table
        this.updateHosts(pingRequest);

        // Convert reports to map, keyed by envId
        Map<String, PingReportBean> reports = convertReports(pingRequest);

        // Find all the appropriate environments for this host and these groups,
        // The converged env map is keyed by envId
        String hostId = pingRequest.getHostId();
        String hostName = pingRequest.getHostName();
        Set<String> groups = pingRequest.getGroups();
        List<EnvironBean> hostEnvs = environDAO.getEnvsByHost(hostName);
        List<EnvironBean> groupEnvs = environDAO.getEnvsByGroups(groups);
        Map<String, EnvironBean> envs = convergeEnvs(hostName, hostEnvs, groupEnvs);
        LOG.debug("Found the following envs {} associated with host {} and group {}.",
                envs.keySet(), hostName, groups);

        // Find all agent records for this host, convert to envId based map
        List<AgentBean> agentBeans = agentDAO.getByHost(hostName);
        Map<String, AgentBean> agents = convertAgentBeans(agentBeans); //database agent

        // Now we have all the relevant envs, reports and agents, let us do some
        // analysis & pick the potential install candidate and uninstall candidate
        GoalAnalyst analyst = new GoalAnalyst(deployDAO, environDAO, hostName, hostId, envs, reports, agents);
        analyst.analysis();

        PingResponseBean response = null;
        Map<String, AgentBean> updateBeans = analyst.getNeedUpdateAgents();
        List<GoalAnalyst.InstallCandidate> installCandidates = analyst.getInstallCandidates();

        // The current thinking is to try the first candidate, even it needs to wait
        if (!installCandidates.isEmpty()) {
            GoalAnalyst.InstallCandidate installCandidate = installCandidates.get(0);
            AgentBean updateBean = installCandidate.updateBean;
            EnvironBean env = installCandidate.env;
            String scheduleId = env.getSchedule_id();
            ScheduleBean schedule = null;
            String hostNumbers = null;
            String cooldownTimes = null;
            Integer currentSession = null;
            Integer totalSessions = null;
            String[] hostNumbersList = null;
            String[] cooldownTimesList = null;
            if (scheduleId != null) {
                schedule = scheduleDAO.getById(scheduleId);
                hostNumbers = schedule.getHost_numbers();
                cooldownTimes = schedule.getCooldown_times();
                currentSession = schedule.getCurrent_session();
                totalSessions = schedule.getTotal_sessions();
                hostNumbersList = hostNumbers.split(",");
                cooldownTimesList = cooldownTimes.split(",");
                LOG.debug("cooldownTimes: {},", cooldownTimes);
                LOG.debug("CooldownTimes: {}", cooldownTimesList);
            }
            
            LOG.debug("ScheudleId is {} ", scheduleId);

            if (scheduleId != null && schedule.getState() == ScheduleState.NOT_STARTED) { // what if it's final session? 
                LOG.debug("Starting deploy on Env {} now. Changed schedule's state to RUNNING");
                ScheduleBean updateScheduleBean = new ScheduleBean();
                updateScheduleBean.setId(schedule.getId());
                updateScheduleBean.setState(ScheduleState.RUNNING);
                updateScheduleBean.setCurrent_session(1);
                scheduleDAO.update(updateScheduleBean, schedule.getId());
            }
            if (installCandidate.needWait) { // new candidate 
                LOG.debug("Checking if host {}, updateBean = {} can deploy", hostName, updateBean);
                if (scheduleId != null && schedule.getState() == ScheduleState.COOLING_DOWN) { 
                    LOG.debug("Time Passed: {} ", System.currentTimeMillis() - schedule.getState_start_time());
                    if (System.currentTimeMillis() - schedule.getState_start_time() > Integer.parseInt(cooldownTimesList[currentSession-1]) * 60000) {
                        ScheduleBean updateScheduleBean = new ScheduleBean();
                        updateScheduleBean.setId(schedule.getId());
                        if (totalSessions == currentSession) {
                            updateScheduleBean.setState(ScheduleState.FINAL);  
                        } else {
                            updateScheduleBean.setState(ScheduleState.RUNNING);  
                            updateScheduleBean.setCurrent_session(currentSession+1);
                        }
                        updateScheduleBean.setState_start_time(System.currentTimeMillis());
                        scheduleDAO.update(updateScheduleBean, schedule.getId());
                    } else {
                        LOG.debug("Env {} is currently cooling down. Host {} will wait until the cooling down period is over.");
                        return NOOP;
                    }
                } 
                if (canDeploy(env, hostName, updateBean)) {
                    // use the updateBean in the installCandidate instead
                    LOG.debug("Host {} can proceed to deploy, updateBean = {}", hostName, updateBean);
                    updateBeans.put(updateBean.getEnv_id(), updateBean);
                    if (schedule!=null) {
                        int totalHosts = 0;
                        for (int i = 0; i < currentSession; i++) {
                            totalHosts+=Integer.parseInt(hostNumbersList[i]);
                        }
                        LOG.debug("Total Hosts is {}", totalHosts);
                        LOG.debug("Deployed agents is {}",agentDAO.countAgentByDeploy(env.getDeploy_id()));

                        if (agentDAO.countAgentByDeploy(env.getDeploy_id()) >= totalHosts && schedule.getState() == ScheduleState.RUNNING) {
                            ScheduleBean updateScheduleBean = new ScheduleBean();
                            updateScheduleBean.setId(schedule.getId());
                            updateScheduleBean.setState(ScheduleState.COOLING_DOWN);
                            updateScheduleBean.setState_start_time(System.currentTimeMillis());
                            scheduleDAO.update(updateScheduleBean, schedule.getId());
                        }
                    }
                    response = generateInstallResponse(installCandidate);
                } else {
                    LOG.debug("Host {} for env {} needs to wait for its turn to install.",
                            hostName, updateBean.getEnv_id());
                }
            } else {
                LOG.debug("Host {} is in the middle of deploy, no need to wait, updateBean = {}",
                        hostName, updateBean);
                // Update the updateBeans to use the updateBean instead
                updateBeans.put(updateBean.getEnv_id(), updateBean);
                response = generateInstallResponse(installCandidate);
            }
        }

        // Delete deprecated agents if necessary
        List<String> needDeleteAgentIds = analyst.getNeedDeleteAgentEnvIds();
        for (String envId : needDeleteAgentIds) {
            LOG.info("Delete host {} record for env {}.", hostId, envId);
            deleteAgentSafely(hostId, envId);
        }

        // Apply ping report change as needed
        if (!updateBeans.isEmpty()) {
            LOG.debug("Update {} agent records for host {}.", updateBeans.size(), hostName);
            updateAgentsSafely(updateBeans.values(), analyst.getErrorMessages());
        }

        if (response != null) {
            LOG.info("Return response {} for host {}.", response, hostName);
            return response;
        }

        List<GoalAnalyst.UninstallCandidate> uninstallCandidates = analyst.getUninstallCandidates();
        if (uninstallCandidates.isEmpty()) {
            LOG.info("Return NOOP for host {} ping, no install or uninstall candidates.", hostName);
            return NOOP;
        }

        // otherwise, we do uninstall
        GoalAnalyst.UninstallCandidate uninstallCandidate = uninstallCandidates.get(0);
        response = generateDeleteResponse(uninstallCandidate);
        LOG.info("Return uninstall response {} for host {}.", response, hostName);
        return response;
    }

    // TODO need to refactor for different opCode
    boolean isFirstStage(AgentBean agentBean) {
        return agentBean.getDeploy_stage() == StateMachines.getFirstStage();
    }

    PingResponseBean generateInstallResponse(GoalAnalyst.InstallCandidate installCandidate) throws Exception {
        EnvironBean envBean = installCandidate.env;
        AgentBean updateBean = installCandidate.updateBean;
        PingReportBean report = installCandidate.report;

        PingResponseBean response = new PingResponseBean();
        if (updateBean != null && updateBean.getState() == AgentState.STOP && updateBean.getDeploy_stage() == DeployStage.STOPPING) {
            response.setOpCode(OpCode.STOP);
        } else {
            response.setOpCode(StateMachines.DEPLOY_TYPE_OPCODE_MAP.get(envBean.getDeploy_type()));
        }

        DeployGoalBean goal = new DeployGoalBean();
        String deployId = envBean.getDeploy_id();

        if (report != null && report.getDeployAlias() != null) {
            // we need to reverse deployId & deployAlias, see transformRollbackDeployId for more details
            goal.setDeployId(report.getDeployAlias());
            goal.setDeployAlias(deployId);
        } else {
            goal.setDeployId(deployId);
        }

        goal.setDeployType(envBean.getDeploy_type());
        goal.setEnvId(envBean.getEnv_id());
        goal.setEnvName(envBean.getEnv_name());
        goal.setStageName(envBean.getStage_name());
        goal.setIsDocker(envBean.getIs_docker());

        // TODO optimize the next stage here based on deploy ( some deploy does not have all the stages )
        DeployStage deployStage = updateBean.getDeploy_stage();
        goal.setDeployStage(deployStage);
        goal.setFirstDeploy(updateBean.getFirst_deploy());

        /*
         * There is no need to generate build/script everytime since they were given to agent at proper stage
         * If there is anything wrong with the previously obtained deploy build/scripts, ( deleted
         * on the agent accidently or system failure, etc.), agent should
         * restart this install from scratch by sending init state about this deploy
         */
        if (updateBean.getDeploy_stage() == StateMachines.getDownloadStage()) {
            LOG.debug("Set build info for goal at {} stage", StateMachines.getDownloadStage());
            DeployBean deployBean = deployCache == null ? deployDAO.getById(deployId) : getFromCache(deployCache, deployId);
            BuildBean buildBean = buildCache == null ? buildDAO.getById(deployBean.getBuild_id()) : getFromCache(buildCache, deployBean.getBuild_id());
            goal.setBuild(buildBean);
        }

        // Pre-Download Stage
        if (isFirstStage(updateBean)) {
            String scriptConfigId = envBean.getSc_config_id();
            if (scriptConfigId != null) {
                Map<String, String> variables = dataHandler.getMapById(scriptConfigId);
                goal.setScriptVariables(variables);
                LOG.debug("Add script varibles {} to goal at {} stage", variables, updateBean.getDeploy_stage());
            }
        }

        // Pass step specfic agent configurations
        Map<String, String> configs = null;
        String agentConfigId = envBean.getAdv_config_id();
        if (agentConfigId != null) {
            // TODO need cache
            String prefix = deployStage.toString() + ".";
            Map<String, String> agentConfigs = dataHandler.getMapById(agentConfigId);
            for (Map.Entry<String, String> entry : agentConfigs.entrySet()) {
                String configName = entry.getKey();
                if (configName.startsWith(prefix)) {
                    configName = configName.substring(prefix.length(), configName.length());
                }
                if (!StringUtils.isEmpty(configName)) {
                    if (configs == null) {
                        configs = new HashMap<>();
                    }
                    configs.put(configName, entry.getValue());
                }
            }
        }
        if (!MapUtils.isEmpty(configs)) {
            LOG.debug("Add agent configs {} to goal", configs);
            goal.setAgentConfigs(configs);
        }

        response.setDeployGoal(goal);
        return response;
    }

    PingResponseBean generateDeleteResponse(GoalAnalyst.UninstallCandidate candidate) throws Exception {
        PingResponseBean response = new PingResponseBean();
        response.setOpCode(OpCode.DELETE);
        DeployGoalBean goal = new DeployGoalBean();
        goal.setEnvId(candidate.report.getEnvId());
        goal.setDeployId(candidate.report.getDeployId());
        goal.setDeployType(candidate.environ.getDeploy_type());
        goal.setDeployStage(candidate.report.getDeployStage());
        goal.setEnvName(candidate.environ.getEnv_name());
        goal.setStageName(candidate.environ.getStage_name());
        goal.setFirstDeploy(false);
        response.setDeployGoal(goal);
        return response;
    }


    private static boolean isApplicable(Integer val) {
        return val != null && val > 0;
    }

    /**
     * Get the parallel count for execution. There are two configs can have effect. The maxParallel that specifies
     * how many hosts and maxParallelPercentage that specifies the percentage of the Hosts. In common cases, there
     * should be only one of them be effective as the UI and Bean update clear the other (Set to 0) when you set one.
     * The rules are simple:
     * 1. If only one of them is set, use that.
     * 2. If both of them are set, use the minimum value
     * @param environBean
     * @param totalHosts
     * @return The maximum parallel count the system respects
     * @throws Exception
     */
    public final static int getFinalMaxParallelCount(EnvironBean environBean, long totalHosts) throws Exception {

        int ret = Constants.DEFAULT_MAX_PARALLEL_HOSTS;
        LOG.debug("Get final maximum parallel count for total capactiy {}", totalHosts);
        boolean numIsApplicable = isApplicable(environBean.getMax_parallel());
        boolean percentageIsApplicable = isApplicable(environBean.getMax_parallel_pct());

        if (!numIsApplicable && percentageIsApplicable) {
            ret = (int) (totalHosts * environBean.getMax_parallel_pct() / 100);
            LOG.debug("Max parallel count {} is decided by percentage solely. Percentage is {}",
                    ret,
                    environBean.getMax_parallel_pct());
        } else if (!percentageIsApplicable && numIsApplicable) {
            ret = environBean.getMax_parallel();
            LOG.debug("Max parallel count {} is decided by number of host solely.", ret);
        } else if (numIsApplicable && percentageIsApplicable) {
            ret = Math.min(environBean.getMax_parallel(),
                    (int) (totalHosts * environBean.getMax_parallel_pct() / 100));
            LOG.debug("Max parallel count {} is decided by combing host {} and percentage {}.",
                    ret, environBean.getMax_parallel(), environBean.getMax_parallel_pct());
        }

        //Ensure the value falls into the range making sense
        if (ret <= 0) {
            LOG.warn("Unexpected ret value {}. Max parallel:{} Max parallel percentage:{} TotalHosts:{}",
                    ret,
                    environBean.getMax_parallel(),
                    environBean.getMax_parallel_pct(),
                    totalHosts);
            ret = 1;
        } else if (ret > totalHosts) {
            LOG.warn("Unexpected ret value {}. Max parallel:{} Max parallel percentage:{} TotalHosts:{}",
                    ret,
                    environBean.getMax_parallel(),
                    environBean.getMax_parallel_pct(),
                    totalHosts);
            ret = (int) totalHosts;
        }
        return ret;
    }
}
