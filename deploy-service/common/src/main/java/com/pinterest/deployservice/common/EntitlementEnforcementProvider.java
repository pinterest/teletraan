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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.pinterest.deployservice.bean.EnvironBean;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Two-layer gate deciding whether a Teletraan cluster ({@code "<env>-<stage>"}) has its capacity
 * governed by entitlements. It is the source of truth for the {@code useEntitlements} state
 * surfaced to the UI (capacity fields greyed out, "View Entitlement" shown), so the deploy service
 * stays in agreement with whatever system produces and consumes the entitlement decisions.
 *
 * <ul>
 *   <li><b>Layer 1 — kill switch.</b> A decider map read from {@link #DEFAULT_DECIDER_FILE} (a JSON
 *       object of string keys to integer values) must have {@link #ENFORCE_DECIDER_KEY} at {@link
 *       #ENFORCE_VALUE}. Any lower value (including a missing/unreadable file) disables it, so the
 *       UI re-enables manual capacity edits.
 *   <li><b>Layer 2 — onboarding allowlist.</b> The cluster id must appear in the allowlist read
 *       from {@link #DEFAULT_ONBOARDED_FILE} (a JSON array of cluster ids). Only listed clusters
 *       are enforced, so a rollout can be staged cluster-by-cluster by editing the list.
 * </ul>
 *
 * <p>Both files are delivered to the host by the managed-data framework. Their parsed results are
 * cached and refreshed at most every {@link #CACHE_REFRESH_THRESHOLD_MINUTES} minutes (see {@link
 * #deciderCache} / {@link #onboardedCache}) to avoid re-reading them on every call, so a decider
 * flip or list change is picked up within that window without a redeploy. Fail-safe: any
 * missing/undelivered/corrupt input resolves to "not enforced" and never throws into the request
 * path.
 */
public class EntitlementEnforcementProvider {

    private static final Logger LOG = LoggerFactory.getLogger(EntitlementEnforcementProvider.class);

    static final int ENFORCE_VALUE = 100;
    static final int CACHE_REFRESH_THRESHOLD_MINUTES = 1;

    // Decider "entitlement_enforcement_teletraan" (Layer 1 kill switch), delivered by the
    // managed-data framework to /var/config/config.manageddata.admin.decider as a JSON map of
    // decider name -> integer value.
    static final String ENFORCE_DECIDER_KEY = "entitlement_enforcement_teletraan";
    static final String DEFAULT_DECIDER_FILE = "/var/config/config.manageddata.admin.decider";
    // Managed-data list "entitlements/onboarded_teletraan" (domain=entitlements,
    // key=onboarded_teletraan), delivered to /var/config/config.manageddata.<domain>.<key>
    // as a JSON array of onboarded clusterIds.
    static final String DEFAULT_ONBOARDED_FILE =
            "/var/config/config.manageddata.entitlements.onboarded_teletraan";

    private final String deciderFilePath;
    private final String onboardedFilePath;
    private final ObjectMapper objectMapper;
    private final LoadingCache<String, Boolean> deciderCache;
    private final LoadingCache<String, Set<String>> onboardedCache;

    public EntitlementEnforcementProvider() {
        this(DEFAULT_DECIDER_FILE, DEFAULT_ONBOARDED_FILE);
    }

    public EntitlementEnforcementProvider(String deciderFilePath, String onboardedFilePath) {
        this.deciderFilePath = deciderFilePath;
        this.onboardedFilePath = onboardedFilePath;
        this.objectMapper = new ObjectMapper();
        this.deciderCache =
                Caffeine.newBuilder()
                        .refreshAfterWrite(CACHE_REFRESH_THRESHOLD_MINUTES, TimeUnit.MINUTES)
                        .maximumSize(1)
                        .build(this::isEnforcementDeciderActive);
        this.onboardedCache =
                Caffeine.newBuilder()
                        .refreshAfterWrite(CACHE_REFRESH_THRESHOLD_MINUTES, TimeUnit.MINUTES)
                        .maximumSize(1)
                        .build(this::onboardedClusters);
    }

    /**
     * The effective {@code useEntitlements} state for {@code clusterId} ({@code "<env>-<stage>"}).
     * Requires both layers: the kill-switch flag active and the cluster onboarded.
     */
    public boolean isEnforced(String clusterId) {
        if (!deciderCache.get(ENFORCE_DECIDER_KEY)) {
            return false;
        }
        return onboardedCache.get(onboardedFilePath).contains(clusterId);
    }

    /**
     * Override {@code env.useEntitlements} with the live gate value so the UI reflects the current
     * kill switch + rollout config rather than the static DB column. No-op for {@code null}.
     */
    public void applyUseEntitlements(EnvironBean env) {
        if (env == null) {
            return;
        }
        env.setUse_entitlements(isEnforced(clusterId(env)));
    }

    /**
     * Batch variant of {@link #applyUseEntitlements(EnvironBean)} that reads the decider map and
     * onboarding list once for the whole collection.
     */
    public void applyUseEntitlements(Collection<EnvironBean> envs) {
        if (envs == null || envs.isEmpty()) {
            return;
        }
        Set<String> onboarded =
                deciderCache.get(ENFORCE_DECIDER_KEY)
                        ? onboardedCache.get(onboardedFilePath)
                        : Collections.emptySet();
        for (EnvironBean env : envs) {
            if (env != null) {
                env.setUse_entitlements(onboarded.contains(clusterId(env)));
            }
        }
    }

    /** Entitlement scope key for a Teletraan env/stage: {@code "<env>-<stage>"}. */
    private static String clusterId(EnvironBean env) {
        return env.getEnv_name() + "-" + env.getStage_name();
    }

    /**
     * Layer 1. True only when the kill-switch decider is at {@link #ENFORCE_VALUE}. A
     * missing/unreadable value resolves to false.
     */
    boolean isEnforcementDeciderActive(String deciderKey) {
        Integer value = deciderMap().getOrDefault(deciderKey, 0);
        return value != null && value >= ENFORCE_VALUE;
    }

    private Map<String, Integer> deciderMap() {
        File file = new File(deciderFilePath);
        if (!file.exists()) {
            LOG.info("Entitlement decider file does not exist: {}", deciderFilePath);
            return Collections.emptyMap();
        }
        try (FileInputStream in = new FileInputStream(file)) {
            return objectMapper.readValue(in, new TypeReference<Map<String, Integer>>() {});
        } catch (IOException e) {
            LOG.error("Failed to read entitlement decider map from file: {}", deciderFilePath, e);
            return Collections.emptyMap();
        }
    }

    /** Layer 2. The set of onboarded Teletraan clusterIds. Missing/unreadable -> empty set. */
    Set<String> onboardedClusters(String onboardedFilePath) {
        File file = new File(onboardedFilePath);
        if (!file.exists()) {
            LOG.info("Entitlement onboarding list does not exist: {}", onboardedFilePath);
            return Collections.emptySet();
        }
        try (FileInputStream in = new FileInputStream(file)) {
            List<String> clusters =
                    objectMapper.readValue(in, new TypeReference<List<String>>() {});
            return new HashSet<>(clusters);
        } catch (IOException e) {
            LOG.error(
                    "Failed to read entitlement onboarding list from file: {}",
                    onboardedFilePath,
                    e);
            return Collections.emptySet();
        }
    }
}
