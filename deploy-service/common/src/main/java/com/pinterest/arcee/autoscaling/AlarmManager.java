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


import com.pinterest.arcee.bean.AsgAlarmBean;
import com.pinterest.arcee.bean.MetricDatumBean;

import java.util.Collection;
import java.util.List;

public interface AlarmManager {

    void putAlarmToPolicy(String actionId, AsgAlarmBean asgAlarmBean) throws Exception;

    void deleteAlarmFromPolicy(AsgAlarmBean asgAlarmBean) throws Exception;

    void putMetricsToAlarm(String groupName, String metricName, Collection<MetricDatumBean> metricDataPoints) throws Exception;

    List<String> listAwsMetrics(String groupName) throws Exception;

    List<MetricDatumBean> getMetricsStatistics(String groupName, String metricName, String startFrom) throws Exception;
}
