/**
 * Copyright (c) 2024 Pinterest, Inc.
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
package com.pinterest.deployservice.fixture;

import com.pinterest.deployservice.bean.AcceptanceType;
import com.pinterest.deployservice.bean.DeployPriority;
import com.pinterest.deployservice.bean.EnvState;
import com.pinterest.deployservice.bean.EnvType;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.EnvironState;
import com.pinterest.deployservice.bean.OverridePolicy;
import java.util.UUID;

public class EnvironBeanFixture {
    public static EnvironBean createRandomEnvironBean() {
        EnvironBean environBean = new EnvironBean();
        environBean.setAccept_type(AcceptanceType.AUTO);
        environBean.setAdv_config_id(UUID.randomUUID().toString().substring(0, 10));
        environBean.setAllow_private_build(false);
        environBean.setDeploy_id(UUID.randomUUID().toString().substring(0, 10));
        environBean.setDescription(UUID.randomUUID().toString());
        environBean.setEnsure_trusted_build(false);
        environBean.setEnv_id(UUID.randomUUID().toString().substring(0, 10));
        environBean.setEnv_name(UUID.randomUUID().toString());
        environBean.setEnv_state(EnvState.NORMAL);
        environBean.setIs_docker(false);
        environBean.setIs_sox(false);
        environBean.setLast_operator(UUID.randomUUID().toString());
        environBean.setLast_update(System.currentTimeMillis());
        environBean.setMax_deploy_day(366);
        environBean.setMax_deploy_num(5100);
        environBean.setMax_parallel_pct(0);
        environBean.setMax_parallel_rp(1);
        environBean.setMax_parallel(10);
        environBean.setNotify_authors(false);
        environBean.setOverride_policy(OverridePolicy.OVERRIDE);
        environBean.setPriority(DeployPriority.NORMAL);
        environBean.setSc_config_id(UUID.randomUUID().toString().substring(0, 10));
        environBean.setStage_name(UUID.randomUUID().toString());
        environBean.setStage_type(EnvType.DEFAULT);
        environBean.setState(EnvironState.NORMAL);
        environBean.setStuck_th(50);
        environBean.setSuccess_th(10000);
        environBean.setWatch_recipients(UUID.randomUUID().toString());
        environBean.setUse_entitlements(Boolean.TRUE);
        return environBean;
    }
}
