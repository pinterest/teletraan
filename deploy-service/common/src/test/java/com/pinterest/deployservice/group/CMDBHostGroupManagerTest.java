/**
 * Copyright (c) 2017 Pinterest, Inc.
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
import java.util.Map;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class CMDBHostGroupManagerTest {

    @Test
    @Ignore
    public void getHostIdsByGroup() throws Exception {
        CMDBHostGroupManager manager = new CMDBHostGroupManager("http://cmdbapi.pinadmin.com");
        Map<String, HostBean> ret = manager.getHostIdsByGroup("adminapp");
        Assert.assertTrue(ret.size() > 0);
    }

    @Test
    @Ignore
    public void getLastInstanceId() throws Exception {
        CMDBHostGroupManager manager = new CMDBHostGroupManager("http://cmdbapi.pinadmin.com");
        String s = manager.getLastInstanceId("adminapp");
        Assert.assertTrue(s != null && s.length() > 0);
    }
}
