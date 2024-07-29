/**
 * Copyright 2016 Pinterest, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pinterest.deployservice.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * Keep the bean and table in sync
 * CREATE TABLE host_tags (
 * host_id          VARCHAR(64)         NOT NULL,
 * env_id           VARCHAR(22)         NOT NULL,
 * tag_name         VARCHAR(64)         NOT NULL,
 * tag_value        VARCHAR(256),
 * create_date      BIGINT              NOT NULL,
 * PRIMARY KEY    (host_id, tag_name)
 * );
 */
public class HostTagBean implements Updatable {
    public final static String UPDATE_CLAUSE =
        "host_id=VALUES(host_id)," +
            "env_id=VALUES(env_id)," +
            "tag_name=VALUES(tag_name)," +
            "tag_value=VALUES(tag_value)," +
            "create_date=VALUES(create_date)";
    @JsonProperty("hostId")
    private String host_id;
    @JsonProperty("envId")
    private String env_id;
    @JsonProperty("tagName")
    private String tag_name;
    @JsonProperty("tagValue")
    private String tag_value;
    @JsonProperty("createDate")
    private Long create_date;

    public String getHost_id() {
        return host_id;
    }

    public void setHost_id(String host_id) {
        this.host_id = host_id;
    }

    public String getTag_name() {
        return tag_name;
    }

    public void setTag_name(String tag_name) {
        this.tag_name = tag_name;
    }

    public String getTag_value() {
        return tag_value;
    }

    public void setTag_value(String tag_value) {
        this.tag_value = tag_value;
    }

    public Long getCreate_date() {
        return create_date;
    }

    public void setCreate_date(Long create_date) {
        this.create_date = create_date;
    }

    public String getEnv_id() {
        return env_id;
    }

    public void setEnv_id(String env_id) {
        this.env_id = env_id;
    }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("host_id", host_id);
        clause.addColumn("env_id", env_id);
        clause.addColumn("tag_name", tag_name);
        clause.addColumn("tag_value", tag_value);
        clause.addColumn("create_date", create_date);
        return clause;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
