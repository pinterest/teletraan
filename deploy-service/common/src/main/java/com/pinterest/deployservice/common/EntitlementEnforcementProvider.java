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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Two-layer gate deciding whether a Teletraan cluster ({@code "<env>-<stage>"}) has its capacity
 * governed by entitlements. It is the source of truth for the {@code useEntitlements} state
 * surfaced to the UI (capacity fields greyed out, "View Entitlement" shown), so the deploy service
 * stays in agreement with whatever system produces and consumes the entitlement decisions.
 *
 * <ul>
 *   <li><b>Layer 1 — kill switch.</b> A feature-flag map read from {@link #DEFAULT_FLAG_FILE} (a
 *       JSON object of string keys to integer values) must have {@link #ENFORCE_FLAG_KEY} at {@link
 *       #ENFORCE_VALUE}. Any lower value (including a missing/unreadable file) disables it, so the
 *       UI re-enables manual capacity edits.
 *   <li><b>Layer 2 — onboarding allowlist.</b> The cluster id must appear in the allowlist read from
 *       {@link #DEFAULT_ONBOARDED_FILE} (a JSON array of cluster ids). Only listed clusters are
 *       enforced, so a rollout can be staged cluster-by-cluster by editing the list.
 * </ul>
 *
 * <p>Both files are expected to be delivered to the host by the deployment's configuration-delivery
 * system; their paths and the flag key are overridable via the environment (the {@code
 * TELETRAAN_ENTITLEMENT_*} variables) or the constructor, so the public defaults stay generic.
 * Fail-safe: any missing/undelivered/corrupt input resolves to "not enforced" and never throws into
 * the request path. The files are re-read on each call so a flag flip or list change is picked up
 * without a redeploy.
 */
public class EntitlementEnforcementProvider {

    private static final Logger LOG =
            LoggerFactory.getLogger(EntitlementEnforcementProvider.class);

    static final int ENFORCE_VALUE = 100;

    // Deployment-specific identifiers default to generic values and can be overridden via the
    // environment, so a deployment points these at its own config-delivery paths and flag key
    // without baking them into source.
    static final String ENFORCE_FLAG_KEY =
            envOrDefault("TELETRAAN_ENTITLEMENT_FLAG_KEY", "entitlement_enforcement");
    static final String DEFAULT_FLAG_FILE =
            envOrDefault("TELETRAAN_ENTITLEMENT_FLAG_FILE", "/var/config/entitlement_flags.json");
    static final String DEFAULT_ONBOARDED_FILE =
            envOrDefault(
                    "TELETRAAN_ENTITLEMENT_ONBOARDED_FILE",
                    "/var/config/entitlement_onboarded.json");

    private final String flagFilePath;
    private final String onboardedFilePath;
    private final ObjectMapper objectMapper;

    public EntitlementEnforcementProvider() {
        this(DEFAULT_FLAG_FILE, DEFAULT_ONBOARDED_FILE);
    }

    public EntitlementEnforcementProvider(String flagFilePath, String onboardedFilePath) {
        this.flagFilePath = flagFilePath;
        this.onboardedFilePath = onboardedFilePath;
        this.objectMapper = new ObjectMapper();
    }

    private static String envOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return (value == null || value.isEmpty()) ? fallback : value;
    }

    /**
     * The effective {@code useEntitlements} state for {@code clusterId} ({@code "<env>-<stage>"}).
     * Requires both layers: the kill-switch flag active and the cluster onboarded.
     */
    public boolean isEnforced(String clusterId) {
        if (!isEnforcementFlagActive()) {
            return false;
        }
        return onboardedClusters().contains(clusterId);
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
     * Batch variant of {@link #applyUseEntitlements(EnvironBean)} that reads the flag map and
     * onboarding list once for the whole collection.
     */
    public void applyUseEntitlements(Collection<EnvironBean> envs) {
        if (envs == null || envs.isEmpty()) {
            return;
        }
        Set<String> onboarded =
                isEnforcementFlagActive() ? onboardedClusters() : Collections.emptySet();
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
     * Layer 1. True only when the kill-switch flag is at {@link #ENFORCE_VALUE}. A
     * missing/unreadable value resolves to false.
     */
    boolean isEnforcementFlagActive() {
        Integer value = flagMap().getOrDefault(ENFORCE_FLAG_KEY, 0);
        return value != null && value >= ENFORCE_VALUE;
    }

    private Map<String, Integer> flagMap() {
        File file = new File(flagFilePath);
        if (!file.exists()) {
            LOG.info("Entitlement flag file does not exist: {}", flagFilePath);
            return Collections.emptyMap();
        }
        try (FileInputStream in = new FileInputStream(file)) {
            return objectMapper.readValue(in, new TypeReference<Map<String, Integer>>() {});
        } catch (IOException e) {
            LOG.error("Failed to read entitlement flag map from file: {}", flagFilePath, e);
            return Collections.emptyMap();
        }
    }

    /** Layer 2. The set of onboarded Teletraan clusterIds. Missing/unreadable -> empty set. */
    Set<String> onboardedClusters() {
        File file = new File(onboardedFilePath);
        if (!file.exists()) {
            LOG.info("Entitlement onboarding list does not exist: {}", onboardedFilePath);
            return Collections.emptySet();
        }
        try (FileInputStream in = new FileInputStream(file)) {
            List<String> clusters = objectMapper.readValue(in, new TypeReference<List<String>>() {});
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
