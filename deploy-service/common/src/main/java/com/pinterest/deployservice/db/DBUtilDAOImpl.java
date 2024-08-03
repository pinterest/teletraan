/**
 * Copyright (c) 2016-2022 Pinterest, Inc.
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

import com.pinterest.deployservice.common.CommonUtils;
import com.pinterest.deployservice.dao.UtilDAO;
import java.sql.Connection;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBUtilDAOImpl implements UtilDAO {
    private static final Logger LOG = LoggerFactory.getLogger(DBUtilDAOImpl.class);
    private static final String GET_LOCK_TEMPLATE = "SELECT GET_LOCK('%s', %d)";
    private static final String RELEASE_LOCK_TEMPLATE = "SELECT RELEASE_LOCK('%s')";
    // In sec, do not set it more then 10 sec, server will kill query takes more than 10 sec
    // Smaller timeout makes sense, we do not want server to be occupied by such requests
    // 0 means return immediately
    private static final int LOCK_TIMEOUT = 0;

    private BasicDataSource dataSource;

    public DBUtilDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    private String ensureValidLockName(String lockName) {
        String lockNameSha = CommonUtils.getShaHex(lockName.getBytes());
        LOG.debug("Converted lock name {} to {}", lockName, lockNameSha);
        return lockNameSha;
    }

    @Override
    public Connection getLock(String id) {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            long status =
                    new QueryRunner()
                            .query(
                                    connection,
                                    String.format(
                                            GET_LOCK_TEMPLATE,
                                            ensureValidLockName(id),
                                            LOCK_TIMEOUT),
                                    SingleResultSetHandlerFactory.<Long>newObjectHandler());
            if (status == 1L) {
                return connection;
            }
        } catch (Exception e) {
            LOG.error("Failed to call getLock on id {}.", id, e);
        }
        DbUtils.closeQuietly(connection);
        return null;
    }

    @Override
    public void releaseLock(String id, Connection connection) {
        try {
            new QueryRunner()
                    .query(
                            connection,
                            String.format(RELEASE_LOCK_TEMPLATE, ensureValidLockName(id)),
                            SingleResultSetHandlerFactory.<Long>newObjectHandler());
        } catch (Exception e) {
            LOG.error("Failed to call releaseLock on id {}.", id, e);
        }
        DbUtils.closeQuietly(connection);
    }
}
