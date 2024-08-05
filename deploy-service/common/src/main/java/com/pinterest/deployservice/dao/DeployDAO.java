/**
 * Copyright (c) 2016-2024 Pinterest, Inc.
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
import com.pinterest.deployservice.bean.DeployQueryResultBean;
import com.pinterest.deployservice.bean.UpdateStatement;
import com.pinterest.deployservice.db.DeployQueryFilter;
import java.sql.SQLException;
import java.util.List;
import org.joda.time.Interval;

/** A collection of methods to help read table DEPLOYS */
public interface DeployDAO {
    DeployBean getById(String deployId) throws Exception;

    DeployQueryResultBean getAllDeploys(DeployQueryFilter filterBean) throws Exception;

    void delete(String deployId) throws Exception;

    void update(String deployId, DeployBean deployBean) throws Exception;

    UpdateStatement genUpdateStatement(String deployId, DeployBean deployBean);

    UpdateStatement genInsertStatement(DeployBean deployBean);

    void insert(DeployBean deployBean) throws Exception;

    // Return upto size number of ACCEPTED deploy whose build publish time is after after
    List<DeployBean> getAcceptedDeploys(String envId, Interval interval, int size) throws Exception;

    // Return ACCEPTED deploy whose suc_date is before before and build publish time is after after
    List<DeployBean> getAcceptedDeploysDelayed(String envId, Interval interval) throws Exception;

    // Return upto size number of ACCEPTED deploy whose suc_date is before before and build publish
    // time is after after
    Long countNonRegularDeploys(String envId, long after) throws Exception;

    // Update state, and other colums, if only if state == currentState
    // Return affected rows, 0 means not updated
    int updateStateSafely(String deployId, String currentState, DeployBean updateBean)
            throws Exception;

    // Count total number of deploys by env_id
    long countDeploysByEnvId(String envId) throws Exception;

    // Delete all unused deploys whose last update time is before timeThreshold
    void deleteUnusedDeploys(String envId, long timeThreshold, long numOfDeploys) throws Exception;

    boolean isThereADeployWithBuildId(String buildId) throws Exception;

    long getDailyDeployCount() throws SQLException;

    long getRunningDeployCount() throws SQLException;
}
