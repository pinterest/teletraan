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
 create table new_instances_reports (
 host_id              VARCHAR(64)        NOT NULL,
 env_id               VARCHAR(64)        NOT NULL,
 launch_time          BIGINT,
 reported             TINYINT,
 PRIMARY KEY (host_id, env_id)
 );
 */
public class NewInstanceReportBean implements Updatable {
    private String host_id;

    private String env_id;

    private Long launch_time;

    private Boolean reported;

    public void setHost_id(String host_id) { this.host_id = host_id; }

    public String getHost_id() { return host_id; }

    public void setEnv_id(String env_id) { this.env_id = env_id; }

    public String getEnv_id() { return env_id; }

    public void setLaunch_time(Long launch_time) { this.launch_time = launch_time; }

    public Long getLaunch_time() { return launch_time; }

    public void setReported(Boolean reported) { this.reported = reported; }

    public Boolean getReported() { return reported; }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("host_id", host_id);
        clause.addColumn("env_id", env_id);
        clause.addColumn("launch_time", launch_time);
        clause.addColumn("reported", reported);
        return clause;
    }
    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
