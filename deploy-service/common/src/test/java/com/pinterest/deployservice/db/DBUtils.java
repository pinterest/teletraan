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
import java.io.InputStreamReader;
import java.sql.Connection;
import org.apache.commons.dbcp.BasicDataSource;
import org.testcontainers.containers.MySQLContainer;

public class DBUtils {

    public static String DATABASE_NAME = "deploy";
    public static String DATABASE_USER = "root";
    public static String DATABASE_PASSWORD = "";

    public static MySQLContainer getContainer() {
        return new MySQLContainer<>()
                .withDatabaseName(DATABASE_NAME)
                .withUsername(DATABASE_USER)
                .withPassword(DATABASE_PASSWORD);
    }

    public static void runMigrations(BasicDataSource dataSource) throws Exception {
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
        conn.close();
    }
}
