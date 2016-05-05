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
package com.pinterest.arcee.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pinterest.clusterservice.bean.AwsVmBean;


public class GroupInfoBean {
    @JsonProperty("groupInfo")
    private GroupBean groupBean;

    @JsonProperty("launchInfo")
    private AwsVmBean awsVmBean;

    public GroupBean getGroupBean() {
         return groupBean;
    }

    public void setGroupBean(GroupBean groupBean) { this.groupBean = groupBean; }

    public AwsVmBean getAwsVmBean() { return awsVmBean; }

    public void setAwsVmBean(AwsVmBean awsVmBean) { this.awsVmBean = awsVmBean; }

}
