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
package com.pinterest.teletraan.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AutoScalingFactory {

    @JsonProperty private String quboleAuthentication;

    @JsonProperty private int spotAutoScalingThreshold;

    public String getQuboleAuthentication() {
        return quboleAuthentication;
    }

    public void setQuboleAuthentication(String quboleAuthentication) {
        this.quboleAuthentication = quboleAuthentication;
    }

    public int getSpotAutoScalingThreshold() {
        return spotAutoScalingThreshold;
    }

    public void setSpotAutoScalingThreshold(Integer spotAutoScalingThreshold) {
        this.spotAutoScalingThreshold = spotAutoScalingThreshold;
    }
}
