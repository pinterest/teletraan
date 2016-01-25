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
package com.pinterest.arcee.bean;

import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.bean.Updatable;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;

/**
 * Keep the bean and table in sync
 * <p/>
 * CREATE TABLE asg_lifecycle_events (
 * token_id       VARCHAR(128)     NOT NULL,
 * hook_id        VARCHAR(64)     NOT NULL,
 * group_name     VARCHAR(64)     NOT NULL,
 * host_id        VARCHAR(64)     NOT NULL,
 * start_date     BIGINT          NOT NULL,
 * PRIMARY KEY (token_id)
 * );
 */
public class AsgLifecycleEventBean implements Updatable {
    private String token_id;
    private String hook_id;
    private String group_name;
    private String host_id;
    private Long start_date;

    public String getToken_id() {
        return token_id;
    }

    public void setToken_id(String token_id) {
        this.token_id = token_id;
    }

    public String getHook_id() {
        return hook_id;
    }

    public void setHook_id(String hook_id) {
        this.hook_id = hook_id;
    }

    public String getGroup_name() {
        return group_name;
    }

    public void setGroup_name(String group_name) {
        this.group_name = group_name;
    }

    public String getHost_id() {
        return host_id;
    }

    public void setHost_id(String hook_id) {
        this.host_id = hook_id;
    }

    public Long getStart_date() {
        return start_date;
    }

    public void setStart_date(Long start_date) {
        this.start_date = start_date;
    }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("token_id", token_id);
        clause.addColumn("hook_id", hook_id);
        clause.addColumn("group_name", group_name);
        clause.addColumn("host_id", host_id);
        clause.addColumn("start_date", start_date);
        return clause;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}