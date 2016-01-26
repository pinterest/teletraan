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
package com.pinterest.teletraan.worker;

import com.pinterest.arcee.autoscaling.AlarmManager;
import com.pinterest.arcee.bean.MetricBean;
import com.pinterest.arcee.bean.MetricDatumBean;
import com.pinterest.arcee.metrics.MetricSource;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.arcee.dao.AlarmDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class MetricsCollector implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(MetricsCollector.class);

    private AlarmDAO alarmDAO;
    private AlarmManager alarmManager;
    private MetricSource metricSource;
    public MetricsCollector(ServiceContext serviceContext) {
        alarmDAO = serviceContext.getAlarmDAO();
        alarmManager = serviceContext.getAlarmManager();
        metricSource = serviceContext.getMetricSource();
    }

    private void processMetric(MetricBean metricBean) throws Exception {
        // get data from tsdb
        Collection<MetricDatumBean> dataPoints = metricSource.getMetrics(metricBean.getMetric_source(), "1m-ago");

        // send data to aws
        alarmManager.putMetricsToAlarm(metricBean.getGroup_name(), metricBean.getMetric_name(), dataPoints);
    }

    private void processBatch() throws Exception {
        List<MetricBean> metricBeans = alarmDAO.getMetrics();
        Collections.shuffle(metricBeans);

        for (MetricBean metricBean : metricBeans) {
            LOG.debug(String.format("Get metric bean: %s", metricBean.toString()));
            if (!metricBean.getFrom_aws_metric()) {
                processMetric(metricBean);
            }
        }
    }

    @Override
    public void run() {
        // TODO we should publish metrics and monitoring these workers
        try {
            LOG.info("Start metrics collector process...");
            processBatch();
        } catch (Throwable t) {
            // Catch all throwable so that subsequent job not suppressed
            LOG.error("Failed to run metric collector", t);
        }
    }
}
