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
package com.pinterest.arcee.bean;


import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.bean.Updatable;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

public class MetricBean implements Updatable {
    private String metric_name;
    private String metric_source;
    private String group_name;
    private Boolean from_aws_metric;

    public void setMetric_name(String metric_name) { this.metric_name = metric_name; }

    public String getMetric_name() { return metric_name; }

    public void setMetric_source(String metric_source) { this.metric_source = metric_source; }

    public String getMetric_source() { return metric_source; }

    public void setGroup_name(String group_name) { this.group_name = group_name; }

    public String getGroup_name() { return group_name; }

    public void setFrom_aws_metric(boolean from_aws_metric) { this.from_aws_metric = from_aws_metric; }

    public boolean getFrom_aws_metric() { return from_aws_metric; }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("metric_name", metric_name);
        clause.addColumn("metric_source", metric_source);
        clause.addColumn("group_name", group_name);
        clause.addColumn("from_aws_metric", from_aws_metric);
        return clause;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
