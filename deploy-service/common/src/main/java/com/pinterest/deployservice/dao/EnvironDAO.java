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

import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.UpdateStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

/** A collection of methods to help interact with Environments tables */
public interface EnvironDAO {
    void insert(EnvironBean bean) throws Exception;

    void update(String envId, EnvironBean bean) throws Exception;

    void update(String envName, String envStage, EnvironBean bean) throws Exception;

    void updateAll(EnvironBean bean) throws Exception;

    void setExternalId(EnvironBean bean, String externalId) throws Exception;

    UpdateStatement genUpdateStatement(String envId, EnvironBean bean);

    void delete(String envId) throws Exception;

    List<String> getAllEnvNames(String nameFilter, long pageIndex, int pageSize) throws Exception;

    EnvironBean getById(String envId) throws Exception;

    EnvironBean getByDeployId(String deployId) throws SQLException;

    List<EnvironBean> getByName(String envName) throws Exception;

    EnvironBean getByStage(String envName, String envStage) throws Exception;

    EnvironBean getByCluster(String clusterName) throws Exception;

    List<String> getOverrideHosts(String envId, String envName, String envStage) throws Exception;

    long countTotalCapacity(String envId, String envName, String envStage) throws Exception;

    List<String> getTotalCapacityHosts(String envId, String envName, String envStage)
            throws Exception;

    Collection<String> getMissingHosts(String envId) throws Exception;

    List<EnvironBean> getEnvsByHost(String host) throws Exception;

    /**
     * Retrieves the main environment ID for the specified host ID.
     *
     * <p>The main environment is where the cluster that the host belongs to is created. In case
     * such an environment does not exist, the method will attempt to retrieve the environment that
     * matches the first group that's known to Teletraan for the specified host.
     *
     * @param hostId The ID of the host.
     * @return The bean represents the main environment for the specified host ID.
     * @throws SQLException if an error occurs while retrieving the main environment ID.
     */
    EnvironBean getMainEnvByHostId(String hostId) throws SQLException;

    /**
     * Retrieves the main environment ID for the specified host name.
     *
     * <p>The main environment is where the cluster that the host belongs to is created. In case
     * such an environment does not exist, the method will attempt to retrieve the environment that
     * matches the first group that's known to Teletraan for the specified host.
     *
     * @param hostName The host name.
     * @return The bean represents the main environment for the specified host name.
     * @throws SQLException if an error occurs while retrieving the main environment ID.
     */
    EnvironBean getMainEnvByHostName(String hostName) throws SQLException;

    List<EnvironBean> getEnvsByGroups(Collection<String> groups) throws Exception;

    List<String> getCurrentDeployIds() throws Exception;

    // Return all
    List<String> getAllEnvIds() throws Exception;

    List<EnvironBean> getAllEnvs() throws Exception;

    List<EnvironBean> getAllSidecarEnvs() throws Exception;

    void deleteSchedule(String envName, String stageName) throws Exception;

    void deleteCluster(String envName, String stageName) throws Exception;

    EnvironBean getEnvByDeployConstraintId(String constraintId) throws Exception;

    void deleteConstraint(String envName, String stageName) throws Exception;
}
