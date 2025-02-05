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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.pinterest.deployservice.db.DatabaseUtil;
import java.util.Map;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.apache.commons.dbcp.BasicDataSource;

@JsonTypeName("mysql")
public class MysqlDataSourceFactory implements DataSourceFactory {

    @JsonProperty private String host;

    @Min(1)
    @Max(65535)
    @JsonProperty
    private int port = 3306;

    @JsonProperty private String userName;

    @JsonProperty private String password;

    @JsonProperty private String pool;

    @JsonProperty private Map<String, String> connectionProperties;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPool() {
        return pool;
    }

    public void setPool(String pool) {
        this.pool = pool;
    }

    public Map<String, String> getConnectionProperties() {
        return connectionProperties;
    }

    public void setConnectionProperties(Map<String, String> connectionProperties) {
        this.connectionProperties = connectionProperties;
    }

    public BasicDataSource build() throws Exception {
        return DatabaseUtil.createMysqlDataSource(
                host, port, userName, password, pool, connectionProperties);
    }
}
