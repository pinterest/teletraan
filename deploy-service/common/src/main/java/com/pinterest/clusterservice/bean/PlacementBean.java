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
package com.pinterest.clusterservice.bean;

import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.bean.Updatable;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;

/*
 * Keep the bean and table in sync
 * <p/>
 * CREATE TABLE IF NOT EXISTS placement (
 * id              VARCHAR(128)    NOT NULL,
 * abstract_name   VARCHAR(128)    NOT NULL,
 * provider_name   VARCHAR(128)    NOT NULL,
 * provider        VARCHAR(128)    NOT NULL,
 * basic           TINYINT(1)      DEFAULT  0,
 * capacity        INT             DEFAULT 0,
 * description     TEXT,
 * PRIMARY KEY (id)
 * );
 */
public class PlacementBean implements Updatable {
    private String id;
    private String abstract_name;
    private String provider_name;
    private CloudProvider provider;
    private Boolean basic;
    private Integer capacity;
    private String description;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAbstract_name() {
        return abstract_name;
    }

    public void setAbstract_name(String abstract_name) {
        this.abstract_name = abstract_name;
    }

    public String getProvider_name() {
        return provider_name;
    }

    public void setProvider_name(String provider_name) {
        this.provider_name = provider_name;
    }

    public CloudProvider getProvider() {
        return provider;
    }

    public void setProvider(CloudProvider provider) {
        this.provider = provider;
    }

    public Boolean getBasic() {
        return basic;
    }

    public void setBasic(Boolean basic) {
        this.basic = basic;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("id", id);
        clause.addColumn("abstract_name", abstract_name);
        clause.addColumn("provider_name", provider_name);
        clause.addColumn("provider", provider);
        clause.addColumn("basic", basic);
        clause.addColumn("capacity", capacity);
        clause.addColumn("description", description);
        return clause;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
