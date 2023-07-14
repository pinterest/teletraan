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

import com.pinterest.deployservice.bean.HostAgentBean;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * A collection of methods to help hosts and groups mapping
 */
public interface HostAgentDAO {
    void insert(HostAgentBean hostAgentBean) throws Exception;

    void update(String hostId, HostAgentBean hostAgentBean) throws Exception;

    void delete(String hostId) throws Exception;

    HostAgentBean getHostByName(String hostName) throws Exception;

    HostAgentBean getHostById(String hostId) throws Exception;

    List<HostAgentBean> getStaleHosts(long after) throws Exception;

    List<HostAgentBean> getStaleEnvHosts(long after) throws Exception;

    List<HostAgentBean> getHostsByAgent(String agentVersion, long pageIndex, int pageSize) throws Exception;
}
