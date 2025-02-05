/**
 * Copyright (c) 2017-2025 Pinterest, Inc.
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

import com.pinterest.deployservice.handler.GoalAnalyst;
import java.util.List;

public class PingResult {

    private PingResponseBean responseBean;
    private List<GoalAnalyst.InstallCandidate> installCandidates;
    private List<GoalAnalyst.UninstallCandidate> uninstallCandidateList;

    public PingResult() {}

    public PingResponseBean getResponseBean() {
        return responseBean;
    }

    public List<GoalAnalyst.InstallCandidate> getInstallCandidates() {
        return installCandidates;
    }

    public List<GoalAnalyst.UninstallCandidate> getUninstallCandidateList() {
        return uninstallCandidateList;
    }

    public PingResult withResponseBean(PingResponseBean bean) {
        this.responseBean = bean;
        return this;
    }

    public PingResult withInstallCandidates(List<GoalAnalyst.InstallCandidate> candidates) {
        this.installCandidates = candidates;
        return this;
    }

    public PingResult withUnInstallCandidates(List<GoalAnalyst.UninstallCandidate> candidates) {
        this.uninstallCandidateList = candidates;
        return this;
    }
}
