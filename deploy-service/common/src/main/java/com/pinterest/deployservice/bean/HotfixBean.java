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
import javax.validation.constraints.NotEmpty;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class HotfixBean extends BaseBean implements Updatable {
    private String id;

    @NotEmpty
    @JsonProperty("envName")
    private String env_name;

    private HotfixState state;

    @JsonProperty("jobNum")
    private String job_num;

    @JsonProperty("jobName")
    private String job_name;

    @NotEmpty
    @JsonProperty("baseDeployId")
    private String base_deploy;

    @JsonProperty("baseCommit")
    private String base_commit;

    private String repo;

    @NotEmpty private String commits;

    private Integer timeout;

    private String operator;

    @JsonProperty("startDate")
    private Long start_time;

    private Integer progress;

    @JsonProperty("errorMessage")
    private String error_message;

    @JsonProperty("lastWorkedOn")
    private Long last_worked_on;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEnv_name() {
        return env_name;
    }

    public void setEnv_name(String env_name) {
        this.env_name = env_name;
    }

    public HotfixState getState() {
        return state;
    }

    public void setState(HotfixState state) {
        this.state = state;
    }

    public String getJob_num() {
        return job_num;
    }

    public void setJob_num(String job_num) {
        this.job_num = job_num;
    }

    public String getJob_name() {
        return job_name;
    }

    public void setJob_name(String job_name) {
        this.job_name = job_name;
    }

    public String getBase_deploy() {
        return base_deploy;
    }

    public void setBase_deploy(String base_deploy) {
        this.base_deploy = base_deploy;
    }

    public String getBase_commit() {
        return base_commit;
    }

    public void setBase_commit(String base_commit) {
        this.base_commit = base_commit;
    }

    public String getRepo() {
        return repo;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public String getCommits() {
        return commits;
    }

    public void setCommits(String commits) {
        this.commits = commits;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = getStringWithinSizeLimit(operator, 32);
    }

    public Long getStart_time() {
        return start_time;
    }

    public void setStart_time(Long start_time) {
        this.start_time = start_time;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public String getError_message() {
        return error_message;
    }

    public void setError_message(String error_message) {
        this.error_message = error_message;
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
        clause.addColumn("env_name", env_name);
        clause.addColumn("state", state);
        clause.addColumn("job_num", job_num);
        clause.addColumn("job_name", job_name);
        clause.addColumn("base_deploy", base_deploy);
        clause.addColumn("base_commit", base_commit);
        clause.addColumn("repo", repo);
        clause.addColumn("commits", commits);
        clause.addColumn("start_time", start_time);
        clause.addColumn("timeout", timeout);
        clause.addColumn("operator", operator);
        clause.addColumn("progress", progress);
        clause.addColumn("error_message", error_message);
        clause.addColumn("last_worked_on", last_worked_on);
        return clause;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
