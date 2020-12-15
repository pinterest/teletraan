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
import com.pinterest.deployservice.bean.*;
import com.pinterest.deployservice.common.Constants;
import com.pinterest.deployservice.common.DeployInternalException;
import com.pinterest.deployservice.common.StateMachines;
import com.pinterest.deployservice.dao.AgentDAO;
import com.pinterest.deployservice.dao.AgentCountDAO;
import com.pinterest.deployservice.dao.AgentErrorDAO;
import com.pinterest.deployservice.dao.BuildDAO;
import com.pinterest.deployservice.dao.DeployConstraintDAO;
import com.pinterest.deployservice.dao.DeployDAO;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.GroupDAO;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.dao.HostAgentDAO;
import com.pinterest.deployservice.dao.HostTagDAO;
import com.pinterest.deployservice.dao.ScheduleDAO;
import com.pinterest.deployservice.dao.UtilDAO;
import com.pinterest.deployservice.pingrequests.PingRequestValidator;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.*;

/**
 * This is where we handle agent ping and return deploy goal!
 */
public class PingHandler {
    private static final Logger LOG = LoggerFactory.getLogger(PingHandler.class);
    private static final PingResponseBean NOOP;
    private static final Set<String> EMPTY_GROUPS;
    //private static final long AGENT_COUNT_CACHE_TTL = 5 * 1000;

    static {
        NOOP = new PingResponseBean();
        NOOP.setOpCode(OpCode.NOOP);

        // TODO better to treat empty group as REAL NULL
        EMPTY_GROUPS = new HashSet<>(Arrays.asList(Constants.NULL_HOST_GROUP));
    }

    private AgentDAO agentDAO;
    private AgentCountDAO agentCountDAO;
    private AgentErrorDAO agentErrorDAO;
    private BasicDataSource dataSource;
    private DeployDAO deployDAO;
    private BuildDAO buildDAO;
    private EnvironDAO environDAO;
    private HostDAO hostDAO;
    private HostAgentDAO hostAgentDAO;
    private UtilDAO utilDAO;
    private ScheduleDAO scheduleDAO;
    private HostTagDAO hostTagDAO;
    private GroupDAO groupDAO;
    private DeployConstraintDAO deployConstraintDAO;
    private DataHandler dataHandler;
    private LoadingCache<String, BuildBean> buildCache;
    private LoadingCache<String, DeployBean> deployCache;
    private List<PingRequestValidator> validators;
    private Long agentCountCacheTtl;

