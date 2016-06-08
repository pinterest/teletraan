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


import org.apache.commons.lang.builder.ReflectionToStringBuilder;

import java.util.Map;

/**
 * baseImageId is the base64UUID created by Teletraan
 * the other resources are represented by abstract name
 */
public class ClusterInfoBean {
    private String clusterName;
    private Integer capacity;
    private CloudProvider provider;
    private String baseImageId;
    private String hostType;
    private String securityZone;
    private String placement;
    private Boolean isDocker;
    private Map<String, String> configs;
    private ClusterState state;

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public CloudProvider getProvider() {
        return provider;
    }

    public void setProvider(CloudProvider provider) {
        this.provider = provider;
    }

    public String getBaseImageId() {
        return baseImageId;
    }

    public void setBaseImageId(String baseImageId) {
        this.baseImageId = baseImageId;
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

    public String getPlacement() {
        return placement;
    }

    public void setPlacement(String placement) {
        this.placement = placement;
    }

    public Boolean getIsDocker() {
        return isDocker;
    }

    public void setIsDocker(Boolean isDocker) {
        this.isDocker = isDocker;
    }

    public Map<String, String> getConfigs() {
        return configs;
    }

    public void setConfigs(Map<String, String> configs) {
        this.configs = configs;
    }

    public ClusterState getState() {
        return state;
    }

    public void setState(ClusterState state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
