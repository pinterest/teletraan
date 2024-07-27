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
package com.pinterest.deployservice.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import org.apache.commons.io.FileUtils;

/**
 * Parse and return DB config info from /var/config/config.services.general_mysql_databases_config &
 * /var/config/config.services.mysql_auth
 *
 * <p>Sample format:
 *
 * <p>{ "scriptrw": { "users": [ { "enabled": true, "password": "12345", "username": "scriptrw" }, {
 * "enabled": false, "password": "67890", "username": "scriptrw2" } ] }, .... { "datadb001": {
 * "master": { "host": "datadb001a", "port": 3306 }, "slave": { "host": "datadb001f", "port": 3306 }
 * }, ...
 */
public class DBConfigReader {
    private static final String MYSQL_GEN_ZK =
            "/var/config/config.services.general_mysql_databases_config";
    private static final String AUTH_FILE = "/var/config/config.services.mysql_auth";

    private String credJson;
    private String dbJson;

    public DBConfigReader() throws Exception {
        dbJson = FileUtils.readFileToString(new File(MYSQL_GEN_ZK));
        credJson = FileUtils.readFileToString(new File(AUTH_FILE));
    }

    DBConfigReader(String dbJson, String credJson) {
        this.dbJson = dbJson;
        this.credJson = credJson;
    }

    public String getHost(String replicaSetName) {
        JsonNode master = read(dbJson, replicaSetName);
        return master.findPath("host").textValue();
    }

    public Integer getPort(String replicaSetName) {
        JsonNode master = read(dbJson, replicaSetName);
        return master.findPath("port").intValue();
    }

    public String getUsername(String role) {
        JsonNode cred = readEnabled(credJson, role);
        return cred.findPath("username").textValue();
    }

    public String getPassword(String role) {
        JsonNode cred = readEnabled(credJson, role);
        return cred.get("password").textValue();
    }

    private JsonNode readEnabled(String jsonInput, String role) {
        JsonNode cred = read(jsonInput, role);
        for (JsonNode node : cred.findPath("users")) {
            if (node.get("enabled").booleanValue()) {
                return node;
            }
        }
        throw new RuntimeException("Missing enabled user");
    }

    private JsonNode read(String jsonInput, String path) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = null;
        try {
            actualObj = mapper.readTree(jsonInput);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return actualObj.get(path);
    }
}
