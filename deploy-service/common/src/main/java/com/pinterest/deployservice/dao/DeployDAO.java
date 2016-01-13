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

import com.pinterest.deployservice.bean.DeployBean;
import com.pinterest.deployservice.db.DeployQueryFilter;
import com.pinterest.deployservice.bean.DeployQueryResultBean;
import com.pinterest.deployservice.bean.UpdateStatement;

import java.util.List;

/**
 * A collection of methods to help read table DEPLOYS
 */
public interface DeployDAO {
    public DeployBean getById(String deployId) throws Exception;

    public DeployQueryResultBean getAllDeploys(DeployQueryFilter filterBean) throws Exception;

    public void delete(String deployId) throws Exception;

    public void update(String deployId, DeployBean deployBean) throws Exception;

    public UpdateStatement genUpdateStatement(String deployId, DeployBean deployBean);

    public UpdateStatement genInsertStatement(DeployBean deployBean);

    public void insert(DeployBean deployBean) throws Exception;

    // Return upto size number of ACCEPTED deploy whose build publish time is after after
    public List<DeployBean> getAcceptedDeploys(String envId, long after, int size) throws Exception;

    // Return upto size number of ACCEPTED deploy whose suc_date is before before and build publish time is after after
    public List<DeployBean> getAcceptedDeploysDelayed(String envId, long before, long after) throws Exception;

    // Return upto size number of ACCEPTED deploy whose suc_date is before before and build publish time is after after
    public Long countNonRegularDeploys(String envId, long after) throws Exception;

    // Update state, and other colums, if only if state == currentState
    // Return affected rows, 0 means not updated
    public int updateStateSafely(String deployId, String currentState, DeployBean updateBean) throws Exception;

    // Count total number of deploys by env_id
    public long countDeploysByEnvId(String envId) throws Exception;

    // Delete all unused deploys whose last update time is before timeThreshold
    public void deleteUnusedDeploys(String envId, long timeThreshold, long numOfDeploys) throws Exception;
}
