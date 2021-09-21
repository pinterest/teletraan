/**
 * Copyright 2021 Pinterest, Inc.
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


public class KnoxKeyReader implements KeyReader {

    private static final Logger LOG = LoggerFactory.getLogger(KnoxKeyReader.class);

    private String testKey = "key";

    private KnoxKeyManager knoxManager;

    public void init(String key) {
        if (knoxManager == null) {
            knoxManager = new KnoxKeyManager();
            knoxManager.init(key);
        }
    }

    public String getKey() {
        if (knoxManager == null) {
            LOG.error("Using default key since knoxManager is null");
            return testKey;
        }
        try {
            String knoxKey = knoxManager.getKey();
            return knoxKey;
        } catch (Exception e) {
            LOG.error("Using default key due to exception :" + e.getMessage());
            return testKey;
        }
    }
}
