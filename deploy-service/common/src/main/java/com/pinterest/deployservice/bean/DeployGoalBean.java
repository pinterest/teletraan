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

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.util.Map;

public class DeployGoalBean {
    private String deployId;
    private DeployType deployType;
    private String envId;
    private String envName;
    private String stageName;
    private EnvType stageType;
    private DeployStage deployStage;
    private BuildBean build;
    private String deployAlias;
    private Map<String, String> agentConfigs;
    private Map<String, String> scriptVariables;
    private Boolean firstDeploy;
    private Boolean isDocker;

    public String getDeployId() {
        return deployId;
    }

    public void setDeployId(String deployId) {
        this.deployId = deployId;
    }

    public String getEnvId() {
        return envId;
    }

    public void setEnvId(String envId) {
        this.envId = envId;
    }

    public String getEnvName() {
        return envName;
    }

    public void setEnvName(String envName) {
        this.envName = envName;
    }

    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public EnvType getStageType() {
        return stageType;
    }

    public void setStageType(EnvType stageType) {
        this.stageType = stageType;
    }

    public DeployStage getDeployStage() {
        return deployStage;
    }

    public void setDeployStage(DeployStage deployStage) {
        this.deployStage = deployStage;
    }

    public BuildBean getBuild() {
        return build;
    }

    public void setBuild(BuildBean build) {
        this.build = build;
    }

    public String getDeployAlias() {
        return deployAlias;
    }

    public void setDeployAlias(String deployAlias) {
        this.deployAlias = deployAlias;
    }

    public Map<String, String> getAgentConfigs() {
        return agentConfigs;
    }

    public void setAgentConfigs(Map<String, String> agentConfigs) {
        this.agentConfigs = agentConfigs;
    }

    public Map<String, String> getScriptVariables() {
        return scriptVariables;
    }

    public void setScriptVariables(Map<String, String> scriptVariables) {
        this.scriptVariables = scriptVariables;
    }

    public Boolean getFirstDeploy() { return firstDeploy; }

    public void setFirstDeploy(Boolean firstDeploy) { this.firstDeploy = firstDeploy; }

    public DeployType getDeployType() {
        return deployType;
    }

    public void setDeployType(DeployType deployType) {
        this.deployType = deployType;
    }

    public Boolean getIsDocker() {
        return isDocker;
    }

    public void setIsDocker(Boolean isDocker) {
        this.isDocker = isDocker;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
