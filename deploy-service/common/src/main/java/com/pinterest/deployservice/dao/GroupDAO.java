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
package com.pinterest.deployservice.dao;

import java.util.List;

public interface GroupDAO {
    List<String> getAllEnvGroups() throws Exception;

    List<String> getEnvsByGroupName(String groupName) throws Exception;

    List<String> getCapacityHosts(String envId) throws Exception;

    void addHostCapacity(String envId, String host) throws Exception;

    void removeHostCapacity(String envId, String host) throws Exception;

    List<String> getCapacityGroups(String envId) throws Exception;

    void addGroupCapacity(String envId, String group) throws Exception;

    void removeGroupCapacity(String envId, String group) throws Exception;
}
