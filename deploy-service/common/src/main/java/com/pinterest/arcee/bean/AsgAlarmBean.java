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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.bean.Updatable;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

/*
CREATE TABLE asg_alarms (
        alarm_id            VARCHAR(22)        NOT NULL,
        metric_name         VARCHAR(64)        NOT NULL,
        metric_source       VARCHAR(128)       NOT NULL,
        comparator          VARCHAR(30)        NOT NULL,
        action_type         VARCHAR(10)        NOT NULL,
        group_name          VARCHAR(64)        NOT NULL,
        threshold           BIGINT             NOT NULL,
        evaluation_time     INT                NOT NULL,
        last_update         BIGINT             NOT NULL,
        from_aws_metric     TINYINT(1)         NOT NULL DEFAULT 0,
);
*/
public class AsgAlarmBean implements Updatable {
    @NotEmpty
    @JsonProperty("alarmId")
    private String alarm_id;

    @JsonIgnore
    private String metric_name;

    @NotEmpty
    @JsonProperty("metricSource")
    private String metric_source;

    @NotEmpty
    @JsonProperty("comparator")
    private String comparator;

    @NotEmpty
    @JsonProperty("actionType")
    private String action_type;

    @NotEmpty
    @JsonProperty("groupName")
    private String group_name;

    @NotNull
    @JsonProperty("threshold")
    private Double threshold;

    @NotNull
    @JsonProperty("evaluationTime")
    private Integer evaluation_time;

    @JsonIgnore
    private Long last_update;

    @NotNull
    @JsonProperty("fromAwsMetric")
    private Boolean from_aws_metric;

    public String getAlarm_id() {
        return alarm_id;
    }

    public void setAlarm_id(String alarm_id) {
        this.alarm_id = alarm_id;
    }

    public String getMetric_name() {
        return metric_name;
    }

    public void setMetric_name(String metric_name) {
        this.metric_name = metric_name;
    }

    public String getMetric_source() {
        return metric_source;
    }

    public void setMetric_source(String metric_source) {
        this.metric_source = metric_source;
    }

    public String getComparator() {
        return comparator;
    }

    public void setComparator(String comparator) {
        this.comparator = comparator;
    }

    public String getAction_type() {
        return action_type;
    }

    public void setAction_type(String action_type) {
        this.action_type = action_type;
    }

    public String getGroup_name() {
        return group_name;
    }

    public void setGroup_name(String group_name) {
        this.group_name = group_name;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    public Integer getEvaluation_time() {
        return evaluation_time;
    }

    public void setEvaluation_time(Integer evaluation_time) {
        this.evaluation_time = evaluation_time;
    }

    public Long getLast_update() {
        return last_update;
    }

    public void setLast_update(Long last_update) {
        this.last_update = last_update;
    }

    public Boolean getFrom_aws_metric() {
        return from_aws_metric;
    }

    public void setFrom_aws_metric(Boolean from_aws_metric) {
        this.from_aws_metric = from_aws_metric;
    }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("alarm_id", alarm_id);
        clause.addColumn("metric_name", metric_name);
        clause.addColumn("metric_source", metric_source);
        clause.addColumn("comparator", comparator);
        clause.addColumn("action_type", action_type);
        clause.addColumn("group_name", group_name);
        clause.addColumn("threshold", threshold);
        clause.addColumn("evaluation_time", evaluation_time);
        clause.addColumn("last_update", last_update);
        clause.addColumn("from_aws_metric", from_aws_metric);
        return clause;
    }

    public final static String UPDATE_CLAUSE =
            "alarm_id=VALUES(alarm_id)," +
                    "metric_name=VALUES(metric_name)," +
                    "metric_source=VALUES(metric_source)," +
                    "comparator=VALUES(comparator)," +
                    "action_type=VALUES(action_type)," +
                    "group_name=VALUES(group_name)," +
                    "threshold=VALUES(threshold)," +
                    "evaluation_time=VALUES(evaluation_time)," +
                    "last_update=VALUES(last_update)," +
                    "from_aws_metric=VALUES(from_aws_metric)";

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
