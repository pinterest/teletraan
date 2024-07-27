/**
 * Copyright 2024 Pinterest, Inc.
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
 * <p/>
 * CREATE TABLE `pindeploy` (
 * `env_id` varchar(22) NOT NULL,
 * `is_pindeploy` tinyint(1) NOT NULL DEFAULT '0',
 * `pipeline` varchar(128) NOT NULL DEFAULT "",
 * PRIMARY KEY (`env_id`)
);
 */
public class PindeployBean implements Updatable {
    @JsonProperty("envId")
    private String env_id;

    @JsonProperty("isPindeploy")
    private Boolean is_pindeploy;

    @JsonProperty("pipeline")
    private String pipeline;

    public String getEnv_id() {
        return env_id;
    }

    public void setEnv_id(String env_id) {
        this.env_id = env_id;
    }

    public Boolean getIs_pindeploy() {
        return is_pindeploy;
    }

    public void setIs_pindeploy(Boolean is_pindeploy) {
        this.is_pindeploy = is_pindeploy;
    }

    public String getPipeline() {
        return pipeline;
    }

    public void setPipeline(String pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("env_id", env_id);
        clause.addColumn("is_pindeploy", is_pindeploy);
        clause.addColumn("pipeline", pipeline);
        return clause;
    }

    public final static String UPDATE_CLAUSE =
        "env_id=VALUES(env_id)," +
            "is_pindeploy=VALUES(is_pindeploy)," +
            "pipeline=VALUES(pipeline)";

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
