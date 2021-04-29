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
package com.pinterest.deployservice.dao;

import com.pinterest.deployservice.bean.HostBean;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * A collection of methods to help hosts and groups mapping
 */
public interface HostDAO {
    List<String> getGroupNamesByHost(String hostName) throws Exception;

    List<String> getHostNamesByGroup(String groupName) throws Exception;

    Collection<String> getHostIdsByGroup(String groupName) throws Exception;

    void insert(HostBean hostBean) throws Exception;

    void insertOrUpdate(String hostName, String ip, String hostId, String state, Set<String> groupNames) throws Exception;

    void updateHostById(String hostId, HostBean hostBean) throws Exception;

    void deleteById(String hostId) throws Exception;

    void deleteAllById(String id) throws Exception;

    void removeHostFromGroup(String hostId, String groupName) throws Exception;

    List<HostBean> getHostsByGroup(String groupName, long pageIndex, int pageSize) throws Exception;

    Long getGroupSize(String groupName) throws Exception;

    List<HostBean> getHosts(String hostName) throws Exception;

    List<HostBean> getAllActiveHostsByGroup(String groupName) throws Exception;

    List<HostBean> getHostsByHostId(String hostId) throws Exception;

    List<HostBean> getTerminatingHosts() throws Exception;

    Collection<HostBean> getHostsByEnvId(String envId) throws Exception;

    HostBean getByEnvIdAndHostId(String envId, String hostId) throws Exception;

    Collection<HostBean> getByEnvIdAndHostName(String envId, String hostName) throws Exception;

    Collection<String> getToBeRetiredHostIdsByGroup(String groupName) throws Exception;

    Collection<String> getToBeRetiredAndFailedHostIdsByGroup(String groupName) throws Exception;

    Collection<String> getNewAndServingBuildHostIdsByGroup(String groupName) throws Exception;

    Collection<String> getNewHostIdsByGroup(String groupName) throws Exception;

    Collection<String> getFailedHostIdsByGroup(String groupName) throws Exception;
}
