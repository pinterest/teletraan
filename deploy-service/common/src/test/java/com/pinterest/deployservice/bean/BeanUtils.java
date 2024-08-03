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
package com.pinterest.deployservice.bean;

import java.time.Instant;
import java.util.UUID;

public class BeanUtils {
    public static HostBean createHostBean(Instant createDate) {
        HostBean bean = new HostBean();
        bean.setHost_id("i-" + UUID.randomUUID().toString().substring(0, 8));
        bean.setGroup_name("testEnv-testStage");
        bean.setCreate_date(createDate.toEpochMilli());
        bean.setLast_update(createDate.plusSeconds(1).toEpochMilli());
        bean.setCan_retire(0);
        bean.setState(HostState.PROVISIONED);
        return bean;
    }
}
