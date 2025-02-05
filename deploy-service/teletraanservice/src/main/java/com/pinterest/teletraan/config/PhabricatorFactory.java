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
package com.pinterest.teletraan.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.pinterest.deployservice.scm.PhabricatorManager;
import com.pinterest.deployservice.scm.SourceControlManager;
import javax.validation.constraints.NotNull;

@JsonTypeName("phabricator")
public class PhabricatorFactory implements SourceControlFactory {
    @NotNull @JsonProperty private String typeName;

    @NotNull @JsonProperty private String urlPrefix;

    @NotNull @JsonProperty private String arc;

    @JsonProperty private String arcrc;

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getUrlPrefix() {
        return urlPrefix;
    }

    public void setUrlPrefix(String urlPrefix) {
        this.urlPrefix = urlPrefix;
    }

    public String getArc() {
        return arc;
    }

    public void setArc(String arc) {
        this.arc = arc;
    }

    public String getArcrc() {
        return arcrc;
    }

    public void setArcrc(String arcrc) {
        this.arcrc = arcrc;
    }

    @Override
    public SourceControlManager create() throws Exception {
        return new PhabricatorManager(typeName, urlPrefix, arc, arcrc);
    }
}
