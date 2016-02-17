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
package com.pinterest.arcee.dao;


import com.pinterest.arcee.bean.GroupBean;

import java.util.List;

public interface GroupInfoDAO {

    void updateGroupInfo(String groupName, GroupBean groupBean) throws Exception;

    void insertGroupInfo(GroupBean groupBean) throws Exception;

    void removeGroup(String groupName) throws Exception;

    GroupBean getGroupInfo(String groupName) throws Exception;

    // get all group information in envs_groups but not in the groups table
    List<String> getNewGroupNames() throws Exception;

    List<String> getExistingGroups(long pageIndex, int pageSize) throws Exception;

    List<String> getToUpdateGroups(long after) throws Exception;

    List<GroupBean> getGroupInfoByAppName(String appName) throws Exception;

    List<String> getGroupNamesByHostIdAndASGStatus(String hostName, String asgStatus) throws Exception;

    List<String> getGroupNamesByEnvNameAndASGStatus(String envName, String asgStatus) throws Exception;

    List<GroupBean> getGroupsByEnvNameAndASGStauts(String envName, String asgStatus) throws Exception;

    List<String> getEnabledHealthCheckGroupNames() throws Exception;
}
