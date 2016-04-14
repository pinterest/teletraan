/*
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
package com.pinterest.clusterservice.bean;


import org.apache.commons.lang.builder.ReflectionToStringBuilder;

import java.util.Map;

public class AwsVmBean {
    private String clusterName;
    private String image;
    private String hostType;
    private String securityZone;
    private Boolean assignPublicIp;
    private String launchConfigId;
    private String role;
    private Map<String, String> userDataConfigs;
    private String subnet;
    private Integer minSize;
    private Integer maxSize;
    private String terminationPolicy;
    // deprecated
    private String rawUserDataString;

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getHostType() {
        return hostType;
    }

    public void setHostType(String hostType) {
        this.hostType = hostType;
    }

    public String getSecurityZone() {
        return securityZone;
    }

    public void setSecurityZone(String securityZone) {
        this.securityZone = securityZone;
    }

    public Boolean getAssignPublicIp() {
        return assignPublicIp;
    }

    public void setAssignPublicIp(Boolean assignPublicIp) {
        this.assignPublicIp = assignPublicIp;
    }

    public String getLaunchConfigId() {
        return launchConfigId;
    }

    public void setLaunchConfigId(String launchConfigId) {
        this.launchConfigId = launchConfigId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Map<String, String> getUserDataConfigs() {
        return userDataConfigs;
    }

    public void setUserDataConfigs(Map<String, String> userDataConfigs) {
        this.userDataConfigs = userDataConfigs;
    }

    public String getSubnet() {
        return subnet;
    }

    public void setSubnet(String subnet) {
        this.subnet = subnet;
    }

    public Integer getMinSize() {
        return minSize;
    }

    public void setMinSize(Integer minSize) {
        this.minSize = minSize;
    }

    public Integer getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(Integer maxSize) {
        this.maxSize = maxSize;
    }

    public String getTerminationPolicy() {
        return terminationPolicy;
    }

    public void setTerminationPolicy(String terminationPolicy) {
        this.terminationPolicy = terminationPolicy;
    }

    public String getRawUserDataString() { return rawUserDataString; }

    public void setRawUserDataString(String rawUserDataString) { this.rawUserDataString = rawUserDataString; }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}