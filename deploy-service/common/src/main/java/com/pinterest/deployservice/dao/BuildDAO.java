/**
 * Copyright 2016 Pinterest, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pinterest.deployservice.dao;

import com.pinterest.deployservice.bean.BuildBean;

import com.google.common.base.Optional;
import org.joda.time.Interval;

import java.util.Collection;
import java.util.List;

/**
 * A collection of methods to help interact with table BUILDS
 */
public interface BuildDAO {
    void insert(BuildBean buildBean) throws Exception;

    BuildBean getById(String buildId) throws Exception;

    // commit in short version
    List<BuildBean> getByCommit7(String scmCommit7, int pageIndex, int pageSize) throws Exception;

    void delete(String buildId) throws Exception;

    BuildBean getLatest(String buildName, String branch) throws Exception;

    List<String> getBuildNames(String nameFilter, int pageIndex, int pageSize) throws Exception;

    List<BuildBean> getByName(String buildName, String branch, int pageIndex, int pageSize) throws Exception;

    List<BuildBean> getByNameDate(String buildName, String branch, long before, long after) throws Exception;

    List<String> getBranches(String buildName) throws Exception;

    // Return up to size number of builds whose publish time is after after
    List<BuildBean> getAcceptedBuilds(String buildName, String branch, Interval interval, int size) throws Exception;

    // Return all distinct build names
    List<String> getAllBuildNames() throws Exception;

    List<BuildBean> getBuildsFromIds(Collection<String> ids) throws Exception;

    // Get total number of builds by build name
    long countBuildsByName(String buildName) throws Exception;

    // Get all unused builds whose publish time is before timeThreshold
    void deleteUnusedBuilds(String buildName, long timeThreshold, long numOfBuilds) throws Exception;

    List<BuildBean> get(String scmCommit, String buildName, String scmBranch,
        Optional<Integer> pageIndex, Optional<Integer> pageSize, Long before, Long after)
        throws Exception;

    List<BuildBean> getByGroupName(String groupName) throws Exception;
}
