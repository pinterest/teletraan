/**
 * Copyright 2021 Pinterest, Inc.
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
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

/**
 * Keep the bean and table in sync
 * <p>
 * CREATE TABLE services (
 * svc_name         VARCHAR(64)         NOT NULL,
 * system_priority  INT,
 * PRIMARY KEY    svc_name
 * );
 */
public class ServiceBean implements Updatable {
    @JsonProperty("svcName")
    private String svc_name;

    @JsonProperty("systemPriority")
    private Integer system_priority;

    public String getSvc_name() {
        return svc_name;
    }
    public void setSvc_name(String name) {
        this.svc_name = name;
    }

    public Integer getSystem_priority() {
        return system_priority;
    }

    public void setSystem_priority(Integer system_priority) {
        this.system_priority = system_priority;
    }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("svc_name", svc_name);
        clause.addColumn("system_priority", system_priority);
        return clause;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
} 