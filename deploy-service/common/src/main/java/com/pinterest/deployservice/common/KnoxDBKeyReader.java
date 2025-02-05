/**
 * Copyright (c) 2018-2025 Pinterest, Inc.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KnoxDBKeyReader {

    private static final Logger LOG = LoggerFactory.getLogger(KnoxDBKeyReader.class);

    // initialize to credentials in test env
    private static String testUserName = "root";
    private static String testPassword = "";
    private static KnoxKeyManager knoxManager;

    public static void init(String roleString) {
        try {
            LOG.info("Init the db key with role {}", roleString);
            if (knoxManager == null) {
                knoxManager = new KnoxKeyManager();
            }
            String key = String.format("mysql:rbac:%s:credentials", roleString);
            knoxManager.init(key);
        } catch (Exception e) {
            LOG.error("Using default credentials due to exception :" + e.getMessage());
        }
        // Note that default credentials will work in test environment but will later cause
        // another exception in prod environments due to wrong credentials.
    }

    /**
     * Get db username
     *
     * @return The db username
     */
    public static String getUserName() {
        if (knoxManager == null) {
            LOG.error("Using default username since knoxManager is null");
            return testUserName;
        }
        try {
            String userSpec;
            String mysqlCredsPriString = knoxManager.getKey();
            userSpec = mysqlCredsPriString.split("\\|")[0];
            String userName = userSpec.split("\\@")[0];
            return userName;
        } catch (Exception e) {
            LOG.error("Using default username due to exception :" + e.getMessage());
            return testUserName;
        }
    }

    /**
     * Get db password
     *
     * @return The db password
     */
    public static String getPassword() {
        if (knoxManager == null) {
            LOG.error("Using default password since knoxManager is null");
            return testPassword;
        }
        try {
            String mysqlCredsPriString = knoxManager.getKey();
            String password = mysqlCredsPriString.split("\\|")[1];
            return password;
        } catch (Exception e) {
            LOG.error("Using default password due to exception :" + e.getMessage());
            return testPassword;
        }
    }
}
