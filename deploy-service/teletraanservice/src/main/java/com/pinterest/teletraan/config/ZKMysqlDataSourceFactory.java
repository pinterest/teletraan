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
package com.pinterest.teletraan.config;

import com.pinterest.deployservice.common.DBConfigReader;
import com.pinterest.deployservice.common.KnoxDBKeyReader;
import com.pinterest.deployservice.db.DatabaseUtil;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.commons.dbcp.BasicDataSource;
import org.hibernate.validator.constraints.NotEmpty;

@JsonTypeName("zkmysql")
public class ZKMysqlDataSourceFactory implements DataSourceFactory {
    @NotEmpty
    @JsonProperty
    private String replicaSet;

    @NotEmpty
    @JsonProperty
    private String role;

    @JsonProperty
    private String pool;

    @JsonProperty
    private boolean useProxy;

    @JsonProperty
    private String proxyHost;

    @JsonProperty
    private int proxyPort;

    public String getReplicaSet() {
        return replicaSet;
    }

    public void setReplicaSet(String replicaSet) {
        this.replicaSet = replicaSet;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPool() {
        return pool;
    }

    public void setPool(String pool) {
        this.pool = pool;
    }

    public BasicDataSource build() throws Exception {
        // use proxyHost:proxyPort if useProxy for database connection
        // otherwise utilize replicaSet
        String host;
        int port;
        if (this.useProxy) {
            host = this.proxyHost;
            port = this.proxyPort;
        } else {
            // get from local ZK maintained DB config file
            DBConfigReader reader = new DBConfigReader();
            host = reader.getHost(replicaSet);
            port = reader.getPort(replicaSet);
        }
        KnoxDBKeyReader.init((role));
        String userName = KnoxDBKeyReader.getUserName();
        String password = KnoxDBKeyReader.getPassword();
        return DatabaseUtil.createMysqlDataSource(host, port, userName, password, pool);
    }
}
