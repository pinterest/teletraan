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
package com.pinterest.deployservice.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import javax.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class WorkerJobBean extends BaseBean implements Updatable, Serializable {

    @JsonProperty("id")
    private String id;

    @NotEmpty
    @JsonProperty("jobType")
    private JobType job_type;

    @NotEmpty
    @JsonProperty("config")
    private String config;

    @NotEmpty
    @JsonProperty("status")
    private Status status;

    @NotEmpty
    @JsonProperty("createAt")
    private long create_at;

    @JsonProperty("lastUpdateAt")
    private Long last_update_at;

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("id", id);
        clause.addColumn("job_type", job_type);
        clause.addColumn("config", config);
        clause.addColumn("status", status);
        clause.addColumn("create_at", create_at);
        clause.addColumn("last_update_at", last_update_at);
        return clause;
    }

    public enum JobType {
        INFRA_APPLY
    }

    public enum Status {
        INITIALIZED,
        RUNNING,
        COMPLETED,
        FAILED
    }
}
