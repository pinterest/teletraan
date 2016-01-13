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


import com.fasterxml.jackson.annotation.JsonProperty;
import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.bean.Updatable;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

/**
 * Keep the bean and table in sync
 * <p>
 * CREATE TABLE images (
 * id               VARCHAR(30)         NOT NULL,
 * app_name         VARCHAR(64)         NOT NULL,
 * publish_info     VARCHAR(512),
 * publish_date     BIGINT              NOT NULL,
 * qualified        TINYINT(1)          NOT NULL DEFAULT 0,
 * PRIMARY KEY    (id)
 * );
 */
public class ImageBean implements Updatable {
    @JsonProperty("id")
    private String id;

    @JsonProperty("app_name")
    private String app_name;

    @JsonProperty("publishInfo")
    private String publish_info;

    @JsonProperty("publishDate")
    private Long publish_date;

    @JsonProperty("qualified")
    private Boolean qualified;

    public String getId() { return id; }

    public void setId(String ami_id) { this.id = ami_id; }

    public String getApp_name() { return app_name; }

    public void setApp_name(String app_name) { this.app_name = app_name; }

    public String getPublish_info() { return publish_info; }

    public void setPublish_info(String publish_info) { this.publish_info = publish_info; }

    public Long getPublish_date() { return publish_date; }

    public void setPublish_date(Long publish_date) { this.publish_date = publish_date; }

    public Boolean getQualified() { return qualified; }

    public void setQualified(Boolean qualified) { this.qualified = qualified; }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("id", id);
        clause.addColumn("app_name", app_name);
        clause.addColumn("publish_info", publish_info);
        clause.addColumn("publish_date", publish_date);
        clause.addColumn("qualified", qualified);
        return clause;
    }

    @Override
    public String toString() { return ReflectionToStringBuilder.toString(this); }

    public final static String UPDATE_CLAUSE =
            "id=VALUES(id)," +
            "app_name=VALUES(app_name)," +
            "publish_info=VALUES(publish_info)," +
            "publish_date=VALUES(publish_date)," +
            "qualified=VALUES(qualified)";
}
