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

import com.pinterest.deployservice.bean.BuildBean;
import com.pinterest.deployservice.bean.DeployBean;

import java.util.List;

/**
 * A collection of methods to help interact with table BUILDS
 */
public interface BuildDAO {
    public void insert(BuildBean buildBean) throws Exception;

    public BuildBean getById(String buildId) throws Exception;

    // commit in short version
    public List<BuildBean> getByCommit7(String scmCommit7, int pageIndex, int pageSize) throws Exception;

    public void delete(String buildId) throws Exception;

    public BuildBean getLatest(String buildName, String branch) throws Exception;

    public List<String> getBuildNames(String nameFilter, int pageIndex, int pageSize) throws Exception;

    public List<BuildBean> getByName(String buildName, String branch, int pageIndex, int pageSize) throws Exception;

    public List<BuildBean> getByNameDate(String buildName, String branch, long before, long after) throws Exception;

    public List<String> getBranches(String buildName) throws Exception;

    // Return up to size number of builds whose publish time is after after
    public List<BuildBean> getAcceptedBuilds(String buildName, String branch, long after, int size) throws Exception;

    // Return all distinct build names
    public List<String> getAllBuildNames() throws Exception;

    // Get total number of builds by build name
    public long countBuildsByName(String buildName) throws Exception;

    // Get all unused builds whose publish time is before timeThreshold
    public void deleteUnusedBuilds(String buildName, long timeThreshold, long numOfBuilds) throws Exception;
}
