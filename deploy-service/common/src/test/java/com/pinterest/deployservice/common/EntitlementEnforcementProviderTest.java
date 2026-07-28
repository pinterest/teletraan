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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pinterest.deployservice.bean.EnvironBean;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EntitlementEnforcementProviderTest {
    private static final String ENV = "example-service";
    private static final String STAGE_ONBOARDED = "prod";
    private static final String STAGE_OTHER = "canary";
    private static final String CLUSTER = ENV + "-" + STAGE_ONBOARDED;

    @TempDir File tempDir;

    /**
     * Decider JSON keyed by the configured decider key, so the test tracks whatever key is in use.
     */
    private static String deciderJson(int value) {
        return "{\"" + EntitlementEnforcementProvider.ENFORCE_DECIDER_KEY + "\": " + value + "}";
    }

    private EntitlementEnforcementProvider providerWith(String deciderJson, String onboardedJson)
            throws Exception {
        File decider = new File(tempDir, "decider");
        File onboarded = new File(tempDir, "onboarded");
        if (deciderJson != null) {
            Files.write(decider.toPath(), deciderJson.getBytes(StandardCharsets.UTF_8));
        }
        if (onboardedJson != null) {
            Files.write(onboarded.toPath(), onboardedJson.getBytes(StandardCharsets.UTF_8));
        }
        return new EntitlementEnforcementProvider(
                decider.getAbsolutePath(), onboarded.getAbsolutePath());
    }

    @Test
    void enforcedWhenFlagActiveAndOnboarded() throws Exception {
        EntitlementEnforcementProvider provider =
                providerWith(deciderJson(100), "[\"" + CLUSTER + "\"]");
        assertTrue(provider.isEnforced(CLUSTER));
    }

    @Test
    void notEnforcedWhenKillSwitchOff() throws Exception {
        EntitlementEnforcementProvider provider =
                providerWith(deciderJson(0), "[\"" + CLUSTER + "\"]");
        assertFalse(provider.isEnforced(CLUSTER));
    }

    @Test
    void notEnforcedDuringPartialRollout() throws Exception {
        EntitlementEnforcementProvider provider =
                providerWith(deciderJson(50), "[\"" + CLUSTER + "\"]");
        assertFalse(provider.isEnforced(CLUSTER));
    }

    @Test
    void notEnforcedWhenNotOnboarded() throws Exception {
        EntitlementEnforcementProvider provider =
                providerWith(deciderJson(100), "[\"other-cluster\"]");
        assertFalse(provider.isEnforced(CLUSTER));
    }

    @Test
    void notEnforcedWhenDeciderKeyMissing() throws Exception {
        EntitlementEnforcementProvider provider =
                providerWith("{\"some_other_decider\": 100}", "[\"" + CLUSTER + "\"]");
        assertFalse(provider.isEnforced(CLUSTER));
    }

    @Test
    void failsSafeWhenFilesMissing() {
        EntitlementEnforcementProvider provider =
                new EntitlementEnforcementProvider(
                        new File(tempDir, "nope-decider").getAbsolutePath(),
                        new File(tempDir, "nope-onboarded").getAbsolutePath());
        assertFalse(provider.isEnforced(CLUSTER));
    }

    @Test
    void failsSafeWhenFilesCorrupt() throws Exception {
        EntitlementEnforcementProvider provider = providerWith("not json", "also not json");
        assertFalse(provider.isEnforced(CLUSTER));
    }

    @Test
    void applyUseEntitlementsSingleOverridesBean() throws Exception {
        EntitlementEnforcementProvider provider =
                providerWith(deciderJson(100), "[\"" + CLUSTER + "\"]");

        EnvironBean enforced = new EnvironBean();
        enforced.setEnv_name(ENV);
        enforced.setStage_name(STAGE_ONBOARDED);
        enforced.setUse_entitlements(false);
        provider.applyUseEntitlements(enforced);
        assertTrue(enforced.getUse_entitlements());

        EnvironBean notOnboarded = new EnvironBean();
        notOnboarded.setEnv_name(ENV);
        notOnboarded.setStage_name(STAGE_OTHER);
        notOnboarded.setUse_entitlements(true);
        provider.applyUseEntitlements(notOnboarded);
        assertFalse(notOnboarded.getUse_entitlements());
    }

    @Test
    void applyUseEntitlementsBatchOverridesEachBean() throws Exception {
        EntitlementEnforcementProvider provider =
                providerWith(deciderJson(100), "[\"" + CLUSTER + "\"]");

        EnvironBean onboarded = new EnvironBean();
        onboarded.setEnv_name(ENV);
        onboarded.setStage_name(STAGE_ONBOARDED);
        onboarded.setUse_entitlements(false);

        EnvironBean other = new EnvironBean();
        other.setEnv_name(ENV);
        other.setStage_name(STAGE_OTHER);
        other.setUse_entitlements(true);

        provider.applyUseEntitlements(Arrays.asList(onboarded, other));
        assertTrue(onboarded.getUse_entitlements());
        assertFalse(other.getUse_entitlements());
    }

    @Test
    void applyUseEntitlementsBatchClearsAllWhenKillSwitchOff() throws Exception {
        EntitlementEnforcementProvider provider =
                providerWith(deciderJson(0), "[\"" + CLUSTER + "\"]");

        EnvironBean bean = new EnvironBean();
        bean.setEnv_name(ENV);
        bean.setStage_name(STAGE_ONBOARDED);
        bean.setUse_entitlements(true);

        provider.applyUseEntitlements(Collections.singletonList(bean));
        assertFalse(bean.getUse_entitlements());
    }

    /**
     * Overwrites the onboarded file to empty after it has been cached; within the refresh window
     * the provider must keep serving the cached (onboarded) result rather than re-reading the file.
     */
    @Test
    void onboardedListIsCachedWithinRefreshWindow() throws Exception {
        EntitlementEnforcementProvider provider =
                providerWith(deciderJson(100), "[\"" + CLUSTER + "\"]");
        assertTrue(provider.isEnforced(CLUSTER));

        write("onboarded", "[]");
        assertTrue(provider.isEnforced(CLUSTER));
    }

    /**
     * Overwrites the decider file to off after it has been cached; within the refresh window the
     * provider must keep serving the cached (enforced) result rather than re-reading the file.
     */
    @Test
    void deciderIsCachedWithinRefreshWindow() throws Exception {
        EntitlementEnforcementProvider provider =
                providerWith(deciderJson(100), "[\"" + CLUSTER + "\"]");
        assertTrue(provider.isEnforced(CLUSTER));

        write("decider", deciderJson(0));
        assertTrue(provider.isEnforced(CLUSTER));
    }

    private void write(String name, String content) throws Exception {
        Files.write(new File(tempDir, name).toPath(), content.getBytes(StandardCharsets.UTF_8));
    }
}
