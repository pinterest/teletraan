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

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class KnoxKeyReader implements KeyReader {
    private static final Logger LOG = LoggerFactory.getLogger(KnoxKeyReader.class);
    private static final String defaultKeyContent = "defaultKeyContent";

    private String knoxKeyId;
    private KnoxKeyManager knoxManager;

    private final LoadingCache<String, Optional<String>> knoxCache;

    public KnoxKeyReader() {
        knoxCache = CacheBuilder.newBuilder()
                .maximumSize(1)
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .build(
                        new CacheLoader<String, Optional<String>>() {
                            @Override
                            public Optional<String> load(String key) throws Exception {
                                return Optional.fromNullable(getKeyInternal());
                            }
                        });
    }

    void setKnoxManager(KnoxKeyManager knoxManager) {
        this.knoxManager = knoxManager;
    }

    public void init(String keyID) {
        if (knoxManager == null) {
            knoxManager = new KnoxKeyManager();
            knoxManager.init(keyID);
            knoxKeyId = keyID;
        }
    }

    public String getKey() {
        try {
            return knoxCache.get(knoxKeyId).orNull();
        } catch (Exception e) {
            LOG.warn("Using default key due to exception", e);
            return defaultKeyContent;
        }
    }

    private String getKeyInternal() {
        if (knoxManager == null) {
            LOG.warn("Using default key since knoxManager is null");
            return defaultKeyContent;
        }
        String key = knoxManager.getKey();
        if (key != null) {
            return key.trim();
        }
        return key;
    }
}
