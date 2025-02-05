/**
 * Copyright (c) 2019-2025 Pinterest, Inc.
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
package com.pinterest.deployservice.allowlists;

import java.util.List;
import org.slf4j.LoggerFactory;

public class BuildAllowlistImpl implements Allowlist {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(BuildAllowlistImpl.class);

    private List<String> validBuildURLs;
    private List<String> trustedBuildURLs;
    private List<String> soxBuildURLs;

    public BuildAllowlistImpl(
            List<String> allowlist, List<String> trustedlist, List<String> soxlist) {
        this.validBuildURLs = allowlist;
        this.trustedBuildURLs = trustedlist;
        this.soxBuildURLs = soxlist;
    }

    // approved checks if build matches approved URL allow list
    public Boolean approved(String buildName) {
        for (String pattern : validBuildURLs) {
            if (buildName.matches(pattern)) {
                return true;
            }
        }
        return false;
    }

    // trusted checks if build matches trusted URL allow list
    public Boolean trusted(String buildName) {
        for (String pattern : trustedBuildURLs) {
            if (buildName.matches(pattern)) {
                return true;
            }
        }
        return false;
    }

    // sox_compliant checks if build matches trusted URL allow list
    public Boolean sox_compliant(String buildName) {
        for (String pattern : soxBuildURLs) {
            if (buildName.matches(pattern)) {
                return true;
            }
        }
        return false;
    }
}
