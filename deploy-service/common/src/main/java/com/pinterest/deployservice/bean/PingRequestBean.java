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
import java.util.Set;
import javax.validation.constraints.NotEmpty;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class PingRequestBean {
    @NotEmpty private String hostId;

    private String hostName;

    private String hostIp;

    private String autoscalingGroup;

    private String availabilityZone;

    private String ec2Tags;

    private String agentVersion;

    private EnvType stageType;

    private Set<String> groups;

    private String accountId;

    private String normandieStatus;

    private String knoxStatus;

    private List<PingReportBean> reports;

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getHostIp() {
        return hostIp;
    }

    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
    }

    public String getAutoscalingGroup() {
        return this.autoscalingGroup;
    }

    public void setAutoscalingGroup(String autoscalingGroup) {
        this.autoscalingGroup = autoscalingGroup;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    public String getEc2Tags() {
        return ec2Tags;
    }

    public void setEc2Tags(String ec2Tags) {
        this.ec2Tags = ec2Tags;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    public void setAgentVersion(String agentVersion) {
        this.agentVersion = agentVersion;
    }

    public EnvType getStageType() {
        return stageType;
    }

    public void setStageType(EnvType stageType) {
        this.stageType = stageType;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public void setGroups(Set<String> groups) {
        this.groups = groups;
    }

    public List<PingReportBean> getReports() {
        return reports;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getNormandieStatus() { return normandieStatus; }

    public void setNormandieStatus(String normandieStatus) { this.normandieStatus = normandieStatus; }

    public String getKnoxStatus() { return knoxStatus; }

    public void setKnoxStatus(String knoxStatus) { this.knoxStatus = knoxStatus; }

    public void setReports(List<PingReportBean> reports) {
        this.reports = reports;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
