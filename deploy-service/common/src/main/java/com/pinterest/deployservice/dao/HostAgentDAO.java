/**
 * Copyright (c) 2016-2023 Pinterest, Inc.
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
package com.pinterest.deployservice.dao;

import com.pinterest.deployservice.bean.HostAgentBean;
import java.sql.SQLException;
import java.util.List;

/** A collection of methods to help hosts and groups mapping */
public interface HostAgentDAO {
    void insert(HostAgentBean hostAgentBean) throws Exception;

    /**
     * Update with the new host agent bean. Only the non-null changed fields are updated.
     *
     * @param hostId
     * @param hostAgentBean the original host agent bean
     * @param newHostAgentBean the new host agent bean
     * @throws SQLException
     */
    void updateChanged(String hostId, HostAgentBean hostAgentBean, HostAgentBean newHostAgentBean)
            throws SQLException;

    void delete(String hostId) throws Exception;

    HostAgentBean getHostByName(String hostName) throws Exception;

    HostAgentBean getHostById(String hostId) throws Exception;

    List<HostAgentBean> getStaleHosts(long lastUpdateBefore) throws SQLException;

    List<HostAgentBean> getStaleHosts(long lastUpdateAfter, long lastUpdateBefore)
            throws SQLException;

    List<HostAgentBean> getStaleEnvHosts(long lastUpdateBefore) throws Exception;

    List<HostAgentBean> getHostsByAgent(String agentVersion, long pageIndex, int pageSize)
            throws Exception;

    long getDistinctHostsCount() throws SQLException;
}
