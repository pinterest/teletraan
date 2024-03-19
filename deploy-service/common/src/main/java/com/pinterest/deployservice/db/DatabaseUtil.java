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
package com.pinterest.deployservice.db;

import com.pinterest.deployservice.bean.UpdateStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility class to insertOrUpdate MySQL datasource. */
public class DatabaseUtil {

    public static final int MAX_WAIT_TIME_FOR_CONN_IN_MS = 200;
    public static final String MYSQL_JDBC_DRIVER = "com.mysql.jdbc.Driver";
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseUtil.class);

    public static BasicDataSource createMysqlDataSource(
            String host, int port, String user, String passwd, String poolSize) {
        // autoReconnect by default is false
        // TODO: queryTimeoutKillsConnection to true, "?queryTimeoutKillsConnection=true"
        String url =
                String.format(
                        "jdbc:mysql://%s:%d/deploy?connectTimeout=5000&socketTimeout=3000&characterEncoding=UTF-8"
                                + "&connectionCollation=utf8mb4_general_ci",
                        host, port);
        LOG.info("mtls is disabled --- host:{}, port:{}, user:{}", host, port, user);
        return createDataSource(
                MYSQL_JDBC_DRIVER, url, user, passwd, poolSize, MAX_WAIT_TIME_FOR_CONN_IN_MS);
    }

    public static BasicDataSource createMysqlDataSource(
            String host,
            int port,
            String user,
            String passwd,
            String poolSize,
            Map<String, String> connectionProperties) {
        // autoReconnect by default is false
        // TODO: queryTimeoutKillsConnection to true, "?queryTimeoutKillsConnection=true"
        String url =
                String.format(
                        "jdbc:mysql://%s:%d/deploy?connectTimeout=5000&socketTimeout=3000&characterEncoding=UTF-8"
                                + "&connectionCollation=utf8mb4_general_ci",
                        host, port);
        LOG.info("mtls is enabled --- host:{}, port:{}, user:{}", host, port, user);
        return createDataSource(
                MYSQL_JDBC_DRIVER,
                url,
                user,
                passwd,
                poolSize,
                MAX_WAIT_TIME_FOR_CONN_IN_MS,
                connectionProperties);
    }

    // Embedded mysql source, for unit test only
    public static BasicDataSource createLocalDataSource(String url) {
        return createDataSource(
                MYSQL_JDBC_DRIVER, url, "root", "", "0:8:8:0", MAX_WAIT_TIME_FOR_CONN_IN_MS, null);
    }

    /**
     * Create a MySQL datasource.
     *
     * @param url the url of the DB.
     * @param user the user name to connect to MySQL as.
     * @param passwd the password for the corresponding MySQL user.
     * @param poolSize the connection pool size string, in the format of
     *     initialSize:maxActive:maxIdle:minIdle.
     * @param maxWaitInMillis the max wait time in milliseconds to get a connection from the pool.
     * @return a BasicDataSource for the target MySQL instance.
     */
    public static BasicDataSource createDataSource(
            String driverClassName,
            String url,
            String user,
            String passwd,
            String poolSize,
            int maxWaitInMillis) {
        return createDataSource(
                driverClassName, url, user, passwd, poolSize, maxWaitInMillis, null);
    }

    public static BasicDataSource createDataSource(
            String driverClassName,
            String url,
            String user,
            String passwd,
            String poolSize,
            int maxWaitInMillis,
            Map<String, String> connectionProperties) {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setUrl(url);
        dataSource.setUsername(user);
        dataSource.setPassword(passwd);
        dataSource.setDefaultAutoCommit(true);
        dataSource.setDefaultReadOnly(false);

        // poolSize parsing, the poolsize string passed in the following format
        // initialSize:maxActive:maxIdle:minIdle
        String[] sizeStrs = poolSize.split(":");
        dataSource.setInitialSize(Integer.parseInt(sizeStrs[0]));
        dataSource.setMaxActive(Integer.parseInt(sizeStrs[1]));
        dataSource.setMaxIdle(Integer.parseInt(sizeStrs[2]));
        dataSource.setMinIdle(Integer.parseInt(sizeStrs[3]));

        dataSource.setValidationQuery("SELECT 1");
        dataSource.setTestOnBorrow(true);
        dataSource.setTestOnReturn(false);
        dataSource.setTestWhileIdle(true);
        dataSource.setMinEvictableIdleTimeMillis(5 * 60 * 1000);
        dataSource.setTimeBetweenEvictionRunsMillis(3 * 60 * 1000);
        // dataSource.setNumTestsPerEvictionRun(3);
        // max wait in milliseconds for a connection.
        dataSource.setMaxWait(maxWaitInMillis);

        if (connectionProperties != null) {
            for (Map.Entry<String, String> entry : connectionProperties.entrySet()) {
                LOG.info(
                        String.format(
                                "Add connection properties %s=%s",
                                entry.getKey(), entry.getValue()));
                dataSource.addConnectionProperty(entry.getKey(), entry.getValue());
            }
        }

        // force connection pool initialization.
        Connection conn = null;
        try {
            // Here not getting the connection from ThreadLocal no need to worry about that.
            conn = dataSource.getConnection();
            LOG.info("mysql conn: {}", conn);
        } catch (SQLException e) {
            LOG.error(
                    String.format(
                            "Failed to get a db connection when creating DataSource, url = %s",
                            url),
                    e);
        } finally {
            DbUtils.closeQuietly(conn);
        }
        LOG.info("mysql dataSource: {}", dataSource);
        return dataSource;
    }

    public static void transactionalUpdate(
            BasicDataSource dataSource, List<UpdateStatement> updateStatements) throws Exception {
        QueryRunner queryRunner = new QueryRunner();
        Connection connection = dataSource.getConnection();
        boolean autoStatus = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            for (UpdateStatement updateStatement : updateStatements) {
                queryRunner.update(
                        connection,
                        updateStatement.getStatement(),
                        updateStatement.getValueArray());
            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(autoStatus);
            DbUtils.closeQuietly(connection);
        }
    }
}
