/**
 * Copyright (c) 2016 Pinterest, Inc.
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
package com.pinterest.deployservice.group;

import com.pinterest.deployservice.bean.HostBean;
import java.util.Collections;
import java.util.Map;

public class DefaultHostGroupManager implements HostGroupManager {

    @Override
    public Map<String, HostBean> getHostIdsByGroup(String groupName) throws Exception {
        return Collections.emptyMap();
    }

    @Override
    public String getLastInstanceId(String groupName) throws Exception {
        return null;
    }
}
