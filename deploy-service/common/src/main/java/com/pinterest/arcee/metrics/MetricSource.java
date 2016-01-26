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

package com.pinterest.arcee.metrics;
import com.pinterest.arcee.bean.MetricDatumBean;
import java.util.Collection;
import java.util.Map;

/**
 * An implementation of MetricSource is capable of put and get
 * specific metrics from metric source.
 */
public interface MetricSource {
    /**
     * send a metric data point to the metric source
     */
    void export(String metricName, Map<String, String> tags, Double value, Long timestamp) throws Exception;

    /**
     * get a list of metric data points from metric source
     * @param metricName the name of metrics
     *        start      the start timestamp in milliseconds
     * @return a list of metric data points
     */
    Collection<MetricDatumBean> getMetrics(String metricName, String start) throws Exception;
}
