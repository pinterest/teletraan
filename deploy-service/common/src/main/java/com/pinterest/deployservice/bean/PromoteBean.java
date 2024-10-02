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
package com.pinterest.deployservice.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pinterest.deployservice.common.Constants;
import io.dropwizard.validation.ValidationMethod;
import java.io.Serializable;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.quartz.CronExpression;

public class PromoteBean extends BaseBean implements Updatable, Serializable {
    @JsonProperty("envId")
    private String env_id;

    @JsonProperty("lastOperator")
    private String last_operator;

    @JsonProperty("lastUpdate")
    private Long last_update;

    @NotNull private PromoteType type;

    @JsonProperty("predStage")
    private String pred_stage;

    @Min(1)
    @Max(Constants.DEFAULT_MAX_PROMOTE_QUEUE_SIZE)
    @JsonProperty("queueSize")
    private Integer queue_size;

    private String schedule;

    @Min(0)
    private Integer delay;

    @JsonProperty("disablePolicy")
    private PromoteDisablePolicy disable_policy;

    @JsonProperty("failPolicy")
    private PromoteFailPolicy fail_policy;

    public String getEnv_id() {
        return env_id;
    }

    public void setEnv_id(String env_id) {
        this.env_id = env_id;
    }

    public String getLast_operator() {
        return last_operator;
    }

    public void setLast_operator(String last_operator) {
        this.last_operator = getStringWithinSizeLimit(last_operator, 64);
    }

    public Long getLast_update() {
        return last_update;
    }

    public void setLast_update(Long last_update) {
        this.last_update = last_update;
    }

    public PromoteType getType() {
        return type;
    }

    public void setType(PromoteType type) {
        this.type = type;
    }

    public String getPred_stage() {
        return pred_stage;
    }

    public void setPred_stage(String pred_stage) {
        this.pred_stage = pred_stage;
    }

    public Integer getQueue_size() {
        return queue_size;
    }

    public void setQueue_size(Integer queue_size) {
        this.queue_size = queue_size;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public Integer getDelay() {
        return delay;
    }

    public void setDelay(Integer delay) {
        this.delay = delay;
    }

    public PromoteDisablePolicy getDisable_policy() {
        return disable_policy;
    }

    public void setDisable_policy(PromoteDisablePolicy disable_policy) {
        this.disable_policy = disable_policy;
    }

    public PromoteFailPolicy getFail_policy() {
        return fail_policy;
    }

    public void setFail_policy(PromoteFailPolicy fail_policy) {
        this.fail_policy = fail_policy;
    }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("env_id", env_id);
        clause.addColumn("last_operator", last_operator);
        clause.addColumn("last_update", last_update);
        clause.addColumn("type", type);
        clause.addColumn("pred_stage", pred_stage);
        clause.addColumn("queue_size", queue_size);
        clause.addColumn("schedule", schedule);
        clause.addColumn("delay", delay);
        clause.addColumn("disable_policy", disable_policy);
        clause.addColumn("fail_policy", fail_policy);
        return clause;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

    @ValidationMethod(message = "schedule must be a valid cron expression")
    @JsonIgnore
    public boolean isScheduleValid() {
        if (type != PromoteType.AUTO) {
            return true;
        }
        if (StringUtils.isBlank(schedule)) {
            return true;
        }

        try {
            new CronExpression(schedule);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
