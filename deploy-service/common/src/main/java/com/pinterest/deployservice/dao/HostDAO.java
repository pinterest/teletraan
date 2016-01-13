/**
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
package com.pinterest.deployservice.dao;

import com.pinterest.deployservice.bean.HostBean;

import java.util.List;
import java.util.Set;

/**
 * A collection of methods to help hosts and groups mapping
 */
public interface HostDAO {
    public List<String> getGroupNamesByHost(String hostName) throws Exception;

    public List<String> getHostNamesByGroup(String groupName) throws Exception;

    public List<String> getHostIdsByGroup(String groupName) throws Exception;

    public void insert(HostBean hostBean) throws Exception;

    public void insertOrUpdate(String hostName, String ip, String hostId, String state, Set<String> groupNames) throws Exception;

    public void updateHostById(String hostId, HostBean hostBean) throws Exception;

    public void deleteById(String hostId) throws Exception;

    public void deleteAllById(String id) throws Exception;

    public void removeHostFromGroup(String hostId, String groupName) throws Exception;

    public List<HostBean> getHostsByGroup(String groupName, long pageIndex, int pageSize) throws Exception;

    public Long getGroupSize(String groupName) throws Exception;

    public List<HostBean> getHosts(String hostName) throws Exception;

    public List<HostBean> getAllActiveHostsByGroup(String groupName) throws Exception;

    public List<HostBean> getHostsByHostId(String hostId) throws Exception;

    public List<HostBean> getTerminatingHosts() throws Exception;

    public List<HostBean> getStaleEnvHosts(long after) throws Exception;

    public HostBean getByEnvIdAndHostId(String envId, String hostId) throws Exception;

}
