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
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.pinterest.deployservice.scm.GithubManager;
import com.pinterest.deployservice.scm.SourceControlManager;
import javax.validation.constraints.NotNull;

@JsonTypeName("github")
public class GithubFactory implements SourceControlFactory {
    @JsonProperty private String token;

    @JsonProperty private String appId;

    @JsonProperty private String appPrivateKeyKnox;

    @JsonProperty private String appOrganization;

    @NotNull @JsonProperty private String typeName;

    @NotNull @JsonProperty private String apiPrefix;

    @NotNull @JsonProperty private String urlPrefix;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppPrivateKeyKnox() {
        return appPrivateKeyKnox;
    }

    public void setAppPrivateKeyKnox(String appPrivateKeyKnox) {
        this.appPrivateKeyKnox = appPrivateKeyKnox;
    }

    public String getAppOrganization() {
        return appOrganization;
    }

    public void setAppOrganization(String appOrganization) {
        this.appOrganization = appOrganization;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getApiPrefix() {
        return apiPrefix;
    }

    public void setApiPrefix(String apiPrefix) {
        this.apiPrefix = apiPrefix;
    }

    public String getUrlPrefix() {
        return urlPrefix;
    }

    public void setUrlPrefix(String urlPrefix) {
        this.urlPrefix = urlPrefix;
    }

    @Override
    public SourceControlManager create() throws Exception {
        return new GithubManager(
                token, appId, appPrivateKeyKnox, appOrganization, typeName, apiPrefix, urlPrefix);
    }
}
