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
package com.pinterest.deployservice.bean;

import java.util.List;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class EnvWebHookBean {
    private List<WebHookBean> preDeployHooks;
    private List<WebHookBean> postDeployHooks;

    public List<WebHookBean> getPreDeployHooks() {
        return preDeployHooks;
    }

    public void setPreDeployHooks(List<WebHookBean> preDeployHooks) {
        this.preDeployHooks = preDeployHooks;
    }

    public List<WebHookBean> getPostDeployHooks() {
        return postDeployHooks;
    }

    public void setPostDeployHooks(List<WebHookBean> postDeployHooks) {
        this.postDeployHooks = postDeployHooks;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
