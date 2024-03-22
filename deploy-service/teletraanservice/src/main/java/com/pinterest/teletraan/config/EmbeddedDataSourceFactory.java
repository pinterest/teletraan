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
package com.pinterest.teletraan.config;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.commons.dbcp.BasicDataSource;

/*
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pinterest.deployservice.db.DatabaseUtil;
import org.apache.commons.dbcp.BasicDataSource;

import com.ibatis.common.jdbc.ScriptRunner;
import com.mysql.management.driverlaunched.ServerLauncherSocketFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.sql.Connection;
*/

@JsonTypeName("embedded")
public class EmbeddedDataSourceFactory implements DataSourceFactory {
    /*
    *
    * Uncomment this block and the imports if you want to use embedded mysql
    *
    private final static String DEFAULT_BASE_DIR = "/tmp/teletraan/db";
    private final static String DEFAULT_DB_NAME = "deploy";
    private final static int DEFAULT_PORT = 3305;

    @JsonProperty
    private String workDir = DEFAULT_BASE_DIR;

    public String getWorkDir() {
        return workDir;
    }

    public void setWorkDir(String workDir) {
        this.workDir = workDir;
    }

    public BasicDataSource build() throws Exception {
        try {
            // making sure we do not have anything running
            ServerLauncherSocketFactory.shutdown(new File(workDir), null);
        } catch (Exception e) {
            // ignore
        }

        BasicDataSource
                DATASOURCE =
                DatabaseUtil.createMXJDataSource(DEFAULT_DB_NAME, workDir, DEFAULT_PORT);
        Connection conn = DATASOURCE.getConnection();
        ScriptRunner runner = new ScriptRunner(conn, false, false);
        runner.runScript(new BufferedReader(new InputStreamReader(
                DatabaseUtil.class.getResourceAsStream("/sql/deploy.sql"))));
        return DATASOURCE;
    }
    */
    public BasicDataSource build() {
        return new BasicDataSource();
    }
}
