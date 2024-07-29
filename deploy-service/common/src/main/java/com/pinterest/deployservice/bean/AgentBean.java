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
package com.pinterest.deployservice.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * Keep the bean and table in sync
 * <p>
 * CREATE TABLE agents (
 * host_id        VARCHAR(64)         NOT NULL,
 * host_name      VARCHAR(64)         NOT NULL,
 * env_id         VARCHAR(22)         NOT NULL,
 * deploy_id      VARCHAR(22)         NOT NULL,
 * deploy_stage   VARCHAR(32)         NOT NULL,
 * state          VARCHAR(32)         NOT NULL,
 * status         VARCHAR(32)         NOT NULL,
 * start_date     BIGINT              NOT NULL,
 * last_update    BIGINT              NOT NULL,
 * last_operator  VARCHAR(64)         NOT NULL,
 * last_err_no    INT                 NOT NULL DEFAULT 0,
 * fail_count     INT                 NOT NULL DEFAULT 0,
 * first_deploy   TINYINT(1)          NOT NULL DEFAULT 0,
 * first_deploy_time     BIGINT              NOT NULL,
 * stage_start_date     BIGINT              NOT NULL,
 * container_health_status VARCHAR(32) NOT NULL DEFAULT "",
 * PRIMARY KEY    (host_id, env_id)
 * );
 */
public class AgentBean implements Updatable {

    @JsonProperty("hostId")
    private String host_id;

    @JsonProperty("hostName")
    private String host_name;

    @JsonProperty("envId")
    private String env_id;

    @JsonProperty("deployId")
    private String deploy_id;

    @JsonProperty("deployStage")
    private DeployStage deploy_stage;

    @JsonProperty("state")
    private AgentState state;

    @JsonProperty("status")
    private AgentStatus status;

    @JsonProperty("startDate")
    private Long start_date;

    @JsonProperty("lastUpdateDate")
    private Long last_update;

    @JsonProperty("lastOperator")
    private String last_operator;

    @JsonProperty("lastErrno")
    private Integer last_err_no;

    @JsonProperty("failCount")
    private Integer fail_count;

    @JsonProperty("firstDeploy")
    private Boolean first_deploy;

    @JsonProperty("firstDeployDate")
    private Long first_deploy_time;

    @JsonProperty("stageStartDate")
    private Long stage_start_date;

    @JsonProperty("containerHealthStatus")
    private String container_health_status;

    public String getHost_id() {
        return host_id;
    }

    public void setHost_id(String host_id) {
        this.host_id = host_id;
    }

    public String getHost_name() {
        return host_name;
    }

    public void setHost_name(String host_name) {
        this.host_name = host_name;
    }

    public String getEnv_id() {
        return env_id;
    }

    public void setEnv_id(String env_id) {
        this.env_id = env_id;
    }

    public String getDeploy_id() {
        return deploy_id;
    }

    public void setDeploy_id(String deploy_id) {
        this.deploy_id = deploy_id;
    }

    public DeployStage getDeploy_stage() {
        return deploy_stage;
    }

    public void setDeploy_stage(DeployStage deploy_stage) {
        this.deploy_stage = deploy_stage;
    }

    public AgentState getState() {
        return state;
    }

    public void setState(AgentState state) {
        this.state = state;
    }

    public AgentStatus getStatus() {
        return status;
    }

    public void setStatus(AgentStatus status) {
        this.status = status;
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

    public Long getStart_date() {
        return start_date;
    }

    public void setStart_date(Long start_date) {
        this.start_date = start_date;
    }

    public Long getLast_update() {
        return last_update;
    }

    public void setLast_update(Long last_update) {
        this.last_update = last_update;
    }

    public String getLast_operator() {
        return last_operator;
    }

    public void setLast_operator(String last_operator) {
        this.last_operator = last_operator;
    }

    public Boolean getFirst_deploy() {
        return first_deploy;
    }

    public void setFirst_deploy(Boolean first_deploy) {
        this.first_deploy = first_deploy;
    }

    public Long getFirst_deploy_time() {
        return first_deploy_time;
    }

    public void setFirst_deploy_time(Long first_deploy_time) {
        this.first_deploy_time = first_deploy_time;
    }

    public Long getStage_start_date() {
        return stage_start_date;
    }

    public void setStage_start_date(Long stage_start_date) {
        this.stage_start_date = stage_start_date;
    }

    public String getContainer_health_status() {
        return container_health_status;
    }

    public void setContainer_health_status(String container_health_status) {
        this.container_health_status = container_health_status;
    }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("host_id", host_id);
        clause.addColumn("host_name", host_name);
        clause.addColumn("env_id", env_id);
        clause.addColumn("deploy_id", deploy_id);
        clause.addColumn("deploy_stage", deploy_stage);
        clause.addColumn("last_err_no", last_err_no);
        clause.addColumn("fail_count", fail_count);
        clause.addColumn("start_date", start_date);
        clause.addColumn("last_update", last_update);
        clause.addColumn("last_operator", last_operator);
        clause.addColumn("state", state);
        clause.addColumn("status", status);
        clause.addColumn("first_deploy", first_deploy);
        clause.addColumn("first_deploy_time", first_deploy_time);
        clause.addColumn("stage_start_date", stage_start_date);
        clause.addColumn("container_health_status", container_health_status);
        return clause;
    }

    public final static String UPDATE_CLAUSE =
        "host_id=VALUES(host_id)," +
            "host_name=VALUES(host_name),"+
            "deploy_id=VALUES(deploy_id)," +
            "deploy_stage=VALUES(deploy_stage)," +
            "start_date=VALUES(start_date)," +
            "last_update=VALUES(last_update)," +
            "last_operator=VALUES(last_operator)," +
            "state=VALUES(state)," +
            "status=VALUES(status)," +
            "fail_count=VALUES(fail_count)," +
            "last_err_no=VALUES(last_err_no)," +
            "first_deploy=VALUES(first_deploy)," +
            "first_deploy_time=CASE WHEN first_deploy_time IS NULL THEN VALUES(first_deploy_time) ELSE first_deploy_time END," +
            "stage_start_date=VALUES(stage_start_date)," +
            "container_health_status=VALUES(container_health_status)";

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
