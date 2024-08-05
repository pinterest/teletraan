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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.beans.Transient;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class TagBean implements Updatable {

    private static final ObjectMapper mapper = new ObjectMapper();

    @JsonProperty("id")
    private String id;

    @JsonProperty("value")
    private TagValue value;

    @JsonProperty("targetType")
    private TagTargetType target_type;

    @JsonProperty("targetId")
    private String target_id;

    @JsonProperty("operator")
    private String operator;

    @JsonProperty("createdDate")
    private Long created_date;

    @JsonProperty("comments")
    private String comments;

    @JsonProperty("metaInfo")
    private String meta_info;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TagValue getValue() {
        return value;
    }

    public void setValue(TagValue value) {
        this.value = value;
    }

    public TagTargetType getTarget_type() {
        return target_type;
    }

    public void setTarget_type(TagTargetType target_type) {
        this.target_type = target_type;
    }

    public String getTarget_id() {
        return target_id;
    }

    public void setTarget_id(String target_id) {
        this.target_id = target_id;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Long getCreated_date() {
        return created_date;
    }

    public void setCreated_date(Long created_date) {
        this.created_date = created_date;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getMeta_info() {
        return meta_info;
    }

    public void setMeta_info(String meta_info) {
        this.meta_info = meta_info;
    }

    @Transient
    public <T> T deserializeTagMetaInfo(Class<T> theClass) throws Exception {
        return mapper.readValue(this.getMeta_info(), theClass);
    }

    @Transient
    public void serializeTagMetaInfo(Object object) throws Exception {
        this.setMeta_info(mapper.writeValueAsString(object));
    }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("id", this.id);
        clause.addColumn("value", this.value.toString());
        clause.addColumn("target_type", this.target_type.toString());
        clause.addColumn("target_id", this.target_id);
        clause.addColumn("operator", this.operator);
        clause.addColumn("created_date", this.created_date);
        clause.addColumn("comments", this.comments);
        clause.addColumn("meta_info", this.meta_info);
        return clause;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
