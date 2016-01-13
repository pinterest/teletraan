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

public class ScalingActivityBean {
    @NotEmpty
    @JsonProperty("description")
    private String description;

    @NotEmpty
    @JsonProperty("cause")
    private String cause;

    @NotEmpty
    @JsonProperty("status")
    private String status;

    @NotEmpty
    @JsonProperty("scalingTime")
    private long scalingTime;

    public void setDescription(String description) { this.description = description; }

    public String getDescription() { return description; }

    public void setCause(String cause) { this.cause = cause; }

    public String getCause() { return cause; }

    public void setStatus(String status) { this.status = status; }

    public String getStatus() { return status; }

    public void setScalingTime(long scalingTime) { this.scalingTime = scalingTime; }

    public long getScalingTime() { return scalingTime; }
}
