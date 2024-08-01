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

/**
 * Keep the bean and table in sync
 * <p/>
 * CREATE TABLE datas (
 * data_id       VARCHAR(22)         NOT NULL,
 * data_kind     VARCHAR(32)         NOT NULL,
 * operator      VARCHAR(64)         NOT NULL,
 * timestamp     BIGINT              NOT NULL,
 * data          TEXT                NOT NULL,
 * PRIMARY KEY   (data_id)
 * );
 */
public class DataBean implements Updatable {
    private String data_id;
    // TODO deprecate data_kind, we should use json all the time
    private String data_kind;
    private String operator;
    private Long timestamp;
    private String data;

    public String getData_id() {
        return data_id;
    }

    public void setData_id(String data_id) {
        this.data_id = data_id;
    }

    public String getData_kind() {
        return data_kind;
    }

    public void setData_kind(String data_kind) {
        this.data_kind = data_kind;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("data_id", data_id);
        clause.addColumn("data_kind", data_kind);
        clause.addColumn("operator", operator);
        clause.addColumn("data", data);
        clause.addColumn("timestamp", timestamp);
        return clause;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
