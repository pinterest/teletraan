/**
 * Copyright (c) 2025 Pinterest, Inc.
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
package com.pinterest.teletraan.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.pinterest.deployservice.ci.CIPlatformManager;
import io.dropwizard.jackson.Discoverable;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type",
        defaultImpl = JenkinsFactory.class)
@JsonSubTypes({
    @JsonSubTypes.Type(value = JenkinsFactory.class, name = "jenkins"),
    @JsonSubTypes.Type(value = BuildkiteFactory.class, name = "buildkite")
})
public interface CIPlatformFactory extends Discoverable {
    CIPlatformManager create() throws Exception;

    Integer getPriority();
}
