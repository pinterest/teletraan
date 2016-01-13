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
package com.pinterest.arcee.autoscaling;

import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.*;
import com.pinterest.arcee.bean.AsgAlarmBean;
import com.pinterest.arcee.bean.MetricDatumBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.*;

public class AwsAlarmManager implements AlarmManager {
    private static final Logger LOG = LoggerFactory.getLogger(AwsAlarmManager.class);
    private static int TOTAL_RETRY = 5;
    private static final String DIMENTION_NAME = "AutoScalingGroupName";
    private static final String METRIC_NAMESPACE = "AWS/EC2";
    private AmazonCloudWatchClient acwClient;

    public AwsAlarmManager(AmazonCloudWatchClient client) {
        acwClient = client;
    }

    @Override
    public void putAlarmToPolicy(String action, AsgAlarmBean asgAlarmBean) throws Exception {
        PutMetricAlarmRequest request = new PutMetricAlarmRequest();
        List<String> ARNs = new LinkedList<>();
        ARNs.add(action);
        request.setAlarmActions(ARNs);
        if (asgAlarmBean.getFrom_aws_metric()) {
            request.setNamespace(METRIC_NAMESPACE);
        } else {
            request.setNamespace(getNameSpace(asgAlarmBean.getGroup_name()));
        }
        request.setDimensions(Arrays.asList(getDimention(asgAlarmBean.getGroup_name())));
        request.setActionsEnabled(true);

        request.setComparisonOperator(ComparisonOperator.fromValue(asgAlarmBean.getComparator()));
        request.setEvaluationPeriods(asgAlarmBean.getEvaluation_time());
        request.setPeriod(60);
        request.setStatistic(Statistic.Average);

        request.setMetricName(asgAlarmBean.getMetric_name());
        request.setThreshold((double) asgAlarmBean.getThreshold());
        request.setAlarmName(getAlarmName(asgAlarmBean));

        acwClient.putMetricAlarm(request);
    }

    @Override
    public void deleteAlarmFromPolicy(AsgAlarmBean asgAlarmBean) throws Exception {
        DeleteAlarmsRequest request = new DeleteAlarmsRequest();
        List<String> alarmNames = new LinkedList<String>();
        alarmNames.add(getAlarmName(asgAlarmBean));
        request.setAlarmNames(alarmNames);
        acwClient.deleteAlarms(request);
    }

    @Override
    public void putMetricsToAlarm(String groupName, String metricName, List<MetricDatumBean> metricDataPoints) throws Exception {
        List<MetricDatum> metricData = new ArrayList<>();
        if (metricDataPoints.isEmpty()) {
            LOG.debug(String.format("There are no metric data for metric %s, for group %s.", metricName, groupName));
            return;
        }

        for (MetricDatumBean metricDataPoint : metricDataPoints) {
            MetricDatum metricDatum = new MetricDatum();
            metricDatum.setMetricName(metricName);
            metricDatum.setTimestamp(new Date(metricDataPoint.getTimestamp()));
            metricDatum.setValue(metricDataPoint.getValue());
            metricDatum.setDimensions(Arrays.asList(getDimention(groupName)));
            metricData.add(metricDatum);
        }

        PutMetricDataRequest request = new PutMetricDataRequest();
        request.setMetricData(metricData);
        request.setNamespace(getNameSpace(groupName));

        for (int retry = 0; retry < TOTAL_RETRY; retry++) {
            try {
                acwClient.putMetricData(request);
                return;
            } catch (InternalServiceException e) {
                LOG.error("Failed to put metrics to aws: ", e);
            }
        }
    }

    @Override
    public List<String> listAwsMetrics(String groupName) throws Exception {
        DimensionFilter dimensionFilter = new DimensionFilter();
        dimensionFilter.setName(DIMENTION_NAME);
        dimensionFilter.setValue(groupName);
        ListMetricsRequest listMetricsRequest = new ListMetricsRequest();
        listMetricsRequest.withDimensions(dimensionFilter).withNamespace(METRIC_NAMESPACE);

        ListMetricsResult listMetricsResult = acwClient.listMetrics(listMetricsRequest);
        List<String> metricName = new ArrayList<>();
        for (Metric metric : listMetricsResult.getMetrics()) {
            metricName.add(metric.getMetricName());
        }
        return metricName;
    }

    @Override
    public List<MetricDatumBean> getMetricsStatistics(String groupName, String metricName, String startFrom) throws Exception {
        GetMetricStatisticsRequest request = new GetMetricStatisticsRequest().withStatistics("Average");
        request.setNamespace(METRIC_NAMESPACE);
        request.setMetricName(metricName);
        request.setDimensions(Arrays.asList(getDimention(groupName)));
        request.setPeriod(60); // minimum period
        request.setStartTime(getDate(startFrom));
        request.setEndTime(new Date());

        List<Datapoint> datapoints = acwClient.getMetricStatistics(request).getDatapoints();
        List<MetricDatumBean> metricDataPoints = new ArrayList<>();
        for (Datapoint datapoint : datapoints) {
            MetricDatumBean dp = new MetricDatumBean();
            dp.setTimestamp(datapoint.getTimestamp().getTime());
            dp.setValue(datapoint.getAverage());
            metricDataPoints.add(dp);
        }
        return metricDataPoints;
    }

    private String getAlarmName(AsgAlarmBean asgAlarmBean) {
        return String.format("%s-alarm-%s", asgAlarmBean.getGroup_name(), asgAlarmBean.getAlarm_id());
    }

    private Dimension getDimention(String groupName) {
        Dimension dimension = new Dimension();
        dimension.setName(DIMENTION_NAME);
        dimension.setValue(groupName);
        return dimension;
    }

    private static Date getDate(String startFrom) {
        String timeFormat = Arrays.asList(startFrom.split("-")).get(0);
        int num = Integer.valueOf(timeFormat.substring(0, timeFormat.length() - 1));
        char amount = timeFormat.charAt(timeFormat.length() - 1);
        if (amount == 'm') {
            return Date.from(ZonedDateTime.now().minusMinutes(num).toInstant());
        } else if (amount == 'h') {
            return Date.from(ZonedDateTime.now().minusHours(num).toInstant());
        } else if (amount == 'd') {
            return Date.from(ZonedDateTime.now().minusDays(num).toInstant());
        } else if (amount == 'w') {
            return Date.from(ZonedDateTime.now().minusWeeks(num).toInstant());
        } else if (amount == 'n') {
            return Date.from(ZonedDateTime.now().minusMonths(num).toInstant());
        } else if (amount == 'y') {
            return Date.from(ZonedDateTime.now().minusYears(num).toInstant());
        } else {
            return Date.from(ZonedDateTime.now().minusSeconds(num).toInstant());
        }
    }

    private String getNameSpace(String groupName) {
        return String.format("Pinterest/%s", groupName);
    }
}
