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

import java.util.List;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class DeployFilterBean {
    private List<String> envIds;
    private List<String> operators;
    private List<DeployType> deployTypes;
    private List<DeployState> deployStates;
    private List<AcceptanceStatus> acceptanceStatuss;
    private String commit;
    private String repo;
    private String branch;
    private Long commitDate;
    private Long before;
    private Long after;
    private Boolean oldestFirst;
    private Integer pageIndex;
    private Integer pageSize;

    public List<String> getEnvIds() {
        return envIds;
    }

    public void setEnvIds(List<String> envIds) {
        this.envIds = envIds;
    }

    public List<String> getOperators() {
        return operators;
    }

    public void setOperators(List<String> operators) {
        this.operators = operators;
    }

    public List<DeployType> getDeployTypes() {
        return deployTypes;
    }

    public void setDeployTypes(List<DeployType> deployTypes) {
        this.deployTypes = deployTypes;
    }

    public List<DeployState> getDeployStates() {
        return deployStates;
    }

    public void setDeployStates(List<DeployState> deployStates) {
        this.deployStates = deployStates;
    }

    public List<AcceptanceStatus> getAcceptanceStatuss() {
        return acceptanceStatuss;
    }

    public void setAcceptanceStatuss(List<AcceptanceStatus> acceptanceStatuss) {
        this.acceptanceStatuss = acceptanceStatuss;
    }

    public String getCommit() {
        return commit;
    }

    public void setCommit(String commit) {
        this.commit = commit;
    }

    public String getRepo() {
        return repo;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public Long getCommitDate() {
        return commitDate;
    }

    public void setCommitDate(Long commitDate) {
        this.commitDate = commitDate;
    }

    public Long getBefore() {
        return before;
    }

    public void setBefore(Long before) {
        this.before = before;
    }

    public Long getAfter() {
        return after;
    }

    public void setAfter(Long after) {
        this.after = after;
    }

    public Boolean getOldestFirst() {
        return oldestFirst;
    }

    public void setOldestFirst(Boolean oldestFirst) {
        this.oldestFirst = oldestFirst;
    }

    public Integer getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(Integer pageIndex) {
        this.pageIndex = pageIndex;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
