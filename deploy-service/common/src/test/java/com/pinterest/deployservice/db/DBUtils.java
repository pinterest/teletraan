/**
 * Copyright (c) 2024 Pinterest, Inc.
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

import com.ibatis.common.jdbc.ScriptRunner;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.commons.dbcp.BasicDataSource;
import org.testcontainers.containers.MySQLContainer;

public class DBUtils {
    private static final String MYSQL_IMAGE_NAME = "mysql:8.0-oracle";
    private static final String MYSQL_URL = "jdbc:mysql://0.0.0.0:3303/deploy?useSSL=false";
    public static String DATABASE_NAME = "deploy";
    public static String DATABASE_USER = "root";
    public static String DATABASE_PASSWORD = "";
    private static MySQLContainer container;
    private static BasicDataSource dataSource;

    public static BasicDataSource createTestDataSource() throws IOException, SQLException {
        if (dataSource == null) {
            setUpDataSource();
        }
        return dataSource;
    }

    public static void truncateAllTables(BasicDataSource dataSource) throws Exception {
        try (Connection conn = dataSource.getConnection();
                Statement query = conn.createStatement();
                Statement stmt = conn.createStatement(); ) {
            ResultSet rs =
                    query.executeQuery(
                            "SELECT table_name FROM information_schema.tables WHERE table_schema = SCHEMA()");
            stmt.addBatch("SET FOREIGN_KEY_CHECKS=0");
            while (rs.next()) {
                String sqlStatement = String.format("TRUNCATE `%s`", rs.getString(1));
                stmt.addBatch(sqlStatement);
            }
            stmt.addBatch("SET FOREIGN_KEY_CHECKS=1");
            stmt.executeBatch();
        }
    }

    private static MySQLContainer getContainer() {
        if (container == null) {
            container =
                    new MySQLContainer(MYSQL_IMAGE_NAME)
                            .withDatabaseName(DATABASE_NAME)
                            .withUsername(DATABASE_USER)
                            .withPassword(DATABASE_PASSWORD);
            container.start();
        }
        return container;
    }

    private static void runMigrations(BasicDataSource dataSource) throws IOException, SQLException {
        Connection conn = dataSource.getConnection();
        ScriptRunner runner = new ScriptRunner(conn, false, true);
        runner.runScript(
                new BufferedReader(
                        new InputStreamReader(
                                DBUtils.class.getResourceAsStream("/sql/cleanup.sql"))));
        runner.runScript(
                new BufferedReader(
                        new InputStreamReader(
                                DBUtils.class.getResourceAsStream("/sql/deploy.sql"))));
        conn.prepareStatement("SET sql_mode=(SELECT REPLACE(@@sql_mode,'ONLY_FULL_GROUP_BY',''));")
                .execute();

        int version = 7;
        int executed = 0;
        while (true) {
            String scriptName = String.format("/sql/schema-update-%d.sql", version);
            try {
                runner.runScript(
                        new InputStreamReader(DBUtils.class.getResourceAsStream(scriptName)));
            } catch (Exception e) {
                if (executed == 0) {
                    throw new RuntimeException(
                            "Could not run a single update script. Check starting version is correct");
                }
                break;
            }
            version++;
            executed++;
        }
        conn.close();
    }

    private static void setUpDataSource() throws IOException, SQLException {
        boolean userLocalMySQLInstance =
                Boolean.parseBoolean(System.getenv("USE_LOCAL_MYSQL_INSTANCE"));
        if (userLocalMySQLInstance) {
            dataSource = DatabaseUtil.createLocalDataSource(MYSQL_URL);
        } else {
            dataSource = DatabaseUtil.createLocalDataSource(getContainer().getJdbcUrl());
        }
        runMigrations(dataSource);
    }
}
