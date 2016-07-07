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

import com.fasterxml.jackson.annotation.JsonProperty;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;


public class AutoScalingRequestBean {
    @NotNull
    @JsonProperty("groupName")
    private String groupName;

    @NotNull
    @JsonProperty("minSize")
    private Integer minSize;

    @NotNull
    @JsonProperty("maxSize")
    private Integer maxSize;

    @JsonProperty("terminationPolicy")
    private String terminationPolicy;

    @JsonProperty("attachExistingInstances")
    private Boolean attachInstances;

    @JsonProperty("enableSpot")
    private Boolean enableSpot;

    @JsonProperty("spotPrice")
    private String spotPrice;

    @JsonProperty("spotRatio")
    private Double spotRatio;

    @JsonProperty("sensitivityRatio")
    private Double sensitivityRatio;

    @JsonProperty("enableResourceLending")
    private Boolean enableResourceLending;

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
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

    public Boolean isAttachInstances() {
        return attachInstances;
    }

    public void setAttachInstances(boolean attachInstances) {
        this.attachInstances = attachInstances;
    }

    public Boolean getEnableSpot() { return enableSpot; }

    public void setEnableSpot(Boolean enableSpot) { this.enableSpot =enableSpot; }

    public String getSpotPrice() { return spotPrice; }

    public void setSpotPrice(String spotPrice) { this.spotPrice = spotPrice; }

    public Double getSpotRatio() { return spotRatio; }

    public void setSpotRatio(Double spotRatio) { this.spotRatio = spotRatio; }

    public Double getSensitivityRatio() { return sensitivityRatio; }

    public void setSensitivityRatio(Double sensitivityRatio) { this.sensitivityRatio =sensitivityRatio; }

    public Boolean getEnableResourceLending() { return enableResourceLending; }

    public void setEnableResourceLending(Boolean enableResourceLending) { this.enableResourceLending = enableResourceLending; }
}