    public PingHandler(ServiceContext serviceContext) {
        agentDAO = serviceContext.getAgentDAO();
        agentCountDAO = serviceContext.getAgentCountDAO();
        agentErrorDAO = serviceContext.getAgentErrorDAO();
        dataSource = serviceContext.getDataSource();
        deployDAO = serviceContext.getDeployDAO();
        buildDAO = serviceContext.getBuildDAO();
        environDAO = serviceContext.getEnvironDAO();
        groupDAO = serviceContext.getGroupDAO();
        hostDAO = serviceContext.getHostDAO();
        hostAgentDAO = serviceContext.getHostAgentDAO();
        utilDAO = serviceContext.getUtilDAO();
        scheduleDAO = serviceContext.getScheduleDAO();
        hostTagDAO = serviceContext.getHostTagDAO();
        deployConstraintDAO = serviceContext.getDeployConstraintDAO();
        dataHandler = new DataHandler(serviceContext);
        validators = serviceContext.getPingRequestValidators();
        agentCountCacheTtl = serviceContext.getAgentCountCacheTtl();

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
    void updateHosts(String hostName, String hostIp, String hostId, Set<String> groups) throws Exception {
        hostDAO.insertOrUpdate(hostName, hostIp, hostId, HostState.ACTIVE.toString(), groups);

        List<String> recordedGroups = hostDAO.getGroupNamesByHost(hostName);
        for (String recordedGroup : recordedGroups) {
            if (!groups.contains(recordedGroup)) {
                LOG.warn("Remove host {} from group {}", hostName, recordedGroup);
                this.hostDAO.removeHostFromGroup(hostId, recordedGroup);
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

    boolean isAgentCountValid(String envId, AgentCountBean agentCountBean) {
        if (agentCountBean == null || agentCountBean.getLast_refresh() == null) {
            LOG.debug("Invalid agent count for env {}", envId);
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - agentCountBean.getLast_refresh() > agentCountCacheTtl) {
            LOG.debug("Expired agent count for env {}, last refresh {}", envId, agentCountBean.getLast_refresh());
            return false;
        }
        LOG.debug("Valid agent count for env {}", envId);
        return true;
    }

    /**
     * Check if we can start deploy on host for certain env. We should not allow
     * more than parallelThreshold hosts in install in the same time
     */
    boolean canDeploy(EnvironBean envBean, String host, AgentBean agentBean) throws Exception {
        // first deploy should always proceed
        if (agentBean.getFirst_deploy()) {
            agentDAO.insertOrUpdate(agentBean);
            LOG.debug("First deploy for env {}/{}, update and proceed on host {}", envBean.getEnv_name(), envBean.getStage_name(), host);
            return true;
        }

        // TODO use ecache to optimize
        // Make sure we do not exceed allowed number of concurrent active deploying agent
        String envId = envBean.getEnv_id();
        AgentCountBean agentCountBean = agentCountDAO.get(envId);
        long totalNonFirstDeployAgents = (isAgentCountValid(envId, agentCountBean) == true) ? agentCountBean.getExisting_count() : agentDAO.countNonFirstDeployingAgent(envId);
        long parallelThreshold = getFinalMaxParallelCount(envBean, totalNonFirstDeployAgents);

        try {
            //Note: This count already excludes first deploy agents, includes agents in STOP state
            long totalDeployingAgents = (isAgentCountValid(envId, agentCountBean) == true) ? agentCountBean.getActive_count() : agentDAO.countDeployingAgent(envId);
            if (totalDeployingAgents >= parallelThreshold) {
                LOG.debug("There are currently {} agent is actively deploying for env {}, host {} will have to wait for its turn.", totalDeployingAgents, envId, host);
                return false;
            }
        } catch (Exception e) {
            LOG.warn("Failed to check if can deploy or not for env = {}, host = {}, exception = {}, return false.", envId, host, e.toString());
            return false;
        }

        // Make sure we also follow the schedule if specified
        if (!canDeploywithSchedule(envBean)) {
            LOG.debug("Env {} schedule does not allow host {} to proceed.", envId, host);
            return false;
        }

        // Make sure we also follow the deploy constraint if specified
        if (!canDeployWithConstraint(agentBean.getHost_id(), envBean)) {
            LOG.debug("Env {} deploy constraint does not allow host {} to proceed.", envId, host);
            return false;
        }

        // Looks like we can proceed with deploy, but let us double check with lock, and
        // Update table to occupy one seat if condition still hold
        String deployLockName = String.format("DEPLOY-%s", envId);
        Connection connection = utilDAO.getLock(deployLockName);
        if (connection != null) {
            LOG.info("Successfully get lock on {}", deployLockName);
            try {
                LOG.debug("Got lock on behavor of host {} for env {}, verify active agents", host, envId);
                long totalActiveAgents = (isAgentCountValid(envId, agentCountBean) == true) ? agentCountBean.getActive_count() : agentDAO.countDeployingAgent(envId);
                if (totalActiveAgents >= parallelThreshold) {
                    LOG.debug("There are currently {} agents actively deploying for env {}, host {} will have to wait for its turn.", totalActiveAgents, envId, host);
                    return false;
                }
                // Make sure again we also follow the schedule if specified
                if (!canDeploywithSchedule(envBean)) {
                    LOG.debug("Env {} schedule does not allow host {} to proceed.", envId, host);
                    return false;
                }


                // Make sure we also follow the deploy constraint if specified
                if (!canDeployWithConstraint(agentBean.getHost_id(), envBean)) {
                    LOG.debug("Env {} deploy constraint does not allow host {} to proceed.", envId, host);
                    return false;
                }

                if (agentCountBean == null) {
                    agentCountBean = new AgentCountBean();
                    agentCountBean.setEnv_id(envId);
                }
                agentCountBean.setExisting_count(totalNonFirstDeployAgents);
                agentCountBean.setActive_count(totalActiveAgents + 1);
                agentCountBean.setDeploy_id(agentBean.getDeploy_id());
                // we invalidate cache after ttl.
                if (isAgentCountValid(envId, agentCountBean) == false) {
                    long now = System.currentTimeMillis();
                    agentCountBean.setLast_refresh(now);
                }
                /* Typically, should update agentCount and agent in transaction, 
                 * however, treating agentCount as cache w/ ttl and 
                 * make sure we update count first and then agent state.
                 */
                LOG.debug("updating count for envId {}, existing_count {}, active_count {}, last_refresh {}, ttl {} ms",
                        envId, agentCountBean.getExisting_count(), agentCountBean.getActive_count(), agentCountBean.getLast_refresh(), agentCountCacheTtl);
                agentCountDAO.insertOrUpdate(agentCountBean);
                agentDAO.insertOrUpdate(agentBean);
                LOG.debug("There are currently only {} agent actively deploying for env {}, update and proceed on host {}.", totalActiveAgents, envId, host);
                return true;
            } catch (Exception e) {
                LOG.warn("Failed to check if can deploy or not for env = {}, host = {}, exception = {}, return false.", envId, host, e.toString());
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

    boolean canDeployWithConstraint(String hostId, EnvironBean envBean) throws Exception {
        String envId = envBean.getEnv_id();
        String constraintId = envBean.getDeploy_constraint_id();

        if (constraintId == null) {
            return true;
        }
        try {
            LOG.info("DeployWithConstraint env {}: verify active agents for host {} constraint {}", envId, hostId, constraintId);

            DeployConstraintBean deployConstraintBean = deployConstraintDAO.getById(constraintId);
            String tagName = deployConstraintBean.getConstraint_key();
            HostTagBean hostTagBean = hostTagDAO.get(hostId, tagName);
            if (deployConstraintBean.getState() != TagSyncState.FINISHED) {
                // tag sync state is NOT FINISHED, means not ready for deploy
                LOG.info("DeployWithConstraint env {}: tag sync state is {} , waiting for tag sync state to be FINISHED",
                    envId, deployConstraintBean.getState());
                return false;
            } else {
                // tag sync state is FINISHED
                if (hostTagBean == null) {
                    // but host tag is MISSING
                    LOG.info("DeployWithConstraint env {}: could not find host {} with tagName {}", envId, hostId, tagName);
                    return false;
                }
            }
            String tagValue = hostTagBean.getTag_value();
            long maxParallelWithHostTag = deployConstraintBean.getMax_parallel();
            long totalActiveAgentsWithHostTag = agentDAO.countDeployingAgentWithHostTag(envBean.getEnv_id(), tagName, tagValue);
            LOG.info("DeployWithConstraint env {} with tag {}:{} : host {} waiting for deploy, current {} deploying hosts",
                envId, tagName, tagValue, hostId, totalActiveAgentsWithHostTag);
            if (totalActiveAgentsWithHostTag >= maxParallelWithHostTag) {
                LOG.info("DeployWithConstraint env {} with tag {}:{} : host {} can not deploy, {} already exceed {} for constraint = {}, return false",
                    envId, tagName, tagValue, hostId, totalActiveAgentsWithHostTag, maxParallelWithHostTag, deployConstraintBean.toString());
                return false;
            }
            if(deployConstraintBean.getConstraint_type() == DeployConstraintType.GROUP_BY_GROUP) {
                // if it is GROUP_BY_GROUP deploy, needs to check pre-requisite tags when pre-requisite tag hosts are all done, then it becomes its turn
                List<String> prerequisiteTagValues = hostTagDAO.getAllPrerequisiteTagValuesByEnvIdAndTagName(envBean.getEnv_id(), tagName, tagValue);
                if(prerequisiteTagValues!= null && prerequisiteTagValues.size() > 0) {
                    // if there is any pre-requisite tag, the hosts tagged by this pre-requisite tag should deploy first
                    long totalExistingHostsWithPrerequisiteTags = hostTagDAO.countHostsByEnvIdAndTags(envBean.getEnv_id(), tagName, prerequisiteTagValues);
                    long totalFinishedAgentsWithPrerequisiteTags = agentDAO.countFinishedAgentsByDeployWithHostTags(envBean.getEnv_id(), envBean.getDeploy_id(), tagName, prerequisiteTagValues);
                    if(totalFinishedAgentsWithPrerequisiteTags < totalExistingHostsWithPrerequisiteTags) {
                        LOG.info("DeployWithConstraint env {} with tag {}:{} : prerequisite tags {} has not finish: finished {} < existing {}, host {} can not deploy.",
                            envId, tagName, tagValue, prerequisiteTagValues, totalFinishedAgentsWithPrerequisiteTags, totalExistingHostsWithPrerequisiteTags, hostId);
                        return false;
                    }
                } else {
                    LOG.info("DeployWithConstraint env {} with tag {}:{} : no prerequisite, {} is the starting point", envId, tagName, tagValue, tagValue);
                }
            }
            LOG.info("DeployWithConstraint env {} with tag {}:{} : host {} can deploy", envId, tagName, tagValue, hostId);
            return true;
        } catch (Exception e) {
            LOG.error(String.format("DeployWithConstraint env %s, failed to check if can deploy or not for host = %s for constraint %s", envId, hostId, constraintId), e);
            return false;
        }
    }

    boolean canDeploywithSchedule(EnvironBean env) throws Exception {
        String scheduleId = env.getSchedule_id();
        if (scheduleId == null) {
            return true;
        }

        ScheduleBean schedule = scheduleDAO.getById(scheduleId);
        String hostNumbers = schedule.getHost_numbers();
        Integer currentSession = schedule.getCurrent_session();
        String[] hostNumbersList = hostNumbers.split(",");

        if (schedule.getState() == ScheduleState.COOLING_DOWN) {
            LOG.debug("Env {} is currently cooling down. Host {} will wait until the cooling down period is over.", env.getEnv_id());
            return false;
        } else {
            int totalHosts = 0;
            for (int i = 0; i < currentSession; i++) {
                totalHosts += Integer.parseInt(hostNumbersList[i]);
            }
            // if deployed max amount
            if (agentDAO.countAgentsByDeploy(env.getDeploy_id()) < totalHosts ||
                schedule.getState() == ScheduleState.FINAL || schedule.getState() == ScheduleState.NOT_STARTED) {
                LOG.debug("{} hosts have been deployed, still can deploy in session {}", totalHosts, currentSession);
                return true;
            } else {
                LOG.debug("{} hosts have been deployed, cannot deploy anymore in session {}", totalHosts, currentSession);
                return false;
            }
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

    private EnvironBean populateEnviron(String asgName) throws Exception {
        if (asgName == null) {
            return null;
        }
        EnvironBean envBean = environDAO.getByCluster(asgName);
        String spot_postfix = "-spot";
        if (envBean == null && asgName.endsWith(spot_postfix)) {
            // spot asg case
            StringUtils.removeEnd(asgName, spot_postfix);
            envBean = environDAO.getByCluster(asgName);
        }
        return envBean;
    }

    private EnvType populateStageType(PingRequestBean pingRequest) throws Exception {
        EnvType stageType = EnvType.PRODUCTION;
        if (pingRequest.getStageType() != null) {
            stageType = pingRequest.getStageType();
        } else {
            EnvironBean envBean = populateEnviron(pingRequest.getAutoscalingGroup());
            if (envBean != null && envBean.getStage_type() != EnvType.DEFAULT) {
                stageType = envBean.getStage_type();
            }
        }
        return stageType;
    }
    
    // Creates composite deploy group. size is limited by group_name size in hosts table.
    // TODO: Consider storing host <-> shard mapping separately.
    private Set<String> shardGroups(PingRequestBean pingRequest) throws Exception {
        List<String> shards = new ArrayList<>();
        EnvType stageType = populateStageType(pingRequest);
        shards.add(stageType.toString().toLowerCase());

        String availabilityZone = pingRequest.getAvailabilityZone();
        if (availabilityZone != null) {
            shards.add(availabilityZone);
        }
        Set<String> groups = new HashSet<>(pingRequest.getGroups());
        if (shards.size() > 0) {
            for (String group: pingRequest.getGroups()) {
                String shardedGroup = group + "-" + String.join("-", shards);
                LOG.info("Updating host {} with sharded group {}", pingRequest.getHostName(), shardedGroup);
                groups.add(shardedGroup);
            }
        }
        return groups;
    }

    /**
     * This is the core function to update agent status and compute deploy goal
     */
    public PingResult ping(PingRequestBean pingRequest) throws Exception {
        // handle empty or unexpected request fields
        pingRequest = normalizePingRequest(pingRequest);

        if (validators != null) {
            // validate requests
            for (PingRequestValidator validator : validators) {
                validator.validate(pingRequest);
            }
        }

        String hostIp = pingRequest.getHostIp();
        String hostId = pingRequest.getHostId();
        String hostName = pingRequest.getHostName();
        Set<String> groups = this.shardGroups(pingRequest);

        // always update the host table
        this.updateHosts(hostName, hostIp, hostId, groups);

        //update agent version for host
        String agent_version = pingRequest.getAgentVersion() != null ? pingRequest.getAgentVersion() : "UNKNOWN";
        HostAgentBean hostAgentBean = new HostAgentBean();
        hostAgentBean.setHost_Id(hostId);
        hostAgentBean.setAgent_Version(agent_version);
        hostAgentDAO.insert(hostAgentBean);

        // Convert reports to map, keyed by envId
        Map<String, PingReportBean> reports = convertReports(pingRequest);

        // Find all the appropriate environments for this host and these groups,
        // The converged env map is keyed by envId
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
            for (GoalAnalyst.InstallCandidate installCandidate : installCandidates) {
                AgentBean updateBean = installCandidate.updateBean;
                EnvironBean env = installCandidate.env;
                if (installCandidate.needWait) {
                    LOG.debug("Checking if host {}, updateBean = {} can deploy", hostName, updateBean);
                    if (canDeploy(env, hostName, updateBean)) {
                        LOG.debug("Host {} can proceed to deploy, updateBean = {}", hostName, updateBean);
                        updateBeans.put(updateBean.getEnv_id(), updateBean);
                        response = generateInstallResponse(installCandidate);
                        break;
                    } else if (updateBean.getFirst_deploy()) {
                        LOG.debug("Host {} needs to wait for first deploy of env {}",
                            hostName, updateBean.getEnv_id());
                            break;
                    } else {
                        LOG.debug("Host {} needs to wait for env {}. Try next env",
                            hostName, updateBean.getEnv_id()); 
                    }
                } else {
                    LOG.debug("Host {} is in the middle of deploy, no need to wait, updateBean = {}",
                        hostName, updateBean);
                    updateBeans.put(updateBean.getEnv_id(), updateBean);
                    response = generateInstallResponse(installCandidate);
                    break;
                }
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
            return new PingResult().withResponseBean(response)
                .withInstallCandidates(installCandidates);
        }

        List<GoalAnalyst.UninstallCandidate> uninstallCandidates = analyst.getUninstallCandidates();
        if (uninstallCandidates.isEmpty()) {
            LOG.info("Return NOOP for host {} ping, no install or uninstall candidates.", hostName);
            return new PingResult().withResponseBean(NOOP)
                .withUnInstallCandidates(uninstallCandidates);
        }

        // otherwise, we do uninstall
        GoalAnalyst.UninstallCandidate uninstallCandidate = uninstallCandidates.get(0);
        response = generateDeleteResponse(uninstallCandidate);
        LOG.info("Return uninstall response {} for host {}.", response, hostName);
        return new PingResult().withResponseBean(response)
            .withUnInstallCandidates(uninstallCandidates);
    }

    // TODO need to refactor for different opCode
    boolean isFirstStage(AgentBean agentBean) {
        return agentBean.getDeploy_stage() == StateMachines.getFirstStage();
    }

    public PingResponseBean generateInstallResponse(GoalAnalyst.InstallCandidate installCandidate) throws Exception {
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
            fillBuildForDeployGoal(goal);
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

    public void fillBuildForDeployGoal(DeployGoalBean goal) throws Exception {
        DeployBean deployBean = deployCache == null ? deployDAO.getById(goal.getDeployId())
                                : getFromCache(deployCache, goal.getDeployId());
        BuildBean buildBean = buildCache == null ? buildDAO.getById(deployBean.getBuild_id())
                               : getFromCache(buildCache, deployBean.getBuild_id());
        goal.setBuild(buildBean);
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
