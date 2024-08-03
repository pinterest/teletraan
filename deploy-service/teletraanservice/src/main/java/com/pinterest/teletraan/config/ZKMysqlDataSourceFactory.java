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
import com.google.common.collect.ImmutableMap;
import com.pinterest.deployservice.common.DBConfigReader;
import com.pinterest.deployservice.common.KnoxDBKeyReader;
import com.pinterest.deployservice.db.DatabaseUtil;
import java.util.Map;
import javax.validation.constraints.NotEmpty;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("zkmysql")
public class ZKMysqlDataSourceFactory implements DataSourceFactory {
    @NotEmpty @JsonProperty private String replicaSet;

    @NotEmpty @JsonProperty private String role;

    @JsonProperty private String pool;

    @JsonProperty private boolean useProxy;

    @JsonProperty private String proxyHost;

    @JsonProperty private int proxyPort;

    @JsonProperty private boolean useMTLS;

    @JsonProperty private String spiffePrefix;

    @JsonProperty private String domainSuffix;

    @JsonProperty private String defaultMtlsPasswd;

    @JsonProperty private String trustUrl;

    @JsonProperty private String trustType;

    @JsonProperty private String trustPasswd;

    @JsonProperty private String clientUrl;

    @JsonProperty private String clientType;

    @JsonProperty private String clientPasswd;

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
        String replicaSetNumber = "001";
        if (this.useProxy) {
            host = this.proxyHost;
            port = this.proxyPort;
        } else {
            // get from local ZK maintained DB config file
            DBConfigReader reader = new DBConfigReader();
            host = reader.getHost(replicaSet);
            port = reader.getPort(replicaSet);
        }

        if (this.useMTLS) {
            String password = this.defaultMtlsPasswd;
            Map<String, String> proxyConnectionProps =
                    ImmutableMap.<String, String>builder()
                            // ssl properties
                            .put("sslMode", "PREFERRED")
                            .put("trustCertificateKeyStoreUrl", this.trustUrl)
                            .put("trustCertificateKeyStoreType", this.trustType)
                            .put("trustCertificateKeyStorePassword", this.trustPasswd)
                            .put("clientCertificateKeyStoreUrl", this.clientUrl)
                            .put("clientCertificateKeyStoreType", this.clientType)
                            .put("clientCertificateKeyStorePassword", this.clientPasswd)
                            .build();
            host = this.replicaSet;
            // we don't need the replica number in the host;
            // if in the configuration we input the number in the replica, we have to remove it.
            if (host.length() > 3) {
                replicaSetNumber = replicaSet.substring(replicaSet.length() - 3);
                if (StringUtils.isNumeric(replicaSetNumber)) {
                    host = replicaSet.substring(0, replicaSet.length() - 3);
                }
            } else {
                throw new Exception(
                        String.format(
                                "ReplicaSet is: %s which is not correct. It should be the replicaset name and replicaset number.",
                                host));
            }
            host += this.domainSuffix;
            String userName = getUserNameFromSpiffeId(replicaSetNumber);
            return DatabaseUtil.createMysqlDataSource(
                    host, port, userName, password, pool, proxyConnectionProps);
        } else {
            KnoxDBKeyReader.init((role));
            String userName = KnoxDBKeyReader.getUserName();
            String password = KnoxDBKeyReader.getPassword();
            return DatabaseUtil.createMysqlDataSource(host, port, userName, password, pool);
        }
    }

    /**
     * For mysql 8 and mtls migration we are generating usernames from spiffe's md5 hash and replica
     * number
     */
    public String getUserNameFromSpiffeId(String databaseReplicaNumber) {
        String md5Hex =
                DigestUtils.md5Hex(
                        this.spiffePrefix
                                + System.getenv("ENV_NAME")
                                + "/"
                                + System.getenv("STAGE_NAME"));
        String spiffeHash = md5Hex.substring(0, 25);
        return spiffeHash + databaseReplicaNumber + "_rw";
    }
}
