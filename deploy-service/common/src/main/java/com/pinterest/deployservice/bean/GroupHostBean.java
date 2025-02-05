/**
 * Copyright (c) 2016-2025 Pinterest, Inc.
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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.validation.constraints.NotNull;

public class GroupHostBean {
    @NotNull
    @JsonProperty("autoscaling_hosts")
    private List<String> autoScalingGroupHosts;

    @NotNull
    @JsonProperty("other_hosts")
    private List<String> hostsInGroup;

    public void setAutoScalingGroupHosts(List<String> autoScalingGroupHosts) {
        this.autoScalingGroupHosts = autoScalingGroupHosts;
    }

    public List<String> getAutoScalingGroupHosts() {
        return autoScalingGroupHosts;
    }

    public void setHostsInGroup(List<String> hostsInGroup) {
        this.hostsInGroup = hostsInGroup;
    }

    public List<String> getHostsInGroup() {
        return hostsInGroup;
    }
}
