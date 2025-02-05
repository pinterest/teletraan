/**
 * Copyright (c) 2016-2025 Pinterest, Inc.
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

import com.google.common.base.Optional;
import com.pinterest.deployservice.bean.BuildBean;
import java.util.Collection;
import java.util.List;
import org.joda.time.Interval;

/** A collection of methods to help interact with table BUILDS */
public interface BuildDAO {
    void insert(BuildBean buildBean) throws Exception;

    BuildBean getById(String buildId) throws Exception;

    // commit in short version
    List<BuildBean> getByCommit7(String scmCommit7, String buildName, int pageIndex, int pageSize)
            throws Exception;

    void delete(String buildId) throws Exception;

    BuildBean getLatest(String buildName, String branch) throws Exception;

    List<String> getBuildNames(String nameFilter, int pageIndex, int pageSize) throws Exception;

    List<BuildBean> getByName(String buildName, String branch, int pageIndex, int pageSize)
            throws Exception;

    List<BuildBean> getByNameDate(String buildName, String branch, long before, long after)
            throws Exception;

    List<String> getBranches(String buildName) throws Exception;

    // Return up to size number of builds whose publish time is after after
    List<BuildBean> getAcceptedBuilds(String buildName, String branch, Interval interval, int limit)
            throws Exception;

    // Return all distinct build names
    List<String> getAllBuildNames() throws Exception;

    List<BuildBean> getBuildsFromIds(Collection<String> ids) throws Exception;

    // Get total number of builds by build name
    long countBuildsByName(String buildName) throws Exception;

    // Get all unused builds whose publish time is before timeThreshold
    void deleteUnusedBuilds(String buildName, long timeThreshold, long numOfBuilds)
            throws Exception;

    List<BuildBean> get(
            String scmCommit,
            String buildName,
            String scmBranch,
            Optional<Integer> pageIndex,
            Optional<Integer> pageSize,
            Long before,
            Long after)
            throws Exception;

    List<BuildBean> getCurrentBuildsByGroupName(String groupName) throws Exception;
}
