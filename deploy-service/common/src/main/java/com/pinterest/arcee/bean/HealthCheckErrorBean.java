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

import com.pinterest.deployservice.bean.AgentState;
import com.pinterest.deployservice.bean.AgentStatus;
import com.pinterest.deployservice.bean.DeployStage;
import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.bean.Updatable;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

/**
 * CREATE TABLE healthcheck_errors (
 * id                  VARCHAR(64)         NOT NULL,
 * env_id              VARCHAR(22)         NOT NULL,
 * deploy_stage        VARCHAR(32)         NOT NULL,
 * agent_state         VARCHAR(32)         NOT NULL,
 * agent_status        VARCHAR(32)         NOT NULL,
 * last_err_no         INT                 NOT NULL DEFAULT 0,
 * fail_count          INT                 NOT NULL DEFAULT 0,
 * error_msg           TEXT                NOT NULL,
 * agent_start_date    BIGINT              NOT NULL,
 * agent_last_update   BIGINT              NOT NULL,
 * PRIMARY KEY (id)
 * );
 */
public class HealthCheckErrorBean implements Updatable {
    private String id;
    private String env_id;
    private DeployStage deploy_stage;
    private AgentState agent_state;
    private AgentStatus agent_status;
    private Integer last_err_no;
    private Integer fail_count;
    private String error_msg;
    private Long agent_start_date;
    private Long agent_last_update;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEnv_id() {
        return env_id;
    }

    public void setEnv_id(String env_id) {
        this.env_id = env_id;
    }

    public DeployStage getDeploy_stage() {
        return deploy_stage;
    }

    public void setDeploy_stage(DeployStage deploy_stage) {
        this.deploy_stage = deploy_stage;
    }

    public AgentState getAgent_state() {
        return agent_state;
    }

    public void setAgent_state(AgentState agent_state) {
        this.agent_state = agent_state;
    }

    public AgentStatus getAgent_status() {
        return agent_status;
    }

    public void setAgent_status(AgentStatus agent_status) {
        this.agent_status = agent_status;
    }

    public Integer getLast_err_no() {
        return last_err_no;
    }

    public void setLast_err_no(Integer last_err_no) {
        this.last_err_no = last_err_no;
    }

    public Integer getFail_count() {
        return fail_count;
    }

    public void setFail_count(Integer fail_count) {
        this.fail_count = fail_count;
    }

    public String getError_msg() {
        return error_msg;
    }

    public void setError_msg(String error_msg) {
        this.error_msg = error_msg;
    }

    public Long getAgent_start_date() {
        return agent_start_date;
    }

    public void setAgent_start_date(Long agent_start_date) {
        this.agent_start_date = agent_start_date;
    }

    public Long getAgent_last_update() {
        return  agent_last_update;
    }

    public void setAgent_last_update(Long agent_last_update) {
        this.agent_last_update = agent_last_update;
    }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("id", id);
        clause.addColumn("env_id", env_id);
        clause.addColumn("deploy_stage", deploy_stage);
        clause.addColumn("agent_state", agent_state);
        clause.addColumn("agent_status", agent_status);
        clause.addColumn("last_err_no", last_err_no);
        clause.addColumn("fail_count", fail_count);
        clause.addColumn("error_msg", error_msg);
        clause.addColumn("agent_start_date", agent_start_date);
        clause.addColumn("agent_last_update", agent_last_update);
        return clause;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
