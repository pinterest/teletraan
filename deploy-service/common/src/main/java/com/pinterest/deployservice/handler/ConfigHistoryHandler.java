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

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import com.pinterest.arcee.bean.AsgAlarmBean;
import com.pinterest.arcee.bean.AutoScalingRequestBean;
import com.pinterest.arcee.bean.GroupBean;
import com.pinterest.arcee.bean.ScalingPoliciesBean;
import com.pinterest.arcee.handler.GroupHandler;
import com.pinterest.clusterservice.bean.AwsVmBean;
import com.pinterest.clusterservice.bean.ClusterBean;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.AlarmBean;
import com.pinterest.deployservice.bean.ConfigHistoryBean;
import com.pinterest.deployservice.bean.EnvWebHookBean;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.MetricsConfigBean;
import com.pinterest.deployservice.bean.PromoteBean;
import com.pinterest.deployservice.common.ChangeFeedJob;
import com.pinterest.deployservice.common.CommonUtils;
import com.pinterest.deployservice.common.Constants;
import com.pinterest.deployservice.dao.ConfigHistoryDAO;
import com.pinterest.deployservice.dao.EnvironDAO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class ConfigHistoryHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigHistoryHandler.class);
    private static final String CHANGEFEED_TEMPLATE = "{\"type\":\"%s\",\"environment\":\"%s\",\"description\":\"%s\",\"author\":\"%s\","
            + "\"automation\":\"%s\",\"source\":\"Teletraan\",\"optional-1\":\"%s\",\"optional-2\":\"\"}";
    private final ConfigHistoryDAO configHistoryDAO;
    private final EnvironDAO environDAO;
    private final GroupHandler groupHandler;
    private final EnvironHandler environHandler;
    private final String changeFeedUrl;
    private final ExecutorService jobPool;

    public ConfigHistoryHandler(ServiceContext serviceContext) {
        configHistoryDAO = serviceContext.getConfigHistoryDAO();
        environDAO = serviceContext.getEnvironDAO();
        groupHandler = new GroupHandler(serviceContext);
        environHandler = new EnvironHandler(serviceContext);
        changeFeedUrl = serviceContext.getChangeFeedUrl();
        jobPool = serviceContext.getJobPool();
    }

    /**
     * Json Field ExclusionStrategy
     * Used to avoid to read hidden thrift obj - __isset_bit_vector
     */
    public class CustomExclusionStrategy implements ExclusionStrategy {
        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            return f.getName().equals("__isset_bit_vector") || f.getName().equals("groupName");
        }

        @Override
        public boolean shouldSkipClass(Class<?> c) {
            return false;
        }
    }

    /**
     * Use config_id to find all configuration history
     */
    public List<ConfigHistoryBean> getConfigHistoryByName(String config_id, int pageIndex, int pageSize) throws Exception {
        return configHistoryDAO.getByConfigId(config_id, pageIndex, pageSize);
    }

    /**
     * add new config history
     * config_id can be env_id for environment config,
     * or group_name for group configuration
     */
    public void updateConfigHistory(String configId, String type, Object request, String operator) throws Exception {
        try {
            ConfigHistoryBean bean = new ConfigHistoryBean();
            bean.setConfig_id(configId);
            bean.setChange_id(CommonUtils.getBase64UUID());
            bean.setCreation_time(System.currentTimeMillis());
            bean.setOperator(operator);
            bean.setType(type);
            Gson gson = new GsonBuilder().addSerializationExclusionStrategy(new CustomExclusionStrategy()).create();
            bean.setConfig_change(gson.toJson(request));

            configHistoryDAO.insert(bean);
        } catch (Exception e) {
            LOG.warn("Failed to persist a config change for group: " + configId, e);
        }
    }

    /**
     * Rollback to previous configuration based on change id
     */
    public void rollbackConfig(String configType, String changeId, String operator) throws Exception {
        try {
            ConfigHistoryBean bean = configHistoryDAO.getByChangeId(changeId);
            String type = bean.getType();
            String configChange = bean.getConfig_change();

            Gson gson = new GsonBuilder().addSerializationExclusionStrategy(new CustomExclusionStrategy()).create();
            if (configType.equals(Constants.CONFIG_TYPE_GROUP)) {
                String groupName = bean.getConfig_id();
                LOG.info(String.format("Rollback group config to the ChangeId: %s, Type:%s for group %s", changeId, type, groupName));

                if (type.equals(Constants.TYPE_ASG_LAUNCH)) {
                    AwsVmBean awsVmBean = gson.fromJson(configChange, AwsVmBean.class);
                    groupHandler.updateCluster(groupName, awsVmBean);
                    updateConfigHistory(groupName, type, awsVmBean, operator);
                } else if (type.equals(Constants.TYPE_ASG_GENERAL)) {
                    GroupBean newBean = gson.fromJson(configChange, GroupBean.class);
                    groupHandler.updateGroupInfo(groupName, newBean);
                    updateConfigHistory(groupName, type, newBean, operator);
                } else if (type.equals(Constants.TYPE_ASG_SCALING)) {
                    AutoScalingRequestBean newBean = gson.fromJson(configChange, AutoScalingRequestBean.class);
                    newBean.setGroupName(groupName);
                    groupHandler.updateAutoScalingGroup(groupName, newBean);
                    updateConfigHistory(groupName, type, newBean, operator);
                } else if (type.equals(Constants.TYPE_ASG_POLICY)) {
                    ScalingPoliciesBean newBean = gson.fromJson(configChange, ScalingPoliciesBean.class);
                    groupHandler.putScalingPolicyToGroup(groupName, newBean);
                    updateConfigHistory(groupName, type, newBean, operator);
                } else if (type.equals(Constants.TYPE_ASG_ALARM)) {
                    List<AsgAlarmBean> newBeans = gson.fromJson(configChange, new TypeToken<ArrayList<AsgAlarmBean>>() {
                    }.getType());
                    groupHandler.updateAlarmsToAutoScalingGroup(groupName, newBeans);
                    updateConfigHistory(groupName, type, newBeans, operator);
                } else {
                    LOG.warn(String.format("Failed to find the type %s to rollback, %s", type, changeId));
                }
            } else if (configType.equals(Constants.CONFIG_TYPE_ENV)) {
                String envId = bean.getConfig_id();
                EnvironBean environBean = environDAO.getById(envId);
                LOG.info(String.format("Rollback environment config to the ChangeId: %s, Type:%s for env %s", changeId, type, environBean.getEnv_name()));

                if (type.equals(Constants.TYPE_ENV_GENERAL)) {
                    EnvironBean newBean = gson.fromJson(configChange, EnvironBean.class);
                    environHandler.updateStage(environBean, newBean, operator);
                    updateConfigHistory(envId, type, newBean, operator);
                } else if (type.equals(Constants.TYPE_ENV_PROMOTE)) {
                    PromoteBean newBean = gson.fromJson(configChange, PromoteBean.class);
                    environHandler.updateEnvPromote(environBean, newBean, operator);
                    updateConfigHistory(envId, type, newBean, operator);
                } else if (type.equals(Constants.TYPE_ENV_SCRIPT)) {
                    Map<String, String> newConfigs = gson.fromJson(configChange, new TypeToken<Map<String, String>>() {
                    }.getType());
                    environHandler.updateScriptConfigs(environBean, newConfigs, operator);
                    updateConfigHistory(envId, type, newConfigs, operator);
                } else if (type.equals(Constants.TYPE_ENV_ADVANCED)) {
                    Map<String, String> newConfigs = gson.fromJson(configChange, new TypeToken<Map<String, String>>() {
                    }.getType());
                    environHandler.updateAdvancedConfigs(environBean, newConfigs, operator);
                    updateConfigHistory(envId, type, newConfigs, operator);
                } else if (type.equals(Constants.TYPE_ENV_METRIC)) {
                    List<MetricsConfigBean> newBeans = gson.fromJson(configChange, new TypeToken<ArrayList<MetricsConfigBean>>() {
                    }.getType());
                    environHandler.updateMetrics(environBean, newBeans, operator);
                    updateConfigHistory(envId, type, newBeans, operator);
                } else if (type.equals(Constants.TYPE_ENV_ALARM)) {
                    List<AlarmBean> newBeans = gson.fromJson(configChange, new TypeToken<ArrayList<AlarmBean>>() {
                    }.getType());
                    environHandler.updateAlarms(environBean, newBeans, operator);
                    updateConfigHistory(envId, type, newBeans, operator);
                } else if (type.equals(Constants.TYPE_ENV_WEBHOOK)) {
                    EnvWebHookBean newBean = gson.fromJson(configChange, EnvWebHookBean.class);
                    environHandler.updateHooks(environBean, newBean, operator);
                    updateConfigHistory(envId, type, newBean, operator);
                } else if (type.equals(Constants.TYPE_ENV_HOST_CAPACITY)) {
                    List<String> newHosts = gson.fromJson(configChange, new TypeToken<ArrayList<String>>() {
                    }.getType());
                    environHandler.updateHosts(environBean, newHosts, operator);
                    updateConfigHistory(envId, type, newHosts, operator);
                } else if (type.equals(Constants.TYPE_ENV_GROUP_CAPACITY)) {
                    List<String> newGroups = gson.fromJson(configChange, new TypeToken<ArrayList<String>>() {
                    }.getType());
                    environHandler.updateGroups(environBean, newGroups, operator);
                    updateConfigHistory(envId, type, newGroups, operator);
                } else {
                    LOG.warn(String.format("Failed to find the type %s to rollback, %s", type, changeId));
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to rollback to previous config, " + changeId, e);
        }
    }

    public void updateChangeFeed(String configType, String configId, String type, String operator) {
        try {
            Gson gson = new GsonBuilder().addSerializationExclusionStrategy(new CustomExclusionStrategy()).create();
            List<ConfigHistoryBean> configHistoryBeans = configHistoryDAO.getLatestChangesByType(configId, type);
            if (configHistoryBeans.size() != 2) {
                return;
            }

            if (configType.equals(Constants.CONFIG_TYPE_GROUP)) {
                LOG.info(String.format("Push group %s config change for %s", type, configId));
                String configHistoryUrl = String.format("https://deploy.pinadmin.com/groups/%s/config_history/", configId);
                String feedPayload = String.format(CHANGEFEED_TEMPLATE, configType, configId, configHistoryUrl,
                                  operator, "False", type);
                if (type.equals(Constants.TYPE_ASG_LAUNCH)) {
                    AwsVmBean newBean = gson.fromJson(configHistoryBeans.get(0).getConfig_change(), AwsVmBean.class);
                    newBean.setLaunchConfigId(null);
                    AwsVmBean oriBean = gson.fromJson(configHistoryBeans.get(1).getConfig_change(), AwsVmBean.class);
                    oriBean.setLaunchConfigId(null);
                    jobPool.submit(new ChangeFeedJob(feedPayload, changeFeedUrl, oriBean, newBean));
                } else if (type.equals(Constants.TYPE_ASG_GENERAL)) {
                    GroupBean newBean = gson.fromJson(configHistoryBeans.get(0).getConfig_change(), GroupBean.class);
                    newBean.setLast_update(null);
                    GroupBean oriBean = gson.fromJson(configHistoryBeans.get(1).getConfig_change(), GroupBean.class);
                    oriBean.setLast_update(null);
                    jobPool.submit(new ChangeFeedJob(feedPayload, changeFeedUrl, oriBean, newBean));
                } else if (type.equals(Constants.TYPE_ASG_SCALING)) {
                    AutoScalingRequestBean newBean = gson.fromJson(configHistoryBeans.get(0).getConfig_change(), AutoScalingRequestBean.class);
                    AutoScalingRequestBean oriBean = gson.fromJson(configHistoryBeans.get(1).getConfig_change(), AutoScalingRequestBean.class);
                    jobPool.submit(new ChangeFeedJob(feedPayload, changeFeedUrl, oriBean, newBean));
                } else if (type.equals(Constants.TYPE_ASG_POLICY)) {
                    ScalingPoliciesBean newBean = gson.fromJson(configHistoryBeans.get(0).getConfig_change(), ScalingPoliciesBean.class);
                    ScalingPoliciesBean oriBean = gson.fromJson(configHistoryBeans.get(1).getConfig_change(), ScalingPoliciesBean.class);
                    jobPool.submit(new ChangeFeedJob(feedPayload, changeFeedUrl, oriBean, newBean));
                } else if (type.equals(Constants.TYPE_ASG_ALARM)) {
                    List<AsgAlarmBean> newBean = gson.fromJson(configHistoryBeans.get(0).getConfig_change(), new TypeToken<ArrayList<AsgAlarmBean>>() {
                                      }.getType());
                    List<AsgAlarmBean> oriBean = gson.fromJson(configHistoryBeans.get(1).getConfig_change(),
                                      new TypeToken<ArrayList<AsgAlarmBean>>() {
                                      }.getType());
                    jobPool.submit(new ChangeFeedJob(feedPayload, changeFeedUrl, oriBean, newBean));
                } else {
                    LOG.warn(String.format("Failed to find the type %s to update changefeed", type));
                }
            } else if (configType.equals(Constants.CONFIG_TYPE_ENV)) {
                EnvironBean environBean = environDAO.getById(configId);
                String envStageName = String.format("%s (%s)", environBean.getEnv_name(), environBean.getStage_name());
                LOG.info(String.format("Push env %s config change for %s", type, envStageName));
                String configHistoryUrl = String.format("https://deploy.pinadmin.com/env/%s/%s/config_history/",
                                  environBean.getEnv_name(), environBean.getStage_name());
                String feedPayload = String.format(CHANGEFEED_TEMPLATE, configType, envStageName, configHistoryUrl, operator, "False", type);
                if (type.equals(Constants.TYPE_ENV_GENERAL)) {
                    EnvironBean newBean = gson.fromJson(configHistoryBeans.get(0).getConfig_change(), EnvironBean.class);
                    newBean.setLast_update(null);
                    newBean.setLast_operator(null);
                    EnvironBean oriBean = gson.fromJson(configHistoryBeans.get(1).getConfig_change(), EnvironBean.class);
                    oriBean.setLast_update(null);
                    oriBean.setLast_operator(null);
                    jobPool.submit(new ChangeFeedJob(feedPayload, changeFeedUrl, oriBean, newBean));
                } else if (type.equals(Constants.TYPE_ENV_PROMOTE)) {
                    PromoteBean newBean = gson.fromJson(configHistoryBeans.get(0).getConfig_change(), PromoteBean.class);
                    newBean.setLast_update(null);
                    newBean.setLast_operator(null);
                    PromoteBean oriBean = gson.fromJson(configHistoryBeans.get(1).getConfig_change(), PromoteBean.class);
                    oriBean.setLast_update(null);
                    oriBean.setLast_operator(null);
                    jobPool.submit(new ChangeFeedJob(feedPayload, changeFeedUrl, oriBean, newBean));
                } else if (type.equals(Constants.TYPE_ENV_SCRIPT)) {
                    Map<String, String> newBean = gson.fromJson(configHistoryBeans.get(0).getConfig_change(), new TypeToken<Map<String, String>>() {
                                      }.getType());
                    Map<String, String> oriBean = gson.fromJson(configHistoryBeans.get(1).getConfig_change(),
                                      new TypeToken<Map<String, String>>() {
                                      }.getType());
                    jobPool.submit(new ChangeFeedJob(feedPayload, changeFeedUrl, oriBean, newBean));
                } else if (type.equals(Constants.TYPE_ENV_ADVANCED)) {
                    Map<String, String> newConfigs = gson.fromJson(configHistoryBeans.get(0).getConfig_change(), new TypeToken<Map<String, String>>() {
                                      }.getType());
                    Map<String, String> oriConfigs = gson.fromJson(configHistoryBeans.get(1).getConfig_change(),
                                      new TypeToken<Map<String, String>>() {
                                      }.getType());
                    jobPool.submit(new ChangeFeedJob(feedPayload, changeFeedUrl, oriConfigs, newConfigs));
                } else if (type.equals(Constants.TYPE_ENV_METRIC)) {
                    List<MetricsConfigBean> newBeans = gson.fromJson(configHistoryBeans.get(0).getConfig_change(), new TypeToken<ArrayList<MetricsConfigBean>>() {
                                      }.getType());
                    List<MetricsConfigBean> oriBeans = gson.fromJson(configHistoryBeans.get(1).getConfig_change(), new TypeToken<ArrayList<MetricsConfigBean>>() {
                                      }.getType());
                    jobPool
                        .submit(new ChangeFeedJob(feedPayload, changeFeedUrl, oriBeans, newBeans));
                } else if (type.equals(Constants.TYPE_ENV_ALARM)) {
                    List<AlarmBean> newBean = gson.fromJson(configHistoryBeans.get(0).getConfig_change(), new TypeToken<ArrayList<AlarmBean>>() {
                                      }.getType());
                    List<AlarmBean> oriBean = gson.fromJson(configHistoryBeans.get(1).getConfig_change(), new TypeToken<ArrayList<AlarmBean>>() {
                                      }.getType());
                    jobPool.submit(new ChangeFeedJob(feedPayload, changeFeedUrl, oriBean, newBean));
                } else if (type.equals(Constants.TYPE_ENV_WEBHOOK)) {
                    EnvWebHookBean newBean = gson.fromJson(configHistoryBeans.get(0).getConfig_change(), EnvWebHookBean.class);
                    EnvWebHookBean oriBean = gson.fromJson(configHistoryBeans.get(1).getConfig_change(), EnvWebHookBean.class);
                    jobPool.submit(new ChangeFeedJob(feedPayload, changeFeedUrl, oriBean, newBean));
                } else if (type.equals(Constants.TYPE_ENV_HOST_CAPACITY)) {
                    List<String> newHosts = gson.fromJson(configHistoryBeans.get(0).getConfig_change(), new TypeToken<ArrayList<String>>() {
                                      }.getType());
                    List<String> oriHosts = gson.fromJson(configHistoryBeans.get(1).getConfig_change(), new TypeToken<ArrayList<String>>() {
                                      }.getType());
                    LOG.debug(String.format("Host update %s, %s", newHosts, oriHosts));
                    jobPool.submit(new ChangeFeedJob(feedPayload, changeFeedUrl, oriHosts, newHosts));
                } else if (type.equals(Constants.TYPE_ENV_GROUP_CAPACITY)) {
                    List<String> newGroups = gson.fromJson(configHistoryBeans.get(0).getConfig_change(), new TypeToken<ArrayList<String>>() {
                                      }.getType());
                    List<String> oriGroups = gson.fromJson(configHistoryBeans.get(1).getConfig_change(), new TypeToken<ArrayList<String>>() {
                                      }.getType());
                    jobPool.submit(new ChangeFeedJob(feedPayload, changeFeedUrl, oriGroups, newGroups));
                } else if (type.equals(Constants.TYPE_ENV_CLUSTER)) {
                    ClusterBean newBean = gson.fromJson(configHistoryBeans.get(0).getConfig_change(), ClusterBean.class);
                    ClusterBean oriBean = gson.fromJson(configHistoryBeans.get(1).getConfig_change(), ClusterBean.class);
                    jobPool.submit(new ChangeFeedJob(feedPayload, changeFeedUrl, oriBean, newBean));
                } else {
                    LOG.warn(String.format("Failed to find the type %s to update changefeed", type));
                }
            }
        } catch (Exception ex) {
            LOG.error("Failed to send notification to change log", ex);
        }
    }
}
