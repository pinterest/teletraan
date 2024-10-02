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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class DeployBean extends BaseBean implements Updatable {
    @JsonProperty("id")
    private String deploy_id;

    private String alias;

    @JsonProperty("envId")
    private String env_id;

    @JsonProperty("buildId")
    private String build_id;

    @JsonProperty("type")
    private DeployType deploy_type;

    private DeployState state;

    @JsonProperty("startDate")
    private Long start_date;

    private String operator;

    @JsonProperty("lastUpdateDate")
    private Long last_update;

    private String description;

    @JsonProperty("successTotal")
    private Integer suc_total;

    @JsonProperty("failTotal")
    private Integer fail_total;

    private Integer total;

    @JsonProperty("successDate")
    private Long suc_date;

    @JsonProperty("acceptanceStatus")
    private AcceptanceStatus acc_status;

    @JsonProperty("fromDeployId")
    private String from_deploy;

    public String getDeploy_id() {
        return deploy_id;
    }

    public void setDeploy_id(String deploy_id) {
        this.deploy_id = deploy_id;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getEnv_id() {
        return env_id;
    }

    public void setEnv_id(String env_id) {
        this.env_id = env_id;
    }

    public String getBuild_id() {
        return build_id;
    }

    public void setBuild_id(String build_id) {
        this.build_id = build_id;
    }

    public Long getStart_date() {
        return start_date;
    }

    public void setStart_date(Long start_date) {
        this.start_date = start_date;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = getStringWithinSizeLimit(operator, 64);
    }

    public Long getLast_update() {
        return last_update;
    }

    public void setLast_update(Long last_update) {
        this.last_update = last_update;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSuc_total() {
        return suc_total;
    }

    public void setSuc_total(Integer suc_total) {
        this.suc_total = suc_total;
    }

    public Integer getFail_total() {
        return fail_total;
    }

    public void setFail_total(Integer fail_total) {
        this.fail_total = fail_total;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public Long getSuc_date() {
        return suc_date;
    }

    public void setSuc_date(Long suc_date) {
        this.suc_date = suc_date;
    }

    public DeployType getDeploy_type() {
        return deploy_type;
    }

    public void setDeploy_type(DeployType deploy_type) {
        this.deploy_type = deploy_type;
    }

    public DeployState getState() {
        return state;
    }

    public void setState(DeployState state) {
        this.state = state;
    }

    public AcceptanceStatus getAcc_status() {
        return acc_status;
    }

    public void setAcc_status(AcceptanceStatus acc_status) {
        this.acc_status = acc_status;
    }

    public String getFrom_deploy() {
        return from_deploy;
    }

    public void setFrom_deploy(String from_deploy) {
        this.from_deploy = from_deploy;
    }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("deploy_id", deploy_id);
        clause.addColumn("deploy_type", deploy_type);
        clause.addColumn("alias", alias);
        clause.addColumn("env_id", env_id);
        clause.addColumn("build_id", build_id);
        clause.addColumn("state", state);
        clause.addColumn("start_date", start_date);
        clause.addColumn("operator", operator);
        clause.addColumn("last_update", last_update);
        clause.addColumn("description", description);
        clause.addColumn("suc_total", suc_total);
        clause.addColumn("suc_date", suc_date);
        clause.addColumn("fail_total", fail_total);
        clause.addColumn("total", total);
        clause.addColumn("acc_status", acc_status);
        clause.addColumn("from_deploy", from_deploy);
        return clause;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
