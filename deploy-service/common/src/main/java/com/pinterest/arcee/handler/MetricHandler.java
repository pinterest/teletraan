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
package com.pinterest.arcee.handler;

import com.pinterest.arcee.bean.MetricDatumBean;
import com.pinterest.arcee.metrics.MetricSource;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.arcee.autoscaling.AlarmManager;
import com.pinterest.arcee.bean.AsgAlarmBean;
import com.pinterest.arcee.dao.AlarmDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class MetricHandler {
    private static final Logger LOG = LoggerFactory.getLogger(MetricHandler.class);
    private static final String COUNTER_NAME = "mimmax:autoscaling.%s.size";
    private static final String COUNTER_NAME_LAUNCH_LATENCY = "mimmax:autoscaling.%s.%s.launchlatency";
    private static final String COUNTER_NAME_DEPLOY_LATENCY = "mimmax:autoscaling.%s.%s.deploylatency";

    private MetricSource client;
    private AlarmDAO alarmDAO;
    private AlarmManager alarmManager;

    public MetricHandler(ServiceContext serviceContext) {
        client = serviceContext.getMetricSource();
        alarmDAO = serviceContext.getAlarmDAO();
        alarmManager = serviceContext.getAlarmManager();
    }

    public Collection<MetricDatumBean> getGroupSizeMetrics(String groupName, String startFrom) throws Exception {
        LOG.info("Get group {} size metrics from openTSDB.", groupName);
        String metricName = String.format(COUNTER_NAME, groupName);
        return client.getMetrics(metricName, startFrom);
    }

    public Collection<MetricDatumBean> getLaunchLatencyMetrics(String envName, String stageName, String startFrom) throws Exception {
        LOG.info(String.format("Get env %s.%s Launch Latency metrics from openTSDB.", envName, stageName));
        String metricName = String.format(COUNTER_NAME_LAUNCH_LATENCY, envName, stageName);
        return client.getMetrics(metricName, startFrom);
    }

    public Collection<MetricDatumBean> getDeployLatencyMetrics(String envName, String stageName, String startFrom) throws Exception {
        LOG.info(String.format("Get env %s.%s Deploy Latency metrics from openTSDB.", envName, stageName));
        String metricName = String.format(COUNTER_NAME_DEPLOY_LATENCY, envName, stageName);
        return client.getMetrics(metricName, startFrom);
    }

    public Collection<MetricDatumBean> getMetricData(String groupName, String metricName, String startFrom) throws Exception {
        AsgAlarmBean asgAlarmBean = alarmDAO.getAlarmInfoByGroupAndMetricSource(groupName, metricName);
        if (!asgAlarmBean.getFrom_aws_metric()) {
            LOG.info("Get metrics {} from openTSDB.", metricName);
            return client.getMetrics(metricName, startFrom);
        } else {
            LOG.info("Get metrics {} from Aws CloudWatch.", metricName);
            return alarmManager.getMetricsStatistics(groupName, metricName, startFrom);
        }
    }

    public Collection<MetricDatumBean> getRawMetricData(String metricName, String startFrom) throws Exception {
        return client.getMetrics(metricName, startFrom);
    }
}
