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
package com.pinterest.deployservice.bean;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeployQueryResultBean {
    private List<DeployBean> deploys;
    private Map<String, TagBean> deployTags;
    private Long total;
    private Boolean truncated;

    public DeployQueryResultBean(List<DeployBean> deploys, Long total, Boolean truncated)  {
        this(deploys,total,new HashMap<String,TagBean>(), truncated);
    }

    public DeployQueryResultBean(List<DeployBean> deploys, Long total, Map<String, TagBean> deployTags, Boolean truncated) {
        this.deploys = deploys;
        this.total = total;
        this.deployTags = deployTags;
        this.truncated = truncated;
    }

    public List<DeployBean> getDeploys() {
        return deploys;
    }

    public Boolean isTruncated() {
        return truncated;
    }

    public Long getTotal() {
        return total;
    }

    public Map<String, TagBean> getDeployTags() { return deployTags; }


    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
