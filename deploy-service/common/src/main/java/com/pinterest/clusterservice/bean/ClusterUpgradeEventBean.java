/*
 * Copyright 2016 Pinterest, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pinterest.clusterservice.bean;

import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.bean.Updatable;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;

/**
 * CREATE TABLE IF NOT EXISTS cluster_upgrade_events (
 * id                      VARCHAR(64)      NOT NULL,
 * cluster_name            VARCHAR(128)     NOT NULL,
 * env_id                  VARCHAR(22)      NOT NULL,
 * state                   VARCHAR(64)      NOT NULL,
 * status                  VARCHAR(32)      NOT NULL,
 * host_ids                TEXT,
 * start_time              BIGINT           NOT NULL,
 * state_start_time        BIGINT           NOT NULL,
 * last_worked_on          BIGINT           NOT NULL,
 * error_message           TEXT,
 * PRIMARY KEY (id)
 * )
 */
public class ClusterUpgradeEventBean implements Updatable {
    private String id;
    private String cluster_name;
    private String env_id;
    private ClusterUpgradeEventState state;
    private ClusterUpgradeEventStatus status;
    private String host_ids;
    private Long start_time;
    private Long state_start_time;
    private Long last_worked_on;
    private String error_message;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCluster_name() {
        return cluster_name;
    }

    public void setCluster_name(String cluster_name) {
        this.cluster_name = cluster_name;
    }

    public String getEnv_id() {
        return env_id;
    }

    public void setEnv_id(String env_id) {
        this.env_id = env_id;
    }

    public ClusterUpgradeEventState getState() {
        return state;
    }

    public void setState(ClusterUpgradeEventState state) {
        this.state = state;
    }

    public ClusterUpgradeEventStatus getStatus() {
        return status;
    }

    public void setStatus(ClusterUpgradeEventStatus status) {
        this.status = status;
    }

    public String getHost_ids() {
        return host_ids;
    }

    public void setHost_ids(String host_ids) {
        this.host_ids = host_ids;
    }

    public Long getStart_time() {
        return start_time;
    }

    public void setStart_time(Long start_time) {
        this.start_time = start_time;
    }

    public Long getState_start_time() {
        return state_start_time;
    }

    public void setState_start_time(Long state_start_time) {
        this.state_start_time = state_start_time;
    }

    public Long getLast_worked_on() {
        return last_worked_on;
    }

    public void setLast_worked_on(Long last_worked_on) {
        this.last_worked_on = last_worked_on;
    }

    public String getError_message() {
        return error_message;
    }

    public void setError_message(String error_message) {
        this.error_message = error_message;
    }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("id", id);
        clause.addColumn("cluster_name", cluster_name);
        clause.addColumn("env_id", env_id);
        clause.addColumn("state", state);
        clause.addColumn("status", status);
        clause.addColumn("host_ids", host_ids);
        clause.addColumn("start_time", start_time);
        clause.addColumn("state_start_time", state_start_time);
        clause.addColumn("last_worked_on", last_worked_on);
        clause.addColumn("error_message", error_message);
        return clause;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

}
