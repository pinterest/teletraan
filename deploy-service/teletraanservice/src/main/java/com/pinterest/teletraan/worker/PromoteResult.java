/**
 * Copyright (c) 2017-2024 Pinterest, Inc.
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
package com.pinterest.teletraan.worker;

import com.pinterest.deployservice.bean.DeployBean;
import com.pinterest.deployservice.bean.EnvironBean;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class PromoteResult {

    public Pair<DeployBean, EnvironBean> predDeployInfo;
    private ResultCode result;
    private String promotedBuild;

    public PromoteResult() {}

    public ResultCode getResult() {
        return result;
    }

    public String getPromotedBuild() {
        return promotedBuild;
    }

    public Pair<DeployBean, EnvironBean> getPredDeployInfo() {
        return predDeployInfo;
    }

    public PromoteResult withResultCode(ResultCode result) {
        this.result = result;
        return this;
    }

    public PromoteResult withBuild(String buildId) {
        this.promotedBuild = buildId;
        return this;
    }

    public PromoteResult withPredDeployBean(DeployBean deployBean, EnvironBean environBean) {
        this.predDeployInfo = new ImmutablePair<DeployBean, EnvironBean>(deployBean, environBean);
        return this;
    }

    public enum ResultCode {
        NotInScheduledTime,
        NoAvailableBuild,
        NoPredEnvironment,
        NoPredEnvironmentDeploy,
        NoCandidateWithinDelayPeriod,
        NoRegularDeployWithinDelayPeriod,
        PromoteBuild,
        PromoteDeploy
    }
}
