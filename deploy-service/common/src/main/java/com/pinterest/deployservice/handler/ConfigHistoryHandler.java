/**
 * Copyright (c) 2016-2024 Pinterest, Inc.
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
import com.pinterest.teletraan.universal.events.AppEventPublisher;
import com.pinterest.teletraan.universal.events.ResourceChangedEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigHistoryHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigHistoryHandler.class);
    private static final String CHANGEFEED_TEMPLATE =
            "{\"type\":\"%s\","
                    + "\"environment\":\"%s\","
                    + "\"url\":\"%s\","
                    + "\"description\":\"%s %s\","
                    + "\"author\":\"%s\","
                    + "\"automation\":\"%s\","
                    + "\"source\":\"Teletraan\","
                    + "\"nimbus_uuid\":\"%s\"}";
    private final ConfigHistoryDAO configHistoryDAO;
    private final EnvironDAO environDAO;
    private final String changeFeedUrl;
    private final ExecutorService jobPool;
    private final AppEventPublisher eventPublisher;

    public ConfigHistoryHandler(ServiceContext serviceContext) {
        configHistoryDAO = serviceContext.getConfigHistoryDAO();
        environDAO = serviceContext.getEnvironDAO();
        changeFeedUrl = serviceContext.getChangeFeedUrl();
        jobPool = serviceContext.getJobPool();
        eventPublisher = serviceContext.getAppEventPublisher();
    }

    /** Json Field ExclusionStrategy Used to avoid to read hidden thrift obj - __isset_bit_vector */
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

    /** Use config_id to find all configuration history */
    public List<ConfigHistoryBean> getConfigHistoryByName(
            String config_id, int pageIndex, int pageSize) throws Exception {
        return configHistoryDAO.getByConfigId(config_id, pageIndex, pageSize);
    }

    /**
     * add new config history config_id can be env_id for environment config, or group_name for
     * group configuration
     */
    public void updateConfigHistory(String configId, String type, Object request, String operator)
            throws Exception {
        try {
            ConfigHistoryBean bean = new ConfigHistoryBean();
            bean.setConfig_id(configId);
            bean.setChange_id(CommonUtils.getBase64UUID());
            bean.setCreation_time(System.currentTimeMillis());
            bean.setOperator(operator);
            bean.setType(type);
            Gson gson =
                    new GsonBuilder()
                            .addSerializationExclusionStrategy(new CustomExclusionStrategy())
                            .create();
            bean.setConfig_change(gson.toJson(request));

            configHistoryDAO.insert(bean);
        } catch (Exception e) {
            LOG.warn("Failed to persist a config change for group: " + configId, e);
        }
    }

    public void updateChangeFeed(
            String configType, String configId, String type, String operator, String nimbusUUID) {
        try {
            Gson gson =
                    new GsonBuilder()
                            .addSerializationExclusionStrategy(new CustomExclusionStrategy())
                            .create();
            List<ConfigHistoryBean> configHistoryBeans =
                    configHistoryDAO.getLatestChangesByType(configId, type);
            if (configHistoryBeans.size() != 2) {
                return;
            }

            EnvironBean environBean = environDAO.getById(configId);
            if (configType.equals(Constants.CONFIG_TYPE_ENV)) {
                String envStageName =
                        String.format(
                                "%s (%s)", environBean.getEnv_name(), environBean.getStage_name());
                LOG.info(String.format("Push env %s config change for %s", type, envStageName));
                String configHistoryUrl =
                        String.format(
                                "https://deploy.pinadmin.com/env/%s/%s/config_history/",
                                environBean.getEnv_name(), environBean.getStage_name());
                String feedPayload =
                        String.format(
                                CHANGEFEED_TEMPLATE,
                                configType,
                                envStageName,
                                configHistoryUrl,
                                configType,
                                type,
                                operator,
                                "False",
                                nimbusUUID);
                if (type.equals(Constants.TYPE_ENV_GENERAL)) {
                    EnvironBean newBean =
                            gson.fromJson(
                                    configHistoryBeans.get(0).getConfig_change(),
                                    EnvironBean.class);
                    newBean.setLast_update(null);
                    newBean.setLast_operator(null);
                    EnvironBean oriBean =
                            gson.fromJson(
                                    configHistoryBeans.get(1).getConfig_change(),
                                    EnvironBean.class);
                    oriBean.setLast_update(null);
                    oriBean.setLast_operator(null);
                    jobPool.submit(new ChangeFeedJob(feedPayload, changeFeedUrl, oriBean, newBean));
                } else if (type.equals(Constants.TYPE_ENV_PROMOTE)) {
                    PromoteBean newBean =
                            gson.fromJson(
                                    configHistoryBeans.get(0).getConfig_change(),
                                    PromoteBean.class);
                    newBean.setLast_update(null);
                    newBean.setLast_operator(null);
                    PromoteBean oriBean =
                            gson.fromJson(
                                    configHistoryBeans.get(1).getConfig_change(),
                                    PromoteBean.class);
                    oriBean.setLast_update(null);
                    oriBean.setLast_operator(null);
                    jobPool.submit(new ChangeFeedJob(feedPayload, changeFeedUrl, oriBean, newBean));
                } else if (type.equals(Constants.TYPE_ENV_SCRIPT)) {
                    Map<String, String> newBean =
                            gson.fromJson(
                                    configHistoryBeans.get(0).getConfig_change(),
                                    new TypeToken<Map<String, String>>() {}.getType());
                    Map<String, String> oriBean =
                            gson.fromJson(
                                    configHistoryBeans.get(1).getConfig_change(),
                                    new TypeToken<Map<String, String>>() {}.getType());
                    jobPool.submit(new ChangeFeedJob(feedPayload, changeFeedUrl, oriBean, newBean));
                } else if (type.equals(Constants.TYPE_ENV_ADVANCED)) {
                    Map<String, String> newConfigs =
                            gson.fromJson(
                                    configHistoryBeans.get(0).getConfig_change(),
                                    new TypeToken<Map<String, String>>() {}.getType());
                    Map<String, String> oriConfigs =
                            gson.fromJson(
                                    configHistoryBeans.get(1).getConfig_change(),
                                    new TypeToken<Map<String, String>>() {}.getType());
                    jobPool.submit(
                            new ChangeFeedJob(feedPayload, changeFeedUrl, oriConfigs, newConfigs));
                } else if (type.equals(Constants.TYPE_ENV_METRIC)) {
                    List<MetricsConfigBean> newBeans =
                            gson.fromJson(
                                    configHistoryBeans.get(0).getConfig_change(),
                                    new TypeToken<ArrayList<MetricsConfigBean>>() {}.getType());
                    List<MetricsConfigBean> oriBeans =
                            gson.fromJson(
                                    configHistoryBeans.get(1).getConfig_change(),
                                    new TypeToken<ArrayList<MetricsConfigBean>>() {}.getType());
                    jobPool.submit(
                            new ChangeFeedJob(feedPayload, changeFeedUrl, oriBeans, newBeans));
                } else if (type.equals(Constants.TYPE_ENV_ALARM)) {
                    List<AlarmBean> newBean =
                            gson.fromJson(
                                    configHistoryBeans.get(0).getConfig_change(),
                                    new TypeToken<ArrayList<AlarmBean>>() {}.getType());
                    List<AlarmBean> oriBean =
                            gson.fromJson(
                                    configHistoryBeans.get(1).getConfig_change(),
                                    new TypeToken<ArrayList<AlarmBean>>() {}.getType());
                    jobPool.submit(new ChangeFeedJob(feedPayload, changeFeedUrl, oriBean, newBean));
                } else if (type.equals(Constants.TYPE_ENV_WEBHOOK)) {
                    EnvWebHookBean newBean =
                            gson.fromJson(
                                    configHistoryBeans.get(0).getConfig_change(),
                                    EnvWebHookBean.class);
                    EnvWebHookBean oriBean =
                            gson.fromJson(
                                    configHistoryBeans.get(1).getConfig_change(),
                                    EnvWebHookBean.class);
                    jobPool.submit(new ChangeFeedJob(feedPayload, changeFeedUrl, oriBean, newBean));
                } else if (type.equals(Constants.TYPE_ENV_HOST_CAPACITY)) {
                    List<String> newHosts =
                            gson.fromJson(
                                    configHistoryBeans.get(0).getConfig_change(),
                                    new TypeToken<ArrayList<String>>() {}.getType());
                    List<String> oriHosts =
                            gson.fromJson(
                                    configHistoryBeans.get(1).getConfig_change(),
                                    new TypeToken<ArrayList<String>>() {}.getType());
                    LOG.debug(String.format("Host update %s, %s", newHosts, oriHosts));
                    jobPool.submit(
                            new ChangeFeedJob(feedPayload, changeFeedUrl, oriHosts, newHosts));
                } else if (type.equals(Constants.TYPE_ENV_GROUP_CAPACITY)) {
                    List<String> newGroups =
                            gson.fromJson(
                                    configHistoryBeans.get(0).getConfig_change(),
                                    new TypeToken<ArrayList<String>>() {}.getType());
                    List<String> oriGroups =
                            gson.fromJson(
                                    configHistoryBeans.get(1).getConfig_change(),
                                    new TypeToken<ArrayList<String>>() {}.getType());
                    jobPool.submit(
                            new ChangeFeedJob(feedPayload, changeFeedUrl, oriGroups, newGroups));
                } else {
                    LOG.warn(
                            String.format("Failed to find the type %s to update changefeed", type));
                }
            }
            eventPublisher.publishEvent(
                    new ResourceChangedEvent(
                            configType + "_" + type,
                            operator,
                            this,
                            System.currentTimeMillis(),
                            "env",
                            StringUtils.defaultString(environBean.getEnv_name()),
                            "stage",
                            StringUtils.defaultString(environBean.getStage_name()),
                            "operator",
                            StringUtils.defaultString(operator),
                            "nimbus",
                            StringUtils.defaultString(nimbusUUID)));
        } catch (Exception ex) {
            LOG.error("Failed to send notification to change log", ex);
        }
    }
}
