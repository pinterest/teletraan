/*
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

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.*;
import com.pinterest.deployservice.common.*;
import com.pinterest.deployservice.dao.AgentDAO;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.GroupDAO;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.dao.PromoteDAO;
import com.pinterest.deployservice.dao.ScheduleDAO;

import com.pinterest.deployservice.bean.ScheduleState;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class EnvironHandler {
    private static final Logger LOG = LoggerFactory.getLogger(EnvironHandler.class);

    private EnvironDAO environDAO;
    private PromoteDAO promoteDAO;
    private AgentDAO agentDAO;
    private GroupDAO groupDAO;
    private HostDAO hostDAO;
    private ScheduleDAO scheduleDAO;
    private CommonHandler commonHandler;
    private DataHandler dataHandler;

    public EnvironHandler(ServiceContext serviceContext) {
        environDAO = serviceContext.getEnvironDAO();
        promoteDAO = serviceContext.getPromoteDAO();
        agentDAO = serviceContext.getAgentDAO();
        groupDAO = serviceContext.getGroupDAO();
        hostDAO = serviceContext.getHostDAO();
        scheduleDAO = serviceContext.getScheduleDAO();
        commonHandler = new CommonHandler(serviceContext);
        dataHandler = new DataHandler(serviceContext);
    }

    void normalizeEnvRequest(EnvironBean envBean, String operator) throws Exception {
        if (envBean.getSuccess_th() != null) {
            Integer successTh = envBean.getSuccess_th();
            if (successTh > 10000 || successTh < 0) {
                throw new DeployInternalException("Success threshold should between 0 and 10000!");
            }
            envBean.setSuccess_th(successTh);
        }

        if (envBean.getMax_deploy_num() != null) {
            int max = envBean.getMax_deploy_num();
            if (max > 0) {
                envBean.setMax_deploy_num(max);
            } else {
                throw new DeployInternalException("Max deploys to keep is below 0!");
            }
        }

        if (envBean.getMax_deploy_day() != null) {
            int max = envBean.getMax_deploy_day();
            if (max > 0) {
                envBean.setMax_deploy_day(max);
            } else {
                throw new DeployInternalException("Max days to keep a deploy is below 0!");
            }
        }

        //If the update contains either max parallel number or percentage. We clear the other by set
        //to 0. Note: the null value of bean won't propagate to the database
        if(envBean.getMax_parallel() != null && envBean.getMax_parallel_pct()==null){
            envBean.setMax_parallel_pct(0);
        }

        if(envBean.getMax_parallel() == null && envBean.getMax_parallel_pct()!=null){
            envBean.setMax_parallel(0);
        }

        if (envBean.getStage_type() == null) {
            envBean.setStage_type(Constants.DEFAULT_STAGE_TYPE);
        }

        envBean.setLast_operator(operator);
        envBean.setLast_update(System.currentTimeMillis());
    }

    void updateEnvBeanDefault(EnvironBean envBean) throws Exception {
        if (envBean.getEnv_state() == null) {
            envBean.setEnv_state(EnvState.NORMAL);
        }

        if (envBean.getDescription() == null) {
            envBean.setDescription(String.format("%s stage for env %s", envBean.getEnv_name(),
                    envBean.getStage_name()));
        }

        if (envBean.getBuild_name() == null) {
            envBean.setBuild_name(envBean.getEnv_name());
        }

        if (envBean.getBranch() == null) {
            envBean.setBranch(Constants.DEFAULT_BRANCH_NAME);
        }

        if (envBean.getMax_parallel() == null) {
            envBean.setMax_parallel(Constants.DEFAULT_MAX_PARALLEL_HOSTS);
        }

        if (envBean.getPriority() == null) {
            envBean.setPriority(Constants.DEFAULT_PRIORITY);
        }

        if (envBean.getStuck_th() == null) {
            envBean.setStuck_th(Constants.DEFAULT_STUCK_THRESHOLD);
        }

        if (envBean.getSuccess_th() == null) {
            //To keep the precision, the default success_th value should be 10000 in DB.
            envBean.setSuccess_th(Constants.DEFAULT_SUCCESS_THRESHOLD * 100);
        }

        if (envBean.getAccept_type() == null) {
            envBean.setAccept_type(Constants.DEFAULT_ACCEPTANCE_TYPE);
        }

        if (envBean.getMax_deploy_num() == null) {
            envBean.setMax_deploy_num(Constants.DEFAULT_DEPLOY_NUM);
        }

        if (envBean.getMax_deploy_day() == null) {
            envBean.setMax_deploy_day(Constants.DEFAULT_DEPLOY_DAY);
        }

        if (envBean.getState() == null) {
            envBean.setState(EnvironState.NORMAL);
        }

        if (envBean.getOverride_policy() == null) {
            envBean.setOverride_policy(Constants.DEFAULT_OVERRIDE_POLICY);
        }

        if (envBean.getStage_type() == null) {
            envBean.setStage_type(Constants.DEFAULT_STAGE_TYPE);
        }
    }

    void updatePromoteBeanDefault(PromoteBean promoteBean) throws Exception {
        if (promoteBean.getType() == null) {
            promoteBean.setType(Constants.DEFAULT_PROMOTE_TYPE);
        }

        if (promoteBean.getQueue_size() == null) {
            promoteBean.setQueue_size(Constants.DEFAULT_MAX_PROMOTE_QUEUE_SIZE);
        }

        if (promoteBean.getDelay() == null) {
            promoteBean.setDelay(Constants.DEFAULT_PROMOTE_DELAY_MINUTES);
        }

        if (promoteBean.getDisable_policy() == null) {
            promoteBean.setDisable_policy(Constants.DEFAULT_PROMOTE_DISABLE_POLICY);
        }

        if (promoteBean.getFail_policy() == null) {
            promoteBean.setFail_policy(Constants.DEFAULT_PROMOTE_FAIL_POLICY);
        }
    }

    public String createEnvStage(EnvironBean envBean, String operator) throws Exception {
        normalizeEnvRequest(envBean, operator);
        updateEnvBeanDefault(envBean);
        String envId = CommonUtils.getBase64UUID();
        envBean.setEnv_id(envId);
        environDAO.insert(envBean);
        return envId;
    }

    public List<AlarmBean> getAlarms(EnvironBean environBean) throws Exception {
        String id = environBean.getAlarm_config_id();
        if (StringUtils.isEmpty(id)) {
            return Collections.emptyList();
        }
        return dataHandler.getDataById(id, AlarmDataFactory.class);
    }

    public void updateAlarms(EnvironBean environBean, List<AlarmBean> alarmBeans, String operator) throws Exception {
        String id = environBean.getAlarm_config_id();
        if (StringUtils.isEmpty(id)) {
            id = dataHandler.insertData(alarmBeans, AlarmDataFactory.class, operator);
            environBean.setAlarm_config_id(id);
            updateStage(environBean, operator);
        } else {
            dataHandler.updateData(id, alarmBeans, AlarmDataFactory.class, operator);
        }
    }

    public List<MetricsConfigBean> getMetrics(EnvironBean environBean) throws Exception {
        String id = environBean.getMetrics_config_id();
        if (StringUtils.isEmpty(id)) {
            return Collections.emptyList();
        }
        return dataHandler.getDataById(id, MetricsDataFactory.class);
    }

    public void updateMetrics(EnvironBean environBean, List<MetricsConfigBean> metricsBeans, String operator) throws Exception {
        String id = environBean.getMetrics_config_id();
        if (StringUtils.isEmpty(id)) {
            id = dataHandler.insertData(metricsBeans, MetricsDataFactory.class, operator);
            environBean.setMetrics_config_id(id);
            updateStage(environBean, operator);
        } else {
            dataHandler.updateData(id, metricsBeans, MetricsDataFactory.class, operator);
        }
    }

    public EnvWebHookBean getHooks(EnvironBean environBean) throws Exception {
        String id = environBean.getWebhooks_config_id();
        if (StringUtils.isEmpty(id)) {
            return new EnvWebHookBean();
        }
        return dataHandler.getDataById(id, WebhookDataFactory.class);
    }

    public void updateHooks(EnvironBean environBean, EnvWebHookBean hookBean, String operator) throws Exception {
        String id = environBean.getWebhooks_config_id();
        if (StringUtils.isEmpty(id)) {
            id = dataHandler.insertData(hookBean, WebhookDataFactory.class, operator);
            environBean.setWebhooks_config_id(id);
            updateStage(environBean, operator);
        } else {
            dataHandler.updateData(id, hookBean, WebhookDataFactory.class, operator);
        }
    }

    public Map<String, String> getAdvancedConfigs(EnvironBean environBean) throws Exception {
        String id = environBean.getAdv_config_id();
        if (StringUtils.isEmpty(id)) {
            return Collections.emptyMap();
        }
        return dataHandler.getMapById(id);
    }

    public void updateAdvancedConfigs(EnvironBean environBean, Map<String, String> configs, String operator) throws Exception {
        String dataId = environBean.getAdv_config_id();
        if (dataId == null) {
            // Create data the first time
            dataId = dataHandler.insertMap(configs, operator);
            environBean.setAdv_config_id(dataId);
            updateStage(environBean, operator);
        } else {
            dataHandler.updateMap(dataId, configs, operator);
        }
    }

    public Map<String, String> getScriptConfigs(EnvironBean environBean) throws Exception {
        String id = environBean.getSc_config_id();
        if (StringUtils.isEmpty(id)) {
            return Collections.emptyMap();
        }
        return dataHandler.getMapById(id);
    }

    public void updateScriptConfigs(EnvironBean environBean, Map<String, String> configs, String operator) throws Exception {
        String dataId = environBean.getSc_config_id();
        if (dataId == null) {
            // Create data the first time
            dataId = dataHandler.insertMap(configs, operator);
            environBean.setSc_config_id(dataId);
            updateStage(environBean, operator);
        } else {
            dataHandler.updateMap(dataId, configs, operator);
        }
    }

    public void updateStage(EnvironBean updateBean, String operator) throws Exception {
        normalizeEnvRequest(updateBean, operator);
        environDAO.update(updateBean.getEnv_name(), updateBean.getStage_name(), updateBean);
    }

    PromoteBean genDefaultEnvPromote(String envId) {
        PromoteBean promote = new PromoteBean();
        promote.setEnv_id(envId);
        promote.setType(Constants.DEFAULT_PROMOTE_TYPE);
        promote.setQueue_size(Constants.DEFAULT_PROMOTE_QUEUE_SIZE);
        promote.setDelay(Constants.DEFAULT_PROMOTE_DELAY_MINUTES);
        promote.setDisable_policy(Constants.DEFAULT_PROMOTE_DISABLE_POLICY);
        promote.setFail_policy(Constants.DEFAULT_PROMOTE_FAIL_POLICY);
        return promote;
    }

    public PromoteBean getEnvPromote(String envName, String stageName) throws Exception {
        EnvironBean envBean = environDAO.getByStage(envName, stageName);
        PromoteBean promoteBean = promoteDAO.getById(envBean.getEnv_id());
        if (promoteBean == null) {
            return genDefaultEnvPromote(envBean.getEnv_id());
        }
        return promoteBean;
    }

    public void updateEnvPromote(EnvironBean envBean, PromoteBean promoteBean, String operator) throws Exception {
        String envId = envBean.getEnv_id();
        promoteBean.setLast_operator(operator);
        promoteBean.setLast_update(System.currentTimeMillis());

        PromoteBean originBean = promoteDAO.getById(envId);
        if (originBean == null) {
            // Provide all the defaults if this is an insert
            updatePromoteBeanDefault(promoteBean);
            promoteBean.setEnv_id(envId);
            promoteDAO.insert(promoteBean);
        } else {
            promoteDAO.update(envId, promoteBean);
        }
    }

    public String resume(EnvironBean envBean, String operator) throws Exception {
        envBean.setEnv_state(EnvState.NORMAL);
        updateStage(envBean, operator);
        return envBean.getEnv_id();
    }

    public String pause(EnvironBean envBean, String operator) throws Exception {
        envBean.setEnv_state(EnvState.PAUSED);
        updateStage(envBean, operator);
        return envBean.getEnv_id();
    }

    public void enable(EnvironBean envBean, String operator) throws Exception {
        envBean.setState(EnvironState.NORMAL);
        updateStage(envBean, operator);
    }

    public void disable(EnvironBean envBean, String operator) throws Exception {
        envBean.setState(EnvironState.DISABLED);
        updateStage(envBean, operator);
    }

    public void enableAll(String operator) throws Exception {
        EnvironBean updateBean = new EnvironBean();
        updateBean.setState(EnvironState.NORMAL);
        updateBean.setLast_operator(operator);
        updateBean.setLast_update(System.currentTimeMillis());
        environDAO.updateAll(updateBean);
    }

    public void disableAll(String operator) throws Exception {
        EnvironBean updateBean = new EnvironBean();
        updateBean.setState(EnvironState.DISABLED);
        updateBean.setLast_operator(operator);
        updateBean.setLast_update(System.currentTimeMillis());
        environDAO.updateAll(updateBean);
    }

    /**
     * A stage is only allowed to be deleted when there is no host and group capacity, e.g.
     * all the agents had been instructed to delete its env ( stop service and delete status etc.)
     */
    public void deleteEnvStage(String envName, String envStage, String operator) throws Exception {
        EnvironBean envBean = getStageSafely(envName, envStage);
        String envId = envBean.getEnv_id();
        List<String> groups = groupDAO.getCapacityGroups(envBean.getEnv_id());
        if (groups != null && !groups.isEmpty()) {
            throw new DeployInternalException("Reject the delete of env %s while it still has group capacity", envId);
        }

        List<String> hosts = groupDAO.getCapacityHosts(envBean.getEnv_id());
        if (hosts != null && !hosts.isEmpty()) {
            throw new DeployInternalException("Reject the delete of env %s while it still has host capacity", envId);
        }

        long total = agentDAO.countAgentByEnv(envId);
        if (total > 0) {
            throw new DeployInternalException("Reject the delete of env %s while there are still %d hosts active", envId, total);
        }
        // TODO make the following transcational
        environDAO.delete(envId);
        promoteDAO.delete(envId);
        if (envBean.getAdv_config_id() != null) {
            dataHandler.deleteData(envBean.getAdv_config_id());
        }
        if (envBean.getSc_config_id() != null) {
            dataHandler.deleteData(envBean.getSc_config_id());
        }
        if (envBean.getAlarm_config_id() != null) {
            dataHandler.deleteData(envBean.getAlarm_config_id());
        }
        if (envBean.getMetrics_config_id() != null) {
            dataHandler.deleteData(envBean.getMetrics_config_id());
        }
    }

    /**
     * UI should check the host exist and can be added first;
     * Make sure UI warn if cause env conflict with existing group capacity
     */
    public void updateHosts(EnvironBean envBean, List<String> hosts, String operator) throws Exception {
        List<String> oldHostList = groupDAO.getCapacityHosts(envBean.getEnv_id());
        Set<String> oldHosts = new HashSet<>();
        oldHosts.addAll(oldHostList);
        for (String host : hosts) {
            if (!oldHosts.contains(host)) {
                groupDAO.addHostCapacity(envBean.getEnv_id(), host);
            } else {
                oldHosts.remove(host);
            }
        }
        for (String host : oldHosts) {
            groupDAO.removeHostCapacity(envBean.getEnv_id(), host);
        }
    }

    public void updateGroups(EnvironBean envBean, List<String> groups, String operator) throws Exception {
        // TODO need to check group env conflicts and reject if so
        List<String> oldGroupList = groupDAO.getCapacityGroups(envBean.getEnv_id());
        Set<String> oldGroups = new HashSet<>();
        oldGroups.addAll(oldGroupList);
        for (String group : groups) {
            if (!oldGroups.contains(group)) {
                groupDAO.addGroupCapacity(envBean.getEnv_id(), group);
            } else {
                oldGroups.remove(group);
            }
        }
        for (String group : oldGroups) {
            if (group == envBean.getCluster_name()) {
                LOG.info("Skipping implicit group %s", group);
                continue;

            }
            groupDAO.removeGroupCapacity(envBean.getEnv_id(), group);
        }
    }

    /**
     * Take this opportunity to update the deploy progress, and return the latest
     * This usually called by UI client to monitor the ongoing deploy.
     */
    public DeployProgressBean updateDeployProgress(EnvironBean envBean) throws Exception {
        // TODO consider to transition and get agent status in one transaction for consistency
        commonHandler.transitionDeployState(envBean.getDeploy_id(), envBean);
        List<AgentBean> agentBeans = agentDAO.getAllByEnv(envBean.getEnv_id());

        long capacityTotal = environDAO.countTotalCapacity(envBean.getEnv_id(), envBean.getEnv_name(), envBean.getStage_name());
        Set<String> capacityHosts = new HashSet<>();
        if (capacityTotal > agentBeans.size()) {
            // Capacity hosts = newly provisioned host + agents
            List<String> capacityHostList = environDAO.getTotalCapacityHosts(envBean.getEnv_id(), envBean.getEnv_name(), envBean.getStage_name());
            capacityHosts.addAll(capacityHostList);
        }

        DeployProgressBean progress = new DeployProgressBean();
        List<AgentBean> agents = new ArrayList<>(agentBeans.size());
        for (AgentBean agentBean : agentBeans) {
            agents.add(agentBean);
            // yep, we've seen it
            if (!capacityHosts.isEmpty()) {
                capacityHosts.remove(agentBean.getHost_name());
            }
        }

        List<HostBean> newHosts = new ArrayList<>();
        for (String hostName : capacityHosts) {
            Collection<HostBean> hostBeans = hostDAO.getByEnvIdAndHostName(envBean.getEnv_id(), hostName);
            if (!hostBeans.isEmpty()) {
                newHosts.add(hostBeans.iterator().next());
            }
        }

        progress.setMissingHosts(new ArrayList<>(environDAO.getMissingHosts(envBean.getEnv_id())));
        progress.setAgents(agents);
        progress.setProvisioningHosts(newHosts);
        return progress;
    }

    EnvironBean getStageSafely(String envName, String envStage) throws Exception {
        EnvironBean envBean = environDAO.getByStage(envName, envStage);
        if (envBean == null) {
            throw new DeployInternalException("env %s/%s does not exist.", envName, envStage);
        }
        return envBean;
    }

    public void stopServiceOnHost(String hostId) throws Exception {
        LOG.info(String.format("Start to stop host %s", hostId));
        AgentBean agentBean = new AgentBean();
        agentBean.setState(AgentState.STOP);
        agentBean.setLast_update(System.currentTimeMillis());
        agentDAO.updateAgentById(hostId, agentBean);

        HostBean hostBean = new HostBean();
        hostBean.setState(HostState.PENDING_TERMINATE);
        hostBean.setLast_update(System.currentTimeMillis());
        hostDAO.updateHostById(hostId, hostBean);
    }

    public void stopServiceOnHosts(Collection<String> hostIds) throws Exception {
        for (String hostId : hostIds) {
            stopServiceOnHost(hostId);
        }
    }
}
