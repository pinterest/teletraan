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
import com.pinterest.deployservice.bean.ASGStatus;

import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;

public class AutoScalingGroupBean {
    @NotEmpty
    @JsonProperty("minSize")
    private int minSize;

    @NotEmpty
    @JsonProperty("maxSize")
    private int maxSize;

    @NotEmpty
    @JsonProperty("status")
    private ASGStatus status;

    @NotEmpty
    @JsonProperty("terminationPolicy")
    private AutoScalingTerminationPolicy terminationPolicy;

    @NotEmpty
    @JsonProperty("instances")
    private List<String> instances;

    @NotEmpty
    @JsonProperty("spotGroup")
    private Boolean spotGroup;

    @NotEmpty
    @JsonProperty("desiredCapacity")
    private int desiredCapacity;

    public int getDesiredCapacity() {
        return desiredCapacity;
    }

    public void setDesiredCapacity(int desiredCapacity) {
        this.desiredCapacity = desiredCapacity;
    }

    public int getMinSize() { return minSize; }

    public void setMinSize(int minSize) { this.minSize = minSize; }

    public int getMaxSize() { return maxSize; }

    public void setMaxSize(int maxSize) { this.maxSize = maxSize; }

    public ASGStatus getStatus() { return status; }

    public void setStatus(ASGStatus status) { this.status = status; }

    public void setTerminationPolicy(AutoScalingTerminationPolicy terminationPolicy) { this.terminationPolicy = terminationPolicy; }

    public AutoScalingTerminationPolicy getTerminationPolicy() { return terminationPolicy; }

    public List<String> getInstances() { return instances; }

    public void setInstances(List<String> instances) { this.instances = instances; }

    public void addToInstances(String instance) { this.instances.add(instance); }

    public void setSpotGroup(Boolean spotGroup) { this.spotGroup = spotGroup; }

    public Boolean getSpotGroup() { return spotGroup; }
}
