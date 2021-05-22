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

import java.util.List;

public class BuildAllowlistFactory {
    @JsonProperty
    private List<String> validBuildURLs;
    @JsonProperty
    private List<String> trustedBuildURLs;
    @JsonProperty
    private List<String> soxBuildURLs;

    public List<String> getValidBuildURLs() {
        return this.validBuildURLs;
    }

    public void setValidBuildURLs(List<String> urls) {
        this.validBuildURLs = urls;
    }

    public List<String> getTrustedBuildURLs() {
        return this.trustedBuildURLs;
    }

    public void setTrustedBuildURLs(List<String> urls) {
        this.trustedBuildURLs = urls;
    }

    public List<String> getsoxBuildURLs() { return this.soxBuildURLs; }

    public void setsoxBuildURLs(List<String> urls) { this.soxBuildURLs = urls; }
}