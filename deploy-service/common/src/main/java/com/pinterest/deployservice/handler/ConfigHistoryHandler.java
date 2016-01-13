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
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.ConfigHistoryBean;
import com.pinterest.deployservice.common.CommonUtils;
import com.pinterest.deployservice.dao.ConfigHistoryDAO;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * This handler used to deal with all configuration change
 */
public class ConfigHistoryHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigHistoryHandler.class);
    private ConfigHistoryDAO configHistoryDAO;
    private GroupHandler groupHandler;

    private static final String TYPE_ASG_GENERAL = "Asg General Config";
    private static final String TYPE_ASG_SCALING = "Asg Scaling Group";
    private static final String TYPE_ASG_POLICY = "Asg Scaling Policy";
    private static final String TYPE_ASG_ALARM = "Asg Scaling Alarm";

    private static final String TYPE_ENV_GENERAL = "Env General Config";
    private static final String TYPE_ENV_PROMOTE = "Env Promote Config";
    private static final String TYPE_ENV_SCRIPT = "Env Script Config";
    private static final String TYPE_ENV_ADVANCED = "Env Advanced Config";
    private static final String TYPE_ENV_HOST_CAPACITY = "Env Host Capacity Config";
    private static final String TYPE_ENV_GROUP_CAPACITY = "Env Group Capacity Config";
    private static final String TYPE_ENV_METRIC = "Env Metrics Config";
    private static final String TYPE_ENV_ALARM = "Env Alarm Config";
    private static final String TYPE_ENV_WEBHOOK = "Env Webhook Config";

    private static final String TYPE_HOST_LAUNCH = "Host Launch";
    private static final String TYPE_HOST_TERMINATE = "Host Terminate";
    private static final String TYPE_HOST_ATTACH = "Host Attach";
    private static final String TYPE_HOST_DETACH = "Host Detach";

    private static final String TYPE_HELATHCHECK_MANAUALLY = "MANUALLY_TRIGGERED Health Check";

    public ConfigHistoryHandler(ServiceContext serviceContext) {
        configHistoryDAO = serviceContext.getConfigHistoryDAO();
        groupHandler = new GroupHandler(serviceContext);
    }

    public static final String getTypeAsgGeneral() {
        return TYPE_ASG_GENERAL;
    }

    public static final String getTypeAsgScaling() {
        return TYPE_ASG_SCALING;
    }

    public static final String getTypeAsgPolicy() {
        return TYPE_ASG_POLICY;
    }

    public static final String getTypeAsgAlarm() {
        return TYPE_ASG_ALARM;
    }

    public static final String getTypeEnvGeneral() {
        return TYPE_ENV_GENERAL;
    }

    public static final String getTypeEnvPromote() {
        return TYPE_ENV_PROMOTE;
    }

    public static final String getTypeEnvScript() {
        return TYPE_ENV_SCRIPT;
    }

    public static final String getTypeEnvAdvanced() {
        return TYPE_ENV_ADVANCED;
    }

    public static final String getTypeEnvHostCapacity() {
        return TYPE_ENV_HOST_CAPACITY;
    }

    public static final String getTypeEnvGroupCapacity() {
        return TYPE_ENV_GROUP_CAPACITY;
    }

    public static final String getTypeEnvMetric() {
        return TYPE_ENV_METRIC;
    }

    public static final String getTypeEnvAlarm() {
        return TYPE_ENV_ALARM;
    }

    public static final String getTypeEnvWebhook() {
        return TYPE_ENV_WEBHOOK;
    }

    public static final String getTypeHostLaunch() {
        return TYPE_HOST_LAUNCH;
    }

    public static final String getTypeHostTerminate() {
        return TYPE_HOST_TERMINATE;
    }

    public static final String getTypeHostAttach() {
        return TYPE_HOST_ATTACH;
    }

    public static final String getTypeHostDetach() {
        return TYPE_HOST_DETACH;
    }

    public static final String getTypeHelathcheckManaually() {
        return TYPE_HELATHCHECK_MANAUALLY;
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
     * or group_name for autoscaling configuration
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
    public void rollbackConfig(String changeId, String operator) throws Exception {
        try {
            ConfigHistoryBean bean = configHistoryDAO.getByChangeId(changeId);
            String groupName = bean.getConfig_id();
            String type = bean.getType();
            String configChange = bean.getConfig_change();
            LOG.info(String.format("Rollback to the ChangeId:%s, Type:%s", changeId, type));

            Gson gson = new GsonBuilder().addSerializationExclusionStrategy(new CustomExclusionStrategy()).create();
            if (type.equals(TYPE_ASG_GENERAL)) {
                GroupBean groupBean = gson.fromJson(configChange, GroupBean.class);
                groupBean.setGroup_name(groupName);
                String userData = new String(Base64.decodeBase64(groupBean.getUser_data()));
                groupBean.setUser_data(userData);
                groupHandler.updateLaunchConfig(groupName, groupBean);
                updateConfigHistory(groupName, type, groupBean, operator);
            } else if (type.equals(TYPE_ASG_SCALING)) {
                AutoScalingRequestBean autoScalingRequestBean = gson.fromJson(configChange, AutoScalingRequestBean.class);
                autoScalingRequestBean.setGroupName(groupName);
                groupHandler.insertOrUpdateAutoScalingGroup(groupName, autoScalingRequestBean);
                updateConfigHistory(groupName, type, autoScalingRequestBean, operator);
            } else if (type.equals(TYPE_ASG_POLICY)) {
                ScalingPoliciesBean request = gson.fromJson(configChange, ScalingPoliciesBean.class);
                groupHandler.putScalingPolicyToGroup(groupName, request);
                updateConfigHistory(groupName, type, request, operator);
            } else if (type.equals(TYPE_ASG_ALARM)) {
                List<AsgAlarmBean> request = gson.fromJson(configChange, new TypeToken<List<AsgAlarmBean>>() {
                }.getType());
                groupHandler.updateAlarmsToAutoScalingGroup(groupName, request);
                updateConfigHistory(groupName, type, request, operator);
            } else {
                LOG.warn(String.format("Failed to find the type %s to rollback, %s", type, changeId));
            }
        } catch (Exception e) {
            LOG.error("Failed to rollback to previous config, " + changeId, e);
        }
    }
}
