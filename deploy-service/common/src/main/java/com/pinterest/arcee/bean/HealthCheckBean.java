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

/**
 * create table health_checks (
 * id                  VARCHAR(64)      NOT NULL,
 * group_name          VARCHAR(64)      NOT NULL,
 * env_id              VARCHAR(22)      NOT NULL,
 * deploy_id           VARCHAR(64)      NOT NULL,
 * ami_id              VARCHAR(64)      NOT NULL,
 * state               VARCHAR(64)      NOT NULL,
 * status              VARCHAR(32)      NOT NULL,
 * type                VARCHAR(32)      NOT NULL,
 * host_id             VARCHAR(64),
 * host_launch_time         BIGINT,
 * host_terminated         TINYINT(1),
 * error_message       VARCHAR(2048),
 * deploy_start_time        BIGINT,
 * deploy_complete_time     BIGINT,
 * state_start_time         BIGINT           NOT NULL,
 * start_time               BIGINT           NOT NULL,
 * last_worked_on           BIGINT           NOT NULL,
 * PRIMARY KEY (id)
 * );
 */
public class HealthCheckBean implements Updatable {
    private String id;
    private String group_name;
    private String env_id;
    private String deploy_id;
    private String ami_id;
    private HealthCheckState state;
    private HealthCheckStatus status;
    private HealthCheckType type;
    private String host_id;
    private Long host_launch_time;
    private Boolean host_terminated;
    private String error_message;
    private Long deploy_start_time;
    private Long deploy_complete_time;
    private Long state_start_time;
    private Long start_time;
    private Long last_worked_on;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGroup_name() {
        return group_name;
    }

    public void setGroup_name(String group_name) {
        this.group_name = group_name;
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

    public String getAmi_id() {
        return ami_id;
    }

    public void setAmi_id(String ami_id) {
        this.ami_id = ami_id;
    }

    public HealthCheckState getState() {
        return state;
    }

    public void setState(HealthCheckState state) {
        this.state = state;
    }

    public HealthCheckStatus getStatus() {
        return status;
    }

    public void setStatus(HealthCheckStatus status) {
        this.status = status;
    }

    public HealthCheckType getType() {
        return type;
    }

    public void setType(HealthCheckType type) {
        this.type = type;
    }

    public String getHost_id() {
        return host_id;
    }

    public void setHost_id(String host_id) {
        this.host_id = host_id;
    }

    public Boolean getHost_terminated() {
        return host_terminated;
    }

    public void setHost_terminated(Boolean host_terminated) {
        this.host_terminated = host_terminated;
    }

    public String getError_message() {
        return error_message;
    }

    public void setError_message(String error_message) {
        this.error_message = error_message;
    }

    public Long getHost_launch_time() {
        return host_launch_time;
    }

    public void setHost_launch_time(Long host_launch_time) {
        this.host_launch_time = host_launch_time;
    }

    public Long getDeploy_start_time() {
        return deploy_start_time;
    }

    public void setDeploy_start_time(Long deploy_start_time) {
        this.deploy_start_time = deploy_start_time;
    }

    public Long getDeploy_complete_time() {
        return deploy_complete_time;
    }

    public void setDeploy_complete_time(Long deploy_complete_time) {
        this.deploy_complete_time = deploy_complete_time;
    }

    public Long getState_start_time() {
        return state_start_time;
    }

    public void setState_start_time(Long state_start_time) {
        this.state_start_time = state_start_time;
    }

    public Long getStart_time() {
        return start_time;
    }

    public void setStart_time(Long start_time) {
        this.start_time = start_time;
    }

    public Long getLast_worked_on() {
        return last_worked_on;
    }

    public void setLast_worked_on(Long last_worked_on) {
        this.last_worked_on = last_worked_on;
    }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("id", id);
        clause.addColumn("group_name", group_name);
        clause.addColumn("env_id", env_id);
        clause.addColumn("deploy_id", deploy_id);
        clause.addColumn("ami_id", ami_id);
        clause.addColumn("state", state);
        clause.addColumn("status", status);
        clause.addColumn("type", type);
        clause.addColumn("host_id", host_id);
        clause.addColumn("host_terminated", host_terminated);
        clause.addColumn("error_message", error_message);
        clause.addColumn("host_launch_time", host_launch_time);
        clause.addColumn("deploy_start_time", deploy_start_time);
        clause.addColumn("deploy_complete_time", deploy_complete_time);
        clause.addColumn("state_start_time", state_start_time);
        clause.addColumn("start_time", start_time);
        clause.addColumn("last_worked_on", last_worked_on);
        return clause;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

}
