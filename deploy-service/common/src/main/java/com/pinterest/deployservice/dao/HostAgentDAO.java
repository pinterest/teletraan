/*
 * Copyright 2020 Pinterest, Inc.
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

import java.sql.SQLException;
import java.util.List;

import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.HostAgentBean;

/**
 * A collection of methods to help hosts and groups mapping
 */
public interface HostAgentDAO {
    void insert(HostAgentBean hostAgentBean) throws Exception;

    void update(String hostId, HostAgentBean hostAgentBean) throws Exception;

    void delete(String hostId) throws Exception;

    HostAgentBean getHostByName(String hostName) throws Exception;

    HostAgentBean getHostById(String hostId) throws Exception;

    List<HostAgentBean> getStaleHosts(long lastUpdateBefore) throws SQLException;

    List<HostAgentBean> getStaleHosts(long lastUpdateAfter, long lastUpdateBefore) throws SQLException;

    List<HostAgentBean> getStaleEnvHosts(long lastUpdateBefore) throws Exception;

    List<HostAgentBean> getHostsByAgent(String agentVersion, long pageIndex, int pageSize) throws Exception;

    long getDistinctHostsCount() throws SQLException;

    /**
     * Retrieves the main environment ID for the specified host ID.
     *
     * The main environment is where the cluster that the host belongs to is created.
     *
     * @param hostId The ID of the host.
     * @return The bean represents the main environment for the specified host ID.
     * @throws SQLException if an error occurs while retrieving the main environment ID.
     */
    EnvironBean getMainEnvIdbyHostId(String hostId) throws SQLException;
}
