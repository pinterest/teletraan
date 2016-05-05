/*
 * Copyright 2016 Pinterest, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pinterest.clusterservice.dao;


import com.pinterest.clusterservice.bean.ClusterUpgradeEventBean;

import java.util.Collection;

public interface ClusterUpgradeEventDAO {
    void insertClusterUpgradeEvent(ClusterUpgradeEventBean bean) throws Exception;

    void updateById(String id, ClusterUpgradeEventBean bean) throws Exception;

    ClusterUpgradeEventBean getById(String id) throws Exception;

    Collection<ClusterUpgradeEventBean> getOngoingEvents() throws Exception;

    Collection<ClusterUpgradeEventBean> getOngoingEventsByCluster(String clusterName) throws Exception;
}
