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
package com.pinterest.deployservice.db;

import com.pinterest.deployservice.dao.GroupDAO;
import java.util.List;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;

public class DBGroupDAOImpl implements GroupDAO {
    private static final String GET_ALL_NON_EMPTY_GROUPS_NAMES =
            "SELECT DISTINCT group_name FROM groups_and_envs";
    private static final String GET_ENV_BY_GROUP =
            "SELECT DISTINCT env_id FROM groups_and_envs WHERE group_name=?";
    private static final String GET_HOSTS = "SELECT * FROM hosts_and_envs WHERE env_id=?";
    private static final String INSERT_HOST = "INSERT INTO hosts_and_envs SET host_name=?,env_id=?";
    private static final String DELETE_HOST =
            "DELETE FROM hosts_and_envs WHERE host_name=? AND env_id=?";
    private static final String GET_GROUPS = "SELECT * FROM groups_and_envs WHERE env_id=?";
    private static final String INSERT_GROUP =
            "INSERT INTO groups_and_envs SET group_name=?,env_id=?";
    private static final String DELETE_GROUP =
            "DELETE FROM groups_and_envs WHERE group_name=? AND env_id=?";

    private BasicDataSource dataSource;

    public DBGroupDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<String> getAllEnvGroups() throws Exception {
        return new QueryRunner(dataSource)
                .query(
                        GET_ALL_NON_EMPTY_GROUPS_NAMES,
                        SingleResultSetHandlerFactory.<String>newListObjectHandler());
    }

    @Override
    public List<String> getEnvsByGroupName(String groupName) throws Exception {
        return new QueryRunner(dataSource)
                .query(
                        GET_ENV_BY_GROUP,
                        SingleResultSetHandlerFactory.<String>newListObjectHandler(),
                        groupName);
    }

    @Override
    public List<String> getCapacityHosts(String envId) throws Exception {
        return new QueryRunner(dataSource)
                .query(
                        GET_HOSTS,
                        SingleResultSetHandlerFactory.<String>newListObjectHandler(),
                        envId);
    }

    @Override
    public void addHostCapacity(String envId, String host) throws Exception {
        new QueryRunner(dataSource).update(INSERT_HOST, host, envId);
    }

    @Override
    public void removeHostCapacity(String envId, String host) throws Exception {
        new QueryRunner(dataSource).update(DELETE_HOST, host, envId);
    }

    @Override
    public List<String> getCapacityGroups(String envId) throws Exception {
        return new QueryRunner(dataSource)
                .query(
                        GET_GROUPS,
                        SingleResultSetHandlerFactory.<String>newListObjectHandler(),
                        envId);
    }

    @Override
    public void addGroupCapacity(String envId, String group) throws Exception {
        new QueryRunner(dataSource).update(INSERT_GROUP, group, envId);
    }

    @Override
    public void removeGroupCapacity(String envId, String group) throws Exception {
        new QueryRunner(dataSource).update(DELETE_GROUP, group, envId);
    }
}
