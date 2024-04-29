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
package com.pinterest.deployservice.common;

import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Map;

/**
 * Parse and return DB config info from
 * /var/config/config.services.general_mysql_databases_config & /var/config/config.services.mysql_auth
 * <p/>
 * Sample format:
 * <p/>
 * {
 * "scriptrw": {
 * "users": [
 * {
 * "enabled": true,
 * "password": "12345",
 * "username": "scriptrw"
 * },
 * {
 * "enabled": false,
 * "password": "67890",
 * "username": "scriptrw2"
 * }
 * ]
 * },
 * ....
 * {
 * "datadb001": {
 * "master": {
 * "host": "datadb001a",
 * "port": 3306
 * },
 * "slave": {
 * "host": "datadb001f",
 * "port": 3306
 * }
 * },
 * ...
 */
public class DBConfigReader {
    private final static String MYSQL_GEN_ZK = "/var/config/config.services.general_mysql_databases_config";
    private final static String AUTH_FILE = "/var/config/config.services.mysql_auth";

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
        return JsonPath.read(dbJson, String.format("$.%s.master.host", replicaSetName));
    }

    public Integer getPort(String replicaSetName) {
        return JsonPath.read(dbJson, String.format("$.%s.master.port", replicaSetName));
    }

    public String getUsername(String role) {
        JSONArray jsonArray = JsonPath.read(credJson, String.format("$.%s.users[?(@.enabled == true)]", role));
        if(jsonArray.size()>0){
            Map hashMap =  (Map) jsonArray.get(0);
            return hashMap.get("username").toString();
        }

        return null;
    }

    public String getPassword(String role) {
        JSONArray jsonArray = JsonPath.read(credJson, String.format("$.%s.users[?(@.enabled == true)]", role));
        if(jsonArray.size()>0){
            Map hashMap =  (Map) jsonArray.get(0);
            return hashMap.get("password").toString();
        }

        return null;
    }
}
