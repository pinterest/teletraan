/*
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
package com.pinterest.clusterservice.cm;

import com.pinterest.clusterservice.bean.ClusterBean;
import com.pinterest.deployservice.bean.HostBean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class DefaultClusterManager implements ClusterManager {
    @Override
    public void createCluster(String clusterName, ClusterBean bean) throws Exception {
    }

    @Override
    public void updateCluster(String clusterName, ClusterBean bean) throws Exception {
    }

    @Override
    public ClusterBean getCluster(String clusterName) throws Exception {
        return new ClusterBean();
    }

    @Override
    public void deleteCluster(String clusterName) throws Exception {
    }

    @Override
    public Collection<HostBean> launchHosts(String clusterName, int num, boolean launchInAsg) throws Exception {
        return Collections.emptyList();
    }

    @Override
    public void terminateHosts(String clusterName, Collection<String> hostIds, boolean replaceHost) throws Exception {
    }

    @Override
    public Collection<String> getHosts(String clusterName, Collection<String> hostIds) throws Exception {
        return new ArrayList<>();
    }
}
