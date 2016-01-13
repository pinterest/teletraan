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

import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.UpdateStatement;

import java.util.Collection;
import java.util.List;

/**
 * A collection of methods to help interact with Environments tables
 */
public interface EnvironDAO {
    public void insert(EnvironBean bean) throws Exception;

    public void update(String envId, EnvironBean bean) throws Exception;

    public void update(String envName, String envStage, EnvironBean bean) throws Exception;

    public UpdateStatement genUpdateStatement(String envId, EnvironBean bean);

    public void delete(String envId) throws Exception;

    public List<String> getAllEnvNames(String nameFilter, long pageIndex, int pageSize) throws Exception;

    public EnvironBean getById(String envId) throws Exception;

    public List<EnvironBean> getByName(String envName) throws Exception;

    public EnvironBean getByStage(String envName, String envStage) throws Exception;

    public List<String> getOverrideHosts(String envId, String envName, String envStage) throws Exception;

    public long countTotalCapacity(String envId, String envName, String envStage) throws Exception;

    public List<String> getTotalCapacityHosts(String envId, String envName, String envStage) throws Exception;

    public List<EnvironBean> getEnvsByHost(String host) throws Exception;

    public List<EnvironBean> getEnvsByGroups(Collection<String> groups) throws Exception;

    public List<String> getCurrentDeployIds() throws Exception;

    // Return all
    public List<String> getAllEnvIds() throws Exception;

}
