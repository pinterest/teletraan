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
package com.pinterest.teletraan.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.pinterest.arcee.metrics.MetricSource;
import com.pinterest.arcee.metrics.StatsboardMetricSource;
import org.hibernate.validator.constraints.NotEmpty;

@JsonTypeName("statsboard")
public class StatsboardMetricSourceFactory implements MetricSourceFactory {
    @NotEmpty
    @JsonProperty
    private String url;

    @NotEmpty
    @JsonProperty
    private String readUrl;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getReadUrl() { return readUrl; }

    public void setReadUrl(String readUrl) { this.readUrl = readUrl; }

    @Override
    public MetricSource create() throws Exception {
        return new StatsboardMetricSource(url, readUrl);
    }
}
