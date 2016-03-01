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


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

public class ScalingPolicyBean {
    @JsonIgnore
    private String policyType;

    @NotNull
    @JsonProperty("scalingType")
    private String scalingType;
    /*
     *
     */
    @NotNull
    @JsonProperty("scaleSize")
    private int scaleSize;

    @NotNull
    @JsonProperty("coolDown")
    private int coolDownTime;

    @JsonIgnore
    private String ARN;

    @JsonIgnore
    private Boolean spotPolicy;

    public String getPolicyType() {
        return policyType;
    }

    public void setPolicyType(String policyType) {
        this.policyType = policyType;
    }

    public String getScalingType() { return scalingType; }

    public void setScalingType(String scalingType) { this.scalingType = scalingType; }

    public int getScaleSize() {
        return scaleSize;
    }

    public void setScaleSize(int scaleSize) {
        this.scaleSize = scaleSize;
    }

    public int getCoolDownTime() {
        return coolDownTime;
    }

    public void setCoolDownTime(int coolDownTime) {
        this.coolDownTime = coolDownTime;
    }

    public String getARN() {
        return ARN;
    }

    public void setARN(String arn) {
        this.ARN = arn;
    }

    public Boolean getSpotPolicy() { return spotPolicy; }

    public void setSpotPolicy(Boolean spotPolicy) { this.spotPolicy = spotPolicy;}
}
