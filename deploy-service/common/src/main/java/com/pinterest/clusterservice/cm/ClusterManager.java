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

import java.util.Collection;

public interface ClusterManager {
    void createCluster(String clusterName, ClusterBean bean) throws Exception;

    void updateCluster(String clusterName, ClusterBean bean) throws Exception;

    ClusterBean getCluster(String clusterName) throws Exception;

    void deleteCluster(String clusterName) throws Exception;

    Collection<HostBean> launchHosts(String clusterName, int num, boolean launchInAsg) throws Exception;

    void terminateHosts(String clusterName, Collection<String> hostIds, boolean replaceHost) throws Exception;

    Collection<String> getHosts(String clusterName, Collection<String> hostIds) throws Exception;
}
