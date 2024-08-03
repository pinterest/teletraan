/**
 * Copyright (c) 2021 Pinterest, Inc.
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

import com.pinterest.deployservice.knox.CommandLineKnox;
import com.pinterest.deployservice.knox.FileSystemKnox;
import com.pinterest.deployservice.knox.Knox;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KnoxKeyManager {
    private final Logger LOG = LoggerFactory.getLogger(KnoxKeyManager.class);

    private Knox mKnox;

    private void registerKey(String key) throws Exception {
        File file = new File("/var/lib/knox/v0/keys/" + key);
        if (!file.exists()) {
            CommandLineKnox cmdKnox = new CommandLineKnox(key, Runtime.getRuntime());

            if (cmdKnox.register() != 0) {
                throw new RuntimeException("Error registering keys: " + key);
            }

            long startTime = System.currentTimeMillis();
            while (!file.exists() && System.currentTimeMillis() - startTime < 5000) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignore) {
                }
            }
        }
        mKnox = new FileSystemKnox(key);
    }

    public void init(String key) {
        try {
            LOG.info("Init the knox key with value {}", key);
            registerKey(key);
        } catch (Exception e) {
            LOG.error("Unable to register key due to exception :" + e.getMessage());
        }
    }

    public String getKey() {
        if (mKnox == null) {
            LOG.error("Returning null key since mKnox is null");
            return null;
        }
        try {
            String knoxKey = new String(mKnox.getPrimaryKey(), "UTF-8");
            return knoxKey;
        } catch (Exception e) {
            LOG.error("Returning null key due to exception :" + e.getMessage());
            return null;
        }
    }
}
